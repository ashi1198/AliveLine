package com.aliveline.app.domain.model

enum class FocusMode { COUNTDOWN, POMODORO }

/**
 * A running focus session. Only the START TIME is stored, never a ticking counter,
 * so the timer stays correct if the app is closed, rotated or killed.
 * COUNTDOWN: focusMinutes is the whole session.
 * POMODORO: focusMinutes / breakMinutes repeat for [rounds] rounds (no break after the last one).
 */
data class FocusSession(
    val taskId: Long?,
    val title: String,
    val bot: BotType,
    val mode: FocusMode,
    val startedAt: Long,
    val focusMinutes: Int,
    val breakMinutes: Int = 0,
    val rounds: Int = 1,
)

enum class FocusPhase { FOCUS, BREAK, DONE }

data class FocusSnapshot(
    val phase: FocusPhase,
    val remainingMs: Long,
    val phaseTotalMs: Long,
    val round: Int,
    val rounds: Int,
) {
    val progress: Float
        get() = if (phaseTotalMs <= 0L) 1f else 1f - remainingMs.toFloat() / phaseTotalMs.toFloat()
}

data class FocusBoundary(val atMs: Long, val message: String)