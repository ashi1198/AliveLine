package com.aliveline.app.domain.model

enum class DecorationType {
    STICKER, PHOTO;

    companion object {
        fun fromName(name: String?): DecorationType = entries.firstOrNull { it.name == name } ?: STICKER
    }
}

/**
 * A sticker or photo placed on a note.
 * content: for STICKER an emoji, or "bot:BRAIN" / "bot:HEALTH" / "bot:SOCIAL"; for PHOTO the saved file name.
 * x, y: the item's CENTER as a fraction (0..1) of the board width and height.
 * scale and rotation (degrees) are applied around that center.
 */
data class NoteDecoration(
    val id: Long = 0,
    val type: DecorationType,
    val content: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val z: Int = 0,
)