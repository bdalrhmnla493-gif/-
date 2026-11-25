package com.rafiq.quran.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// النتائج تأتي من QuranSearch.kt الموجود في المشروع، والنص العثماني يُعرض كما هو دون أي تعديل.
data class SearchResult(
    val surahNumber: Int,
    val surahName: String,
    val ayah: Int,
    val page: Int,
    val juz: Int,
    val ayahText: String
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<SearchResult>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@Composable
fun QuranSearchScreen(
    query: String,
    state: SearchUiState,
    selectedIndex: Int,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onSelect: (Int) -> Unit,
    onOpenResult: (SearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("ابحث في القرآن الكريم") },
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "بحث")
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح البحث")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            when (state) {
                SearchUiState.Idle -> CenterText("اكتب كلمة أو عبارة للبحث")
                SearchUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is SearchUiState.Error -> CenterText(state.message)
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        CenterText("لا توجد نتائج")
                    } else {
                        ResultsHeader(
                            total = state.results.size,
                            selectedIndex = selectedIndex,
                            onPrev = { if (selectedIndex > 0) onSelect(selectedIndex - 1) },
                            onNext = {
                                if (selectedIndex < state.results.lastIndex) onSelect(selectedIndex + 1)
                            }
                        )
                        ResultsList(state.results, selectedIndex, onSelect, onOpenResult)
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultsHeader(
    total: Int,
    selectedIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${selectedIndex + 1} من $total نتيجة",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onPrev, enabled = selectedIndex > 0) { Text("السابق") }
        TextButton(onClick = onNext, enabled = selectedIndex < total - 1) { Text("التالي") }
    }
}

@Composable
private fun ResultsList(
    results: List<SearchResult>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onOpenResult: (SearchResult) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in results.indices) listState.animateScrollToItem(selectedIndex)
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(results, key = { _, r -> "${r.surahNumber}:${r.ayah}" }) { index, r ->
            val selected = index == selectedIndex
            OutlinedCard(
                onClick = { onSelect(index); onOpenResult(r) },
                border = CardDefaults.outlinedCardBorder().let {
                    if (selected) androidx.compose.foundation.BorderStroke(
                        2.dp, MaterialTheme.colorScheme.secondary
                    ) else it
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${r.surahName} • الآية ${r.ayah}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "ص ${r.page} • ج ${r.juz}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Text(r.ayahText, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
