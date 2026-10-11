package com.aliveline.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.ui.theme.AliveColors

val NoteColor.accent: Color
    get() = when (this) {
        NoteColor.DEFAULT -> Color(0xFF4A5170)
        NoteColor.LAVENDER -> Color(0xFF9B8CFF)
        NoteColor.MINT -> Color(0xFF5FD99B)
        NoteColor.PEACH -> Color(0xFFFF9A5C)
        NoteColor.ROSE -> Color(0xFFFF7A9C)
        NoteColor.SKY -> Color(0xFF5BB8FF)
        NoteColor.LEMON -> Color(0xFFFFD166)
        NoteColor.SLATE -> Color(0xFF8A94B8)
    }

val NoteColor.label: String
    get() = when (this) {
        NoteColor.DEFAULT -> "Default"
        NoteColor.LAVENDER -> "Lavender"
        NoteColor.MINT -> "Mint"
        NoteColor.PEACH -> "Peach"
        NoteColor.ROSE -> "Rose"
        NoteColor.SKY -> "Sky"
        NoteColor.LEMON -> "Lemon"
        NoteColor.SLATE -> "Slate"
    }

/** Background of a note card: the normal surface, softly tinted. */
fun NoteColor.cardColor(): Color =
    if (this == NoteColor.DEFAULT) AliveColors.Surface
    else accent.copy(alpha = 0.20f).compositeOver(AliveColors.Surface)

/** Background of the whole editor screen: a very light wash. */
fun NoteColor.screenTint(): Color =
    if (this == NoteColor.DEFAULT) AliveColors.Background
    else accent.copy(alpha = 0.12f).compositeOver(AliveColors.Background)