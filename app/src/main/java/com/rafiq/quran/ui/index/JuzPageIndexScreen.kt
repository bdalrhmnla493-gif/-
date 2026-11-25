package com.rafiq.quran.ui.index

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// البيانات تأتي من mushaf-page-metadata.json / QuranRepository الموجودين في المشروع، ولا تُنشأ هنا.
data class JuzItem(
    val number: Int,
    val startSurahName: String,
    val startAyah: Int,
    val startPage: Int
)

data class PageItem(
    val number: Int,
    val surahName: String,
    val juz: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuzPageIndexScreen(
    juzList: List<JuzItem>,
    pages: List<PageItem>,
    currentPage: Int?,
    onOpenJuz: (JuzItem) -> Unit,
    onOpenPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var tab by remember { mutableIntStateOf(0) }

        Column(modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("الأجزاء") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("الصفحات") })
            }
            when (tab) {
                0 -> JuzList(juzList, onOpenJuz)
                else -> PageList(pages, currentPage, onOpenPage)
            }
        }
    }
}

@Composable
private fun JuzList(items: List<JuzItem>, onClick: (JuzItem) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("لا توجد بيانات") }
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(items, key = { it.number }) { j ->
            Row(
                Modifier.fillMaxWidth().clickable { onClick(j) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    j.number.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(32.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("الجزء ${j.number}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "يبدأ من ${j.startSurahName} • الآية ${j.startAyah}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("ص ${j.startPage}", style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PageList(
    items: List<PageItem>,
    currentPage: Int?,
    onOpenPage: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, items) {
        val n = query.trim().toIntOrNull()
        when {
            query.isBlank() -> items
            n != null -> items.filter { it.number == n }
            else -> emptyList()
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { v -> query = v.filter { it.isDigit() }.take(3) },
            label = { Text("اذهب إلى صفحة") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("لا توجد نتائج") }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(filtered, key = { it.number }) { p ->
                    val isCurrent = p.number == currentPage
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenPage(p.number) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            p.number.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(40.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(p.surahName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "الجزء ${p.juz}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCurrent) {
                            Text(
                                "الحالية",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
