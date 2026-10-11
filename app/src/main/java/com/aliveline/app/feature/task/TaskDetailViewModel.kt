package com.aliveline.app.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.Subtask
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.isDoneOn
import com.aliveline.app.domain.model.progressOn
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.ui.dueLabelOn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TaskDetailUiState {
    data object Loading : TaskDetailUiState
    data object NotFound : TaskDetailUiState
    data class Content(
        val task: Task,
        val isDone: Boolean,
        val progress: Int,
        val dueLabel: String?,
    ) : TaskDetailUiState
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TaskRepository,
) : ViewModel() {

    private val taskId: Long = checkNotNull(savedStateHandle.get<Long>("taskId"))

    val uiState: StateFlow<TaskDetailUiState> = repo.observeTask(taskId)
        .map<Task?, TaskDetailUiState> { task ->
            if (task == null) {
                TaskDetailUiState.NotFound
            } else {
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                TaskDetailUiState.Content(
                    task = task,
                    isDone = task.isDoneOn(today, zone),
                    progress = task.progressOn(today, zone),
                    dueLabel = task.dueLabelOn(today, zone),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState.Loading)

    fun onSubtaskToggled(subtask: Subtask) {
        viewModelScope.launch { repo.setSubtaskDone(subtask.id, !subtask.done) }
    }

    fun onMarkDone() {
        viewModelScope.launch { repo.setTaskCompletedAt(taskId, System.currentTimeMillis()) }
    }

    fun onUndo() {
        viewModelScope.launch { repo.setTaskCompletedAt(taskId, null) }
    }

    fun onDelete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteTask(taskId)
            onDeleted()
        }
    }
}