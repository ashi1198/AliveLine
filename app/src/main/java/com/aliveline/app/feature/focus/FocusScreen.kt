package com.aliveline.app.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.FocusCalculator
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.domain.model.FocusMode
import com.aliveline.app.domain.model.FocusPhase
import com.aliveline.app.domain.model.FocusSession
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(
    onClose: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    when (val state = ui) {
        FocusUiState.Loading -> Box(Modifier.fillMaxSize())
        FocusUiState.None -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No focus session running.", color = AliveColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClose) { Text("Back") }
        }
        is FocusUiState.Active -> FocusContent(
            session = state.session,
            onBack = onClose,
            onStop = { viewModel.stop(onClose) },
            onCompleteTask = { viewModel.completeTaskAndStop(state.session.taskId, onClose) },
        )
    }
}

@Composable
private fun FocusContent(
    session: FocusSession,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onCompleteTask: () -> Unit,
) {
    val sound = LocalSoundPlayer.current
    val accent = session.bot.palette().base

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val snap = FocusCalculator.snapshot(session, now)

    LaunchedEffect(snap.phase) {
        when (snap.phase) {
            FocusPhase.DONE -> sound?.play(Sfx.CELEBRATE)
            FocusPhase.BREAK -> sound?.play(Sfx.WAKE)
            FocusPhase.FOCUS -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Focus", color = AliveColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))
        BotAvatar(
            type = session.bot,
            state = when (snap.phase) {
                FocusPhase.FOCUS -> BotState.FOCUSED
                FocusPhase.BREAK -> BotState.HAPPY
                FocusPhase.DONE -> BotState.CELEBRATING
            },
            modifier = Modifier.size(170.dp),
        )
        Text(
            session.title,
            color = AliveColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            when (snap.phase) {
                FocusPhase.FOCUS ->
                    if (session.mode == FocusMode.POMODORO) "Focus \u00B7 round ${snap.round} of ${snap.rounds}" else "Focus time"
                FocusPhase.BREAK -> "Break \u00B7 stretch and sip some water"
                FocusPhase.DONE -> "Session complete. Nice work!"
            },
            color = accent,
        )

        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (snap.phase == FocusPhase.DONE) 1f else snap.progress },
                modifier = Modifier.size(220.dp),
                color = accent,
                trackColor = AliveColors.Surface,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round,
            )
            Text(
                TimeFormat.clock(snap.remainingMs),
                color = AliveColors.TextPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.weight(1f))
        if (snap.phase == FocusPhase.DONE) {
            if (session.taskId != null) {
                Button(
                    onClick = onCompleteTask,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                ) { Text("Mark task as done") }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Close") }
        } else {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Stop session") }
        }
        Spacer(Modifier.height(24.dp))
    }
}