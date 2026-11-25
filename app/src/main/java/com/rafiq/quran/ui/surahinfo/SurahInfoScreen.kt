package com.rafiq.quran.ui.surahinfo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// الحقول الأساسية تأتي من بيانات المشروع (mushaf-page-metadata.json / QuranRepository).
// الحقول الاختيارية (ترتيب النزول، سبب التسمية، النبذة) تُعرض فقط إذا توفر لها مصدر موثق،
// ولا يُولَّد لها أي محتوى هنا.
data class SurahInfo(
    val number: Int,
    val name: String,
    val ayahCount: Int,
    val isMakki: Boolean,
    val startPage: Int,
    val startJuz: Int,
    val revelationOrder: Int? = null,
    val nameReason: String? = null,
    val summary: String? = null,
    val sourceName: String? = null
)

sealed interface SurahInfoState {
    data object Loading : SurahInfoState
    data class Ready(val info: SurahInfo) : SurahInfoState
    data class Error(val message: String) : SurahInfoState
}

@Composable
fun SurahInfoScreen(
    state: SurahInfoState,
    onOpenSurah: (SurahInfo) -> Unit,
    onOpenTafsir: (SurahInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (state) {
            SurahInfoState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is SurahInfoState.Error -> Box(modifier.fillMaxSize(), Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is SurahInfoState.Ready -> InfoContent(state.info, onOpenSurah, onOpenTafsir, modifier)
        }
    }
}

@Composable
private fun InfoContent(
    info: SurahInfo,
    onOpenSurah: (SurahInfo) -> Unit,
    onOpenTafsir: (SurahInfo) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("سورة ${info.name}", style = MaterialTheme.typography.headlineSmall)

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("رقم السورة", info.number.toString())
                InfoRow("عدد الآيات", info.ayahCount.toString())
                InfoRow("النوع", if (info.isMakki) "مكية" else "مدنية")
                InfoRow("صفحة البداية", info.startPage.toString())
                InfoRow("الجزء", info.startJuz.toString())
                info.revelationOrder?.let { InfoRow("ترتيب النزول", it.toString()) }
            }
        }

        info.nameReason?.takeIf { it.isNotBlank() }?.let {
            InfoSection("سبب التسمية", it, info.sourceName)
        }
        info.summary?.takeIf { it.isNotBlank() }?.let {
            InfoSection("نبذة عن السورة", it, info.sourceName)
        }

        Button(onClick = { onOpenSurah(info) }, modifier = Modifier.fillMaxWidth()) {
            Text("فتح السورة في المصحف")
        }
        OutlinedButton(onClick = { onOpenTafsir(info) }, modifier = Modifier.fillMaxWidth()) {
            Text("التفسير")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun InfoSection(title: String, body: String, source: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge)
        source?.takeIf { it.isNotBlank() }?.let {
            Text(
                "المصدر: $it",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
