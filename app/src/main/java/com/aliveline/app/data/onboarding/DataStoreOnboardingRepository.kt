package com.aliveline.app.data.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.OnboardingProgress
import com.aliveline.app.domain.repository.OnboardingRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private object Keys {
    val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
    val CONNECTED = stringSetPreferencesKey("connected_bots")
    val INTRODUCED = stringSetPreferencesKey("introduced_bots")
}

@Singleton
class DataStoreOnboardingRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : OnboardingRepository {

    override val progress: Flow<OnboardingProgress> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            OnboardingProgress(
                welcomeSeen = prefs[Keys.WELCOME_SEEN] ?: false,
                connected = prefs[Keys.CONNECTED].toBots(),
                introduced = prefs[Keys.INTRODUCED].toBots(),
            )
        }

    override suspend fun markWelcomeSeen() {
        store.edit { it[Keys.WELCOME_SEEN] = true }
    }

    override suspend fun connect(bot: BotType) {
        store.edit { it[Keys.CONNECTED] = (it[Keys.CONNECTED] ?: emptySet()) + bot.name }
    }

    override suspend fun markIntroduced(bot: BotType) {
        store.edit { it[Keys.INTRODUCED] = (it[Keys.INTRODUCED] ?: emptySet()) + bot.name }
    }

    override suspend fun skipAll() {
        val all = BotType.entries.map { it.name }.toSet()
        store.edit {
            it[Keys.WELCOME_SEEN] = true
            it[Keys.CONNECTED] = all
            it[Keys.INTRODUCED] = all
        }
    }

    override suspend fun reset() {
        store.edit { it.clear() }
    }
}

private fun Set<String>?.toBots(): Set<BotType> =
    orEmpty().mapNotNull { name -> runCatching { BotType.valueOf(name) }.getOrNull() }.toSet()
