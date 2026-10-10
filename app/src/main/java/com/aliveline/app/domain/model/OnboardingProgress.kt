package com.aliveline.app.domain.model

/** Raw, persisted facts about onboarding. The UI never stores these itself. */
data class OnboardingProgress(
    val welcomeSeen: Boolean = false,
    val connected: Set<BotType> = emptySet(),
    val introduced: Set<BotType> = emptySet(),
)

/** Which screen onboarding should show right now. */
sealed interface OnboardingStep {
    data object Welcome : OnboardingStep
    data class ConnectBot(val bot: BotType) : OnboardingStep
    data class MeetBot(val bot: BotType) : OnboardingStep
    data object Done : OnboardingStep
}

/**
 * Pure function: persisted progress -> current step.
 * Because the step is DERIVED from persisted data, rotation, process death and
 * app restarts all land the user on the correct screen with no extra code.
 */
fun OnboardingProgress.currentStep(): OnboardingStep {
    if (!welcomeSeen) return OnboardingStep.Welcome
    for (bot in BotType.entries) {
        if (bot !in connected) return OnboardingStep.ConnectBot(bot)
        if (bot !in introduced) return OnboardingStep.MeetBot(bot)
    }
    return OnboardingStep.Done
}
