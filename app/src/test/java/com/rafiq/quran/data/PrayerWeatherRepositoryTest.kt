package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerWeatherRepositoryTest {
    @Test
    fun normalizeTimeAcceptsApiTimesWithTimezoneSuffix() {
        assertEquals("05:12", PrayerWeatherRepository.normalizeTime("05:12 (EET)"))
        assertEquals("14:07", PrayerWeatherRepository.normalizeTime("14:07"))
        assertEquals("00:03", PrayerWeatherRepository.normalizeTime("0:03"))
    }

    @Test
    fun normalizeTimeRejectsInvalidValues() {
        assertNull(PrayerWeatherRepository.normalizeTime(null))
        assertNull(PrayerWeatherRepository.normalizeTime(""))
        assertNull(PrayerWeatherRepository.normalizeTime("24:00"))
        assertNull(PrayerWeatherRepository.normalizeTime("12:60"))
        assertNull(PrayerWeatherRepository.normalizeTime("not-a-time"))
    }

    @Test
    fun weatherCodesMapToSafeArabicDescriptions() {
        assertEquals("سماء صافية", PrayerWeatherRepository.weatherDescription(0))
        assertEquals("غائم", PrayerWeatherRepository.weatherDescription(3))
        assertEquals("أمطار", PrayerWeatherRepository.weatherDescription(63))
        assertEquals("عواصف رعدية", PrayerWeatherRepository.weatherDescription(95))
        assertEquals("حالة الطقس غير محددة", PrayerWeatherRepository.weatherDescription(100))
    }
}
