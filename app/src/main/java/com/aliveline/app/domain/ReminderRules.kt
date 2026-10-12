package com.aliveline.app.domain

import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.domain.model.dueDate
import com.aliveline.app.domain.model.isDoneOn
import com.aliveline.app.domain.model.isScheduledOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Pure rules for when a reminder fires and whether it should show. Easy to unit test. */
object ReminderRules {

    private fun millisAt(date: LocalDate, minute: Int, zone: ZoneId): Long {
        val m = minute.coerceIn(0, 1439)
        return date.atTime(LocalTime.of(m / 60, m % 60)).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * The one day a one-time task reminds you: its deadline day, or, with no deadline,
     * the first time the reminder time comes around after the task was created.
     */
    fun oneTimeFireDate(task: Task, zone: ZoneId): LocalDate? {
        val minute = task.reminderMinute ?: return null
        task.dueDate(zone)?.let { return it }
        val createdDay = Instant.ofEpochMilli(task.createdAt).atZone(zone).toLocalDate()
        return if (millisAt(createdDay, minute, zone) > task.createdAt) createdDay else createdDay.plusDays(1)
    }

    /** When the next reminder should fire, or null if there is none. */
    fun nextTrigger(task: Task, nowMs: Long, zone: ZoneId): Long? {
        val minute = task.reminderMinute ?: return null

        if (task.kind == TaskKind.ONE_TIME) {
            if (task.completedAt != null) return null
            val date = oneTimeFireDate(task, zone) ?: return null
            return millisAt(date, minute, zone).takeIf { it > nowMs }
        }

        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        for (offset in 0..8L) {
            val date = today.plusDays(offset)
            if (!task.isScheduledOn(date, zone)) continue
            val at = millisAt(date, minute, zone)
            if (at > nowMs) return at
        }
        return null
    }

    /** At fire time: only notify if the task is scheduled today and is still not done. */
    fun shouldNotify(task: Task, today: LocalDate, zone: ZoneId): Boolean =
        task.reminderMinute != null &&
                task.isScheduledOn(today, zone) &&
                !task.isDoneOn(today, zone)

    fun isOneTime(task: Task): Boolean = task.kind == TaskKind.ONE_TIME
}