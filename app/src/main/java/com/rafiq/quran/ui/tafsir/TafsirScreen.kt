package com.rafiq.quran.ui.tafsir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// نص الآية يُقرأ من مصدر القرآن المعتمد في المشروع ولا يُعدَّل هنا.
// نص التفسير يأتي من مصدر موثق ومرخّص فقط، واسم الكتاب والجهة إلزاميان.
// إذا لم يتوفر مصدر، تُعرض حالة "غير متاح" ولا يُولَّد أي تفسير.
data class TafsirEntry(
    val surahName: String,
    val ayah: Int,
    val ayahText: String,
    val tafsirText: String,
    val bookName: String,
    val sourceName: String
)

sealed interface TafsirUiState {
    data object Loading : TafsirUiState
    data object Unavailable : TafsirUiState
    data class Ready(val entry: TafsirEntry, val hasPrev: Boolean, val hasNext: Boolean) : TafsirUiState
    data class Error(val message: String) : TafsirUiState
}

@Composable
fun TafsirScreen(
    state: TafsirUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCopy: (TafsirEntry) -> Unit,
    onShare: (TafsirEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (state) {
            TafsirUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            TafsirUiState.Unavailable -> Box(modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "التفسير غير متاح لهذه الآية حاليًا",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is TafsirUiState.Error -> Box(modifier.fillMaxSize(), Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is TafsirUiState.Ready -> TafsirContent(state, onPrev, onNext, onCopy, onShare, modifier)
        }
    }
}

@Composable
private fun TafsirContent(
    state: TafsirUiState.Ready,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCopy: (TafsirEntry) -> Unit,
    onShare: (TafsirEntry) -> Unit,
    modifier: Modifier
) {
    val entry = state.entry
    var fontSize by remember { mutableFloatStateOf(18f) }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "${entry.surahName} • الآية ${entry.ayah}",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedCard(Modifier.fillMaxWidth()) {
                Text(
                    entry.ayahText,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Text(
                entry.tafsirText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.7f).sp
                )
            )
            Text(
                "المصدر: ${entry.bookName} — ${entry.sourceName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TextButton(onClick = onPrev, enabled = state.hasPrev) { Text("السابقة") }
            TextButton(onClick = onNext, enabled = state.hasNext) { Text("التالية") }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { if (fontSize > 14f) fontSize -= 2f },
                enabled = fontSize > 14f
            ) { Text("A-") }
            TextButton(
                onClick = { if (fontSize < 32f) fontSize += 2f },
                enabled = fontSize < 32f
            ) { Text("A+") }
            TextButton(onClick = { onCopy(entry) }) { Text("نسخ") }
            TextButton(onClick = { onShare(entry) }) { Text("مشاركة") }
        }
    }
}
