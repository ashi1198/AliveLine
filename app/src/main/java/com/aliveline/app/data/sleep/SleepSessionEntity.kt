package com.aliveline.app.data.sleep

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startAt: Long,
    val endAt: Long?,
    val usageTracked: Boolean,
    val pickups: Int,
    val screenOnMs: Long,
    val firstHalfHourMs: Long,
)