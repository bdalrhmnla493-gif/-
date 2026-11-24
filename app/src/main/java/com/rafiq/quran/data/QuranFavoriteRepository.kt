package com.rafiq.quran.data

data class QuranAyahReference(
    val surahNumber: Int,
    val ayahNumber: Int
)

/**
 * Normalizes favorite ayah references stored as "surah:ayah" keys.
 */
object QuranFavoriteRepository {
    fun decode(keys: Set<String>): Set<QuranAyahReference> =
        keys.mapNotNull { key ->
            val parts = key.split(':', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val surah = parts[0].toIntOrNull() ?: return@mapNotNull null
            val ayah = parts[1].toIntOrNull() ?: return@mapNotNull null
            if (surah !in 1..114 || ayah < 1) return@mapNotNull null
            QuranAyahReference(surah, ayah)
        }.toSet()

    fun toggle(
        keys: Set<String>,
        surahNumber: Int,
        ayahNumber: Int
    ): Set<String> {
        require(surahNumber in 1..114 && ayahNumber > 0) {
            "Favorite ayah reference is invalid."
        }
        val current = decode(keys).toMutableSet()
        val reference = QuranAyahReference(surahNumber, ayahNumber)
        if (!current.add(reference)) current.remove(reference)
        return current.map { "${it.surahNumber}:${it.ayahNumber}" }.toSet()
    }
}
