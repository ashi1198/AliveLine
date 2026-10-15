package com.aliveline.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.AccentColor
import com.aliveline.app.domain.model.AppSettings
import com.aliveline.app.domain.model.ReminderStyle
import com.aliveline.app.domain.model.ThemeStyle
import com.aliveline.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    /** Null until DataStore has delivered the saved settings. */
    val settings: StateFlow<AppSettings?> = repo.settings
        .map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onNameChange(name: String) { viewModelScope.launch { repo.setUserName(name) } }
    fun onSoundEnabledChange(enabled: Boolean) { viewModelScope.launch { repo.setSoundEnabled(enabled) } }
    fun onVolumeChange(volume: Float) { viewModelScope.launch { repo.setSoundVolume(volume) } }
    fun onThemeChange(theme: ThemeStyle) { viewModelScope.launch { repo.setTheme(theme) } }
    fun onAccentChange(accent: AccentColor) { viewModelScope.launch { repo.setAccent(accent) } }
    fun onReminderStyleChange(style: ReminderStyle) { viewModelScope.launch { repo.setReminderStyle(style) } }
}