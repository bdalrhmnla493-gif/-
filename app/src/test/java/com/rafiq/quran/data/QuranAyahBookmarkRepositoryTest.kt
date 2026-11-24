package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranAyahBookmarkRepositoryTest {
    private fun bookmark(
        id: String = "bookmark-1",
        surah: Int = 2,
        ayah: Int = 255,
        label: String = "آية الكرسي",
        color: String = "#B99A60",
        createdAt: Long = 1_700_000_000_000L
    ) = QuranAyahBookmark(id, surah, ayah, label, color, createdAt)

    @Test
    fun addAndDecodePreservesArabicLabelAndMetadata() {
        val saved = QuranAyahBookmarkRepository.add(emptySet(), bookmark())
        val decoded = QuranAyahBookmarkRepository.decode(saved)

        assertEquals(1, decoded.size)
        assertEquals(bookmark(), decoded.single())
    }

    @Test
    fun addWithExistingIdUpdatesLabelAndColorWithoutChangingCreatedAt() {
        val original = bookmark()
        val updated = original.copy(label = "اسم معدل", colorHex = "#2E7D5B")
        val saved = QuranAyahBookmarkRepository.add(
            QuranAyahBookmarkRepository.add(emptySet(), original),
            updated
        )

        assertEquals(listOf(updated), QuranAyahBookmarkRepository.decode(saved))
    }

    @Test
    fun removeDeletesOnlyTheRequestedBookmark() {
        val first = bookmark()
        val second = bookmark(id = "bookmark-2", surah = 1, ayah = 1, label = "الفاتحة")
        val saved = QuranAyahBookmarkRepository.add(
            QuranAyahBookmarkRepository.add(emptySet(), first),
            second
        )

        val remaining = QuranAyahBookmarkRepository.decode(
            QuranAyahBookmarkRepository.remove(saved, first.id)
        )

        assertEquals(listOf(second), remaining)
    }

    @Test
    fun decodeIgnoresMalformedRecordsAndSortsByCreationTime() {
        val older = bookmark(id = "older", createdAt = 10)
        val newer = bookmark(id = "newer", createdAt = 20)
        val saved = QuranAyahBookmarkRepository.add(
            QuranAyahBookmarkRepository.add(setOf("not-json"), newer),
            older
        )

        val decoded = QuranAyahBookmarkRepository.decode(saved)
        assertEquals(listOf(older, newer), decoded)
        assertTrue(decoded.all { it.colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$")) })
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRejectsOutOfRangeSurah() {
        QuranAyahBookmarkRepository.add(emptySet(), bookmark(surah = 115))
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRejectsInvalidColor() {
        QuranAyahBookmarkRepository.add(emptySet(), bookmark(color = "gold"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRejectsBlankLabel() {
        QuranAyahBookmarkRepository.add(emptySet(), bookmark(label = "  "))
    }
}
