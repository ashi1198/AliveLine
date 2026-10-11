package com.aliveline.app.domain

import java.util.Locale

object TimeFormat {

    /** 570 -> "09:30". Wraps at midnight, so 1440 -> "00:00". */
    fun minuteOfDay(minute: Int): String {
        val m = ((minute % 1440) + 1440) % 1440
        return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)
    }

    /** 90_000 ms -> "01:30"; 3_661_000 ms -> "1:01:01". Rounds up so the clock never shows 00:00 early. */
    fun clock(ms: Long): String {
        val totalSeconds = ((ms.coerceAtLeast(0L)) + 999L) / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    fun durationLabel(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60}"
    }
}