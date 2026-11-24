package com.rafiq.quran.data

import org.json.JSONObject

data class MushafNamedPageBookmark(
    val id: String,
    val pageNumber: Int,
    val label: String,
    val colorHex: String,
    val createdAtMillis: Long
)

/** Keeps user-labelled page bookmarks separate from legacy page-number keys. */
object MushafNamedPageBookmarkRepository {
    private val colorPattern = Regex("^#[0-9A-Fa-f]{6}$")

    fun decode(records: Set<String>): List<MushafNamedPageBookmark> =
        records.mapNotNull { raw ->
            runCatching {
                val json = JSONObject(raw)
                MushafNamedPageBookmark(
                    id = json.getString("id"),
                    pageNumber = json.getInt("page"),
                    label = json.getString("label"),
                    colorHex = json.getString("color"),
                    createdAtMillis = json.getLong("createdAt")
                ).takeIf(::isValid)
            }.getOrNull()
        }.sortedWith(compareBy<MushafNamedPageBookmark> { it.createdAtMillis }.thenBy { it.id })

    fun add(records: Set<String>, bookmark: MushafNamedPageBookmark): Set<String> {
        require(isValid(bookmark)) { "Page bookmark data is invalid." }
        val remaining = decode(records).filterNot {
            it.id == bookmark.id || it.pageNumber == bookmark.pageNumber
        }
        return (remaining + bookmark).map(::encode).toSet()
    }

    fun removePage(records: Set<String>, pageNumber: Int): Set<String> =
        decode(records)
            .filterNot { it.pageNumber == pageNumber }
            .map(::encode)
            .toSet()

    private fun isValid(bookmark: MushafNamedPageBookmark): Boolean =
        bookmark.id.isNotBlank() &&
            bookmark.pageNumber in 1..604 &&
            bookmark.label.isNotBlank() &&
            bookmark.label.length <= 80 &&
            colorPattern.matches(bookmark.colorHex) &&
            bookmark.createdAtMillis >= 0L

    private fun encode(bookmark: MushafNamedPageBookmark): String =
        JSONObject()
            .put("id", bookmark.id)
            .put("page", bookmark.pageNumber)
            .put("label", bookmark.label)
            .put("color", bookmark.colorHex)
            .put("createdAt", bookmark.createdAtMillis)
            .toString()
}
