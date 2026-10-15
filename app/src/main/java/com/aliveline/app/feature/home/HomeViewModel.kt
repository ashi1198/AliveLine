package com.aliveline.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.BotChatter
import com.aliveline.app.domain.BotConditionCalculator
import com.aliveline.app.domain.HistoryRules
import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Completion
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.completedOn
import com.aliveline.app.domain.model.currentTodos
import com.aliveline.app.domain.repository.SettingsRepository
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BotSummary(
    val bot: BotType,
    val condition: BotCondition,
    val pendingCount: Int,
    val state: BotState,
    val doneCount: Int = 0,
)

data class ChatterUi(val bot: BotType, val text: String)

data class HomeUiState(
    val greeting: String = "Hello",
    val dateLabel: String = "",
    val streak: Int = 0,
    val recap: String? = null,
    val bots: List<BotSummary> = BotType.entries.map {
        BotSummary(it, BotCondition.HEALTHY, 0, BotState.IDLE)
    },
    val chatter: ChatterUi? = null,
    val todos: List<TaskItem> = emptyList(),
    val doneToday: List<TaskItem> = emptyList(),
    val hasAnyTasks: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TaskRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val celebrating = MutableStateFlow<BotType?>(null)
    private val chatterIndex = MutableStateFlow(0)
    private val userName = settings.settings.map { it.userName }.distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> =
        combine(
            repo.observeTasks(),
            repo.observeCompletions(),
            celebrating,
            chatterIndex,
            userName,
        ) { tasks, completions, celebratingBot, index, name ->
            buildState(tasks, completions, celebratingBot, index, name)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onCompleteClicked(taskId: Long, bot: BotType) {
        viewModelScope.launch {
            repo.setTaskCompletedAt(taskId, System.currentTimeMillis())
            celebrating.value = bot
            delay(2_200)
            if (celebrating.value == bot) celebrating.value = null
        }
    }

    /** Tapping the speech bubble hears from the next bot. */
    fun onChatterTapped() {
        chatterIndex.update { it + 1 }
    }

    private fun buildState(
        tasks: List<Task>,
        completions: List<Completion>,
        celebratingBot: BotType?,
        chatterIdx: Int,
        name: String,
    ): HomeUiState {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val today = now.toLocalDate()

        val timeGreeting = when {
            now.hour < 12 -> "Good morning"
            now.hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val greeting = if (name.isBlank()) timeGreeting else "$timeGreeting, ${name.trim()}"

        val missed = HistoryRules.missedByBot(tasks, completions, today, zone)

        val bots = BotType.entries.map { bot ->
            val botTasks = tasks.filter { it.bot == bot }
            val condition = BotConditionCalculator.calculate(botTasks, today, zone, missed.getValue(bot))
            BotSummary(
                bot = bot,
                condition = condition,
                pendingCount = botTasks.currentTodos(today, zone).size,
                state = if (celebratingBot == bot) BotState.CELEBRATING else condition.toBotState(),
                doneCount = botTasks.completedOn(today, zone).size,
            )
        }

        // The bot that could use the most care speaks first; tapping moves on to the others.
        val ordered = bots.sortedByDescending { it.condition.ordinal }
        val speaker = ordered[Math.floorMod(chatterIdx, ordered.size)]
        val chatter = ChatterUi(
            bot = speaker.bot,
            text = BotChatter.line(
                bot = speaker.bot,
                condition = speaker.condition,
                hour = now.hour,
                pending = speaker.pendingCount,
                doneToday = speaker.doneCount,
                seed = today.dayOfYear * 7 + chatterIdx,
            ),
        )

        return HomeUiState(
            greeting = greeting,
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
            streak = HistoryRules.streak(completions, today),
            recap = HistoryRules.recapLine(
                done = HistoryRules.doneOn(completions, today.minusDays(1)),
                missed = missed.values.sum(),
            ),
            bots = bots,
            chatter = chatter,
            todos = tasks.currentTodos(today, zone).take(8).map { it.toItem(today, zone) },
            doneToday = tasks.completedOn(today, zone).map { it.toItem(today, zone) },
            hasAnyTasks = tasks.isNotEmpty(),
        )
    }
}