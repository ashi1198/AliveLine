package com.aliveline.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.aliveline.app.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private object SettingsKeys {
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    override val soundEnabled: Flow<Boolean> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[SettingsKeys.SOUND_ENABLED] ?: true }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        store.edit { it[SettingsKeys.SOUND_ENABLED] = enabled }
    }
}