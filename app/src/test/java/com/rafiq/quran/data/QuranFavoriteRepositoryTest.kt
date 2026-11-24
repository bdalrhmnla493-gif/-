package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranFavoriteRepositoryTest {
    @Test
    fun toggleAddsAndRemovesFavoriteAyah() {
        val saved = QuranFavoriteRepository.toggle(emptySet(), 2, 255)
        assertEquals(setOf("2:255"), saved)

        val removed = QuranFavoriteRepository.toggle(saved, 2, 255)
        assertEquals(emptySet<String>(), removed)
    }

    @Test
    fun decodeIgnoresMalformedAndOutOfRangeReferences() {
        val decoded = QuranFavoriteRepository.decode(
            setOf("1:1", "114:6", "0:1", "2:0", "abc:3", "2:bad", "2:1:extra")
        )
        assertEquals(
            setOf(QuranAyahReference(1, 1), QuranAyahReference(114, 6)),
            decoded
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun toggleRejectsInvalidSurahNumber() {
        QuranFavoriteRepository.toggle(emptySet(), 115, 1)
    }
}
