package com.aliveline.app.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.introBody
import com.aliveline.app.ui.introTitle
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors
import kotlinx.coroutines.delay

@Composable
fun MeetBotScreen(bot: BotType, onContinue: () -> Unit) {
    val accent = bot.palette().base
    val sound = LocalSoundPlayer.current
    var awake by remember { mutableStateOf(false) }
    var botState by remember { mutableStateOf(BotState.SLEEPY) }

    // Wake-up sequence: asleep -> excited -> waving.
    LaunchedEffect(bot) {
        delay(350)
        awake = true
        sound?.play(Sfx.WAKE)
        botState = BotState.EXCITED
        delay(1300)
        botState = BotState.WAVING
    }
    val botAlpha by animateFloatAsState(if (awake) 1f else .45f, tween(500), label = "wake")
    val glow by animateFloatAsState(if (awake) 1f else 0f, tween(600), label = "glow")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp)) // same height as the Skip row on the connect screen

        Box(Modifier.fillMaxWidth().height(290.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val socket = Offset(size.width / 2f, StageDims.SocketY.toPx())
                val plugW = StageDims.PlugW.toPx()
                val plugH = StageDims.PlugH.toPx()
                val target = Offset(socket.x, socket.y + plugH * StageDims.SeatFraction)
                if (glow > 0f) {
                    val r = 150.dp.toPx()
                    drawCircle(
                        Brush.radialGradient(listOf(accent.copy(alpha = .45f * glow), Color.Transparent), socket, r),
                        r,
                        socket,
                    )
                }
                drawLine(
                    Color(0xFF2C3354),
                    Offset(target.x, target.y + plugH / 2f),
                    Offset(target.x, size.height),
                    7.dp.toPx(),
                    StrokeCap.Round,
                )
                drawSocket(socket, StageDims.SocketR.toPx(), accent, glow)
                drawPlugShape(Offset(target.x - plugW / 2f, target.y - plugH / 2f), Size(plugW, plugH), accent, awake)
            }
            BotAvatar(
                type = bot,
                state = botState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = StageDims.BotTop)
                    .size(StageDims.BotSize)
                    .alpha(botAlpha),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            bot.introTitle,
            color = AliveColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            bot.introBody,
            color = AliveColors.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AliveColors.Primary),
        ) { Text("Let's go \u2192", fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }
}
