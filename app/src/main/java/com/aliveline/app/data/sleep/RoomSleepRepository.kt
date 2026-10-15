package com.aliveline.app.data.sleep

import com.aliveline.app.domain.model.PhoneUse
import com.aliveline.app.domain.model.SleepSession
import com.aliveline.app.domain.repository.SleepRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomSleepRepository @Inject constructor(
    private val dao: SleepDao,
) : SleepRepository {

    override fun observeSessions(): Flow<List<SleepSession>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun start(startAt: Long): Long = dao.insert(
        SleepSessionEntity(
            startAt = startAt,
            endAt = null,
            usageTracked = false,
            pickups = 0,
            screenOnMs = 0,
            firstHalfHourMs = 0,
        ),
    )

    override suspend fun finish(id: Long, endAt: Long, usageTracked: Boolean, phoneUse: PhoneUse?) =
        dao.finish(
            id = id,
            endAt = endAt,
            usageTracked = usageTracked,
            pickups = phoneUse?.pickups ?: 0,
            screenOnMs = phoneUse?.screenOnMs ?: 0L,
            firstHalfHourMs = phoneUse?.firstHalfHourMs ?: 0L,
        )

    override suspend fun delete(id: Long) = dao.delete(id)
}

private fun SleepSessionEntity.toDomain(): SleepSession = SleepSession(
    id = id,
    startAt = startAt,
    endAt = endAt,
    usageTracked = usageTracked,
    pickups = pickups,
    screenOnMs = screenOnMs,
    firstHalfHourMs = firstHalfHourMs,
)