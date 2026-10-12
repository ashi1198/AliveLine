package com.aliveline.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Background, surface and primary follow the theme and accent chosen in Settings. */
object AliveColors {
    val Background: Color
        @Composable @ReadOnlyComposable get() = LocalAppPalette.current.background

    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalAppPalette.current.surface

    val Primary: Color
        @Composable @ReadOnlyComposable get() = LocalAppPalette.current.primary

    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF9AA3C0)
}