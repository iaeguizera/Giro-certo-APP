package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GiroPrimaryDark,
    secondary = GiroSecondaryDark,
    tertiary = GiroTertiaryDark,
    background = GiroBackgroundDark,
    surface = GiroSurfaceDark,
    onPrimary = Color(0xFF0E0B08),        // High contrast very dark warm black on bright orange primary
    onSecondary = Color(0xFF0E0B08),      // High contrast very dark warm black on secondary
    onBackground = Color(0xFFFAFAF9),     // Bright, clean warm-white for readability
    onSurface = Color(0xFFFAFAF9),        // Bright, clean warm-white for card text
    onSurfaceVariant = Color(0xFFC4B4A9), // Beautiful warm sand-grey for metadata/hints
    primaryContainer = GiroPrimaryDark.copy(alpha = 0.12f),
    onPrimaryContainer = GiroPrimaryDark,
    secondaryContainer = GiroSecondaryDark.copy(alpha = 0.12f),
    onSecondaryContainer = GiroSecondaryDark,
    surfaceVariant = Color(0xFF221A16)    // Slightly lighter warm espresso for input backgrounds and highlights
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    // Force Dark Theme only, removing Light Mode support
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
