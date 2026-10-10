package com.aliveline.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.OnboardingProgress
import com.aliveline.app.domain.model.OnboardingStep
import com.aliveline.app.domain.model.currentStep
import com.aliveline.app.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OnboardingUiState {
    /** DataStore hasn't emitted yet; show nothing so we don't flash the wrong screen. */
    data object Loading : OnboardingUiState
    data class Ready(val step: OnboardingStep) : OnboardingUiState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: OnboardingRepository,
) : ViewModel() {

    val uiState: StateFlow<OnboardingUiState> = repo.progress
        .map<OnboardingProgress, OnboardingUiState> { OnboardingUiState.Ready(it.currentStep()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState.Loading)

    fun onGetStarted() { viewModelScope.launch { repo.markWelcomeSeen() } }
    fun onBotConnected(bot: BotType) { viewModelScope.launch { repo.connect(bot) } }
    fun onIntroFinished(bot: BotType) { viewModelScope.launch { repo.markIntroduced(bot) } }
    fun onSkip() { viewModelScope.launch { repo.skipAll() } }
    fun onResetForDev() { viewModelScope.launch { repo.reset() } }
}
