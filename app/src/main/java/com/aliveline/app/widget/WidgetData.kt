package com.aliveline.app.widget

import com.aliveline.app.domain.BotConditionCalculator
import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.model.currentTodos
import com.aliveline.app.ui.conditionMessage
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.dueLabelOn
import java.time.LocalDate
import java.time.ZoneId

data class WidgetData(
    val bot: BotType,
    val condition: BotCondition,
    val todoTitle: String?,
    val todoMeta: String?,
    val message: String,
)

/** Pure logic that decides what the widget shows, so it can be unit tested. */
object WidgetDataBuilder {

    /**
     * @param preferredBot when set, the widget shows exactly that bot (its mood and its own top todo).
     * When null, it shows the bot that owns the overall top todo, or the happiest bot if nothing is pending.
     */
    fun build(
        tasks: List<Task>,
        today: LocalDate,
        zone: ZoneId,
        preferredBot: BotType? = null,
    ): WidgetData {
        if (preferredBot != null) return buildFor(preferredBot, tasks, today, zone)

        val conditions = BotType.entries.associateWith { bot ->
            BotConditionCalculator.calculate(tasks.filter { it.bot == bot }, today, zone)
        }

        // The bot that owns the top todo is shown, so the face and the todo always match.
        val top = tasks.currentTodos(today, zone).firstOrNull()
        if (top != null) {
            val condition = conditions.getValue(top.bot)
            return WidgetData(
                bot = top.bot,
                condition = condition,
                todoTitle = top.title,
                todoMeta = top.dueLabelOn(today, zone),
                message = top.bot.conditionMessage(condition),
            )
        }

        // Nothing to do: show the happiest bot (the first one if they are tied).
        val bot = BotType.entries.minByOrNull { conditions.getValue(it).ordinal } ?: BotType.BRAIN
        return WidgetData(
            bot = bot,
            condition = conditions.getValue(bot),
            todoTitle = null,
            todoMeta = null,
            message = if (tasks.isEmpty()) "Tap to add your first task." else "All clear for now. Nice rhythm!",
        )
    }

    private fun buildFor(bot: BotType, tasks: List<Task>, today: LocalDate, zone: ZoneId): WidgetData {
        val botTasks = tasks.filter { it.bot == bot }
        val condition = BotConditionCalculator.calculate(botTasks, today, zone)
        val top = botTasks.currentTodos(today, zone).firstOrNull()

        if (top != null) {
            return WidgetData(
                bot = bot,
                condition = condition,
                todoTitle = top.title,
                todoMeta = top.dueLabelOn(today, zone),
                message = bot.conditionMessage(condition),
            )
        }
        return WidgetData(
            bot = bot,
            condition = condition,
            todoTitle = null,
            todoMeta = null,
            message = if (botTasks.isEmpty()) {
                "No tasks yet. Add one for ${bot.displayName}."
            } else {
                "All clear for now. Nice rhythm!"
            },
        )
    }
}