package com.aliveline.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.aliveline.app.domain.model.BotType
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.delay

enum class BotState { IDLE, HAPPY, EXCITED, THINKING, TIRED, SLEEPY, CELEBRATING, WAVING }

/** A pure data description of how a bot looks in a given state. */
private class Look(
    val lid: Float,          // 0 = eyes open, 1 = closed
    val smile: Float,        // -1 sad .. 1 big open smile
    val arms: Float,         // 0 down .. 1 raised
    val pupilY: Float,       // -1 up .. 1 down
    val bounce: Float,       // bounce height as fraction of size
    val bounceMs: Int,
    val wave: Float = 0f,    // 1 = right arm waves
    val sparkle: Boolean = false,
    val zzz: Boolean = false,
)

private fun BotState.look() = when (this) {
    BotState.IDLE -> Look(0f, 0.4f, 0f, 0f, 0.012f, 1800)
    BotState.HAPPY -> Look(0f, 0.9f, 0.3f, 0f, 0.04f, 600)
    BotState.EXCITED -> Look(0f, 1f, 1f, 0f, 0.07f, 380, sparkle = true)
    BotState.THINKING -> Look(0f, 0f, 0.2f, -0.8f, 0.01f, 1800)
    BotState.TIRED -> Look(0.55f, -0.4f, 0f, 0.6f, 0.005f, 2800)
    BotState.SLEEPY -> Look(1f, 0f, 0f, 0f, 0.005f, 3200, zzz = true)
    BotState.CELEBRATING -> Look(0f, 1f, 1f, 0f, 0.10f, 320, sparkle = true)
    BotState.WAVING -> Look(0f, 0.9f, 0f, 0f, 0.02f, 900, wave = 1f)
}

internal class Palette(val base: Color, val light: Color, val dark: Color)

internal fun BotType.palette() = when (this) {
    BotType.BRAIN -> Palette(Color(0xFF8F8CF5), Color(0xFFB9B7FB), Color(0xFF5E5BC9))
    BotType.HEALTH -> Palette(Color(0xFF5FD99B), Color(0xFF95EDBC), Color(0xFF34A872))
    BotType.SOCIAL -> Palette(Color(0xFFFF9A5C), Color(0xFFFFC08F), Color(0xFFD9743A))
}

