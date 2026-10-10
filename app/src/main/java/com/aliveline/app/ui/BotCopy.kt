package com.aliveline.app.ui

import com.aliveline.app.domain.model.BotType

val BotType.displayName: String
    get() = when (this) {
        BotType.BRAIN -> "Brain Bot"
        BotType.HEALTH -> "Health Bot"
        BotType.SOCIAL -> "Social Bot"
    }

val BotType.tagline: String
    get() = when (this) {
        BotType.BRAIN -> "Learn \u2022 Focus \u2022 Grow"
        BotType.HEALTH -> "Sleep \u2022 Move \u2022 Recover"
        BotType.SOCIAL -> "Connect \u2022 Share \u2022 Belong"
    }

val BotType.introTitle: String
    get() = when (this) {
        BotType.BRAIN -> "Hello!\nI'm your Brain Bot."
        BotType.HEALTH -> "Hi there!\nI'm your Health Bot."
        BotType.SOCIAL -> "Hey!\nI'm your Social Bot."
    }

val BotType.introBody: String
    get() = when (this) {
        BotType.BRAIN -> "I'll help you learn, focus and grow. Together we'll achieve your goals, one step at a time."
        BotType.HEALTH -> "I'll help you sleep, move and recover, so you have the energy for everything else."
        BotType.SOCIAL -> "I'll help you stay connected with the people and experiences around you."
    }
