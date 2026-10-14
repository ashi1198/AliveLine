package com.aliveline.app.data.note

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.collections.isNotEmpty
import kotlin.collections.map

@Dao
abstract class NoteDao {

    @Transaction
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    abstract fun observeAll(): Flow<List<NoteWithDecorations>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    abstract fun observeById(id: Long): Flow<NoteWithDecorations?>

    @Transaction
    @Query("SELECT * FROM notes WHERE taskId = :taskId ORDER BY pinned DESC, updatedAt DESC")
    abstract fun observeForTask(taskId: Long): Flow<List<NoteWithDecorations>>

    @Upsert
    abstract suspend fun upsert(note: NoteEntity): Long

    @Insert
    abstract suspend fun insertDecorations(decorations: List<NoteDecorationEntity>)

    @Query("DELETE FROM note_decorations WHERE noteId = :noteId")
    abstract suspend fun deleteDecorations(noteId: Long)

    @Query("SELECT content FROM note_decorations WHERE noteId = :noteId AND type = 'PHOTO'")
    abstract suspend fun photoNames(noteId: Long): List<String>

    /** Saves the note and replaces all of its stickers and photos in one transaction. */
    @Transaction
    open suspend fun saveWithDecorations(note: NoteEntity, decorations: List<NoteDecorationEntity>): Long {
        val rowId = upsert(note)
        val id = if (note.id > 0) note.id else rowId
        deleteDecorations(id)
        if (decorations.isNotEmpty()) insertDecorations(decorations.map { it.copy(noteId = id) })
        return id
    }

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    abstract suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    abstract suspend fun delete(id: Long)
}