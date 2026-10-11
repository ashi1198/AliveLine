package com.aliveline.app.ui

import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.BotType

fun BotCondition.toBotState(): BotState = when (this) {
    BotCondition.THRIVING -> BotState.HAPPY
    BotCondition.HEALTHY -> BotState.IDLE
    BotCondition.OKAY -> BotState.IDLE
    BotCondition.TIRED -> BotState.TIRED
    BotCondition.STRUGGLING -> BotState.TIRED
    BotCondition.CRITICAL -> BotState.SLEEPY
}

val BotCondition.label: String
    get() = when (this) {
        BotCondition.THRIVING -> "Thriving"
        BotCondition.HEALTHY -> "Healthy"
        BotCondition.OKAY -> "Okay"
        BotCondition.TIRED -> "Tired"
        BotCondition.STRUGGLING -> "Low energy"
        BotCondition.CRITICAL -> "Needs a hand"
    }

/** Supportive, never guilt-based. */
fun BotType.conditionMessage(condition: BotCondition): String = when (condition) {
    BotCondition.THRIVING -> "We're on a roll. Nice rhythm!"
    BotCondition.HEALTHY -> "Feeling good and ready when you are."
    BotCondition.OKAY -> "Steady. One small step would feel great."
    BotCondition.TIRED -> when (this) {
        BotType.BRAIN -> "Your Brain Bot could use a little focus."
        BotType.HEALTH -> "Looks like Health Bot needs some rest."
        BotType.SOCIAL -> "Social Bot would love a friendly catch-up."
    }
    BotCondition.STRUGGLING -> "Let's ease back in, one small thing at a time."
    BotCondition.CRITICAL -> "No pressure. One tiny step and we start recovering."
}