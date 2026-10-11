package com.aliveline.app.domain.repository

import com.aliveline.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeNote(id: Long): Flow<Note?>
    fun observeNotesForTask(taskId: Long): Flow<List<Note>>
    suspend fun save(note: Note): Long
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun delete(id: Long)
}