package com.aliveline.app.domain.repository

import com.aliveline.app.domain.model.AccentColor
import com.aliveline.app.domain.model.AppSettings
import com.aliveline.app.domain.model.ThemeStyle
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setSoundVolume(volume: Float)
    suspend fun setUserName(name: String)
    suspend fun setTheme(theme: ThemeStyle)
    suspend fun setAccent(accent: AccentColor)
}