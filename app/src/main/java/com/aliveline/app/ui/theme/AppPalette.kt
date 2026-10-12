package com.aliveline.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aliveline.app.domain.model.AccentColor
import com.aliveline.app.domain.model.ThemeStyle

data class AppPalette(val background: Color, val surface: Color, val primary: Color)

val ThemeStyle.backgroundColor: Color
    get() = when (this) {
        ThemeStyle.NAVY -> Color(0xFF0B1020)
        ThemeStyle.MIDNIGHT -> Color(0xFF000000)
        ThemeStyle.PLUM -> Color(0xFF150C20)
        ThemeStyle.FOREST -> Color(0xFF0A1511)
    }

val ThemeStyle.surfaceColor: Color
    get() = when (this) {
        ThemeStyle.NAVY -> Color(0xFF151B30)
        ThemeStyle.MIDNIGHT -> Color(0xFF111119)
        ThemeStyle.PLUM -> Color(0xFF241636)
        ThemeStyle.FOREST -> Color(0xFF13251D)
    }

val ThemeStyle.title: String
    get() = when (this) {
        ThemeStyle.NAVY -> "Navy"
        ThemeStyle.MIDNIGHT -> "Midnight"
        ThemeStyle.PLUM -> "Plum"
        ThemeStyle.FOREST -> "Forest"
    }

val AccentColor.color: Color
    get() = when (this) {
        AccentColor.LAVENDER -> Color(0xFF7C7CF5)
        AccentColor.MINT -> Color(0xFF3FD18F)
        AccentColor.CORAL -> Color(0xFFFF8A5C)
        AccentColor.SKY -> Color(0xFF4DA8FF)
        AccentColor.ROSE -> Color(0xFFFF6B9A)
        AccentColor.GOLD -> Color(0xFFFFC145)
    }

val AccentColor.title: String
    get() = when (this) {
        AccentColor.LAVENDER -> "Lavender"
        AccentColor.MINT -> "Mint"
        AccentColor.CORAL -> "Coral"
        AccentColor.SKY -> "Sky"
        AccentColor.ROSE -> "Rose"
        AccentColor.GOLD -> "Gold"
    }

fun buildPalette(theme: ThemeStyle, accent: AccentColor): AppPalette =
    AppPalette(theme.backgroundColor, theme.surfaceColor, accent.color)

val DefaultPalette: AppPalette = buildPalette(ThemeStyle.NAVY, AccentColor.LAVENDER)

val LocalAppPalette = staticCompositionLocalOf { DefaultPalette }