package com.aliveline.app.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private fun Long.toLocalDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun Task.dueDate(zone: ZoneId): LocalDate? = dueAt?.toLocalDate(zone)

fun Task.completedDate(zone: ZoneId): LocalDate? = completedAt?.toLocalDate(zone)

/** One-time tasks stay done forever; recurring tasks and habits are only done for the day they were completed. */
fun Task.isDoneOn(today: LocalDate, zone: ZoneId): Boolean {
    val done = completedDate(zone) ?: return false
    return if (kind == TaskKind.ONE_TIME) true else done == today
}

fun Task.isScheduledOn(date: LocalDate, zone: ZoneId): Boolean = when (kind) {
    TaskKind.ONE_TIME -> true
    else -> when (repeat) {
        Repeat.NONE, Repeat.DAILY -> true
        Repeat.WEEKDAYS -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
        Repeat.WEEKLY -> createdAt.toLocalDate(zone).dayOfWeek == date.dayOfWeek
    }
}

fun Task.daysLeft(today: LocalDate, zone: ZoneId): Int? =
    dueDate(zone)?.let { ChronoUnit.DAYS.between(today, it).toInt() }

fun Task.progressOn(today: LocalDate, zone: ZoneId): Int = when {
    isDoneOn(today, zone) -> 100
    subtasks.isNotEmpty() -> subtasks.count { it.done } * 100 / subtasks.size
    else -> 0
}

/** Tasks the user can still do today, soonest deadline first, undated last. */
fun List<Task>.currentTodos(today: LocalDate, zone: ZoneId): List<Task> =
    filter { it.isScheduledOn(today, zone) && !it.isDoneOn(today, zone) }
        .sortedWith(
            compareBy<Task> { it.dueAt == null }
                .thenBy { it.dueAt ?: Long.MAX_VALUE }
                .thenBy { it.createdAt },
        )

fun List<Task>.completedOn(today: LocalDate, zone: ZoneId): List<Task> =
    filter { it.completedDate(zone) == today }.sortedByDescending { it.completedAt }