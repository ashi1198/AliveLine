package com.aliveline.app.domain.model

enum class Achievement(
    val title: String,
    val description: String,
    val emoji: String,
    val target: Int,
) {
    FIRST_PLAN("First Step", "Create your first task", "\uD83C\uDF31", 1),
    PLANNER("Planner", "Create 5 tasks", "\uD83D\uDDD3\uFE0F", 5),
    FIRST_DONE("Nice Start", "Complete your first task", "\u2B50", 1),
    TEN_DONE("Ten Strong", "Complete 10 tasks", "\uD83C\uDFC5", 10),
    HAT_TRICK("Hat Trick", "Complete 3 tasks in one day", "\uD83C\uDFA9", 3),
    BRAIN_BOOST("Brain Boost", "Complete 3 Brain tasks", "\uD83E\uDDE0", 3),
    HEALTH_HERO("Health Hero", "Complete 3 Health tasks", "\uD83D\uDCAA", 3),
    SOCIAL_BUTTERFLY("Social Butterfly", "Complete 3 Social tasks", "\uD83E\uDD8B", 3),
    WHOLE_CREW("Whole Crew", "Complete a task for every bot", "\uD83E\uDD1D", 3),
    SUBTASK_SLAYER("Step by Step", "Finish 5 subtasks", "\u2705", 5),
    FIRST_NOTE("Thoughtful", "Write your first note", "\uD83D\uDCDD", 1),
    COLOR_LOVER("Colorful Mind", "Use 3 different note colors", "\uD83C\uDFA8", 3),
    PIN_PRO("Pin Pro", "Pin a note", "\uD83D\uDCCC", 1),
}