package com.rafiq.quran.ui.bookmarks

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
import androidx.compose.runtime.CompositionLocalProvider

// تُحفظ وتُقرأ من التخزين المحلي الموجود (DataStore/Room)، ولا يُنشأ هنا أي نص قرآني.
data class BookmarkItem(
    val id: Long,
    val name: String,
    val surahName: String,
    val ayah: Int,
    val page: Int,
    val juz: Int,
    val colorArgb: Long,
    val createdAt: Long
)

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkItem>,
    onOpen: (BookmarkItem) -> Unit,
    onRename: (BookmarkItem, String) -> Unit,
    onDelete: (BookmarkItem) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var query by remember { mutableStateOf("") }
        var newestFirst by remember { mutableStateOf(true) }
        var renaming by remember { mutableStateOf<BookmarkItem?>(null) }
        var deleting by remember { mutableStateOf<BookmarkItem?>(null) }

        val shown = remember(query, newestFirst, bookmarks) {
            val q = query.trim()
            bookmarks
                .filter { q.isEmpty() || it.name.contains(q) || it.surahName.contains(q) }
                .let { list ->
                    if (newestFirst) list.sortedByDescending { it.createdAt }
                    else list.sortedBy { it.createdAt }
                }
        }

        Column(modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("ابحث في العلامات") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = newestFirst,
                    onClick = { newestFirst = !newestFirst },
                    label = { Text(if (newestFirst) "الأحدث" else "الأقدم") }
                )
            }

            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(if (bookmarks.isEmpty()) "لا توجد علامات محفوظة" else "لا توجد نتائج")
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(shown, key = { it.id }) { b ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(b) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(14.dp).clip(CircleShape)
                                    .background(Color(b.colorArgb))
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    b.name.ifBlank { "${b.surahName} • ${b.ayah}" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "${b.surahName} • الآية ${b.ayah} • ص ${b.page} • ج ${b.juz}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { renaming = b }) { Text("تعديل") }
                            IconButton(onClick = { deleting = b }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف العلامة")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        renaming?.let { b ->
            var text by remember(b.id) { mutableStateOf(b.name) }
            AlertDialog(
                onDismissRequest = { renaming = null },
                title = { Text("تعديل اسم العلامة") },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onRename(b, text.trim()); renaming = null }) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renaming = null }) { Text("إلغاء") }
                }
            )
        }

        deleting?.let { b ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("حذف العلامة") },
                text = { Text("هل تريد حذف هذه العلامة؟") },
                confirmButton = {
                    TextButton(onClick = { onDelete(b); deleting = null }) { Text("حذف") }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = null }) { Text("إلغاء") }
                }
            )
        }
    }
}