package com.aliveline.app.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.domain.repository.NoteRepository
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
) {
    val canSave: Boolean get() = title.isNotBlank() || body.isNotBlank()
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notes: NoteRepository,
    private val tasks: TaskRepository,
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("id") ?: -1L
    private val initialTaskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    private var createdAt: Long = 0L

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

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onBodyChange(v: String) = _state.update { it.copy(body = v) }
    fun onTagChange(v: String) = _state.update { it.copy(tag = v) }
    fun onPinToggled() = _state.update { it.copy(pinned = !it.pinned) }
    fun onColorChange(v: NoteColor) = _state.update { it.copy(color = v) }
    fun onDetach() = _state.update { it.copy(taskId = null, linkedTaskTitle = null) }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            notes.save(
                Note(
                    id = if (noteId > 0) noteId else 0,
                    title = s.title.trim(),
                    body = s.body.trim(),
                    tag = s.tag.trim().removePrefix("#").ifBlank { null },
                    pinned = s.pinned,
                    taskId = s.taskId,
                    color = s.color,
                    createdAt = if (createdAt == 0L) now else createdAt,
                    updatedAt = now,
                ),
            )
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