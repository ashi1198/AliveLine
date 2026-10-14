package com.aliveline.app.domain

import com.aliveline.app.domain.model.NoteDecoration

/** Pure editing rules for stickers and photos, so they are easy to unit test. */
object DecorationMath {
    const val MIN_SCALE = 0.3f
    const val MAX_SCALE = 4f

    fun transform(
        d: NoteDecoration,
        dx: Float,
        dy: Float,
        zoom: Float,
        rotationDeg: Float,
    ): NoteDecoration = d.copy(
        x = (d.x + dx).coerceIn(0f, 1f),
        y = (d.y + dy).coerceIn(0f, 1f),
        scale = (d.scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE),
        rotation = (d.rotation + rotationDeg) % 360f,
    )

    fun bringToFront(list: List<NoteDecoration>, id: Long): List<NoteDecoration> {
        val top = (list.maxOfOrNull { it.z } ?: 0) + 1
        return list.map { if (it.id == id) it.copy(z = top) else it }
    }

    /** Turns any z values into a clean 0, 1, 2, ... order. */
    fun normalizeZ(list: List<NoteDecoration>): List<NoteDecoration> =
        list.sortedBy { it.z }.mapIndexed { index, d -> d.copy(z = index) }

    fun duplicate(list: List<NoteDecoration>, id: Long, newId: Long): List<NoteDecoration> {
        val source = list.firstOrNull { it.id == id } ?: return list
        val top = (list.maxOfOrNull { it.z } ?: 0) + 1
        return list + source.copy(
            id = newId,
            x = (source.x + 0.06f).coerceIn(0f, 1f),
            y = (source.y + 0.06f).coerceIn(0f, 1f),
            z = top,
        )
    }
}