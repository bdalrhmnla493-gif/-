package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MushafBookmarkRepositoryTest {
    @Test
    fun toggleAddsAndRemovesPageBookmark() {
        val added = MushafBookmarkRepository.toggle(emptySet(), 23)
        assertEquals(setOf("23"), added)

        val removed = MushafBookmarkRepository.toggle(added, 23)
        assertEquals(emptySet<String>(), removed)
    }

    @Test
    fun decodeIgnoresCorruptAndOutOfRangePageNumbers() {
        val decoded = MushafBookmarkRepository.decode(
            setOf("1", "604", "0", "605", "not-a-page")
        )
        assertEquals(setOf(1, 604), decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toggleRejectsInvalidPageNumbers() {
        MushafBookmarkRepository.toggle(emptySet(), 605)
    }
}
