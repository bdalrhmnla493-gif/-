package com.rafiq.quran.ui.reciters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// قائمة القراء والمصادر تأتي من مصدر صوتي مرخّص فقط، واسم المصدر والترخيص إلزاميان لكل قارئ.
// لا يُنشأ هنا أي قارئ أو تسجيل. إذا لم يتوفر مصدر مرخّص تُعرض حالة "غير متاح".
data class Reciter(
    val id: String,
    val name: String,
    val sourceName: String,
    val licenseNote: String
)

sealed interface RecitersUiState {
    data object Loading : RecitersUiState
    data object Unavailable : RecitersUiState
    data class Ready(val reciters: List<Reciter>, val selectedId: String?) : RecitersUiState
    data class Error(val message: String) : RecitersUiState
}

@Composable
fun RecitersScreen(
    state: RecitersUiState,
    onSelect: (Reciter) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (state) {
            RecitersUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            RecitersUiState.Unavailable -> Box(modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "التلاوات غير متاحة حاليًا",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is RecitersUiState.Error -> Box(modifier.fillMaxSize(), Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is RecitersUiState.Ready -> RecitersList(state, onSelect, modifier)
        }
    }
}

@Composable
private fun RecitersList(
    state: RecitersUiState.Ready,
    onSelect: (Reciter) -> Unit,
    modifier: Modifier
) {
    if (state.reciters.isEmpty()) {
        Box(modifier.fillMaxSize(), Alignment.Center) {
            Text("لا يوجد قراء", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(state.reciters, key = { it.id }) { r ->
            OutlinedCard(onClick = { onSelect(r) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(selected = r.id == state.selectedId, onClick = { onSelect(r) })
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "المصدر: ${r.sourceName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            r.licenseNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
