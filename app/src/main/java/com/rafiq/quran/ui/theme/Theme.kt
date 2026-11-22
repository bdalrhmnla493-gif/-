package com.rafiq.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Black = Color(0xFF0A0A0A)
val DarkGreen = Color(0xFF0B3D2E)
val Gold = Color(0xFFD4AF37)
val SoftWhite = Color(0xFFEDEDED)

private val RafiqColors = darkColorScheme(
    primary = Gold,
    onPrimary = Black,
    secondary = DarkGreen,
    onSecondary = SoftWhite,
    background = Black,
    onBackground = SoftWhite,
    surface = DarkGreen,
    onSurface = SoftWhite
)

@Composable
fun RafiqTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RafiqColors,
        content = content
    )
}
