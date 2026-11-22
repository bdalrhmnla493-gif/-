package com.rafiq.quran.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val hijriDate: String?,
    val timezone: String,
    val methodName: String
)

data class CurrentWeather(
    val temperatureCelsius: Double,
    val weatherCode: Int
) {
    val description: String
        get() = PrayerWeatherRepository.weatherDescription(weatherCode)
}

object PrayerWeatherRepository {
    suspend fun fetchPrayerTimes(latitude: Double, longitude: Double): PrayerTimes =
        withContext(Dispatchers.IO) {
            require(EgyptLocationsRepository.isValidCoordinates(latitude, longitude))
            val date = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
            val address = "https://api.aladhan.com/v1/timings/$date" +
                "?latitude=$latitude&longitude=$longitude&method=5&school=0"
            val json = request(address)
            parsePrayerTimes(json)
        }

    suspend fun fetchCurrentWeather(latitude: Double, longitude: Double): CurrentWeather =
        withContext(Dispatchers.IO) {
            require(EgyptLocationsRepository.isValidCoordinates(latitude, longitude))
            val address = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code&temperature_unit=celsius&timezone=auto"
            val json = request(address)
            parseCurrentWeather(json)
        }

    internal fun parsePrayerTimes(content: String): PrayerTimes {
        val root = JSONObject(content)
        if (root.optInt("code", -1) != 200) {
            throw IllegalStateException("خدمة مواقيت الصلاة لم تُرجع بيانات صالحة.")
        }
        val data = root.getJSONObject("data")
        val timings = data.getJSONObject("timings")
        val meta = data.optJSONObject("meta")
        val hijri = data.optJSONObject("date")?.optJSONObject("hijri")
        val hijriMonth = hijri?.optJSONObject("month")?.optString("ar").orEmpty()
        val hijriDate = if (hijri != null && hijriMonth.isNotBlank()) {
            listOf(hijri.optString("day"), hijriMonth, hijri.optString("year"))
                .filter { it.isNotBlank() }
                .joinToString(" ")
        } else {
            null
        }

        return PrayerTimes(
            fajr = requireTime(timings, "Fajr"),
            sunrise = requireTime(timings, "Sunrise"),
            dhuhr = requireTime(timings, "Dhuhr"),
            asr = requireTime(timings, "Asr"),
            maghrib = requireTime(timings, "Maghrib"),
            isha = requireTime(timings, "Isha"),
            hijriDate = hijriDate,
            timezone = meta?.optString("timezone")?.takeIf { !it.isNullOrBlank() } ?: "Africa/Cairo",
            methodName = meta?.optJSONObject("method")?.optString("name")
                ?.takeIf { !it.isNullOrBlank() } ?: "الهيئة المصرية العامة للمساحة"
        )
    }

    internal fun parseCurrentWeather(content: String): CurrentWeather {
        val root = JSONObject(content)
        val current = root.getJSONObject("current")
        val temperature = current.getDouble("temperature_2m")
        val code = current.getInt("weather_code")
        require(temperature.isFinite() && temperature in -100.0..70.0)
        require(code in 0..99)
        return CurrentWeather(temperature, code)
    }

    internal fun normalizeTime(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("^\\s*(\\d{1,2}):(\\d{2})").find(raw) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    internal fun weatherDescription(code: Int): String = when (code) {
        0 -> "سماء صافية"
        1 -> "غائم جزئيًا"
        2 -> "غائم جزئيًا"
        3 -> "غائم"
        45, 48 -> "شبورة أو ضباب"
        in 51..57 -> "رذاذ"
        in 61..67 -> "أمطار"
        in 71..77 -> "ثلوج"
        in 80..82 -> "زخات مطر"
        in 85..86 -> "زخات ثلج"
        in 95..99 -> "عواصف رعدية"
        else -> "حالة الطقس غير محددة"
    }

    private fun requireTime(timings: JSONObject, key: String): String =
        normalizeTime(timings.optString(key)) ?: throw IllegalStateException("وقت $key غير متاح.")

    private fun request(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (status !in 200..299 || body.isBlank()) {
                throw IllegalStateException("تعذر الاتصال بخدمة البيانات. حاول مرة أخرى عند توفر الإنترنت.")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }
}
