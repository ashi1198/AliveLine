package com.aliveline.app.data.sleep

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {

    @Query("SELECT * FROM sleep_sessions ORDER BY startAt DESC")
    fun observeAll(): Flow<List<SleepSessionEntity>>

    @Insert
    suspend fun insert(session: SleepSessionEntity): Long

    @Query(
        "UPDATE sleep_sessions SET endAt = :endAt, usageTracked = :usageTracked, pickups = :pickups, " +
                "screenOnMs = :screenOnMs, firstHalfHourMs = :firstHalfHourMs WHERE id = :id",
    )
    suspend fun finish(
        id: Long,
        endAt: Long,
        usageTracked: Boolean,
        pickups: Int,
        screenOnMs: Long,
        firstHalfHourMs: Long,
    )

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun delete(id: Long)
}