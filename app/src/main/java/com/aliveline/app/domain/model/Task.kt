package com.aliveline.app.domain.model

enum class TaskKind { ONE_TIME, RECURRING, HABIT }

enum class Repeat { NONE, DAILY, WEEKDAYS, WEEKLY }

data class Subtask(
    val id: Long = 0,
    val title: String,
    val done: Boolean = false,
)

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val bot: BotType,
    val kind: TaskKind = TaskKind.ONE_TIME,
    val repeat: Repeat = Repeat.NONE,
    val dueAt: Long? = null,        // epoch millis (end of the due day)
    val completedAt: Long? = null,  // epoch millis of last completion
    val createdAt: Long = 0,
    val subtasks: List<Subtask> = emptyList(),
)