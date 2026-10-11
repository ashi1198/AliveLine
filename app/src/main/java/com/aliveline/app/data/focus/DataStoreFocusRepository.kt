package com.aliveline.app.data.focus

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.FocusMode
import com.aliveline.app.domain.model.FocusSession
import com.aliveline.app.domain.repository.FocusRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private object FocusKeys {
    val STARTED_AT = longPreferencesKey("focus_started_at")
    val TASK_ID = longPreferencesKey("focus_task_id")
    val TITLE = stringPreferencesKey("focus_title")
    val BOT = stringPreferencesKey("focus_bot")
    val MODE = stringPreferencesKey("focus_mode")
    val FOCUS_MIN = intPreferencesKey("focus_focus_min")
    val BREAK_MIN = intPreferencesKey("focus_break_min")
    val ROUNDS = intPreferencesKey("focus_rounds")
}

@Singleton
class DataStoreFocusRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : FocusRepository {

    override val session: Flow<FocusSession?> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs.toSession() }

    override suspend fun start(session: FocusSession) {
        store.edit { prefs ->
            prefs[FocusKeys.STARTED_AT] = session.startedAt
            prefs[FocusKeys.TASK_ID] = session.taskId ?: -1L
            prefs[FocusKeys.TITLE] = session.title
            prefs[FocusKeys.BOT] = session.bot.name
            prefs[FocusKeys.MODE] = session.mode.name
            prefs[FocusKeys.FOCUS_MIN] = session.focusMinutes
            prefs[FocusKeys.BREAK_MIN] = session.breakMinutes
            prefs[FocusKeys.ROUNDS] = session.rounds
        }
    }

    override suspend fun clear() {
        store.edit { prefs ->
            prefs.remove(FocusKeys.STARTED_AT)
            prefs.remove(FocusKeys.TASK_ID)
            prefs.remove(FocusKeys.TITLE)
            prefs.remove(FocusKeys.BOT)
            prefs.remove(FocusKeys.MODE)
            prefs.remove(FocusKeys.FOCUS_MIN)
            prefs.remove(FocusKeys.BREAK_MIN)
            prefs.remove(FocusKeys.ROUNDS)
        }
    }
}

private fun Preferences.toSession(): FocusSession? {
    val startedAt = this[FocusKeys.STARTED_AT] ?: return null
    return FocusSession(
        taskId = this[FocusKeys.TASK_ID]?.takeIf { it > 0 },
        title = this[FocusKeys.TITLE] ?: "Focus",
        bot = runCatching { BotType.valueOf(this[FocusKeys.BOT].orEmpty()) }.getOrDefault(BotType.BRAIN),
        mode = runCatching { FocusMode.valueOf(this[FocusKeys.MODE].orEmpty()) }.getOrDefault(FocusMode.COUNTDOWN),
        startedAt = startedAt,
        focusMinutes = this[FocusKeys.FOCUS_MIN] ?: 25,
        breakMinutes = this[FocusKeys.BREAK_MIN] ?: 0,
        rounds = this[FocusKeys.ROUNDS] ?: 1,
    )
}