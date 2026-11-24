package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafPageRepositoryTest {
    private val metadataJson = """
        {
          "source": "Tanzil Quran Metadata",
          "license": "Creative Commons Attribution 3.0",
          "pages": [
            {"page": 1, "surah": 1, "ayah": 1},
            {"page": 2, "surah": 1, "ayah": 4},
            {"page": 3, "surah": 2, "ayah": 1}
          ],
          "juz": [
            {"index": 1, "surah": 1, "ayah": 1},
            {"index": 2, "surah": 2, "ayah": 1}
          ]
        }
    """.trimIndent()

    @Test
    fun parsesContiguousMushafPagesAndJuz() {
        val parsed = MushafPageRepository.parse(metadataJson)
        assertEquals(3, parsed.pages.size)
        assertEquals(2, parsed.juz.size)
        assertEquals(2, parsed.pages[1].page)
        assertEquals(2, parsed.juz[1].index)
    }

    @Test
    fun findsCorrectPageAndJuzForAnAyah() {
        val parsed = MushafPageRepository.parse(metadataJson)
        assertEquals(1, MushafPageRepository.findPageNumber(parsed.pages, 1, 3))
        assertEquals(2, MushafPageRepository.findPageNumber(parsed.pages, 1, 6))
        assertEquals(3, MushafPageRepository.findPageNumber(parsed.pages, 2, 1))
        assertEquals(1, MushafPageRepository.findJuzNumber(parsed.juz, 1, 7))
        assertEquals(2, MushafPageRepository.findJuzNumber(parsed.juz, 2, 1))
    }

    @Test
    fun pageContainsAyahsUntilTheNextPageBoundary() {
        val parsed = MushafPageRepository.parse(metadataJson)
        val surahs = listOf(
            QuranSurah(
                number = 1,
                name = "الفاتحة",
                englishName = "The Opening",
                transliteration = "Al-Fatihah",
                revelationType = "Meccan",
                ayahs = (1..7).map { QuranAyah(it, "آية $it") }
            ),
            QuranSurah(
                number = 2,
                name = "البقرة",
                englishName = "The Cow",
                transliteration = "Al-Baqarah",
                revelationType = "Medinan",
                ayahs = listOf(QuranAyah(1, "الم"))
            )
        )

        val result = MushafPageRepository.getPageAyahs(surahs, parsed.pages, 2)
        assertEquals(listOf(4, 5, 6, 7), result.map { it.ayahNumber })
        assertTrue(result.all { it.surahNumber == 1 })
    }
}
