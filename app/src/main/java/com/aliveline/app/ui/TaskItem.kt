package com.aliveline.app.ui

import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.domain.model.completedDate
import com.aliveline.app.domain.model.daysLeft
import com.aliveline.app.domain.model.isDoneOn
import com.aliveline.app.domain.model.progressOn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What a task row needs to display. Built from the domain Task. */
data class TaskItem(
    val id: Long,
    val title: String,
    val bot: BotType,
    val dueLabel: String?,
    val progress: Int,
    val doneAtLabel: String?,
)

fun Task.dueLabelOn(today: LocalDate, zone: ZoneId): String? {
    if (isDoneOn(today, zone)) {
        return if (kind == TaskKind.ONE_TIME) "Completed" else "Done today"
    }
    if (kind != TaskKind.ONE_TIME) {
        return when (repeat) {
            Repeat.WEEKDAYS -> "Every weekday"
            Repeat.WEEKLY -> "Every week"
            else -> "Every day"
        }
    }
    val d = daysLeft(today, zone) ?: return null
    return when {
        d < 0 -> if (d == -1) "Was due yesterday" else "Was due ${-d} days ago"
        d == 0 -> "Due today"
        d == 1 -> "Due tomorrow"
        else -> "$d days left"
    }
}

fun Task.toItem(today: LocalDate, zone: ZoneId): TaskItem {
    val doneToday = completedDate(zone) == today
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    return TaskItem(
        id = id,
        title = title,
        bot = bot,
        dueLabel = dueLabelOn(today, zone),
        progress = progressOn(today, zone),
        doneAtLabel = if (doneToday) {
            completedAt?.let { formatter.format(Instant.ofEpochMilli(it).atZone(zone)) }
        } else {
            null
        },
    )
}