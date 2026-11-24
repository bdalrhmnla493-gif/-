package com.rafiq.quran.data

/**
 * Validates and updates page-number bookmarks stored in Preferences DataStore.
 * Only real Madinah Mushaf page numbers are kept.
 */
object MushafBookmarkRepository {
    private const val FIRST_PAGE = 1
    private const val LAST_PAGE = 604

    fun decode(bookmarkedPageKeys: Set<String>): Set<Int> =
        bookmarkedPageKeys
            .mapNotNull { it.toIntOrNull()?.takeIf { page -> page in FIRST_PAGE..LAST_PAGE } }
            .toSortedSet()

    fun toggle(bookmarkedPageKeys: Set<String>, pageNumber: Int): Set<String> {
        require(pageNumber in FIRST_PAGE..LAST_PAGE) {
            "Bookmark page must be between 1 and 604."
        }
        val current = decode(bookmarkedPageKeys).toMutableSet()
        if (!current.add(pageNumber)) current.remove(pageNumber)
        return current.map { it.toString() }.toSet()
    }
}
