package com.aliveline.app.domain.repository

import com.aliveline.app.domain.model.PhoneUse
import com.aliveline.app.domain.model.SleepSession
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    /** Newest night first. The running one (if any) has endAt = null. */
    fun observeSessions(): Flow<List<SleepSession>>
    suspend fun start(startAt: Long): Long
    suspend fun finish(id: Long, endAt: Long, usageTracked: Boolean, phoneUse: PhoneUse?)
    suspend fun delete(id: Long)
}