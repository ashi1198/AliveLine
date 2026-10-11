package com.aliveline.app.feature.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.FocusMode
import com.aliveline.app.ui.theme.AliveColors

/** What the session is for. taskId is null for a free-standing session started from a bot screen. */
data class StartFocusRequest(
    val taskId: Long?,
    val title: String,
    val bot: BotType,
    val suggestedMinutes: Int?,
)

private data class PomodoroPreset(val focus: Int, val rest: Int, val rounds: Int) {
    val label: String get() = "$focus / $rest \u00D7 $rounds"
}

private val presets = listOf(
    PomodoroPreset(25, 5, 4),
    PomodoroPreset(50, 10, 3),
    PomodoroPreset(15, 3, 4),
)

@Composable
fun StartFocusDialog(
    request: StartFocusRequest,
    onDismiss: () -> Unit,
    onStarted: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(FocusMode.COUNTDOWN) }
    var minutes by remember(request) { mutableIntStateOf(request.suggestedMinutes ?: 25) }
    var presetIndex by remember { mutableIntStateOf(0) }

    val options = remember(request) {
        (listOfNotNull(request.suggestedMinutes) + listOf(25, 45, 60, 90, 120)).distinct().sorted()
    }

    fun begin() {
        val choice = if (mode == FocusMode.COUNTDOWN) {
            FocusChoice(FocusMode.COUNTDOWN, minutes, 0, 1)
        } else {
            val p = presets[presetIndex]
            FocusChoice(FocusMode.POMODORO, p.focus, p.rest, p.rounds)
        }
        viewModel.start(request, choice, onStarted)
    }

    val latestBegin by rememberUpdatedState { begin() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Start either way. Without the permission the timer still works, there is just no notification.
        latestBegin()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AliveColors.Surface,
        titleContentColor = AliveColors.TextPrimary,
        textContentColor = AliveColors.TextSecondary,
        title = { Text("Start focus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(request.title, color = AliveColors.TextPrimary, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == FocusMode.COUNTDOWN,
                        onClick = { mode = FocusMode.COUNTDOWN },
                        label = { Text("Countdown") },
                    )
                    FilterChip(
                        selected = mode == FocusMode.POMODORO,
                        onClick = { mode = FocusMode.POMODORO },
                        label = { Text("Pomodoro") },
                    )
                }

                if (mode == FocusMode.COUNTDOWN) {
                    Text("How long?", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        options.forEach { option ->
                            FilterChip(
                                selected = minutes == option,
                                onClick = { minutes = option },
                                label = { Text(TimeFormat.durationLabel(option)) },
                            )
                        }
                    }
                } else {
                    Text("Focus / break \u00D7 rounds (minutes)", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEachIndexed { index, preset ->
                            FilterChip(
                                selected = presetIndex == index,
                                onClick = { presetIndex = index },
                                label = { Text(preset.label) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        begin()
                    }
                },
            ) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}