package com.rafiq.quran.data

import org.json.JSONObject

data class QuranFavoriteNote(
    val surahNumber: Int,
    val ayahNumber: Int,
    val note: String,
    val colorHex: String,
    val updatedAtMillis: Long
)

/** Stores optional private notes attached to favorite verses. */
object QuranFavoriteNoteRepository {
    private val colorPattern = Regex("^#[0-9A-Fa-f]{6}$")

    fun decode(records: Set<String>): List<QuranFavoriteNote> =
        records.mapNotNull { raw ->
            runCatching {
                val json = JSONObject(raw)
                QuranFavoriteNote(
                    surahNumber = json.getInt("surah"),
                    ayahNumber = json.getInt("ayah"),
                    note = json.getString("note"),
                    colorHex = json.getString("color"),
                    updatedAtMillis = json.getLong("updatedAt")
                ).takeIf(::isValid)
            }.getOrNull()
        }.distinctBy { it.surahNumber to it.ayahNumber }

    fun upsert(records: Set<String>, note: QuranFavoriteNote): Set<String> {
        require(isValid(note)) { "Favorite note data is invalid." }
        return (decode(records)
            .filterNot { it.surahNumber == note.surahNumber && it.ayahNumber == note.ayahNumber } +
            note).map(::encode).toSet()
    }

    fun remove(records: Set<String>, surahNumber: Int, ayahNumber: Int): Set<String> =
        decode(records)
            .filterNot { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
            .map(::encode)
            .toSet()

    private fun isValid(note: QuranFavoriteNote): Boolean =
        note.surahNumber in 1..114 &&
            note.ayahNumber > 0 &&
            note.note.isNotBlank() &&
            note.note.length <= 500 &&
            colorPattern.matches(note.colorHex) &&
            note.updatedAtMillis >= 0L

    private fun encode(note: QuranFavoriteNote): String =
        JSONObject()
            .put("surah", note.surahNumber)
            .put("ayah", note.ayahNumber)
            .put("note", note.note)
            .put("color", note.colorHex)
            .put("updatedAt", note.updatedAtMillis)
            .toString()
}
