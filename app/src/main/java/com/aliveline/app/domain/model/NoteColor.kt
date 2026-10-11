package com.aliveline.app.domain.model

/** Stored by name in the database. Unknown or missing names fall back to DEFAULT. */
enum class NoteColor {
    DEFAULT, LAVENDER, MINT, PEACH, ROSE, SKY, LEMON, SLATE;

    companion object {
        fun fromName(name: String?): NoteColor = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}