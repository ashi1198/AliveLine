package com.aliveline.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.SoundViewModel
import com.aliveline.app.audio.tapSound
import com.aliveline.app.domain.model.OnboardingStep
import com.aliveline.app.feature.onboarding.ConnectBotScreen
import com.aliveline.app.feature.onboarding.MeetBotScreen
import com.aliveline.app.feature.onboarding.OnboardingUiState
import com.aliveline.app.feature.onboarding.OnboardingViewModel
import com.aliveline.app.feature.onboarding.WelcomeScreen
import com.aliveline.app.ui.navigation.MainScaffold
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun AliveLineApp(viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sound = hiltViewModel<SoundViewModel>().player

    CompositionLocalProvider(LocalSoundPlayer provides sound) {
        Box(
            Modifier
                .fillMaxSize()
                .tapSound(sound)
                .background(AliveColors.Background)
                .safeDrawingPadding(),
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                label = "root",
            ) { state ->
                when (state) {
                    OnboardingUiState.Loading -> Box(Modifier.fillMaxSize())
                    is OnboardingUiState.Ready -> when (val step = state.step) {
                        OnboardingStep.Welcome -> WelcomeScreen(onGetStarted = viewModel::onGetStarted)
                        is OnboardingStep.ConnectBot -> ConnectBotScreen(
                            bot = step.bot,
                            onConnected = { viewModel.onBotConnected(step.bot) },
                            onSkip = viewModel::onSkip,
                        )
                        is OnboardingStep.MeetBot -> MeetBotScreen(
                            bot = step.bot,
                            onContinue = { viewModel.onIntroFinished(step.bot) },
                        )
                        OnboardingStep.Done -> MainScaffold(onReplayOnboarding = viewModel::onResetForDev)
                    }
                }
            }
        }
    }
}