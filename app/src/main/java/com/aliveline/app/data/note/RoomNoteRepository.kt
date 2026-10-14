package com.aliveline.app.data.note

import com.aliveline.app.data.photo.PhotoStore
import com.aliveline.app.domain.model.DecorationType
import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.filter
import kotlin.collections.map

@Singleton
class RoomNoteRepository @Inject constructor(
    private val dao: NoteDao,
    private val photos: PhotoStore,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeNote(id: Long): Flow<Note?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeNotesForTask(taskId: Long): Flow<List<Note>> =
        dao.observeForTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun save(note: Note): Long {
        val before = if (note.id > 0) dao.photoNames(note.id) else emptyList()
        val id = dao.saveWithDecorations(note.toEntity(), note.decorations.map { it.toEntity(noteId = 0) })

        // Photos that are no longer on the note should not keep taking up space.
        val after = note.decorations.filter { it.type == DecorationType.PHOTO }.map { it.content }.toSet()
        (before.toSet() - after).forEach(photos::delete)
        return id
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)

    override suspend fun delete(id: Long) {
        val names = dao.photoNames(id)
        dao.delete(id)
        names.toSet().forEach(photos::delete)
    }
}