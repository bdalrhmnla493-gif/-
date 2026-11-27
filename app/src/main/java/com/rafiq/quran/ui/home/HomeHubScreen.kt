package com.rafiq.quran.ui.home

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.format.TextStyle
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

// كل المحتوى الديني يُمرَّر من بيانات المشروع الموثقة. لا يُكتب أي نص قرآني أو ذكر هنا.
data class HomeAyah(val text: String, val surahName: String, val ayah: Int)
data class HomeDhikr(val text: String, val source: String?)
data class HomeLastRead(val surahName: String, val ayah: Int, val page: Int?)

sealed interface HomeSection<out T> {
    data object Loading : HomeSection<Nothing>
    data object Unavailable : HomeSection<Nothing>
    data class Ready<T>(val value: T) : HomeSection<T>
}

private data class HomePalette(
    val bg: Color, val card: Color, val green: Color, val onGreen: Color,
    val gold: Color, val text: Color, val muted: Color, val border: Color
)

@Composable
private fun palette(): HomePalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) HomePalette(
        bg = Color(0xFF111A15), card = Color(0xFF18241D),
        green = Color(0xFF1F3A2C), onGreen = Color(0xFFF6F1E5),
        gold = Color(0xFFD4B87C), text = Color(0xFFEFE9DA),
        muted = Color(0xFFA9B4A9), border = Color(0xFF2A3B31)
    ) else HomePalette(
        bg = Color(0xFFF7F2E6), card = Color(0xFFFFFDF7),
        green = Color(0xFF3E5B45), onGreen = Color(0xFFF6F1E5),
        gold = Color(0xFFC9AE72), text = Color(0xFF2B3A31),
        muted = Color(0xFF7A8479), border = Color(0xFFE8E0CC)
    )
}

private val HijriMonths = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

/** التاريخ من المنطقة الزمنية الافتراضية لجهاز المستخدم. التاريخ الهجري متاح على API 26 فأعلى. */
@Suppress("NewApi")
fun todayTexts(): Pair<String, String> {
    val ar = Locale("ar", "EG")
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        val localCalendar = Calendar.getInstance()
        val dayName = localCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, ar).orEmpty()
        val monthName = localCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, ar).orEmpty()
        val greg = dayName + " " + localCalendar.get(Calendar.DAY_OF_MONTH) + " " +
            monthName + " " + localCalendar.get(Calendar.YEAR) + " م"
        return "التاريخ الهجري غير متاح على إصدار Android هذا" to greg
    }

    val today = LocalDate.now(ZoneId.systemDefault())
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, ar)
    val monthName = today.month.getDisplayName(TextStyle.FULL, ar)
    val greg = dayName + " " + today.dayOfMonth + " " + monthName + " " + today.year + " م"
    val h = HijrahDate.from(today)
    val hijri = dayName + " " + h.get(ChronoField.DAY_OF_MONTH) + " " +
        HijriMonths[h.get(ChronoField.MONTH_OF_YEAR) - 1] + " " +
        h.get(ChronoField.YEAR) + " هـ"
    return hijri to greg
}

@Composable
fun HomeHubScreen(
    hijriText: String,
    gregorianText: String,
    lastRead: HomeLastRead?,
    ayah: HomeSection<HomeAyah>,
    dhikr: HomeSection<HomeDhikr>,
    onOpenQuran: () -> Unit,
    onContinue: () -> Unit,
    onRefreshAyah: () -> Unit,
    onOpenAzkar: () -> Unit,
    onCopyDhikr: (HomeDhikr) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    topContent: @Composable ColumnScope.() -> Unit = {},
    additionalContent: @Composable ColumnScope.() -> Unit = {},
    onOpenMore: (() -> Unit)? = null
) {
    val p = palette()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier.fillMaxSize().background(p.bg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (headerContent != null) headerContent() else Header(p, onSettings)
            topContent()
            DateCard(p, hijriText, gregorianText)
            QuranCard(p, lastRead, onOpenQuran, onContinue)
            AyahCard(p, ayah, onRefreshAyah)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCard(p, "الأذكار", "أذكار الصباح والمساء", onOpenAzkar, Modifier.weight(1f))
                MiniCard(p, "المزيد", "الإعدادات والأقسام", onOpenMore, Modifier.weight(1f))
            }
            DhikrCard(p, dhikr, onCopyDhikr)
            additionalContent()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SoftCard(p: HomePalette, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
private fun GoldLine(p: HomePalette) =
    HorizontalDivider(Modifier.width(40.dp), thickness = 1.5.dp, color = p.gold)

@Composable
private fun Header(p: HomePalette, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("رفيق القرآن", style = MaterialTheme.typography.headlineMedium, color = p.green)
            Text("معك .. في كل حين", style = MaterialTheme.typography.bodyMedium, color = p.muted)
            Spacer(Modifier.height(6.dp))
            GoldLine(p)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", tint = p.muted)
        }
    }
}

