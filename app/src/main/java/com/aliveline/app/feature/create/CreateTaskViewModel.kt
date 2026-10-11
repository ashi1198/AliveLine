package com.aliveline.app.feature.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.Subtask
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DeadlineOption(val label: String, val days: Long?) {
    NONE("No deadline", null),
    TODAY("Today", 0),
    TOMORROW("Tomorrow", 1),
    IN_3_DAYS("In 3 days", 3),
    NEXT_WEEK("Next week", 7),
}

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val bot: BotType = BotType.BRAIN,
    val kind: TaskKind = TaskKind.ONE_TIME,
    val repeat: Repeat = Repeat.DAILY,
    val deadline: DeadlineOption = DeadlineOption.NONE,
    val startMinute: Int? = null,
    val durationMin: Int = 60,
    val subtasks: List<String> = emptyList(),
    val newSubtask: String = "",
) {
    val canSave: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TaskRepository,
) : ViewModel() {

    private val initialBot: BotType = savedStateHandle.get<String>("bot")
        ?.let { name -> runCatching { BotType.valueOf(name) }.getOrNull() }
        ?: BotType.BRAIN

    private val _state = MutableStateFlow(CreateTaskUiState(bot = initialBot))
    val state: StateFlow<CreateTaskUiState> = _state.asStateFlow()

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onBotChange(v: BotType) = _state.update { it.copy(bot = v) }
    fun onKindChange(v: TaskKind) = _state.update { it.copy(kind = v) }
    fun onRepeatChange(v: Repeat) = _state.update { it.copy(repeat = v) }
    fun onDeadlineChange(v: DeadlineOption) = _state.update { it.copy(deadline = v) }
    fun onStartMinuteChange(v: Int?) = _state.update { it.copy(startMinute = v) }
    fun onDurationChange(v: Int) = _state.update { it.copy(durationMin = v) }
    fun onNewSubtaskChange(v: String) = _state.update { it.copy(newSubtask = v) }

    fun onAddSubtask() = _state.update {
        val text = it.newSubtask.trim()
        if (text.isEmpty()) it else it.copy(subtasks = it.subtasks + text, newSubtask = "")
    }

    fun onRemoveSubtask(index: Int) = _state.update {
        it.copy(subtasks = it.subtasks.filterIndexed { i, _ -> i != index })
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        val zone = ZoneId.systemDefault()
        val dueAt = if (s.kind == TaskKind.ONE_TIME) {
            s.deadline.days?.let { days ->
                LocalDate.now(zone).plusDays(days).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
            }
        } else {
            null
        }
        val pending = s.newSubtask.trim().takeIf { it.isNotEmpty() }
        val allSubtasks = s.subtasks + listOfNotNull(pending)

        viewModelScope.launch {
            repo.addTask(
                Task(
                    title = s.title.trim(),
                    description = s.description.trim(),
                    bot = s.bot,
                    kind = s.kind,
                    repeat = if (s.kind == TaskKind.ONE_TIME) Repeat.NONE else s.repeat,
                    dueAt = dueAt,
                    createdAt = System.currentTimeMillis(),
                    startMinute = s.startMinute,
                    durationMin = if (s.startMinute != null) s.durationMin else null,
                    subtasks = allSubtasks.map { Subtask(title = it) },
                ),
            )
            onSaved()
        }
    }
}