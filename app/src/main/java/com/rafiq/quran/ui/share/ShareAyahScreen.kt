package com.rafiq.quran.ui.share

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

// نص الآية يُقرأ من مصدر القرآن المعتمد في المشروع ويُعرض كما هو دون أي تعديل.
// هذه الشاشة واجهة اختيار فقط: إنشاء الصورة والمشاركة الفعلية يتمان خارجها
// (مثلًا عبر Canvas/Bitmap وIntent المشاركة) عند استدعاء onShareText أو onShareImage.
data class ShareAyahContent(
    val surahName: String,
    val fromAyah: Int,
    val toAyah: Int,
    val ayahText: String
)

enum class ShareBackground(val label: String) {
    EMERALD("زمردي"),
    IVORY("عاجي"),
    NIGHT("ليلي")
}

data class ShareImageOptions(
    val background: ShareBackground,
    val showAppMark: Boolean
)

@Composable
fun ShareAyahScreen(
    content: ShareAyahContent,
    onCopy: (ShareAyahContent) -> Unit,
    onShareText: (ShareAyahContent) -> Unit,
    onShareImage: (ShareAyahContent, ShareImageOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var background by remember { mutableStateOf(ShareBackground.EMERALD) }
        var showAppMark by remember { mutableStateOf(true) }

        val reference = if (content.fromAyah == content.toAyah)
            "${content.surahName} • الآية ${content.fromAyah}"
        else
            "${content.surahName} • الآيات ${content.fromAyah}–${content.toAyah}"

        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("مشاركة الآية", style = MaterialTheme.typography.headlineSmall)

            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(reference, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    Text(content.ayahText, style = MaterialTheme.typography.titleLarge)
                }
            }

            Text("خلفية الصورة", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareBackground.entries.forEach { bg ->
                    FilterChip(
                        selected = background == bg,
                        onClick = { background = bg },
                        label = { Text(bg.label) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار علامة رفيق القرآن", modifier = Modifier.weight(1f))
                Switch(checked = showAppMark, onCheckedChange = { showAppMark = it })
            }

            Button(
                onClick = { onShareImage(content, ShareImageOptions(background, showAppMark)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("مشاركة كصورة") }
            OutlinedButton(
                onClick = { onShareText(content) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("مشاركة كنص") }
            OutlinedButton(
                onClick = { onCopy(content) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("نسخ") }
        }
    }
}
