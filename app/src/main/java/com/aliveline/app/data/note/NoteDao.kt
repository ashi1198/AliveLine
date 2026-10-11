package com.aliveline.app.data.note

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE taskId = :taskId ORDER BY pinned DESC, updatedAt DESC")
    fun observeForTask(taskId: Long): Flow<List<NoteEntity>>

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}