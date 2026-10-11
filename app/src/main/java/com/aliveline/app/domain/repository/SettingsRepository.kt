package com.aliveline.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val soundEnabled: Flow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)
}