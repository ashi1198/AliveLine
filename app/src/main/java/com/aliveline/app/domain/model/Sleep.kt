package com.aliveline.app.domain.model

/** One night. endAt is null while you are still asleep. */
data class SleepSession(
    val id: Long = 0,
    val startAt: Long,
    val endAt: Long? = null,
    val usageTracked: Boolean = false, // true when phone use was measured for this night
    val pickups: Int = 0,
    val screenOnMs: Long = 0,
    val firstHalfHourMs: Long = 0,
) {
    val isActive: Boolean get() = endAt == null
    val durationMs: Long get() = ((endAt ?: startAt) - startAt).coerceAtLeast(0L)
}

/** The screen turned on or off at this moment. */
data class ScreenEvent(val timeMs: Long, val screenOn: Boolean)

/** What the phone did between "Going to sleep" and "I'm awake". */
data class PhoneUse(
    val pickups: Int,
    val screenOnMs: Long,
    val firstHalfHourMs: Long,
)