package com.aliveline.app.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.data.sleep.ScreenActivityReader
import com.aliveline.app.domain.SleepRules
import com.aliveline.app.domain.model.SleepSession
import com.aliveline.app.domain.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SleepUiState(
    val active: SleepSession? = null,
    val history: List<SleepSession> = emptyList(), // finished nights, newest first
    val hasUsageAccess: Boolean = false,
    val usageSupported: Boolean = true,
    val loaded: Boolean = false,
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repo: SleepRepository,
    private val reader: ScreenActivityReader,
) : ViewModel() {

    private val hasAccess = MutableStateFlow(reader.hasAccess())

    val uiState: StateFlow<SleepUiState> =
        combine(repo.observeSessions(), hasAccess) { sessions, access ->
            SleepUiState(
                active = sessions.firstOrNull { it.isActive },
                history = sessions.filter { !it.isActive },
                hasUsageAccess = access,
                usageSupported = reader.supported,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepUiState())

    /** Called when the screen comes back to the front, e.g. after the user visited system settings. */
    fun refreshAccess() {
        hasAccess.value = reader.hasAccess()
    }

    fun onGoingToSleep() {
        viewModelScope.launch {
            if (uiState.value.active == null) repo.start(System.currentTimeMillis())
        }
    }

    fun onWake() {
        val session = uiState.value.active ?: return
        viewModelScope.launch {
            val end = System.currentTimeMillis()
            val tracked = reader.supported && reader.hasAccess()
            val phoneUse = if (tracked) {
                withContext(Dispatchers.IO) {
                    SleepRules.analyze(session.startAt, end, reader.events(session.startAt, end))
                }
            } else {
                null
            }
            repo.finish(session.id, end, tracked, phoneUse)
        }
    }

    /** Throws away the running night, e.g. when "Going to sleep" was tapped by mistake. */
    fun onCancel() {
        val session = uiState.value.active ?: return
        viewModelScope.launch { repo.delete(session.id) }
    }
}