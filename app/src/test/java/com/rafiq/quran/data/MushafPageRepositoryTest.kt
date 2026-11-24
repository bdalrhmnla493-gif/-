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

    private fun completeMetadata(
        finalPage: MushafPageBoundary = MushafPageBoundary(604, 112, 1),
        finalJuz: MushafJuzBoundary = MushafJuzBoundary(30, 78, 1),
        page21: MushafPageBoundary = MushafPageBoundary(21, 2, 135),
        page22: MushafPageBoundary = MushafPageBoundary(22, 2, 142)
    ) = MushafPageMetadata(
        pages = (1..604).map { page ->
            when (page) {
                1 -> MushafPageBoundary(1, 1, 1)
                21 -> page21
                22 -> page22
                604 -> finalPage
                in 2..20 -> MushafPageBoundary(page, 2, page)
                else -> MushafPageBoundary(page, 2, page + 120)
            }
        },
        juz = (1..30).map { index ->
            when (index) {
                1 -> MushafJuzBoundary(1, 1, 1)
                30 -> finalJuz
                else -> MushafJuzBoundary(index, 2, index)
            }
        }
    )

    @Test
    fun acceptsOnlyComplete604PageAnd30JuzMetadata() {
        MushafPageRepository.validateCompleteMushaf(completeMetadata())
    }

    @Test
    fun validatesKnownPage21AndPage22StartingAyahs() {
        val metadata = completeMetadata()
        assertEquals(MushafPageBoundary(21, 2, 135), metadata.pages[20])
        assertEquals(MushafPageBoundary(22, 2, 142), metadata.pages[21])
        MushafPageRepository.validateCompleteMushaf(metadata)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncorrectPage21StartingAyah() {
        MushafPageRepository.validateCompleteMushaf(
            completeMetadata(page21 = MushafPageBoundary(21, 2, 134))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncorrectPage22StartingAyah() {
        MushafPageRepository.validateCompleteMushaf(
            completeMetadata(page22 = MushafPageBoundary(22, 2, 143))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncorrectFinalMushafPageBoundary() {
        MushafPageRepository.validateCompleteMushaf(
            completeMetadata(finalPage = MushafPageBoundary(604, 114, 1))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncorrectFinalJuzBoundary() {
        MushafPageRepository.validateCompleteMushaf(
            completeMetadata(finalJuz = MushafJuzBoundary(30, 77, 1))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncompleteMushafMetadata() {
        val incomplete = MushafPageMetadata(
            pages = listOf(MushafPageBoundary(1, 1, 1)),
            juz = listOf(MushafJuzBoundary(1, 1, 1))
        )

        MushafPageRepository.validateCompleteMushaf(incomplete)
    }

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
