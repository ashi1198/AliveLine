package com.aliveline.app.data.note

import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomNoteRepository @Inject constructor(
    private val dao: NoteDao,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeNote(id: Long): Flow<Note?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeNotesForTask(taskId: Long): Flow<List<Note>> =
        dao.observeForTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun save(note: Note): Long {
        val rowId = dao.upsert(note.toEntity())
        return if (note.id > 0) note.id else rowId
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)

    override suspend fun delete(id: Long) = dao.delete(id)
}