package com.aliveline.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.tagline
import com.aliveline.app.ui.theme.AliveColors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConnectBotScreen(bot: BotType, onConnected: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSkip) { Text("Skip", color = AliveColors.TextSecondary) }
        }
        PlugStage(bot = bot, onConnected = onConnected, modifier = Modifier.weight(1f).fillMaxWidth())
        BotLabelCard(bot)
        Spacer(Modifier.height(24.dp))
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable

private fun PlugStage(bot: BotType, onConnected: () -> Unit, modifier: Modifier = Modifier) {
    val accent = bot.palette().base
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnConnected by rememberUpdatedState(onConnected)
    val sound = LocalSoundPlayer.current
    BoxWithConstraints(modifier) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        val w = with(density) { boxWidth.toPx() }
        val h = with(density) { boxHeight.toPx() }
        val plugWpx = with(density) { StageDims.PlugW.toPx() }
        val plugHpx = with(density) { StageDims.PlugH.toPx() }
        val socketR = with(density) { StageDims.SocketR.toPx() }
        val snapRadius = with(density) { 60.dp.toPx() }
        val socket = Offset(w / 2f, with(density) { StageDims.SocketY.toPx() })
        val target = Offset(socket.x, socket.y + plugHpx * StageDims.SeatFraction)
        val start = Offset(
            w / 2f,
            minOf(socket.y + with(density) { 200.dp.toPx() }, h - with(density) { 110.dp.toPx() }),
        )

        val plugPos = remember(start) { Animatable(start, Offset.VectorConverter) }
        val glow = remember { Animatable(0f) }
        var connecting by remember { mutableStateOf(false) }
        val botAlpha by animateFloatAsState(if (connecting) 1f else .45f, tween(500), label = "botAlpha")

        BotAvatar(
            type = bot,
            state = if (connecting) BotState.EXCITED else BotState.SLEEPY,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = StageDims.BotTop)
                .size(StageDims.BotSize)
                .alpha(botAlpha),
        )

        // Socket, glow and cable are drawn in the draw phase only; plugPos is read there,
        // so dragging never recomposes this screen.
        Canvas(Modifier.fillMaxSize()) {
            val g = glow.value
            if (g > 0f) {
                val r = size.minDimension * .8f
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = .45f * g), Color.Transparent), socket, r),
                    r,
                    socket,
                )
            }
            val base = Offset(plugPos.value.x, plugPos.value.y + plugHpx / 2f)
            val anchor = Offset(w * .92f, h + 20.dp.toPx())
            val cable = Path().apply {
                moveTo(anchor.x, anchor.y)
                cubicTo(anchor.x, anchor.y - 120.dp.toPx(), base.x, base.y + 120.dp.toPx(), base.x, base.y)
            }
            drawPath(cable, Color(0xFF2C3354), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))

            val near = (plugPos.value - target).getDistance() < snapRadius * 1.6f
            drawSocket(socket, socketR, accent, if (connecting) 1f else if (near) .6f else 0f)
        }

        Text(
            text = "Drag the plug\nto bring me to life!",
            color = AliveColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = StageDims.SocketY + 54.dp)
                .alpha(if (connecting) 0f else 1f),
        )

        Box(
            Modifier
                .offset {
                    IntOffset(
                        (plugPos.value.x - plugWpx / 2f).roundToInt(),
                        (plugPos.value.y - plugHpx / 2f).roundToInt(),
                    )
                }
                .size(StageDims.PlugW, StageDims.PlugH)
                .pointerInput(connecting) {
                    if (connecting) return@pointerInput
                    detectDragGestures(
                        onDragStart = { sound?.play(Sfx.PICKUP) },
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch { plugPos.snapTo(plugPos.value + drag) }
                        },
                        onDragEnd = {
                            scope.launch {
                                if ((plugPos.value - target).getDistance() < snapRadius) {
                                    connecting = true
                                    sound?.play(Sfx.SNAP)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    plugPos.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                                    glow.animateTo(1f, tween(350))
                                    delay(300)
                                    currentOnConnected()
                                } else {
                                    plugPos.animateTo(start, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { plugPos.animateTo(start, spring()) } },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) { drawPlugShape(Offset.Zero, size, accent, connecting) }
        }
    }
}

@Composable
private fun BotLabelCard(bot: BotType) {
    Surface(shape = RoundedCornerShape(20.dp), color = AliveColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BotAvatar(bot, BotState.SLEEPY, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(bot.displayName, color = AliveColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(bot.tagline, color = AliveColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
