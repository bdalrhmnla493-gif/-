package com.rafiq.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class QuranAyah(
    val number: Int,
    val text: String
)

data class QuranSurah(
    val number: Int,
    val name: String,
    val englishName: String,
    val transliteration: String,
    val revelationType: String,
    val ayahs: List<QuranAyah>
)

internal data class ParsedQuranLine(
    val surahNumber: Int,
    val ayahNumber: Int,
    val text: String
)

object QuranRepository {
    internal fun validateSurahNumberOrder(surahNumbers: List<Int>) {
        require(surahNumbers == (1..114).toList()) {
            "ترتيب بيانات السور يجب أن يضم السور من 1 إلى 114 دون نقص أو تكرار."
        }
    }

    internal fun parseAyahLine(line: String): ParsedQuranLine? {
        val parts = line.split('|', limit = 3)
        if (parts.size != 3) return null

        val surahNumber = parts[0].toIntOrNull() ?: return null
        val ayahNumber = parts[1].toIntOrNull() ?: return null
        val ayahText = parts[2]

        if (surahNumber !in 1..114 || ayahNumber < 1 || ayahText.isBlank()) {
            return null
        }

        return ParsedQuranLine(surahNumber, ayahNumber, ayahText)
    }

    suspend fun load(context: Context): List<QuranSurah> = withContext(Dispatchers.IO) {
        val metadataText = context.assets.open("surah-metadata.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val metadata = JSONObject(metadataText).getJSONArray("surahs")
        val metadataSurahNumbers = (0 until metadata.length()).map { index ->
            metadata.getJSONObject(index).getInt("number")
        }
        validateSurahNumberOrder(metadataSurahNumbers)

        val ayahsBySurah = Array(115) { mutableListOf<QuranAyah>() }
        context.assets.open("quran-uthmani.txt")
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines ->
                lines.forEach { line ->
                    val parsed = parseAyahLine(line)
                    if (parsed != null) {
                        ayahsBySurah[parsed.surahNumber].add(
                            QuranAyah(parsed.ayahNumber, parsed.text)
                        )
                    }
                }
            }

        val result = ArrayList<QuranSurah>(114)
        for (index in 0 until metadata.length()) {
            val item = metadata.getJSONObject(index)
            val number = item.getInt("number")
            val expectedAyahCount = item.getInt("ayahCount")
            val ayahs = ayahsBySurah[number].sortedBy { it.number }

            if (ayahs.size != expectedAyahCount) {
                throw IllegalStateException("عدد آيات السورة " + number + " لا يطابق بيانات المصحف.")
            }
            for (indexAyah in ayahs.indices) {
                if (ayahs[indexAyah].number != indexAyah + 1) {
                    throw IllegalStateException("ترقيم آيات السورة " + number + " غير متصل.")
                }
            }
            result.add(
                QuranSurah(
                    number = number,
                    name = item.getString("name"),
                    englishName = item.getString("englishName"),
                    transliteration = item.getString("transliteration"),
                    revelationType = item.getString("revelationType"),
                    ayahs = ayahs
                )
            )
        }

        if (result.size != 114 || result.sumOf { it.ayahs.size } != 6236) {
            throw IllegalStateException("بيانات المصحف غير مكتملة.")
        }
        result
    }
}
