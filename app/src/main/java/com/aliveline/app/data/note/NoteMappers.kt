package com.aliveline.app.data.note

import com.aliveline.app.domain.model.DecorationType
import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.domain.model.NoteDecoration

fun NoteWithDecorations.toDomain(): Note = Note(
    id = note.id,
    title = note.title,
    body = note.body,
    tag = note.tag,
    pinned = note.pinned,
    taskId = note.taskId,
    color = NoteColor.fromName(note.color),
    createdAt = note.createdAt,
    updatedAt = note.updatedAt,
    decorations = decorations
        .sortedWith(compareBy({ it.z }, { it.id }))
        .map { it.toDomain() },
)

fun NoteDecorationEntity.toDomain(): NoteDecoration = NoteDecoration(
    id = id,
    type = DecorationType.fromName(type),
    content = content,
    x = x,
    y = y,
    scale = scale,
    rotation = rotation,
    z = z,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    tag = tag,
    pinned = pinned,
    taskId = taskId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    color = color.name,
)

/** The id is always 0 so Room creates a fresh row; decorations are replaced as a set on every save. */
fun NoteDecoration.toEntity(noteId: Long): NoteDecorationEntity = NoteDecorationEntity(
    id = 0,
    noteId = noteId,
    type = type.name,
    content = content,
    x = x,
    y = y,
    scale = scale,
    rotation = rotation,
    z = z,
)