package com.aliveline.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.ReminderRules
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.aliveline.app.domain.model.ReminderStyle
data class ReminderItem(
    val taskId: Long,
    val title: String,
    val bot: BotType,
    val timeLabel: String,
    val whenLabel: String,
    val minute: Int,
    val isAlarm: Boolean = false,
)

data class RemindersUiState(val items: List<ReminderItem> = emptyList())

@HiltViewModel
class RemindersViewModel @Inject constructor(
    repo: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = repo.observeTasks()
        .map { build(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    private fun build(tasks: List<Task>): RemindersUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val items = tasks
            .filter { it.reminderMinute != null && !(it.kind == TaskKind.ONE_TIME && it.completedAt != null) }
            .map { task ->
                val minute = task.reminderMinute ?: 0
                ReminderItem(
                    taskId = task.id,
                    title = task.title,
                    bot = task.bot,
                    timeLabel = TimeFormat.minuteOfDay(minute),
                    whenLabel = whenLabel(task, today, zone),
                    minute = minute,
                    isAlarm = task.reminderStyle == ReminderStyle.ALARM,
                )
            }
            .sortedBy { it.minute }
        return RemindersUiState(items)
    }

    private fun whenLabel(task: Task, today: LocalDate, zone: ZoneId): String {
        if (task.kind != TaskKind.ONE_TIME) {
            return when (task.repeat) {
                Repeat.WEEKDAYS -> "Every weekday"
                Repeat.WEEKLY -> "Every week"
                else -> "Every day"
            }
        }
        val date = ReminderRules.oneTimeFireDate(task, zone) ?: return ""
        return when {
            date.isBefore(today) -> "Time has passed"
            date == today -> "Today"
            date == today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    }
}