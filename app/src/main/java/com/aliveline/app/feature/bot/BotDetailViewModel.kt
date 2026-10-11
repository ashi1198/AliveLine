package com.aliveline.app.feature.bot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.BotConditionCalculator
import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.completedOn
import com.aliveline.app.domain.model.currentTodos
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.TaskItem
import com.aliveline.app.ui.toBotState
import com.aliveline.app.ui.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BotDetailUiState(
    val bot: BotType,
    val condition: BotCondition = BotCondition.HEALTHY,
    val botState: BotState = BotState.IDLE,
    val todos: List<TaskItem> = emptyList(),
    val doneToday: List<TaskItem> = emptyList(),
)

@HiltViewModel
class BotDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TaskRepository,
) : ViewModel() {

    private val bot: BotType = BotType.valueOf(checkNotNull(savedStateHandle.get<String>("botType")))
    private val celebrating = MutableStateFlow(false)

    val uiState: StateFlow<BotDetailUiState> =
        combine(repo.observeTasks(), celebrating) { all, isCelebrating ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val tasks = all.filter { it.bot == bot }
            val condition = BotConditionCalculator.calculate(tasks, today, zone)
            BotDetailUiState(
                bot = bot,
                condition = condition,
                botState = if (isCelebrating) BotState.CELEBRATING else condition.toBotState(),
                todos = tasks.currentTodos(today, zone).map { it.toItem(today, zone) },
                doneToday = tasks.completedOn(today, zone).map { it.toItem(today, zone) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BotDetailUiState(bot))

    fun onCompleteClicked(taskId: Long) {
        viewModelScope.launch {
            repo.setTaskCompletedAt(taskId, System.currentTimeMillis())
            celebrating.value = true
            delay(2_200)
            celebrating.value = false
        }
    }
}