package com.aliveline.app.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.model.matching
import com.aliveline.app.domain.repository.NoteRepository
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteItem(val note: Note, val taskTitle: String?)

data class NotesUiState(
    val query: String = "",
    val selectedTag: String? = null,
    val tags: List<String> = emptyList(),
    val pinned: List<NoteItem> = emptyList(),
    val others: List<NoteItem> = emptyList(),
    val totalCount: Int = 0,
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val notes: NoteRepository,
    tasks: TaskRepository,
) : ViewModel() {

    // Kept in SavedStateHandle so the search text survives rotation and process death.
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val tag = savedStateHandle.getStateFlow<String?>(KEY_TAG, null)

    val uiState: StateFlow<NotesUiState> =
        combine(notes.observeNotes(), tasks.observeTasks(), query, tag) { all, taskList, q, selectedTag ->
            val titles = taskList.associate { it.id to it.title }
            val visible = all.matching(q, selectedTag)
            fun item(note: Note) = NoteItem(note, note.taskId?.let { titles[it] })
            NotesUiState(
                query = q,
                selectedTag = selectedTag,
                tags = all.mapNotNull { it.tag }.distinct().sorted(),
                pinned = visible.filter { it.pinned }.map(::item),
                others = visible.filter { !it.pinned }.map(::item),
                totalCount = all.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun onQueryChange(value: String) { savedStateHandle[KEY_QUERY] = value }

    fun onTagSelected(value: String?) { savedStateHandle[KEY_TAG] = value }

    fun onTogglePin(note: Note) {
        viewModelScope.launch { notes.setPinned(note.id, !note.pinned) }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_TAG = "tag"
    }
}