package com.rafiq.quran.data

import org.json.JSONObject

data class QuranAyahBookmark(
    val id: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val label: String,
    val colorHex: String,
    val createdAtMillis: Long
)

/**
 * Stores user-created ayah bookmarks separately from the last-read position,
 * page bookmarks, and the favorite-ayah list.
 */
object QuranAyahBookmarkRepository {
    private val colorPattern = Regex("^#[0-9A-Fa-f]{6}$")

    fun decode(records: Set<String>): List<QuranAyahBookmark> =
        records.mapNotNull { raw ->
            runCatching {
                val json = JSONObject(raw)
                QuranAyahBookmark(
                    id = json.getString("id"),
                    surahNumber = json.getInt("surah"),
                    ayahNumber = json.getInt("ayah"),
                    label = json.getString("label"),
                    colorHex = json.getString("color"),
                    createdAtMillis = json.getLong("createdAt")
                ).takeIf(::isValid)
            }.getOrNull()
        }.sortedWith(compareBy<QuranAyahBookmark> { it.createdAtMillis }.thenBy { it.id })

    fun add(records: Set<String>, bookmark: QuranAyahBookmark): Set<String> {
        require(isValid(bookmark)) { "Ayah bookmark data is invalid." }
        val remaining = decode(records).filterNot { it.id == bookmark.id }
        return (remaining + bookmark).map(::encode).toSet()
    }

    fun remove(records: Set<String>, bookmarkId: String): Set<String> =
        decode(records)
            .filterNot { it.id == bookmarkId }
            .map(::encode)
            .toSet()

    private fun isValid(bookmark: QuranAyahBookmark): Boolean =
        bookmark.id.isNotBlank() &&
            bookmark.surahNumber in 1..114 &&
            bookmark.ayahNumber > 0 &&
            bookmark.label.isNotBlank() &&
            bookmark.label.length <= 80 &&
            colorPattern.matches(bookmark.colorHex) &&
            bookmark.createdAtMillis >= 0L

    private fun encode(bookmark: QuranAyahBookmark): String =
        JSONObject()
            .put("id", bookmark.id)
            .put("surah", bookmark.surahNumber)
            .put("ayah", bookmark.ayahNumber)
            .put("label", bookmark.label)
            .put("color", bookmark.colorHex)
            .put("createdAt", bookmark.createdAtMillis)
            .toString()
}