@Composable
private fun DateCard(p: HomePalette, hijri: String, greg: String) {
    SoftCard(p) {
        Text(hijri, style = MaterialTheme.typography.titleMedium, color = p.text)
        Spacer(Modifier.height(2.dp))
        Text(greg, style = MaterialTheme.typography.bodyMedium, color = p.muted)
    }
}

@Composable
private fun QuranCard(p: HomePalette, last: HomeLastRead?, onOpen: () -> Unit, onContinue: () -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = p.green, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("القرآن الكريم", style = MaterialTheme.typography.headlineSmall, color = p.onGreen)
            Text("اقرأ وتأمّل .. واطمئن قلبك", style = MaterialTheme.typography.bodyMedium,
                color = p.onGreen.copy(alpha = 0.8f))
            if (last != null) {
                Spacer(Modifier.height(4.dp))
                val pageText = last.page?.let { " • صفحة " + it }.orEmpty()
                Text("آخر موضع: " + last.surahName + " • الآية " + last.ayah + pageText,
                    style = MaterialTheme.typography.bodySmall, color = p.gold)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = if (last == null) onOpen else onContinue,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.onGreen, contentColor = p.green),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (last == null) "ابدأ القراءة" else "متابعة القراءة") }
        }
    }
}

@Composable
private fun AyahCard(p: HomePalette, state: HomeSection<HomeAyah>, onRefresh: () -> Unit) {
    SoftCard(p) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("آية عشوائية", style = MaterialTheme.typography.titleSmall, color = p.gold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "آية أخرى", tint = p.green)
            }
        }
        when (state) {
            HomeSection.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            HomeSection.Unavailable -> Text("بيانات الآيات غير متاحة حاليًا", color = p.muted)
            is HomeSection.Ready -> {
                Text(
                    "﴿ " + state.value.text + " ﴾",
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 42.sp),
                    color = p.text, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(state.value.surahName + " : " + state.value.ayah,
                    style = MaterialTheme.typography.bodyMedium, color = p.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MiniCard(p: HomePalette, title: String, subtitle: String, onClick: (() -> Unit)?, modifier: Modifier) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(20.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        shadowElevation = 2.dp,
        modifier = modifier.heightIn(min = 96.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = if (onClick == null) p.muted else p.green)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = p.muted)
        }
    }
}

@Composable
private fun DhikrCard(p: HomePalette, state: HomeSection<HomeDhikr>, onCopy: (HomeDhikr) -> Unit) {
    SoftCard(p) {
        Text("ذكر اليوم", style = MaterialTheme.typography.titleSmall, color = p.gold)
        Spacer(Modifier.height(8.dp))
        when (state) {
            HomeSection.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            HomeSection.Unavailable -> Text("قيد التجهيز", color = p.muted)
            is HomeSection.Ready -> {
                Text(state.value.text, style = MaterialTheme.typography.titleMedium,
                    color = p.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                state.value.source?.takeIf { it.isNotBlank() }?.let {
                    Text("المصدر: " + it, style = MaterialTheme.typography.labelMedium, color = p.muted)
                }
                TextButton(onClick = { onCopy(state.value) }) { Text("نسخ", color = p.green) }
            }
        }
    }
}