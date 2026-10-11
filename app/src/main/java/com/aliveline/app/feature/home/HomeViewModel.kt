package com.aliveline.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.BotConditionCalculator
import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.completedOn
import com.aliveline.app.domain.model.currentTodos
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.TaskItem
import com.aliveline.app.ui.toBotState
import com.aliveline.app.ui.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BotSummary(
    val bot: BotType,
    val condition: BotCondition,
    val pendingCount: Int,
    val state: BotState,
)

data class HomeUiState(
    val greeting: String = "Hello",
    val dateLabel: String = "",
    val bots: List<BotSummary> = BotType.entries.map {
        BotSummary(it, BotCondition.HEALTHY, 0, BotState.IDLE)
    },
    val todos: List<TaskItem> = emptyList(),
    val doneToday: List<TaskItem> = emptyList(),
    val hasAnyTasks: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    private val celebrating = MutableStateFlow<BotType?>(null)

    val uiState: StateFlow<HomeUiState> =
        combine(repo.observeTasks(), celebrating) { tasks, celebratingBot ->
            buildState(tasks, celebratingBot)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onCompleteClicked(taskId: Long, bot: BotType) {
        viewModelScope.launch {
            repo.setTaskCompletedAt(taskId, System.currentTimeMillis())
            celebrating.value = bot
            delay(2_200)
            if (celebrating.value == bot) celebrating.value = null
        }
    }

    private fun buildState(tasks: List<Task>, celebratingBot: BotType?): HomeUiState {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val today = now.toLocalDate()

        val greeting = when {
            now.hour < 12 -> "Good morning"
            now.hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        val bots = BotType.entries.map { bot ->
            val botTasks = tasks.filter { it.bot == bot }
            val condition = BotConditionCalculator.calculate(botTasks, today, zone)
            BotSummary(
                bot = bot,
                condition = condition,
                pendingCount = botTasks.currentTodos(today, zone).size,
                state = if (celebratingBot == bot) BotState.CELEBRATING else condition.toBotState(),
            )
        }

        return HomeUiState(
            greeting = greeting,
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
            bots = bots,
            todos = tasks.currentTodos(today, zone).take(8).map { it.toItem(today, zone) },
            doneToday = tasks.completedOn(today, zone).map { it.toItem(today, zone) },
            hasAnyTasks = tasks.isNotEmpty(),
        )
    }
}