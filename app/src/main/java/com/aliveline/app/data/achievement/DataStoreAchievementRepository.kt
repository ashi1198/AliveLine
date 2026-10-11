package com.aliveline.app.data.achievement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.aliveline.app.domain.model.Achievement
import com.aliveline.app.domain.repository.AchievementRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val UNLOCKED_KEY = stringSetPreferencesKey("achievements_unlocked")

@Singleton
class DataStoreAchievementRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : AchievementRepository {

    override val unlocked: Flow<Set<Achievement>> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[UNLOCKED_KEY].orEmpty()
                .mapNotNull { name -> runCatching { Achievement.valueOf(name) }.getOrNull() }
                .toSet()
        }

    override suspend fun markUnlocked(achievements: Set<Achievement>) {
        store.edit { prefs ->
            prefs[UNLOCKED_KEY] = (prefs[UNLOCKED_KEY] ?: emptySet()) + achievements.map { it.name }
        }
    }
}