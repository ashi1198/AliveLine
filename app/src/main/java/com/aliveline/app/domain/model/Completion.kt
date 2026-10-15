package com.aliveline.app.domain.model

/** One finished task on one day. Kept even after the task is deleted, so recaps and streaks stay true. */
data class Completion(
    val taskId: Long,
    val bot: BotType,
    val epochDay: Long,
    val completedAt: Long,
)