package com.rafiq.quran.ui.repeat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// واجهة إعداد التكرار فقط. التنفيذ الفعلي (التكرار والمزامنة) يتم في المشغل الصوتي،
// والحدود (أرقام الآيات والصفحات) تأتي من بيانات المشروع، ولا تُحسب هنا.
enum class RepeatScope(val label: String) {
    AYAH("آية"),
    RANGE("نطاق"),
    PAGE("صفحة"),
    SURAH("سورة"),
    JUZ("جزء")
}

enum class AfterRepeat(val label: String) {
    NEXT("الانتقال للآية التالية"),
    RESTART("إعادة النطاق"),
    STOP("إيقاف التلاوة")
}

data class RepeatConfig(
    val scope: RepeatScope,
    val startAyah: Int?,
    val endAyah: Int?,
    val times: Int,
    val infinite: Boolean,
    val after: AfterRepeat
)

@Composable
fun RepeatSettingsScreen(
    initial: RepeatConfig,
    maxAyahInSurah: Int,
    onApply: (RepeatConfig) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var scope by remember { mutableStateOf(initial.scope) }
        var start by remember { mutableStateOf(initial.startAyah?.toString() ?: "") }
        var end by remember { mutableStateOf(initial.endAyah?.toString() ?: "") }
        var times by remember { mutableStateOf(initial.times.toString()) }
        var infinite by remember { mutableStateOf(initial.infinite) }
        var after by remember { mutableStateOf(initial.after) }

        val needsRange = scope == RepeatScope.RANGE
        val s = start.toIntOrNull()
        val e = end.toIntOrNull()
        val t = times.toIntOrNull()
        val rangeValid = !needsRange ||
            (s != null && e != null && s >= 1 && e <= maxAyahInSurah && s <= e)
        val timesValid = infinite || (t != null && t in 1..999)
        val canApply = rangeValid && timesValid

        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("إعدادات التكرار", style = MaterialTheme.typography.headlineSmall)

            Text("نطاق التكرار", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatScope.entries.forEach { sc ->
                    FilterChip(
                        selected = scope == sc,
                        onClick = { scope = sc },
                        label = { Text(sc.label) }
                    )
                }
            }

            if (needsRange) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { v -> start = v.filter { it.isDigit() }.take(3) },
                        label = { Text("من الآية") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { v -> end = v.filter { it.isDigit() }.take(3) },
                        label = { Text("إلى الآية") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!rangeValid) {
                    Text(
                        "أدخل نطاقًا صحيحًا من 1 إلى $maxAyahInSurah",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text("عدد مرات التكرار", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تكرار مفتوح", modifier = Modifier.weight(1f))
                Switch(checked = infinite, onCheckedChange = { infinite = it })
            }
            if (!infinite) {
                OutlinedTextField(
                    value = times,
                    onValueChange = { v -> times = v.filter { it.isDigit() }.take(3) },
                    label = { Text("عدد المرات") },
                    singleLine = true,
                    isError = !timesValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("بعد انتهاء التكرار", style = MaterialTheme.typography.titleMedium)
            AfterRepeat.entries.forEach { a ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = after == a, onClick = { after = a })
                    Text(a.label)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        onApply(
                            RepeatConfig(
                                scope = scope,
                                startAyah = if (needsRange) s else null,
                                endAyah = if (needsRange) e else null,
                                times = t ?: 1,
                                infinite = infinite,
                                after = after
                            )
                        )
                    },
                    enabled = canApply,
                    modifier = Modifier.weight(1f)
                ) { Text("تطبيق") }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("إلغاء")
                }
            }
        }
    }
}
