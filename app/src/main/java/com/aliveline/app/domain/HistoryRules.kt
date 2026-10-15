package com.aliveline.app.domain

import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Completion
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.domain.model.isScheduledOn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure rules built on the completion history: missed habits, streaks and the gentle daily recap. */
object HistoryRules {

    private fun createdDate(task: Task, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(task.createdAt).atZone(zone).toLocalDate()

    /**
     * Per bot, how many habits or recurring tasks were scheduled yesterday and not finished.
     * One-time tasks never count, and neither does a habit created yesterday (it never had a fair chance).
     */
    fun missedByBot(
        tasks: List<Task>,
        completions: List<Completion>,
        today: LocalDate,
        zone: ZoneId,
    ): Map<BotType, Int> {
        val yesterday = today.minusDays(1)
        val doneIds = completions
            .filter { it.epochDay == yesterday.toEpochDay() }
            .map { it.taskId }
            .toSet()

        return BotType.entries.associateWith { bot ->
            tasks.count { task ->
                task.bot == bot &&
                        task.kind != TaskKind.ONE_TIME &&
                        task.isScheduledOn(yesterday, zone) &&
                        createdDate(task, zone).isBefore(yesterday) &&
                        task.id !in doneIds
            }
        }
    }

    fun doneOn(completions: List<Completion>, day: LocalDate): Int =
        completions.count { it.epochDay == day.toEpochDay() }

    /**
     * Consecutive days with at least one finished task, ending today.
     * If nothing is finished yet today, the streak still counts up to yesterday.
     */
    fun streak(completions: List<Completion>, today: LocalDate): Int {
        val days = completions.map { it.epochDay }.toSet()
        var day = today.toEpochDay()
        if (day !in days) day -= 1
        var count = 0
        while (day in days) {
            count++
            day--
        }
        return count
    }

    /** A supportive look back at yesterday, or null when there is nothing worth saying. Never blames. */
    fun recapLine(done: Int, missed: Int): String? {
        val tasks = if (done == 1) "1 task" else "$done tasks"
        val habits = if (missed == 1) "1 habit" else "$missed habits"
        return when {
            done > 0 && missed == 0 -> "Yesterday you finished $tasks. Nice rhythm!"
            done > 0 -> "Yesterday you finished $tasks, and $habits slipped by. Fresh start today?"
            missed > 0 -> "Yesterday was a quiet one. Want to restart with just one habit?"
            else -> null
        }
    }
}