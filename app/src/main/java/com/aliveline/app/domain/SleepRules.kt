package com.aliveline.app.domain

import com.aliveline.app.domain.model.PhoneUse
import com.aliveline.app.domain.model.ScreenEvent

/** Pure rules for the sleep tracker: phone use from screen events, labels and a supportive summary. */
object SleepRules {
    const val FIRST_WINDOW_MS = 30 * 60_000L

    /**
     * The screen is on when you tap "Going to sleep", so we start with it on.
     * - screenOnMs: all the time the screen was on between the two taps.
     * - firstHalfHourMs: the part of that inside the first 30 minutes (how long it took to put the phone down).
     * - pickups: how many times the screen turned on after that. The last time you wake the phone to tap
     *   "I'm awake" is not a pickup.
     */
    fun analyze(startMs: Long, endMs: Long, events: List<ScreenEvent>): PhoneUse {
        val windowEnd = minOf(endMs, startMs + FIRST_WINDOW_MS)
        var total = 0L
        var first = 0L

        fun addInterval(from: Long, to: Long) {
            val a = maxOf(from, startMs)
            val b = minOf(to, endMs)
            if (b > a) total += b - a
            val c = minOf(to, windowEnd)
            if (c > a) first += c - a
        }

        var on = true
        var lastOn = startMs
        var lastOnWasPickup = false
        var pickups = 0

        events
            .filter { it.timeMs in startMs..endMs }
            .sortedBy { it.timeMs }
            .forEach { event ->
                if (!event.screenOn && on) {
                    addInterval(lastOn, event.timeMs)
                    on = false
                } else if (event.screenOn && !on) {
                    on = true
                    lastOn = event.timeMs
                    lastOnWasPickup = true
                    pickups++
                }
            }

        if (on) {
            if (lastOnWasPickup) pickups-- else addInterval(lastOn, endMs)
        }
        return PhoneUse(pickups.coerceAtLeast(0), total, first)
    }

    /** 7 h 35 min, 45 min, 8 h. */
    fun durationLabel(ms: Long): String {
        val totalMinutes = (ms.coerceAtLeast(0L) / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> "$minutes min"
            minutes == 0 -> "$hours h"
            else -> "$hours h $minutes min"
        }
    }

    fun screenLabel(ms: Long): String = if (ms < 60_000L) "under a minute" else durationLabel(ms)

    /** Supportive, never scolding. */
    fun message(durationMs: Long, phone: PhoneUse?): String {
        val hours = durationMs / 3_600_000.0
        return when {
            hours < 5.0 -> "Looks like we need a little more rest today. Be gentle with yourself."
            hours < 7.0 -> "A bit short, but you made it through. An earlier wind-down could help tonight."
            hours <= 9.0 ->
                if (phone != null && phone.firstHalfHourMs > 15 * 60_000L) {
                    "A solid night. Putting the phone down a little sooner could make it even better."
                } else {
                    "A solid night. Nice rhythm!"
                }
            else -> "A long rest. I hope you feel recharged."
        }
    }
}