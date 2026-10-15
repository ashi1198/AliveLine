package com.aliveline.app.domain

import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.completedOn
import com.aliveline.app.domain.model.currentTodos
import com.aliveline.app.domain.model.daysLeft
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure, easy-to-test wellbeing score for one bot.
 * Completing things lifts the score; overdue items weigh most; nothing here is punishing:
 * with no tasks a bot is simply Healthy, and a couple of finished tasks always recovers it.
 *
 * [missedYesterday] is how many habits or recurring tasks of this bot slipped by yesterday.
 * It only weighs a little (5 points each, at most 3 counted).
 */
object BotConditionCalculator {
    fun calculate(
        tasks: List<Task>,
        today: LocalDate,
        zone: ZoneId,
        missedYesterday: Int = 0,
    ): BotCondition {
        val done = tasks.completedOn(today, zone).size
        val pending = tasks.currentTodos(today, zone)
        val overdue = pending.count { (it.daysLeft(today, zone) ?: 0) < 0 }
        val dueSoon = pending.size - overdue

        val score = (
                70 + 15 * done - 20 * overdue - 5 * dueSoon - 5 * missedYesterday.coerceIn(0, 3)
                ).coerceIn(0, 100)
        return when {
            score >= 90 -> BotCondition.THRIVING
            score >= 70 -> BotCondition.HEALTHY
            score >= 50 -> BotCondition.OKAY
            score >= 35 -> BotCondition.TIRED
            score >= 20 -> BotCondition.STRUGGLING
            else -> BotCondition.CRITICAL
        }
    }
}