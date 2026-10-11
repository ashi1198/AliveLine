package com.aliveline.app.feature.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.palette
import kotlin.random.Random

private class Particle(
    val x0: Float,      // start position as a fraction of width
    val y0: Float,      // start position as a fraction of height
    val vx: Float,      // fractions of width per second
    val vy: Float,      // fractions of height per second (negative = up)
    val gravity: Float,
    val spin: Float,
    val sizeDp: Float,
    val color: Color,
    val round: Boolean,
)

private fun rnd(min: Float, max: Float) = min + Random.nextFloat() * (max - min)

private fun confettiColors(celebration: Celebration): List<Color> {
    val bots = if (celebration.level == CelebrationLevel.BIG) {
        BotType.entries
    } else {
        celebration.bot?.let { listOf(it) } ?: BotType.entries
    }
    return bots.flatMap { bot ->
        val palette = bot.palette()
        listOf(palette.base, palette.light)
    } + Color(0xFFFFD166) + Color.White
}

private fun generateParticles(celebration: Celebration): List<Particle> {
    val colors = confettiColors(celebration)
    val big = celebration.level == CelebrationLevel.BIG
    val cannonPerSide = if (big) 45 else 26
    val rain = if (big) 50 else 0
    val list = mutableListOf<Particle>()

    // Two "cannons" in the bottom corners.
    listOf(true, false).forEach { fromLeft ->
        repeat(cannonPerSide) {
            val dir = if (fromLeft) 1f else -1f
            list += Particle(
                x0 = if (fromLeft) 0.02f else 0.98f,
                y0 = 0.98f,
                vx = dir * rnd(0.15f, 0.9f),
                vy = -rnd(0.9f, 1.8f),
                gravity = 1.9f,
                spin = rnd(-2f, 2f),
                sizeDp = rnd(6f, 11f),
                color = colors.random(),
                round = Random.nextInt(4) == 0,
            )
        }
    }
    // A shower from the top for big celebrations.
    repeat(rain) {
        list += Particle(
            x0 = rnd(0f, 1f),
            y0 = rnd(-0.25f, 0f),
            vx = rnd(-0.1f, 0.1f),
            vy = rnd(0.1f, 0.5f),
            gravity = 0.5f,
            spin = rnd(-2f, 2f),
            sizeDp = rnd(6f, 11f),
            color = colors.random(),
            round = Random.nextInt(4) == 0,
        )
    }
    return list
}

@Composable
fun ConfettiBurst(celebration: Celebration, modifier: Modifier = Modifier) {
    val durationMs = if (celebration.level == CelebrationLevel.BIG) 3_200 else 2_200
    val progress = remember(celebration.id) { Animatable(0f) }
    val particles = remember(celebration.id) { generateParticles(celebration) }

    LaunchedEffect(celebration.id) {
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val seconds = t * durationMs / 1000f
        val alpha = ((1f - t) / 0.3f).coerceIn(0f, 1f)

        particles.forEach { p ->
            val x = (p.x0 + p.vx * seconds) * size.width
            val y = (p.y0 + p.vy * seconds + 0.5f * p.gravity * seconds * seconds) * size.height
            if (y > size.height + 40f || x < -40f || x > size.width + 40f) return@forEach

            val s = p.sizeDp.dp.toPx()
            val color = p.color.copy(alpha = alpha)
            if (p.round) {
                drawCircle(color, s / 2f, Offset(x, y))
            } else {
                rotate(p.spin * seconds * 360f, Offset(x, y)) {
                    drawRect(color, Offset(x - s / 2f, y - s / 3f), Size(s, s * 0.66f))
                }
            }
        }
    }
}