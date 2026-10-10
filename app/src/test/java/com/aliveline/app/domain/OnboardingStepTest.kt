package com.aliveline.app.domain

import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.OnboardingProgress
import com.aliveline.app.domain.model.OnboardingStep
import com.aliveline.app.domain.model.currentStep
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepTest {

    @Test
    fun freshInstall_showsWelcome() {
        assertEquals(OnboardingStep.Welcome, OnboardingProgress().currentStep())
    }

    @Test
    fun afterWelcome_asksToConnectBrainFirst() {
        val p = OnboardingProgress(welcomeSeen = true)
        assertEquals(OnboardingStep.ConnectBot(BotType.BRAIN), p.currentStep())
    }

    @Test
    fun connectedButNotIntroduced_showsIntro() {
        val p = OnboardingProgress(welcomeSeen = true, connected = setOf(BotType.BRAIN))
        assertEquals(OnboardingStep.MeetBot(BotType.BRAIN), p.currentStep())
    }

    @Test
    fun brainDone_movesToHealth() {
        val p = OnboardingProgress(
            welcomeSeen = true,
            connected = setOf(BotType.BRAIN),
            introduced = setOf(BotType.BRAIN),
        )
        assertEquals(OnboardingStep.ConnectBot(BotType.HEALTH), p.currentStep())
    }

    @Test
    fun allBotsIntroduced_isDone() {
        val all = BotType.entries.toSet()
        val p = OnboardingProgress(welcomeSeen = true, connected = all, introduced = all)
        assertEquals(OnboardingStep.Done, p.currentStep())
    }
}
