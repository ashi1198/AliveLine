package com.aliveline.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aliveline.app.domain.model.AccentColor
import com.aliveline.app.domain.model.AppSettings
import com.aliveline.app.domain.model.ThemeStyle
import com.aliveline.app.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private object SettingsKeys {
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val SOUND_VOLUME = floatPreferencesKey("sound_volume")
    val USER_NAME = stringPreferencesKey("user_name")
    val THEME = stringPreferencesKey("theme_style")
    val ACCENT = stringPreferencesKey("accent_color")
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                userName = prefs[SettingsKeys.USER_NAME].orEmpty(),
                soundEnabled = prefs[SettingsKeys.SOUND_ENABLED] ?: true,
                soundVolume = (prefs[SettingsKeys.SOUND_VOLUME] ?: 0.8f).coerceIn(0f, 1f),
                theme = ThemeStyle.fromName(prefs[SettingsKeys.THEME]),
                accent = AccentColor.fromName(prefs[SettingsKeys.ACCENT]),
            )
        }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        store.edit { it[SettingsKeys.SOUND_ENABLED] = enabled }
    }

    override suspend fun setSoundVolume(volume: Float) {
        store.edit { it[SettingsKeys.SOUND_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    override suspend fun setUserName(name: String) {
        store.edit { it[SettingsKeys.USER_NAME] = name.take(24) }
    }

    override suspend fun setTheme(theme: ThemeStyle) {
        store.edit { it[SettingsKeys.THEME] = theme.name }
    }

    override suspend fun setAccent(accent: AccentColor) {
        store.edit { it[SettingsKeys.ACCENT] = accent.name }
    }
}