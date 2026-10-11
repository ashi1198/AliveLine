package com.aliveline.app.domain

import com.aliveline.app.domain.model.FocusBoundary
import com.aliveline.app.domain.model.FocusMode
import com.aliveline.app.domain.model.FocusPhase
import com.aliveline.app.domain.model.FocusSession
import com.aliveline.app.domain.model.FocusSnapshot

/** Pure functions: (session, current time) -> what the timer shows. Easy to unit test. */
object FocusCalculator {
    private const val MINUTE = 60_000L

    fun totalMs(s: FocusSession): Long = when (s.mode) {
        FocusMode.COUNTDOWN -> s.focusMinutes * MINUTE
        FocusMode.POMODORO ->
            (s.rounds - 1).coerceAtLeast(0) * (s.focusMinutes + s.breakMinutes) * MINUTE + s.focusMinutes * MINUTE
    }

    fun snapshot(s: FocusSession, nowMs: Long): FocusSnapshot {
        val elapsed = (nowMs - s.startedAt).coerceAtLeast(0L)
        val total = totalMs(s)
        val rounds = s.rounds.coerceAtLeast(1)
        if (elapsed >= total) return FocusSnapshot(FocusPhase.DONE, 0L, 0L, rounds, rounds)

        return when (s.mode) {
            FocusMode.COUNTDOWN -> FocusSnapshot(FocusPhase.FOCUS, total - elapsed, total, 1, 1)
            FocusMode.POMODORO -> {
                val focusMs = s.focusMinutes * MINUTE
                val breakMs = s.breakMinutes * MINUTE
                val cycle = focusMs + breakMs
                val index = (elapsed / cycle).toInt()
                val pos = elapsed % cycle
                if (pos < focusMs) {
                    FocusSnapshot(FocusPhase.FOCUS, focusMs - pos, focusMs, index + 1, rounds)
                } else {
                    FocusSnapshot(FocusPhase.BREAK, cycle - pos, breakMs, index + 1, rounds)
                }
            }
        }
    }

    /** Moments when something changes, used to schedule notifications. */
    fun boundaries(s: FocusSession): List<FocusBoundary> = when (s.mode) {
        FocusMode.COUNTDOWN ->
            listOf(FocusBoundary(s.startedAt + totalMs(s), "Focus session complete. Nice work!"))
        FocusMode.POMODORO -> buildList {
            val focusMs = s.focusMinutes * MINUTE
            val cycle = (s.focusMinutes + s.breakMinutes) * MINUTE
            for (i in 0 until s.rounds) {
                val focusEnd = s.startedAt + i * cycle + focusMs
                if (i < s.rounds - 1) {
                    add(FocusBoundary(focusEnd, "Time for a break. You earned it."))
                    add(FocusBoundary(s.startedAt + (i + 1) * cycle, "Break's over. Ready for the next round?"))
                } else {
                    add(FocusBoundary(focusEnd, "Focus session complete. Nice work!"))
                }
            }
        }
    }
}