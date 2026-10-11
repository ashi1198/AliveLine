package com.aliveline.app.domain.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val tag: String? = null,
    val pinned: Boolean = false,
    val taskId: Long? = null,   // task this note is attached to (goals come later)
    val color: NoteColor = NoteColor.DEFAULT,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

val Note.displayTitle: String
    get() = title.ifBlank { body.lineSequence().firstOrNull().orEmpty() }.ifBlank { "Untitled" }

/** Pure search + tag filter used by the Notes screen. Keeps the incoming order. */
fun List<Note>.matching(query: String, tag: String?): List<Note> {
    val q = query.trim()
    return filter { note ->
        val tagOk = tag == null || note.tag.equals(tag, ignoreCase = true)
        val textOk = q.isEmpty() ||
                note.title.contains(q, ignoreCase = true) ||
                note.body.contains(q, ignoreCase = true) ||
                (note.tag?.contains(q, ignoreCase = true) == true)
        tagOk && textOk
    }
}