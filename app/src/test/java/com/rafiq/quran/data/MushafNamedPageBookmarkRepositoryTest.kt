package com.rafiq.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafNamedPageBookmarkRepositoryTest {
    private fun bookmark(
        id: String = "page-bookmark-1",
        page: Int = 23,
        label: String = "مراجعة الصفحة",
        color: String = "#B99A60",
        createdAt: Long = 1_700_000_000_000L
    ) = MushafNamedPageBookmark(id, page, label, color, createdAt)

    @Test
    fun addAndDecodePreserveLabelColorAndDate() {
        val decoded = MushafNamedPageBookmarkRepository.decode(
            MushafNamedPageBookmarkRepository.add(emptySet(), bookmark())
        )
        assertEquals(listOf(bookmark()), decoded)
    }

    @Test
    fun addReplacesExistingBookmarkForSamePage() {
        val original = bookmark()
        val updated = original.copy(label = "اسم جديد", colorHex = "#2E7D5B")
        val saved = MushafNamedPageBookmarkRepository.add(
            MushafNamedPageBookmarkRepository.add(emptySet(), original),
            updated
        )
        assertEquals(listOf(updated), MushafNamedPageBookmarkRepository.decode(saved))
    }

    @Test
    fun removePageKeepsOtherPages() {
        val first = bookmark()
        val second = bookmark(id = "page-bookmark-2", page = 24, label = "صفحة أخرى")
        val saved = MushafNamedPageBookmarkRepository.add(
            MushafNamedPageBookmarkRepository.add(emptySet(), first),
            second
        )
        val remaining = MushafNamedPageBookmarkRepository.decode(
            MushafNamedPageBookmarkRepository.removePage(saved, 23)
        )
        assertEquals(listOf(second), remaining)
    }

    @Test
    fun decodeIgnoresMalformedRecords() {
        val decoded = MushafNamedPageBookmarkRepository.decode(setOf("not-json"))
        assertTrue(decoded.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRejectsOutOfRangePage() {
        MushafNamedPageBookmarkRepository.add(emptySet(), bookmark(page = 605))
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRejectsBlankLabel() {
        MushafNamedPageBookmarkRepository.add(emptySet(), bookmark(label = " "))
    }
}
