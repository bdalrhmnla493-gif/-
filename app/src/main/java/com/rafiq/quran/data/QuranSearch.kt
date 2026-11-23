package com.rafiq.quran.data

data class QuranSearchResult(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val text: String
)

/**
 * Searches the locally stored Quran text without changing the original Uthmani text.
 * Matching ignores Arabic vowel marks and common Alef variants only.
 */
object QuranSearch {
    private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val alefVariants = Regex("[أإآٱ]")

    internal fun normalizeArabic(value: String): String =
        value
            .replace(arabicMarks, "")
            .replace(alefVariants, "ا")
            .replace('ى', 'ي')
            .trim()
            .replace(Regex("\\s+"), " ")

    fun search(
        surahs: List<QuranSurah>,
        query: String,
        limit: Int = 100
    ): List<QuranSearchResult> {
        require(limit > 0) { "Search result limit must be positive." }
        val normalizedQuery = normalizeArabic(query)
        if (normalizedQuery.isBlank()) return emptyList()

        return buildList {
            for (surah in surahs) {
                for (ayah in surah.ayahs) {
                    if (normalizeArabic(ayah.text).contains(normalizedQuery)) {
                        add(
                            QuranSearchResult(
                                surahNumber = surah.number,
                                surahName = surah.name,
                                ayahNumber = ayah.number,
                                text = ayah.text
                            )
                        )
                        if (size >= limit) return@buildList
                    }
                }
            }
        }
    }
}
