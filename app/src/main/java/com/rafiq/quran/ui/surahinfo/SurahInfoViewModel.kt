package com.rafiq.quran.ui.surahinfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.rafiq.quran.data.MushafPageMetadata
import com.rafiq.quran.data.MushafPageRepository
import com.rafiq.quran.data.QuranSurah

class SurahInfoViewModel : ViewModel() {
    var state by mutableStateOf<SurahInfoState>(SurahInfoState.Loading)
        private set

    fun load(surah: QuranSurah, pageMetadata: MushafPageMetadata?) {
        if (pageMetadata == null) {
            state = SurahInfoState.Loading
            return
        }

        val isMakki = when (surah.revelationType) {
            "Meccan" -> true
            "Medinan" -> false
            else -> {
                state = SurahInfoState.Error("نوع نزول السورة غير متاح في بيانات القرآن.")
                return
            }
        }

        val startPage = MushafPageRepository.findPageNumber(
            pageMetadata.pages,
            surah.number,
            1
        )
        val startJuz = MushafPageRepository.findJuzNumber(
            pageMetadata.juz,
            surah.number,
            1
        )

        state = SurahInfoState.Ready(
            SurahInfo(
                number = surah.number,
                name = surah.name,
                ayahCount = surah.ayahs.size,
                isMakki = isMakki,
                startPage = startPage,
                startJuz = startJuz,
                revelationOrder = null,
                nameReason = null,
                summary = null,
                sourceName = null
            )
        )
    }
}
