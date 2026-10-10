package com.aliveline.app.domain.repository

import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.OnboardingProgress
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val progress: Flow<OnboardingProgress>
    suspend fun markWelcomeSeen()
    suspend fun connect(bot: BotType)
    suspend fun markIntroduced(bot: BotType)
    suspend fun skipAll()
    suspend fun reset()
}
