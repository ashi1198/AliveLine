package com.aliveline.app.feature.notes

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.data.photo.PhotoStore
import com.aliveline.app.domain.DecorationMath
import com.aliveline.app.domain.model.DecorationType
import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.domain.model.NoteDecoration
import com.aliveline.app.domain.repository.NoteRepository
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val title: String = "",
    val body: String = "",
    val tag: String = "",
    val pinned: Boolean = false,
    val color: NoteColor = NoteColor.DEFAULT,
    val taskId: Long? = null,
    val linkedTaskTitle: String? = null,
    val isNew: Boolean = true,
    val loaded: Boolean = false,
    val decorations: List<NoteDecoration> = emptyList(),
    val selectedId: Long? = null,
    val importing: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() || body.isNotBlank() || decorations.isNotEmpty()
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notes: NoteRepository,
    private val tasks: TaskRepository,
    val photoStore: PhotoStore,
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("id") ?: -1L
    private val initialTaskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    private var createdAt: Long = 0L

    // Decorations that have not been saved yet use temporary NEGATIVE ids.
    private var nextTempId = -1L
    private val addedPhotos = mutableSetOf<String>()
    private var saved = false

    private val _state = MutableStateFlow(NoteEditorUiState(taskId = initialTaskId, isNew = noteId <= 0))
    val state: StateFlow<NoteEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (noteId > 0) {
                notes.observeNote(noteId).first()?.let { n ->
                    createdAt = n.createdAt
                    _state.update {
                        it.copy(
                            title = n.title,
                            body = n.body,
                            tag = n.tag.orEmpty(),
                            pinned = n.pinned,
                            color = n.color,
                            taskId = n.taskId,
                            decorations = n.decorations,
                            isNew = false,
                        )
                    }
                }
            }
            _state.value.taskId?.let { id ->
                val task = tasks.observeTask(id).first()
                _state.update { it.copy(linkedTaskTitle = task?.title) }
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    override fun onCleared() {
        // Leaving without saving: photos copied during this session would be orphans.
        if (!saved) addedPhotos.forEach(photoStore::delete)
        super.onCleared()
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onBodyChange(v: String) = _state.update { it.copy(body = v) }
    fun onTagChange(v: String) = _state.update { it.copy(tag = v) }
    fun onPinToggled() = _state.update { it.copy(pinned = !it.pinned) }
    fun onColorChange(v: NoteColor) = _state.update { it.copy(color = v) }
    fun onDetach() = _state.update { it.copy(taskId = null, linkedTaskTitle = null) }

    // ----- stickers and photos -----

    fun addSticker(content: String) = addDecoration(DecorationType.STICKER, content)

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            val name = photoStore.import(uri)
            if (name != null) {
                addedPhotos += name
                addDecoration(DecorationType.PHOTO, name)
            }
            _state.update { it.copy(importing = false) }
        }
    }

    private fun addDecoration(type: DecorationType, content: String) {
        val id = nextTempId--
        _state.update { s ->
            val top = (s.decorations.maxOfOrNull { it.z } ?: -1) + 1
            val decoration = NoteDecoration(
                id = id,
                type = type,
                content = content,
                x = 0.5f + (Random.nextFloat() * 0.3f - 0.15f),
                y = 0.5f + (Random.nextFloat() * 0.3f - 0.15f),
                scale = 1f,
                rotation = Random.nextFloat() * 20f - 10f,
                z = top,
            )
            s.copy(decorations = s.decorations + decoration, selectedId = id)
        }
    }

    fun onSelect(id: Long?) = _state.update { it.copy(selectedId = id) }

    fun onTransform(id: Long, dx: Float, dy: Float, zoom: Float, rotationDeg: Float) = _state.update { s ->
        s.copy(
            selectedId = id,
            decorations = s.decorations.map {
                if (it.id == id) DecorationMath.transform(it, dx, dy, zoom, rotationDeg) else it
            },
        )
    }

    fun onBringToFront() = _state.update { s ->
        val id = s.selectedId ?: return@update s
        s.copy(decorations = DecorationMath.bringToFront(s.decorations, id))
    }

    fun onDuplicate() {
        val newId = nextTempId--
        _state.update { s ->
            val id = s.selectedId ?: return@update s
            s.copy(decorations = DecorationMath.duplicate(s.decorations, id, newId), selectedId = newId)
        }
    }

    fun onDeleteSelected() = _state.update { s ->
        val id = s.selectedId ?: return@update s
        s.copy(decorations = s.decorations.filter { it.id != id }, selectedId = null)
    }

    // ----- save / delete -----

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        val now = System.currentTimeMillis()

        val note = Note(
            id = if (noteId > 0) noteId else 0,
            title = s.title.trim(),
            body = s.body.trim(),
            tag = s.tag.trim().removePrefix("#").ifBlank { null },
            pinned = s.pinned,
            taskId = s.taskId,
            color = s.color,
            createdAt = if (createdAt == 0L) now else createdAt,
            updatedAt = now,
            decorations = DecorationMath.normalizeZ(s.decorations),
        )
        val usedPhotos = s.decorations.filter { it.type == DecorationType.PHOTO }.map { it.content }.toSet()

        viewModelScope.launch {
            notes.save(note)
            saved = true
            (addedPhotos - usedPhotos).forEach(photoStore::delete)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (noteId > 0) notes.delete(noteId)
            onDeleted()
        }
    }
}