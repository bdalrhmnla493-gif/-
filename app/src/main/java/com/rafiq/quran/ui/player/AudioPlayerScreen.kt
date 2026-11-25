package com.rafiq.quran.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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

// حالة التشغيل تأتي من مشغل صوتي حقيقي (Media3 أو ما يستخدمه المشروع) ومن مصدر مرخّص فقط.
// هذه الشاشة واجهة تحكم فقط: لا تشغّل صوتًا ولا تنشئ تسجيلات بنفسها.
// إذا لم يتوفر مصدر مرخّص تُعرض حالة "غير متاح".
data class NowPlaying(
    val reciterName: String,
    val surahName: String,
    val currentAyah: Int?,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val speed: Float,
    val sourceName: String
)

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data object Unavailable : PlayerUiState
    data class Ready(val playing: NowPlaying) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

@Composable
fun AudioPlayerScreen(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (state) {
            PlayerUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            PlayerUiState.Unavailable -> Box(modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "التلاوة غير متاحة حاليًا",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is PlayerUiState.Error -> Box(modifier.fillMaxSize(), Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is PlayerUiState.Ready -> PlayerContent(
                state.playing, onPlayPause, onNext, onPrevious,
                onSeekBack, onSeekForward, onSeekTo, onSpeedChange, modifier
            )
        }
    }
}

@Composable
private fun PlayerContent(
    p: NowPlaying,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = p.durationMs.coerceAtLeast(1L)
    val fraction = dragging ?: (p.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f)

    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("سورة ${p.surahName}", style = MaterialTheme.typography.headlineSmall)
        Text(p.reciterName, style = MaterialTheme.typography.titleMedium)
        p.currentAyah?.let {
            Text(
                "الآية $it",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = fraction,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { onSeekTo((it * duration).toLong()) }
                    dragging = null
                }
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(p.positionMs), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(formatTime(p.durationMs), style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = onSeekBack) { Text("-10ث") }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "السابق")
            }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
                Icon(
                    if (p.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (p.isPlaying) "إيقاف مؤقت" else "تشغيل"
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "التالي")
            }
            TextButton(onClick = onSeekForward) { Text("+10ث") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            speeds.forEach { s ->
                FilterChip(
                    selected = p.speed == s,
                    onClick = { onSpeedChange(s) },
                    label = { Text("${s}x") }
                )
            }
        }

        Text(
            "المصدر: ${p.sourceName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
