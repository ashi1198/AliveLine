package com.aliveline.app.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.TimeBlock
import com.aliveline.app.domain.TimeBlockLayout
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.domain.model.completedDate
import com.aliveline.app.domain.model.currentTodos
import com.aliveline.app.domain.model.daysLeft
import com.aliveline.app.domain.model.isDoneOn
import com.aliveline.app.domain.model.isScheduledOn
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.ui.TaskItem
import com.aliveline.app.ui.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapBlockUi(
    val id: Long,
    val title: String,
    val bot: BotType,
    val startMin: Int,
    val endMin: Int,
    val done: Boolean,
    val lane: Int,
    val lanes: Int,
) {
    val durationMin: Int get() = endMin - startMin
}

data class MapUiState(
    val dateLabel: String = "",
    val blocks: List<MapBlockUi> = emptyList(),
    val unscheduled: List<TaskItem> = emptyList(),
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<MapUiState> = repo.observeTasks()
        .map { build(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun onSchedule(taskId: Long, startMin: Int, durationMin: Int) {
        viewModelScope.launch { repo.setSchedule(taskId, startMin, durationMin) }
    }

    fun onMove(block: MapBlockUi, newStartMin: Int) {
        viewModelScope.launch { repo.setSchedule(block.id, newStartMin, block.durationMin) }
    }

    /** A one-time task with a later deadline belongs to its due day, not to today's map. */
    private fun Task.belongsToToday(today: LocalDate, zone: ZoneId): Boolean =
        isScheduledOn(today, zone) &&
                (kind != TaskKind.ONE_TIME || dueAt == null || (daysLeft(today, zone) ?: 0) <= 0)

    private fun build(tasks: List<Task>): MapUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val onMap = tasks.filter { task ->
            task.startMinute != null &&
                    task.belongsToToday(today, zone) &&
                    (!task.isDoneOn(today, zone) || task.completedDate(zone) == today)
        }

        val timeBlocks = onMap.map { task ->
            val start = task.startMinute!!.coerceIn(0, 1439)
            val duration = (task.durationMin ?: 60).coerceAtLeast(15)
            TimeBlock(task.id, start, (start + duration).coerceAtMost(1440))
        }
        val placed = TimeBlockLayout.place(timeBlocks).associateBy { it.id }

        val blocks = onMap.mapNotNull { task ->
            placed[task.id]?.let { p ->
                MapBlockUi(
                    id = task.id,
                    title = task.title,
                    bot = task.bot,
                    startMin = p.startMin,
                    endMin = p.endMin,
                    done = task.isDoneOn(today, zone),
                    lane = p.lane,
                    lanes = p.lanes,
                )
            }
        }

        val unscheduled = tasks.currentTodos(today, zone)
            .filter { it.startMinute == null && it.belongsToToday(today, zone) }
            .map { it.toItem(today, zone) }

        return MapUiState(
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
            blocks = blocks,
            unscheduled = unscheduled,
        )
    }
}