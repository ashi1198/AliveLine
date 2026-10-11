package com.aliveline.app.data.task

import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.Subtask
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(default)

fun TaskWithSubtasks.toDomain(): Task = Task(
    id = task.id,
    title = task.title,
    description = task.description,
    bot = enumOrDefault(task.bot, BotType.BRAIN),
    kind = enumOrDefault(task.kind, TaskKind.ONE_TIME),
    repeat = enumOrDefault(task.repeatRule, Repeat.NONE),
    dueAt = task.dueAt,
    completedAt = task.completedAt,
    createdAt = task.createdAt,
    startMinute = task.startMinute,
    durationMin = task.durationMin,
    subtasks = subtasks.sortedBy { it.id }.map { Subtask(it.id, it.title, it.done) },
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    bot = bot.name,
    kind = kind.name,
    repeatRule = repeat.name,
    dueAt = dueAt,
    completedAt = completedAt,
    createdAt = createdAt,
    startMinute = startMinute,
    durationMin = durationMin,
)

fun Subtask.toEntity(taskId: Long): SubtaskEntity =
    SubtaskEntity(id = id, taskId = taskId, title = title, done = done)