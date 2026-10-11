package com.aliveline.app.feature.bot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.BotChatter
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
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BotDetailUiState(
    val bot: BotType,
    val condition: BotCondition = BotCondition.HEALTHY,
    val botState: BotState = BotState.IDLE,
    val chatter: String = "",
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
    private val waving = MutableStateFlow(false)
    private val chatterIndex = MutableStateFlow(0)

    val uiState: StateFlow<BotDetailUiState> =
        combine(repo.observeTasks(), celebrating, waving, chatterIndex) { all, isCelebrating, isWaving, index ->
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            val today = now.toLocalDate()
            val tasks = all.filter { it.bot == bot }
            val condition = BotConditionCalculator.calculate(tasks, today, zone)
            val pending = tasks.currentTodos(today, zone)
            val done = tasks.completedOn(today, zone)

            BotDetailUiState(
                bot = bot,
                condition = condition,
                botState = when {
                    isCelebrating -> BotState.CELEBRATING
                    isWaving -> BotState.WAVING
                    else -> condition.toBotState()
                },
                chatter = BotChatter.line(
                    bot = bot,
                    condition = condition,
                    hour = now.hour,
                    pending = pending.size,
                    doneToday = done.size,
                    seed = today.dayOfYear * 7 + bot.ordinal * 3 + index,
                ),
                todos = pending.map { it.toItem(today, zone) },
                doneToday = done.map { it.toItem(today, zone) },
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

    /** Tapping the bot makes it wave and say something new. */
    fun onBotTapped() {
        chatterIndex.update { it + 1 }
        viewModelScope.launch {
            waving.value = true
            delay(1_400)
            waving.value = false
        }
    }
}