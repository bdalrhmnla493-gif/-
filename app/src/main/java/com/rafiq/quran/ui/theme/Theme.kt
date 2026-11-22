package com.rafiq.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gold = Color(0xFFC7AD7B)
private val SoftGreen = Color(0xFF78917C)
private val Green = Color(0xFF526D57)
private val Ink = Color(0xFF303A32)
private val Muted = Color(0xFF858C83)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Ink,
    tertiary = SoftGreen,
    background = Color(0xFFF7F6F0),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF0E9),
    onSurfaceVariant = Muted
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AB39D),
    onPrimary = Color(0xFF142018),
    secondary = Gold,
    onSecondary = Color(0xFF211B10),
    tertiary = Color(0xFFB9C8B9),
    background = Color(0xFF101711),
    onBackground = Color(0xFFE9EEE8),
    surface = Color(0xFF19231C),
    onSurface = Color(0xFFE9EEE8),
    surfaceVariant = Color(0xFF263329),
    onSurfaceVariant = Color(0xFFB8C3B9)
)

@Composable
fun RafiqTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
