package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranRepositoryTest {
    @Test
    fun acceptsCompleteSurahMetadataInMushafOrder() {
        QuranRepository.validateSurahNumberOrder((1..114).toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingSurahMetadata() {
        QuranRepository.validateSurahNumberOrder((1..113).toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicatedOrOutOfOrderSurahMetadata() {
        QuranRepository.validateSurahNumberOrder((1..113).toList() + 113)
    }

    @Test
    fun parsesAyahLineAndPreservesArabicText() {
        val parsed = QuranRepository.parseAyahLine("1|1|بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ")
        assertEquals(1, parsed?.surahNumber)
        assertEquals(1, parsed?.ayahNumber)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", parsed?.text)
    }

    @Test
    fun skipsHeadersAndMalformedLines() {
        assertNull(QuranRepository.parseAyahLine("# Tanzil copyright notice"))
        assertNull(QuranRepository.parseAyahLine("not|an|ayah"))
    }

    @Test
    fun rejectsSurahOutsideQuranRange() {
        assertNull(QuranRepository.parseAyahLine("115|1|نص"))
        assertNull(QuranRepository.parseAyahLine("0|1|نص"))
    }
}
