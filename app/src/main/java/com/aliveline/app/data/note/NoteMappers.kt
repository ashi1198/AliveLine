package com.aliveline.app.data.note

import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.model.NoteColor

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    body = body,
    tag = tag,
    pinned = pinned,
    taskId = taskId,
    color = NoteColor.fromName(color),
    createdAt = createdAt,
    updatedAt = updatedAt,
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