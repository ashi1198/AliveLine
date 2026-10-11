package com.aliveline.app.domain.repository

import com.aliveline.app.domain.model.Achievement
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    val unlocked: Flow<Set<Achievement>>
    suspend fun markUnlocked(achievements: Set<Achievement>)
}