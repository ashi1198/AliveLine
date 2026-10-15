package com.aliveline.app.feature.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.ReminderStyle
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.Subtask
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.aliveline.app.domain.repository.SettingsRepository
data class EditableSubtask(val id: Long, val title: String, val done: Boolean)

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val bot: BotType = BotType.BRAIN,
    val kind: TaskKind = TaskKind.ONE_TIME,
    val repeat: Repeat = Repeat.DAILY,
    val dueDate: LocalDate? = null,
    val startMinute: Int? = null,
    val durationMin: Int = 60,
    val reminderMinute: Int? = null,
    val reminderStyle: ReminderStyle = ReminderStyle.NOTIFICATION,
    val subtasks: List<EditableSubtask> = emptyList(),
    val newSubtask: String = "",
    val isEditing: Boolean = false,
    val loaded: Boolean = true,
) {
    val canSave: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TaskRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    /** Set only on the edit route ("edit/{taskId}"). */
    private val editId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }

    private val initialBot: BotType = savedStateHandle.get<String>("bot")
        ?.let { name -> runCatching { BotType.valueOf(name) }.getOrNull() }
        ?: BotType.BRAIN

    private var original: Task? = null

    private val _state = MutableStateFlow(
        CreateTaskUiState(bot = initialBot, isEditing = editId != null, loaded = editId == null),
    )
    val state: StateFlow<CreateTaskUiState> = _state.asStateFlow()

    init {
        if (editId != null) {
            viewModelScope.launch {
                val zone = ZoneId.systemDefault()
                val task = repo.observeTask(editId).first()
                original = task
                if (task != null) {
                    _state.value = CreateTaskUiState(
                        title = task.title,
                        description = task.description,
                        bot = task.bot,
                        kind = task.kind,
                        repeat = if (task.repeat == Repeat.NONE) Repeat.DAILY else task.repeat,
                        dueDate = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() },
                        startMinute = task.startMinute,
                        durationMin = task.durationMin ?: 60,
                        reminderMinute = task.reminderMinute,
                        reminderStyle = task.reminderStyle,
                        subtasks = task.subtasks.map { EditableSubtask(it.id, it.title, it.done) },
                        isEditing = true,
                        loaded = true,
                    )
                } else {
                    _state.update { it.copy(loaded = true) }
                }
            }
        }
    }
    init {
        if (editId == null) {
            viewModelScope.launch {
                val style = settings.settings.first().reminderStyle
                _state.update { it.copy(reminderStyle = style) }
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onBotChange(v: BotType) = _state.update { it.copy(bot = v) }
    fun onKindChange(v: TaskKind) = _state.update { it.copy(kind = v) }
    fun onRepeatChange(v: Repeat) = _state.update { it.copy(repeat = v) }
    fun onDueDateChange(v: LocalDate?) = _state.update { it.copy(dueDate = v) }
    fun onStartMinuteChange(v: Int?) = _state.update { it.copy(startMinute = v) }
    fun onDurationChange(v: Int) = _state.update { it.copy(durationMin = v) }
    fun onReminderChange(v: Int?) = _state.update { it.copy(reminderMinute = v) }
    fun onReminderStyleChange(v: ReminderStyle) = _state.update { it.copy(reminderStyle = v) }
    fun onNewSubtaskChange(v: String) = _state.update { it.copy(newSubtask = v) }

    fun onAddSubtask() = _state.update {
        val text = it.newSubtask.trim()
        if (text.isEmpty()) it else it.copy(subtasks = it.subtasks + EditableSubtask(0, text, false), newSubtask = "")
    }

    fun onRemoveSubtask(index: Int) = _state.update {
        it.copy(subtasks = it.subtasks.filterIndexed { i, _ -> i != index })
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        val zone = ZoneId.systemDefault()
        val dueAt = if (s.kind == TaskKind.ONE_TIME) {
            s.dueDate?.atTime(23, 59)?.atZone(zone)?.toInstant()?.toEpochMilli()
        } else {
            null
        }
        val pending = s.newSubtask.trim().takeIf { it.isNotEmpty() }
        val subtasks = (s.subtasks + listOfNotNull(pending?.let { EditableSubtask(0, it, false) }))
            .map { Subtask(id = it.id, title = it.title, done = it.done) }

        viewModelScope.launch {
            val base = original
            if (base == null) {
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
                        reminderMinute = s.reminderMinute,
                        reminderStyle = s.reminderStyle,
                        subtasks = subtasks,
                    ),
                )
            } else {
                repo.updateTask(
                    base.copy(
                        title = s.title.trim(),
                        description = s.description.trim(),
                        bot = s.bot,
                        kind = s.kind,
                        repeat = if (s.kind == TaskKind.ONE_TIME) Repeat.NONE else s.repeat,
                        dueAt = dueAt,
                        startMinute = s.startMinute,
                        durationMin = if (s.startMinute != null) s.durationMin else null,
                        reminderMinute = s.reminderMinute,
                        reminderStyle = s.reminderStyle,
                        subtasks = subtasks,
                    ),
                )
            }
            onSaved()
        }
    }
}