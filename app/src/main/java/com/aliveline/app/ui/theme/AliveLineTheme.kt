package com.aliveline.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.aliveline.app.domain.model.AppSettings

@Composable
fun AliveLineTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val palette = remember(settings.theme, settings.accent) {
        buildPalette(settings.theme, settings.accent)
    }
    // Light accents (gold, mint) need dark text on buttons.
    val onPrimary = if (palette.primary.luminance() > 0.45f) Color(0xFF10131F) else Color.White

    val scheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = onPrimary,
        background = palette.background,
        onBackground = Color.White,
        surface = palette.surface,
        onSurface = Color.White,
    )

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}