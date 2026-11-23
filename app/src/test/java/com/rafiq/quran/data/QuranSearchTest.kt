package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranSearchTest {
    @Test
    fun normalizationIgnoresTashkeelAndNormalizesAlefVariants() {
        assertEquals(
            "الله الرحمن الرحيم",
            QuranSearch.normalizeArabic("ٱللَّهُ الرَّحْمَٰنِ الرَّحِيمِ")
        )
        assertEquals("انا", QuranSearch.normalizeArabic("أَنَا"))
    }

    @Test
    fun searchFindsAyatAcrossSurahsAndPreservesOriginalText() {
        val first = QuranSurah(
            number = 1,
            name = "الفاتحة",
            englishName = "The Opening",
            transliteration = "Al-Fatihah",
            revelationType = "مكية",
            ayahs = listOf(QuranAyah(1, "بِسْمِ ٱللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"))
        )
        val second = QuranSurah(
            number = 2,
            name = "البقرة",
            englishName = "The Cow",
            transliteration = "Al-Baqarah",
            revelationType = "مدنية",
            ayahs = listOf(QuranAyah(2, "وَإِلَٰهُكُمْ إِلَٰهٌ وَاحِدٌ"))
        )

        val results = QuranSearch.search(listOf(first, second), "الله الرحمن")

        assertEquals(1, results.size)
        assertEquals(1, results.single().surahNumber)
        assertEquals("الفاتحة", results.single().surahName)
        assertEquals(1, results.single().ayahNumber)
        assertEquals("بِسْمِ ٱللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", results.single().text)
    }

    @Test
    fun blankQueryReturnsNoResultsAndLimitIsRespected() {
        val surah = QuranSurah(
            number = 1,
            name = "اختبار",
            englishName = "Test",
            transliteration = "Test",
            revelationType = "مكية",
            ayahs = listOf(
                QuranAyah(1, "اللَّهُ أَحَدٌ"),
                QuranAyah(2, "اللَّهُ الصَّمَدُ")
            )
        )

        assertTrue(QuranSearch.search(listOf(surah), "   ").isEmpty())
        assertEquals(1, QuranSearch.search(listOf(surah), "الله", limit = 1).size)
    }
}
