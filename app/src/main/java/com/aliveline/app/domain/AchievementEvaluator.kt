package com.aliveline.app.domain

import com.aliveline.app.domain.model.Achievement
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.completedOn
import java.time.LocalDate
import java.time.ZoneId

/** Pure: turns the current tasks and notes into a progress number per achievement. */
object AchievementEvaluator {

    fun progress(
        tasks: List<Task>,
        notes: List<Note>,
        today: LocalDate,
        zone: ZoneId,
    ): Map<Achievement, Int> {
        val completed = tasks.filter { it.completedAt != null }
        fun completedFor(bot: BotType) = completed.count { it.bot == bot }

        return mapOf(
            Achievement.FIRST_PLAN to tasks.size,
            Achievement.PLANNER to tasks.size,
            Achievement.FIRST_DONE to completed.size,
            Achievement.TEN_DONE to completed.size,
            Achievement.HAT_TRICK to tasks.completedOn(today, zone).size,
            Achievement.BRAIN_BOOST to completedFor(BotType.BRAIN),
            Achievement.HEALTH_HERO to completedFor(BotType.HEALTH),
            Achievement.SOCIAL_BUTTERFLY to completedFor(BotType.SOCIAL),
            Achievement.WHOLE_CREW to BotType.entries.count { completedFor(it) > 0 },
            Achievement.SUBTASK_SLAYER to tasks.sumOf { task -> task.subtasks.count { it.done } },
            Achievement.FIRST_NOTE to notes.size,
            Achievement.COLOR_LOVER to notes.map { it.color }.filter { it != NoteColor.DEFAULT }.distinct().size,
            Achievement.PIN_PRO to notes.count { it.pinned },
        )
    }

    fun unlocked(
        tasks: List<Task>,
        notes: List<Note>,
        today: LocalDate,
        zone: ZoneId,
    ): Set<Achievement> =
        progress(tasks, notes, today, zone).filter { (achievement, value) -> value >= achievement.target }.keys
}