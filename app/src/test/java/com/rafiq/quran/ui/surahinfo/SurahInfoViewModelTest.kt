package com.rafiq.quran.ui.surahinfo

import com.rafiq.quran.data.MushafJuzBoundary
import com.rafiq.quran.data.MushafPageBoundary
import com.rafiq.quran.data.MushafPageMetadata
import com.rafiq.quran.data.QuranAyah
import com.rafiq.quran.data.QuranSurah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SurahInfoViewModelTest {
    private val pageMetadata = MushafPageMetadata(
        pages = listOf(
            MushafPageBoundary(page = 1, surah = 1, ayah = 1),
            MushafPageBoundary(page = 187, surah = 9, ayah = 1),
            MushafPageBoundary(page = 604, surah = 112, ayah = 1)
        ),
        juz = listOf(
            MushafJuzBoundary(index = 1, surah = 1, ayah = 1),
            MushafJuzBoundary(index = 10, surah = 8, ayah = 41),
            MushafJuzBoundary(index = 30, surah = 78, ayah = 1)
        )
    )

    @Test
    fun loadsAlFatihahAsMakkiOnPageOneAndJuzOne() {
        val info = loadInfo(
            surahNumber = 1,
            name = "الفاتحة",
            ayahCount = 7,
            revelationType = "Meccan"
        )

        assertEquals(1, info.number)
        assertEquals("الفاتحة", info.name)
        assertEquals(7, info.ayahCount)
        assertTrue(info.isMakki)
        assertEquals(1, info.startPage)
        assertEquals(1, info.startJuz)
        assertOptionalDetailsAreAbsent(info)
    }

    @Test
    fun loadsAtTawbahAsMedinanOnPage187AndJuzTen() {
        val info = loadInfo(
            surahNumber = 9,
            name = "التوبة",
            ayahCount = 129,
            revelationType = "Medinan"
        )

        assertEquals(9, info.number)
        assertEquals("التوبة", info.name)
        assertEquals(129, info.ayahCount)
        assertEquals(false, info.isMakki)
        assertEquals(187, info.startPage)
        assertEquals(10, info.startJuz)
        assertOptionalDetailsAreAbsent(info)
    }

    @Test
    fun loadsAnNasAsMakkiOnPage604AndJuzThirty() {
        val info = loadInfo(
            surahNumber = 114,
            name = "الناس",
            ayahCount = 6,
            revelationType = "Meccan"
        )

        assertEquals(114, info.number)
        assertEquals("الناس", info.name)
        assertEquals(6, info.ayahCount)
        assertTrue(info.isMakki)
        assertEquals(604, info.startPage)
        assertEquals(30, info.startJuz)
        assertOptionalDetailsAreAbsent(info)
    }

    @Test
    fun remainsLoadingUntilPageMetadataIsAvailable() {
        val viewModel = SurahInfoViewModel()
        val surah = sampleSurah(1, "الفاتحة", 7, "Meccan")

        assertTrue(viewModel.state is SurahInfoState.Loading)

        viewModel.load(surah = surah, pageMetadata = null)
        assertTrue(viewModel.state is SurahInfoState.Loading)

        viewModel.load(surah = surah, pageMetadata = pageMetadata)
        assertTrue(viewModel.state is SurahInfoState.Ready)
    }

    @Test
    fun reportsErrorForUnknownRevelationTypeInsteadOfGuessing() {
        val viewModel = SurahInfoViewModel()

        viewModel.load(
            surah = sampleSurah(1, "الفاتحة", 7, "Unknown"),
            pageMetadata = pageMetadata
        )

        val state = viewModel.state
        assertTrue(state is SurahInfoState.Error)
        assertEquals(
            "نوع نزول السورة غير متاح في بيانات القرآن.",
            (state as SurahInfoState.Error).message
        )
    }

    private fun loadInfo(
        surahNumber: Int,
        name: String,
        ayahCount: Int,
        revelationType: String
    ): SurahInfo {
        val viewModel = SurahInfoViewModel()
        viewModel.load(
            surah = sampleSurah(surahNumber, name, ayahCount, revelationType),
            pageMetadata = pageMetadata
        )

        val state = viewModel.state
        assertTrue("Expected Ready state but was $state", state is SurahInfoState.Ready)
        return (state as SurahInfoState.Ready).info
    }

    private fun sampleSurah(
        number: Int,
        name: String,
        ayahCount: Int,
        revelationType: String
    ) = QuranSurah(
        number = number,
        name = name,
        englishName = "Test Surah",
        transliteration = "Test",
        revelationType = revelationType,
        ayahs = (1..ayahCount).map { ayahNumber ->
            QuranAyah(number = ayahNumber, text = "")
        }
    )

    private fun assertOptionalDetailsAreAbsent(info: SurahInfo) {
        assertNull(info.revelationOrder)
        assertNull(info.nameReason)
        assertNull(info.summary)
        assertNull(info.sourceName)
    }
}
