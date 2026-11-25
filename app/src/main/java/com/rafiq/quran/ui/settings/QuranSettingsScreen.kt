package com.rafiq.quran.ui.settings

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
import kotlin.math.roundToInt

// واجهة إعدادات فقط. القيم تُحفظ في DataStore الموجود في المشروع عبر onChange.
// هذه الإعدادات لا تغيّر شكل قارئ المصحف المعتمد أو صور صفحاته أو نصه.
enum class AppThemeMode(val label: String) {
    SYSTEM("حسب الجهاز"),
    LIGHT("فاتح"),
    DARK("داكن")
}

enum class FlipDirection(val label: String) {
    RTL("من اليمين إلى اليسار"),
    LTR("من اليسار إلى اليمين")
}

data class QuranSettings(
    val themeMode: AppThemeMode,
    val tafsirFontSize: Int,
    val keepScreenOn: Boolean,
    val showToolsOnOpen: Boolean,
    val autoFlipEnabled: Boolean,
    val autoFlipSeconds: Int,
    val flipDirection: FlipDirection,
    val twoPagesInLandscape: Boolean
)

@Composable
fun QuranSettingsScreen(
    settings: QuranSettings,
    onChange: (QuranSettings) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("إعدادات القرآن", style = MaterialTheme.typography.headlineSmall)

            SectionTitle("المظهر")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemeMode.entries.forEach { m ->
                    FilterChip(
                        selected = settings.themeMode == m,
                        onClick = { onChange(settings.copy(themeMode = m)) },
                        label = { Text(m.label) }
                    )
                }
            }

            Text(
                "حجم خط التفسير: ${settings.tafsirFontSize}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.tafsirFontSize.toFloat(),
                onValueChange = { onChange(settings.copy(tafsirFontSize = it.roundToInt())) },
                valueRange = 14f..32f,
                steps = 8
            )

            HorizontalDivider()
            SectionTitle("القراءة")
            SwitchRow("إبقاء الشاشة مضاءة", settings.keepScreenOn) {
                onChange(settings.copy(keepScreenOn = it))
            }
            SwitchRow("إظهار الأدوات عند فتح المصحف", settings.showToolsOnOpen) {
                onChange(settings.copy(showToolsOnOpen = it))
            }
            SwitchRow("صفحتان متجاورتان في العرض الأفقي", settings.twoPagesInLandscape) {
                onChange(settings.copy(twoPagesInLandscape = it))
            }

            HorizontalDivider()
            SectionTitle("التقليب التلقائي")
            SwitchRow("تفعيل التقليب التلقائي", settings.autoFlipEnabled) {
                onChange(settings.copy(autoFlipEnabled = it))
            }
            if (settings.autoFlipEnabled) {
                Text(
                    "المدة بين الصفحات: ${settings.autoFlipSeconds} ثانية",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.autoFlipSeconds.toFloat(),
                    onValueChange = { onChange(settings.copy(autoFlipSeconds = it.roundToInt())) },
                    valueRange = 5f..120f
                )
            }
            Text("اتجاه التقليب", style = MaterialTheme.typography.titleSmall)
            FlipDirection.entries.forEach { d ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = settings.flipDirection == d,
                        onClick = { onChange(settings.copy(flipDirection = d)) }
                    )
                    Text(d.label)
                }
            }

            HorizontalDivider()
            OutlinedButton(onClick = onResetDefaults, modifier = Modifier.fillMaxWidth()) {
                Text("إعادة الإعدادات الافتراضية")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
