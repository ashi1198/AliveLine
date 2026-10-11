package com.aliveline.app.domain

import com.aliveline.app.domain.model.BotCondition
import com.aliveline.app.domain.model.BotType

/**
 * Picks what a bot says. Pure and deterministic: the same inputs always give the same line,
 * so the text never flickers on recomposition. Change [seed] to hear a different line.
 * Tone rule: supportive, never guilt-based.
 */
object BotChatter {

    fun line(
        bot: BotType,
        condition: BotCondition,
        hour: Int,
        pending: Int,
        doneToday: Int,
        seed: Int,
    ): String {
        val pool = when {
            condition == BotCondition.TIRED ||
                    condition == BotCondition.STRUGGLING ||
                    condition == BotCondition.CRITICAL -> low.getValue(bot)
            hour >= 22 || hour < 5 -> night
            doneToday >= 2 || condition == BotCondition.THRIVING -> cheer.getValue(bot)
            pending == 0 -> calm.getValue(bot)
            Math.floorMod(seed, 2) == 0 -> nudge.getValue(bot)
            else -> timeOfDay(hour)
        }
        return pool[Math.floorMod(seed / 2, pool.size)]
    }

    private fun timeOfDay(hour: Int): List<String> = when (hour) {
        in 5..11 -> morning
        in 12..16 -> afternoon
        else -> evening
    }

    private val low = mapOf(
        BotType.BRAIN to listOf(
            "I could use a little focus today. One tiny step is plenty.",
            "Brain fog happens. Want to try just five minutes together?",
            "No pressure. Pick the smallest thing and we'll ease in.",
        ),
        BotType.HEALTH to listOf(
            "I could use some rest. A glass of water and a stretch?",
            "Slow days count too. One gentle thing is enough.",
            "Let's be kind to our body today. Tiny steps.",
        ),
        BotType.SOCIAL to listOf(
            "I'd love a friendly catch-up when you have a moment.",
            "Even a quick hello counts. Want to send one?",
            "No rush. One kind message is a great start.",
        ),
    )

    private val cheer = mapOf(
        BotType.BRAIN to listOf(
            "My thoughts are buzzing. That felt great!",
            "Look at us go! Learning mode is on.",
            "My circuits are happy. Keep that rhythm!",
        ),
        BotType.HEALTH to listOf(
            "Energy up! That was a great move.",
            "Feeling strong. Let's keep this going!",
            "Hydrated, happy and rolling.",
        ),
        BotType.SOCIAL to listOf(
            "Connections feel good! Thanks for that.",
            "Warm and fuzzy over here!",
            "Look at us staying close to people!",
        ),
    )

    private val calm = mapOf(
        BotType.BRAIN to listOf(
            "All quiet up here. Maybe jot down an idea?",
            "Nothing waiting. Want to plan something new to learn?",
        ),
        BotType.HEALTH to listOf(
            "Nothing on the list. A short walk might feel nice.",
            "All clear! Maybe a good stretch?",
        ),
        BotType.SOCIAL to listOf(
            "Nothing planned. Maybe message someone you miss?",
            "All clear! Who could use a hello?",
        ),
    )

    private val nudge = mapOf(
        BotType.BRAIN to listOf(
            "Got a minute to focus? I'm ready when you are.",
            "One study step now, and I'll be glowing.",
        ),
        BotType.HEALTH to listOf(
            "Ready for a little movement? I'm in.",
            "A small healthy habit would make my day.",
        ),
        BotType.SOCIAL to listOf(
            "Someone would love to hear from you.",
            "A quick message could make today brighter.",
        ),
    )

    private val morning = listOf(
        "Good morning! Ready for a gentle start?",
        "Fresh day, fresh page. What's first?",
        "Morning! Let's pick one thing to start.",
    )

    private val afternoon = listOf(
        "Afternoon check-in. How's the day going?",
        "Halfway there. Nice pace.",
        "Still plenty of day left. We've got this.",
    )

    private val evening = listOf(
        "Evening! A calm finish to the day.",
        "Almost done for today. Anything quick left?",
        "Winding down. Rest counts too.",
    )

    private val night = listOf(
        "It's late. Rest is part of the plan too.",
        "Sleepy time soon. Tomorrow's a new start.",
        "Wind down, friend. We'll pick this up tomorrow.",
    )
}