@Composable
fun BotAvatar(
    type: BotType,
    state: BotState,
    modifier: Modifier = Modifier.size(160.dp),
) {
    val pal = type.palette()
    val look = state.look()
    val textMeasurer = rememberTextMeasurer()

    val infinite = rememberInfiniteTransition(label = "bot")
    val bounce by infinite.animateFloat(
        0f, 1f,
        infiniteRepeat(look.bounceMs),
        label = "bounce",
    )
    val waveSwing by infinite.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(330, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "waveSwing",
    )
    val zzz by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "zzz",
    )

    val blink = remember { Animatable(0f) }
    LaunchedEffect(state) {
        blink.snapTo(0f)
        if (state == BotState.SLEEPY) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2000, 4500))
            blink.animateTo(1f, tween(80))
            blink.animateTo(0f, tween(100))
        }
    }
    val gaze = remember { Animatable(0f) }
    LaunchedEffect(state) {
        gaze.snapTo(0f)
        if (state == BotState.SLEEPY || state == BotState.THINKING) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(1500, 3500))
            gaze.animateTo(Random.nextFloat() * 2f - 1f, tween(250))
        }
    }

    val lid by animateFloatAsState(look.lid, label = "lid")
    val smile by animateFloatAsState(look.smile, label = "smile")
    val arms by animateFloatAsState(look.arms, label = "arms")
    val pupilY by animateFloatAsState(look.pupilY, label = "pupil")
    val waveAmt by animateFloatAsState(look.wave, label = "wave")

    Canvas(modifier) {
        val s = size.minDimension
        val eyeInk = Color(0xFF1B1F3B)
        val outline = pal.dark.copy(alpha = 0.55f)
        val lw = s * .014f

        // soft ground shadow (does not bounce)
        drawOval(Color.Black.copy(alpha = 0.28f), Offset(s * .24f, s * .925f), Size(s * .52f, s * .06f))

        translate(top = -bounce * look.bounce * s) {
            // feet
            drawOval(pal.dark, Offset(s * .32f, s * .86f), Size(s * .17f, s * .085f))
            drawOval(pal.dark, Offset(s * .51f, s * .86f), Size(s * .17f, s * .085f))

            // body
            drawRoundRect(
                Brush.verticalGradient(listOf(pal.base, pal.dark), startY = s * .62f, endY = s * .89f),
                Offset(s * .30f, s * .62f), Size(s * .40f, s * .27f), CornerRadius(s * .14f),
            )
            drawOval(Color.White.copy(alpha = 0.18f), Offset(s * .40f, s * .72f), Size(s * .20f, s * .12f))

            // arms + hands
            val armY = s * (.74f - .36f * arms)
            drawLine(pal.base, Offset(s * .31f, s * .71f), Offset(s * .15f, armY), s * .075f, StrokeCap.Round)
            drawCircle(pal.light, s * .05f, Offset(s * .15f, armY))

            val rArms = max(arms, waveAmt)
            val rX = s * (.85f + .045f * waveSwing * waveAmt)
            val rY = s * (.74f - .36f * rArms)
            drawLine(pal.base, Offset(s * .69f, s * .71f), Offset(rX, rY), s * .075f, StrokeCap.Round)
            drawCircle(pal.light, s * .05f, Offset(rX, rY))

            // side nubs ("ears")
            drawCircle(pal.dark, s * .05f, Offset(s * .115f, s * .44f))
            drawCircle(pal.dark, s * .05f, Offset(s * .885f, s * .44f))

            // character-specific top detail (drawn before the head so the head overlaps its base)
            when (type) {
                BotType.BRAIN -> {
                    val bumps = listOf(
                        Triple(.38f, .15f, .075f), Triple(.50f, .115f, .085f), Triple(.62f, .15f, .075f),
                        Triple(.44f, .17f, .06f), Triple(.56f, .17f, .06f),
                    )
                    bumps.forEach { (x, y, r) -> drawCircle(pal.dark, s * (r + .012f), Offset(s * x, s * y)) }
                    bumps.forEach { (x, y, r) -> drawCircle(pal.light, s * r, Offset(s * x, s * y)) }
                    val fold = Path().apply {
                        moveTo(s * .46f, s * .10f)
                        quadraticBezierTo(s * .50f, s * .15f, s * .54f, s * .10f)
                    }
                    drawPath(fold, pal.dark.copy(alpha = .5f), style = Stroke(lw, cap = StrokeCap.Round))
                }
                BotType.HEALTH -> {
                    val heart = Color(0xFFFF6B8A)
                    val hp = Path().apply {
                        moveTo(s * .5f, s * .205f)
                        cubicTo(s * .36f, s * .12f, s * .42f, s * .02f, s * .5f, s * .09f)
                        cubicTo(s * .58f, s * .02f, s * .64f, s * .12f, s * .5f, s * .205f)
                        close()
                    }
                    drawPath(hp, heart)
                    drawCircle(Color.White.copy(alpha = .6f), s * .014f, Offset(s * .455f, s * .085f))
                }
                BotType.SOCIAL -> {
                    val curl = Path().apply {
                        moveTo(s * .5f, s * .21f)
                        cubicTo(s * .5f, s * .12f, s * .59f, s * .13f, s * .58f, s * .07f)
                        cubicTo(s * .57f, s * .01f, s * .47f, s * .03f, s * .49f, s * .09f)
                    }
                    drawPath(curl, pal.dark, style = Stroke(s * .05f, cap = StrokeCap.Round))
                    drawPath(curl, pal.light, style = Stroke(s * .032f, cap = StrokeCap.Round))
                }
            }

            // head
            drawRoundRect(
                Brush.verticalGradient(listOf(pal.light, pal.base), startY = s * .18f, endY = s * .72f),
                Offset(s * .12f, s * .18f), Size(s * .76f, s * .54f), CornerRadius(s * .26f),
            )
            drawRoundRect(
                outline, Offset(s * .12f, s * .18f), Size(s * .76f, s * .54f), CornerRadius(s * .26f),
                style = Stroke(lw),
            )
            // glossy highlight
            drawOval(Color.White.copy(alpha = 0.22f), Offset(s * .22f, s * .23f), Size(s * .18f, s * .07f))

            // eyes
            val closed = max(lid, blink.value)
            listOf(.36f, .64f).forEach { x ->
                val c = Offset(s * x, s * .42f)
                val r = s * .105f
                drawCircle(Color.White, r, c)
                val pupil = c + Offset(gaze.value * r * .3f, pupilY * r * .3f)
                drawCircle(eyeInk, r * .62f, pupil)
                drawCircle(Color.White, r * .17f, pupil + Offset(-r * .18f, -r * .22f))
                val clip = Path().apply { addOval(Rect(c, r)) }
                clipPath(clip) {
                    drawRect(pal.base, c - Offset(r, r), Size(r * 2, r * 2 * closed))
                }
                drawCircle(outline, r, c, style = Stroke(lw * .8f))
            }

            // cheeks
            val cheekAlpha = (0.30f + 0.25f * smile).coerceIn(0.15f, 0.6f)
            drawCircle(Color(0xFFFF7A93).copy(alpha = cheekAlpha), s * .045f, Offset(s * .24f, s * .545f))
            drawCircle(Color(0xFFFF7A93).copy(alpha = cheekAlpha), s * .045f, Offset(s * .76f, s * .545f))

            // mouth: open smile when very happy, otherwise a curve
            if (smile > 0.8f) {
                val open = ((smile - 0.8f) / 0.2f).coerceIn(0f, 1f)
                val mouth = Path().apply {
                    moveTo(s * .42f, s * .55f)
                    quadraticBezierTo(s * .5f, s * (.55f + .06f + .12f * open), s * .58f, s * .55f)
                    close()
                }
                drawPath(mouth, eyeInk)
            } else {
                val mouth = Path().apply {
                    moveTo(s * .42f, s * .56f)
                    quadraticBezierTo(s * .5f, s * (.56f + .07f * smile), s * .58f, s * .56f)
                }
                drawPath(mouth, eyeInk, style = Stroke(s * .022f, cap = StrokeCap.Round))
            }
        }

        // sparkles for excited / celebrating
        if (look.sparkle) {
            val gold = Color(0xFFFFD166)
            listOf(
                Offset(.06f, .20f), Offset(.94f, .24f), Offset(.14f, .06f), Offset(.88f, .08f),
            ).forEachIndexed { i, p ->
                val pulse = if (i % 2 == 0) bounce else 1f - bounce
                drawCircle(gold.copy(alpha = .4f + .6f * pulse), s * (.012f + .014f * pulse), Offset(s * p.x, s * p.y))
            }
        }

        // floating Zzz when asleep
        if (look.zzz) {
            listOf(0f, .33f, .66f).forEachIndexed { i, delayFrac ->
                val t = (zzz + delayFrac) % 1f
                drawText(
                    textMeasurer,
                    "z",
                    Offset(s * (.74f + .08f * t + .03f * i), s * (.20f - .16f * t)),
                    TextStyle(
                        color = Color.White.copy(alpha = 1f - t),
                        fontSize = (s * (.08f + .05f * t)).toSp(),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private fun infiniteRepeat(ms: Int) =
    infiniteRepeatable<Float>(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse)
