package com.rafiq.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gold = Color(0xFFD8B65C)
private val Emerald = Color(0xFF145B45)
private val Navy = Color(0xFF172B3A)

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Color(0xFF211B0D),
    tertiary = Navy,
    background = Color(0xFFF6F4ED),
    onBackground = Color(0xFF18211C),
    surface = Color(0xFFFFFEFA),
    onSurface = Color(0xFF18211C),
    surfaceVariant = Color(0xFFEAE8DE),
    onSurfaceVariant = Color(0xFF59645D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF78B99A),
    onPrimary = Color(0xFF082217),
    secondary = Gold,
    onSecondary = Color(0xFF251D08),
    tertiary = Color(0xFF9EB9CA),
    background = Color(0xFF0B120E),
    onBackground = Color(0xFFE9EFE9),
    surface = Color(0xFF131D17),
    onSurface = Color(0xFFE9EFE9),
    surfaceVariant = Color(0xFF202D25),
    onSurfaceVariant = Color(0xFFB9C6BC)
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
