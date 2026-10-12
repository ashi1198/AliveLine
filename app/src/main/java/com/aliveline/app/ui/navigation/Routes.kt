package com.aliveline.app.ui.navigation

import com.aliveline.app.domain.model.BotType

object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val NOTES = "notes"
    const val REMINDERS = "reminders"
    const val ACHIEVEMENTS = "achievements"
    const val FOCUS = "focus"

    const val SETTINGS = "settings"
    const val BOT_PATTERN = "bot/{botType}"
    const val TASK_PATTERN = "task/{taskId}"
    const val CREATE_PATTERN = "create?bot={bot}"
    const val NOTE_PATTERN = "note?id={id}&taskId={taskId}"

    fun bot(type: BotType) = "bot/${type.name}"
    fun task(id: Long) = "task/$id"
    fun create(bot: BotType? = null) = if (bot == null) "create" else "create?bot=${bot.name}"

    /** Open a note editor: pass id to edit, taskId to start a new note attached to that task. */
    fun note(id: Long? = null, taskId: Long? = null) = "note?id=${id ?: -1}&taskId=${taskId ?: -1}"
}