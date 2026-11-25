package com.rafiq.quran.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// نص الآية يُقرأ كما هو من مصدر القرآن المعتمد في المشروع (quran-uthmani.txt)، ولا يُعدَّل هنا.
// التعليق الشخصي حقل منفصل ولا يمس نص الآية.
data class FavoriteAyah(
    val id: Long,
    val surahName: String,
    val ayah: Int,
    val page: Int,
    val juz: Int,
    val ayahText: String,
    val note: String,
    val colorArgb: Long,
    val createdAt: Long
)

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteAyah>,
    onOpen: (FavoriteAyah) -> Unit,
    onSaveNote: (FavoriteAyah, String) -> Unit,
    onDelete: (FavoriteAyah) -> Unit,
    onCopy: (FavoriteAyah) -> Unit,
    onShare: (FavoriteAyah) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var query by remember { mutableStateOf("") }
        var editing by remember { mutableStateOf<FavoriteAyah?>(null) }
        var deleting by remember { mutableStateOf<FavoriteAyah?>(null) }

        val shown = remember(query, favorites) {
            val q = query.trim()
            favorites
                .filter { q.isEmpty() || it.surahName.contains(q) || it.note.contains(q) }
                .sortedByDescending { it.createdAt }
        }

        Column(modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("ابحث في المفضلة") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(if (favorites.isEmpty()) "لا توجد آيات مفضلة" else "لا توجد نتائج")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { f ->
                        OutlinedCard(
                            onClick = { onOpen(f) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        Modifier.size(12.dp).clip(CircleShape)
                                            .background(Color(f.colorArgb))
                                    )
                                    Text(
                                        "${f.surahName} • الآية ${f.ayah}",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "ص ${f.page} • ج ${f.juz}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Text(f.ayahText, style = MaterialTheme.typography.titleLarge)
                                if (f.note.isNotBlank()) {
                                    Text(
                                        f.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { editing = f }) { Text("تعليق") }
                                    TextButton(onClick = { onCopy(f) }) { Text("نسخ") }
                                    TextButton(onClick = { onShare(f) }) { Text("مشاركة") }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { deleting = f }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "حذف من المفضلة")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        editing?.let { f ->
            var text by remember(f.id) { mutableStateOf(f.note) }
            AlertDialog(
                onDismissRequest = { editing = null },
                title = { Text("التعليق الشخصي") },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        minLines = 3
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onSaveNote(f, text.trim()); editing = null }) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editing = null }) { Text("إلغاء") }
                }
            )
        }

        deleting?.let { f ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("حذف من المفضلة") },
                text = { Text("هل تريد حذف هذه الآية من المفضلة؟") },
                confirmButton = {
                    TextButton(onClick = { onDelete(f); deleting = null }) { Text("حذف") }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = null }) { Text("إلغاء") }
                }
            )
        }
    }
}
