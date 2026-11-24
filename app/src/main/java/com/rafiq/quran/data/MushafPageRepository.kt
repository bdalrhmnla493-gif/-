package com.rafiq.quran.data

import android.content.Context
import org.json.JSONObject

data class MushafPageBoundary(
    val page: Int,
    val surah: Int,
    val ayah: Int
)

data class MushafJuzBoundary(
    val index: Int,
    val surah: Int,
    val ayah: Int
)

data class MushafPageMetadata(
    val pages: List<MushafPageBoundary>,
    val juz: List<MushafJuzBoundary>
)

data class MushafPageAyah(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val text: String
)

object MushafPageRepository {
    fun load(context: Context): MushafPageMetadata {
        val content = context.assets.open("mushaf-page-metadata.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parse(content)
    }

    internal fun parse(content: String): MushafPageMetadata {
        val root = JSONObject(content)
        val pageArray = root.getJSONArray("pages")
        val juzArray = root.getJSONArray("juz")
        val pages = ArrayList<MushafPageBoundary>(pageArray.length())
        val juz = ArrayList<MushafJuzBoundary>(juzArray.length())

        for (index in 0 until pageArray.length()) {
            val item = pageArray.getJSONObject(index)
            pages += MushafPageBoundary(
                page = item.getInt("page"),
                surah = item.getInt("surah"),
                ayah = item.getInt("ayah")
            )
        }
        for (index in 0 until juzArray.length()) {
            val item = juzArray.getJSONObject(index)
            juz += MushafJuzBoundary(
                index = item.getInt("index"),
                surah = item.getInt("surah"),
                ayah = item.getInt("ayah")
            )
        }

        require(pages.isNotEmpty() && pages.first().page == 1) {
            "بيانات صفحات المصحف غير مكتملة."
        }
        require(pages.zipWithNext().all { (first, second) ->
            second.page == first.page + 1 && isBefore(first.surah, first.ayah, second.surah, second.ayah)
        }) {
            "ترتيب صفحات المصحف غير صحيح."
        }
        require(juz.isNotEmpty() && juz.first().index == 1) {
            "بيانات الأجزاء غير مكتملة."
        }
        require(juz.zipWithNext().all { (first, second) ->
            second.index == first.index + 1 && isBefore(first.surah, first.ayah, second.surah, second.ayah)
        }) {
            "ترتيب أجزاء المصحف غير صحيح."
        }
        require(pages.all { it.surah in 1..114 && it.ayah > 0 } &&
            juz.all { it.surah in 1..114 && it.ayah > 0 }) {
            "حدود المصحف تحتوي على مواضع غير صالحة."
        }

        return MushafPageMetadata(pages, juz)
    }

    fun findPageNumber(pages: List<MushafPageBoundary>, surah: Int, ayah: Int): Int {
        require(pages.isNotEmpty())
        val target = pages.lastOrNull {
            isAtOrBefore(it.surah, it.ayah, surah, ayah)
        } ?: pages.first()
        return target.page
    }

    fun findJuzNumber(juz: List<MushafJuzBoundary>, surah: Int, ayah: Int): Int {
        if (juz.isEmpty()) return 1
        return juz.lastOrNull {
            isAtOrBefore(it.surah, it.ayah, surah, ayah)
        }?.index ?: juz.first().index
    }

    fun getPageAyahs(
        surahs: List<QuranSurah>,
        pages: List<MushafPageBoundary>,
        pageNumber: Int
    ): List<MushafPageAyah> {
        val start = pages.firstOrNull { it.page == pageNumber } ?: return emptyList()
        val next = pages.firstOrNull { it.page == pageNumber + 1 }

        return buildList {
            for (surah in surahs) {
                for (ayah in surah.ayahs) {
                    val afterStart = isAtOrBefore(start.surah, start.ayah, surah.number, ayah.number)
                    val beforeEnd = next == null ||
                        isBefore(surah.number, ayah.number, next.surah, next.ayah)
                    if (afterStart && beforeEnd) {
                        add(
                            MushafPageAyah(
                                surahNumber = surah.number,
                                surahName = surah.name,
                                ayahNumber = ayah.number,
                                text = ayah.text
                            )
                        )
                    }
                }
            }
        }
    }

    private fun isAtOrBefore(
        firstSurah: Int,
        firstAyah: Int,
        secondSurah: Int,
        secondAyah: Int
    ): Boolean = firstSurah < secondSurah ||
        (firstSurah == secondSurah && firstAyah <= secondAyah)

    private fun isBefore(
        firstSurah: Int,
        firstAyah: Int,
        secondSurah: Int,
        secondAyah: Int
    ): Boolean = firstSurah < secondSurah ||
        (firstSurah == secondSurah && firstAyah < secondAyah)
}
