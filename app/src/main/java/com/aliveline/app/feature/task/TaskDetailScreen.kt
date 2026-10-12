package com.aliveline.app.feature.task

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.domain.model.Repeat
import com.aliveline.app.domain.model.Subtask
import com.aliveline.app.domain.model.TaskKind
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onAddNote: (Long) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sound = LocalSoundPlayer.current

    when (val s = state) {
        TaskDetailUiState.Loading -> Box(Modifier.fillMaxSize())
        TaskDetailUiState.NotFound -> Column(Modifier.fillMaxSize().padding(16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("This task is no longer here.", color = AliveColors.TextSecondary)
        }
        is TaskDetailUiState.Content -> TaskDetailContent(
            content = s,
            onBack = onBack,
            onEdit = { onEdit(s.task.id) },
            onToggleSubtask = {
                sound?.play(Sfx.TOGGLE)
                viewModel.onSubtaskToggled(it)
            },
            onMarkDone = {
                sound?.play(Sfx.COMPLETE, s.task.bot)
                viewModel.onMarkDone()
            },
            onUndo = {
                sound?.play(Sfx.UNDO)
                viewModel.onUndo()
            },
            onDelete = {
                sound?.play(Sfx.DELETE)
                viewModel.onDelete(onBack)
            },
            onOpenNote = onOpenNote,
            onAddNote = { onAddNote(s.task.id) },
        )
    }
}

@Composable
private fun TaskDetailContent(
    content: TaskDetailUiState.Content,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
    onMarkDone: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onAddNote: () -> Unit,
) {
    val task = content.task
    val accent = task.bot.palette().base

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit task")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete task")
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BotAvatar(
                    task.bot,
                    if (content.isDone) BotState.HAPPY else BotState.IDLE,
                    Modifier.size(130.dp),
                )
            }

            Text(task.title, color = AliveColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(task.bot.displayName, color = accent, fontWeight = FontWeight.SemiBold)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { content.progress / 100f },
                    color = accent,
                    trackColor = AliveColors.Surface,
                    modifier = Modifier.weight(1f),
                )
                Text("  ${content.progress}%", color = AliveColors.TextSecondary)
            }

            content.dueLabel?.let { InfoRow("Status", it) }
            if (task.kind != TaskKind.ONE_TIME) {
                InfoRow("Repeat", repeatText(task.repeat))
            }
            task.startMinute?.let { start ->
                val end = start + (task.durationMin ?: 60)
                InfoRow("On the map", "${TimeFormat.minuteOfDay(start)} - ${TimeFormat.minuteOfDay(end)}")
            }
            task.reminderMinute?.let { InfoRow("Reminder", TimeFormat.minuteOfDay(it)) }
            if (task.description.isNotBlank()) {
                Text(task.description, color = AliveColors.TextSecondary)
            }

            if (task.subtasks.isNotEmpty()) {
                val doneCount = task.subtasks.count { it.done }
                Text(
                    "Subtasks  $doneCount/${task.subtasks.size}",
                    color = AliveColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                task.subtasks.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sub.done, onCheckedChange = { onToggleSubtask(sub) })
                        Text(sub.title, color = AliveColors.TextPrimary)
                    }
                }
            }

            TaskNotesSection(onOpenNote = onOpenNote, onAddNote = onAddNote)
        }

        if (content.isDone) {
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Done \u2713  (tap to undo)") }
        } else {
            Button(
                onClick = onMarkDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
            ) { Text("Mark as done") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = AliveColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, color = AliveColors.TextPrimary)
    }
}

private fun repeatText(repeat: Repeat): String = when (repeat) {
    Repeat.WEEKDAYS -> "Every weekday"
    Repeat.WEEKLY -> "Every week"
    else -> "Every day"
}