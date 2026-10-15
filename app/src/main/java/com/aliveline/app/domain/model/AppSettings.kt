package com.aliveline.app.domain.model

enum class ThemeStyle {
    NAVY, MIDNIGHT, PLUM, FOREST;

    companion object {
        fun fromName(name: String?): ThemeStyle = entries.firstOrNull { it.name == name } ?: NAVY
    }
}

enum class AccentColor {
    LAVENDER, MINT, CORAL, SKY, ROSE, GOLD;

    companion object {
        fun fromName(name: String?): AccentColor = entries.firstOrNull { it.name == name } ?: LAVENDER
    }
}

data class AppSettings(
    val userName: String = "",
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val theme: ThemeStyle = ThemeStyle.NAVY,
    val accent: AccentColor = AccentColor.LAVENDER,
    val reminderStyle: ReminderStyle = ReminderStyle.NOTIFICATION, // what new reminders start as
)