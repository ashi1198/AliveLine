package com.aliveline.app.feature.create

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.ReminderStyle
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.ui.components.TimePickerDialog
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.theme.AliveColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun CreateTaskScreen(
    onClose: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val sound = LocalSoundPlayer.current
    val context = LocalContext.current

    var showStartPicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askForNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!s.loaded) {
        Box(Modifier.fillMaxSize())
    } else {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val due = s.dueDate

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                Text(
                    if (s.isEditing) "Edit task" else "New task",
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
                        FilterChip(
                            selected = due == null,
                            onClick = { viewModel.onDueDateChange(null) },
                            label = { Text("No deadline") },
                        )
                        FilterChip(
                            selected = due == today,
                            onClick = { viewModel.onDueDateChange(today) },
                            label = { Text("Today") },
                        )
                        FilterChip(
                            selected = due == tomorrow,
                            onClick = { viewModel.onDueDateChange(tomorrow) },
                            label = { Text("Tomorrow") },
                        )
                        val custom = due != null && due != today && due != tomorrow
                        FilterChip(
                            selected = custom,
                            onClick = { showDatePicker = true },
                            label = { Text(if (custom && due != null) due.format(shortDate) else "Pick a date") },
                        )
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
                    OutlinedButton(onClick = { showStartPicker = true }) {
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

                FieldLabel("Reminder (optional)")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showReminderPicker = true }) {
                        Text(s.reminderMinute?.let { "Remind me at ${TimeFormat.minuteOfDay(it)}" } ?: "Add a reminder")
                    }
                    if (s.reminderMinute != null) {
                        TextButton(onClick = { viewModel.onReminderChange(null) }) { Text("Clear") }
                    }
                }
                val start = s.startMinute
                if (start != null && s.reminderMinute == null) {
                    TextButton(
                        onClick = {
                            viewModel.onReminderChange(start)
                            askForNotificationsIfNeeded()
                        },
                    ) { Text("Use the start time (${TimeFormat.minuteOfDay(start)})") }
                }
                if (s.reminderMinute != null) {
                    ChipRow {
                        FilterChip(
                            selected = s.reminderStyle == ReminderStyle.NOTIFICATION,
                            onClick = { viewModel.onReminderStyleChange(ReminderStyle.NOTIFICATION) },
                            label = { Text("Gentle nudge") },
                        )
                        FilterChip(
                            selected = s.reminderStyle == ReminderStyle.ALARM,
                            onClick = { viewModel.onReminderStyleChange(ReminderStyle.ALARM) },
                            label = { Text("Alarm") },
                        )
                    }
                    Text(
                        if (s.reminderStyle == ReminderStyle.ALARM) {
                            "Rings with your alarm sound until you mark it done or snooze it."
                        } else {
                            "A gentle nudge in your notifications."
                        },
                        color = AliveColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    "Reminders only appear if the task isn't done yet.",
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                )

                FieldLabel("Subtasks")
                s.subtasks.forEachIndexed { index, sub ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(sub.title, color = AliveColors.TextPrimary, modifier = Modifier.weight(1f))
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
            ) { Text(if (s.isEditing) "Save changes" else "Save task", fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
        }

        if (showStartPicker) {
            TimePickerDialog(
                initialMinute = s.startMinute ?: (9 * 60),
                onConfirm = {
                    viewModel.onStartMinuteChange(it)
                    showStartPicker = false
                },
                onDismiss = { showStartPicker = false },
            )
        }
        if (showReminderPicker) {
            TimePickerDialog(
                initialMinute = s.reminderMinute ?: s.startMinute ?: (9 * 60),
                onConfirm = {
                    viewModel.onReminderChange(it)
                    showReminderPicker = false
                    askForNotificationsIfNeeded()
                },
                onDismiss = { showReminderPicker = false },
            )
        }
        if (showDatePicker) {
            DeadlineDatePicker(
                initial = due,
                onConfirm = {
                    viewModel.onDueDateChange(it)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineDatePicker(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } else {
                        onDismiss()
                    }
                },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
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