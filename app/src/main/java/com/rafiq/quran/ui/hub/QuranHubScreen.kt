package com.rafiq.quran.ui.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
private fun hubCardContainer(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1C4A3B) else Color(0xFF153D32)

@Composable
private fun hubCardContent(): Color =
    if (isSystemInDarkTheme()) Color(0xFFF6F1E5) else Color(0xFFF6F1E5)

@Composable
private fun hubOutline(): Color =
    if (isSystemInDarkTheme()) Color(0xFF2E4036) else Color(0xFFD9D0BC)

data class LastReading(val surahName: String, val ayah: Int, val page: Int, val juz: Int)
data class HubCounts(val surahs: Int, val juz: Int, val pages: Int)

sealed interface HubState {
    data object Loading : HubState
    data class Ready(val last: LastReading?, val counts: HubCounts) : HubState
    data class Error(val message: String, val onRetry: () -> Unit) : HubState
}

@Composable
fun QuranHubScreen(
    state: HubState,
    onContinue: () -> Unit,
    onOpenMushaf: () -> Unit,
    onSearch: () -> Unit,
    onSurahIndex: () -> Unit,
    onJuzIndex: () -> Unit,
    onPageIndex: () -> Unit,
    onPageBookmarks: () -> Unit,
    onAyahBookmarks: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HubHeader(onSearch, onSettings)
            when (state) {
                HubState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is HubState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = state.onRetry) { Text("إعادة المحاولة") }
                }
                is HubState.Ready -> {
                    ContinueCard(state.last, onContinue, onOpenMushaf)
                    SearchEntry(onSearch)
                    Text("الفهارس", style = MaterialTheme.typography.titleMedium)
                    Tiles(
                        listOf(
                            Tile("فهرس السور", "${state.counts.surahs} سورة", Icons.AutoMirrored.Filled.List, onSurahIndex),
                            Tile("الأجزاء", "${state.counts.juz} جزءًا", Icons.Filled.DateRange, onJuzIndex),
                            Tile("الصفحات", "${state.counts.pages} صفحة", Icons.Filled.Search, onPageIndex),
                            Tile("علامات الصفحات", "مواضعك المحفوظة", Icons.Filled.Star, onPageBookmarks),
                            Tile("علامات الآيات", "أسماء وألوان", Icons.Filled.Star, onAyahBookmarks),
                            Tile("المفضلة", "مع ملاحظاتك", Icons.Filled.Favorite, onFavorites)
                        )
                    )
                    Text("قريبًا", style = MaterialTheme.typography.titleMedium)
                    ComingSoonRow("التلاوات الصوتية")
                    ComingSoonRow("التفسير ومعاني الكلمات")
                    Text(
                        "تُفعَّل بعد اعتماد مصادرها وحقوق استخدامها.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HubHeader(onSearch: () -> Unit, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("القرآن الكريم", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                Modifier.width(48.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Filled.Search, contentDescription = "بحث في القرآن")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "إعدادات المصحف")
        }
    }
}

@Composable
private fun ContinueCard(last: LastReading?, onContinue: () -> Unit, onOpenMushaf: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = hubCardContainer(),
            contentColor = hubCardContent()
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (last == null) {
                Text("ابدأ القراءة", style = MaterialTheme.typography.titleLarge)
                GoldButton("فتح المصحف", onOpenMushaf)
            } else {
                Text(
                    "آخر موضع قرأت فيه",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "سورة ${last.surahName} • الآية ${last.ayah}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "الصفحة ${last.page} • الجزء ${last.juz}",
                    style = MaterialTheme.typography.bodyMedium
                )
                GoldButton("متابعة القراءة", onContinue, showPlay = true)
            }
        }
    }
}

@Composable
private fun GoldButton(text: String, onClick: () -> Unit, showPlay: Boolean = false) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        if (showPlay) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
        }
        Text(text)
    }
}

@Composable
private fun SearchEntry(onSearch: () -> Unit) {
    OutlinedCard(
        onClick = onSearch,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, hubOutline()),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text("ابحث عن سورة أو آية", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class Tile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun Tiles(items: List<Tile>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile -> TileCard(tile, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TileCard(t: Tile, modifier: Modifier) {
    Surface(
        onClick = t.onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hubOutline()),
        modifier = modifier.heightIn(min = 72.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(t.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(t.title, style = MaterialTheme.typography.titleSmall)
                Text(t.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ComingSoonRow(title: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().alpha(0.7f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f))
            Text("قيد التجهيز", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
