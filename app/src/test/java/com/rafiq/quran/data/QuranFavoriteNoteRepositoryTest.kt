package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranFavoriteNoteRepositoryTest {
    private fun note(
        surah: Int = 2,
        ayah: Int = 255,
        text: String = "أراجع تفسير هذه الآية",
        color: String = "#B99A60",
        updatedAt: Long = 1_700_000_000_000L
    ) = QuranFavoriteNote(surah, ayah, text, color, updatedAt)

    @Test
    fun upsertAndDecodePreserveArabicNoteAndColor() {
        val decoded = QuranFavoriteNoteRepository.decode(
            QuranFavoriteNoteRepository.upsert(emptySet(), note())
        )
        assertEquals(listOf(note()), decoded)
    }

    @Test
    fun upsertReplacesExistingNoteForSameAyah() {
        val first = note(text = "ملاحظة أولى")
        val second = note(text = "ملاحظة معدلة", updatedAt = 20L)
        val saved = QuranFavoriteNoteRepository.upsert(
            QuranFavoriteNoteRepository.upsert(emptySet(), first),
            second
        )
        assertEquals(listOf(second), QuranFavoriteNoteRepository.decode(saved))
    }

    @Test
    fun removeDeletesOnlyTheSelectedAyahNote() {
        val first = note()
        val second = note(surah = 1, ayah = 1, text = "الفاتحة")
        val saved = QuranFavoriteNoteRepository.upsert(
            QuranFavoriteNoteRepository.upsert(emptySet(), first),
            second
        )
        val remaining = QuranFavoriteNoteRepository.decode(
            QuranFavoriteNoteRepository.remove(saved, 2, 255)
        )
        assertEquals(listOf(second), remaining)
    }

    @Test
    fun decodeIgnoresMalformedRecords() {
        val saved = QuranFavoriteNoteRepository.upsert(setOf("not-json"), note())
        val decoded = QuranFavoriteNoteRepository.decode(saved)
        assertEquals(1, decoded.size)
        assertTrue(decoded.single().colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun upsertRejectsInvalidSurah() {
        QuranFavoriteNoteRepository.upsert(emptySet(), note(surah = 115))
    }

    @Test(expected = IllegalArgumentException::class)
    fun upsertRejectsBlankNote() {
        QuranFavoriteNoteRepository.upsert(emptySet(), note(text = "  "))
    }
}
