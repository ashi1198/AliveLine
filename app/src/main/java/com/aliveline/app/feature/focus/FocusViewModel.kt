package com.aliveline.app.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.FocusMode
import com.aliveline.app.domain.model.FocusSession
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.focus.FocusController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FocusUiState {
    data object Loading : FocusUiState
    data object None : FocusUiState
    data class Active(val session: FocusSession) : FocusUiState
}

data class FocusChoice(
    val mode: FocusMode,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val rounds: Int,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val controller: FocusController,
    private val tasks: TaskRepository,
) : ViewModel() {

    val ui: StateFlow<FocusUiState> = controller.session
        .map<FocusSession?, FocusUiState> { s -> if (s == null) FocusUiState.None else FocusUiState.Active(s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState.Loading)

    fun start(request: StartFocusRequest, choice: FocusChoice, onStarted: () -> Unit) {
        viewModelScope.launch {
            controller.start(
                FocusSession(
                    taskId = request.taskId,
                    title = request.title,
                    bot = request.bot,
                    mode = choice.mode,
                    startedAt = System.currentTimeMillis(),
                    focusMinutes = choice.focusMinutes.coerceAtLeast(1),
                    breakMinutes = choice.breakMinutes.coerceAtLeast(0),
                    rounds = choice.rounds.coerceAtLeast(1),
                ),
            )
            onStarted()
        }
    }

    fun stop(onStopped: () -> Unit) {
        viewModelScope.launch {
            controller.stop()
            onStopped()
        }
    }

    fun completeTaskAndStop(taskId: Long?, onDone: () -> Unit) {
        viewModelScope.launch {
            if (taskId != null) tasks.setTaskCompletedAt(taskId, System.currentTimeMillis())
            controller.stop()
            onDone()
        }
    }
}