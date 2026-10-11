package com.aliveline.app.feature.create

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.ui.components.TimePickerDialog
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun CreateTaskScreen(
    onClose: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val sound = LocalSoundPlayer.current
    var showTimePicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text(
                "New task",
                color = AliveColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = s.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("What do you want to do?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldLabel("Bot")
            ChipRow {
                BotType.entries.forEach { bot ->
                    FilterChip(
                        selected = s.bot == bot,
                        onClick = { viewModel.onBotChange(bot) },
                        label = { Text(bot.displayName) },
                    )
                }
            }

            FieldLabel("Type")
            ChipRow {
                TaskKind.entries.forEach { kind ->
                    FilterChip(
                        selected = s.kind == kind,
                        onClick = { viewModel.onKindChange(kind) },
                        label = { Text(kindLabel(kind)) },
                    )
                }
            }

            if (s.kind == TaskKind.ONE_TIME) {
                FieldLabel("Deadline")
                ChipRow {
                    DeadlineOption.entries.forEach { option ->
                        FilterChip(
                            selected = s.deadline == option,
                            onClick = { viewModel.onDeadlineChange(option) },
                            label = { Text(option.label) },
                        )
                    }
                }
            } else {
                FieldLabel("Repeat")
                ChipRow {
                    listOf(Repeat.DAILY, Repeat.WEEKDAYS, Repeat.WEEKLY).forEach { repeat ->
                        FilterChip(
                            selected = s.repeat == repeat,
                            onClick = { viewModel.onRepeatChange(repeat) },
                            label = { Text(repeatLabel(repeat)) },
                        )
                    }
                }
            }

            FieldLabel("Time on the Daily Map (optional)")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(s.startMinute?.let { TimeFormat.minuteOfDay(it) } ?: "Pick a time")
                }
                if (s.startMinute != null) {
                    TextButton(onClick = { viewModel.onStartMinuteChange(null) }) { Text("Clear") }
                }
            }
            if (s.startMinute != null) {
                FieldLabel("Duration")
                ChipRow {
                    listOf(15, 30, 45, 60, 90, 120, 180).forEach { minutes ->
                        FilterChip(
                            selected = s.durationMin == minutes,
                            onClick = { viewModel.onDurationChange(minutes) },
                            label = { Text(TimeFormat.durationLabel(minutes)) },
                        )
                    }
                }
            }

            FieldLabel("Subtasks")
            s.subtasks.forEachIndexed { index, text ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text, color = AliveColors.TextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.onRemoveSubtask(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove subtask")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = s.newSubtask,
                    onValueChange = viewModel::onNewSubtaskChange,
                    label = { Text("Add a subtask") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::onAddSubtask) { Text("Add") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                sound?.play(Sfx.SAVE)
                viewModel.save(onClose)
            },
            enabled = s.canSave,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AliveColors.Primary),
        ) { Text("Save task", fontSize = 16.sp) }
        Spacer(Modifier.height(16.dp))
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialMinute = s.startMinute ?: (9 * 60),
            onConfirm = {
                viewModel.onStartMinuteChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = AliveColors.TextSecondary, fontSize = 13.sp)
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private fun kindLabel(kind: TaskKind) = when (kind) {
    TaskKind.ONE_TIME -> "One-time"
    TaskKind.RECURRING -> "Recurring"
    TaskKind.HABIT -> "Habit"
}

private fun repeatLabel(repeat: Repeat) = when (repeat) {
    Repeat.WEEKDAYS -> "Weekdays"
    Repeat.WEEKLY -> "Weekly"
    else -> "Every day"
}