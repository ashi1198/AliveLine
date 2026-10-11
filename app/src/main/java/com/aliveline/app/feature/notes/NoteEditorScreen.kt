package com.aliveline.app.feature.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.ui.accent
import com.aliveline.app.ui.label
import com.aliveline.app.ui.screenTint
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun NoteEditorScreen(
    onClose: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val sound = LocalSoundPlayer.current
    val background by animateColorAsState(s.color.screenTint(), tween(250), label = "noteBackground")

    if (!s.loaded) {
        Box(Modifier.fillMaxSize())
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .background(background)
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                Text(
                    if (s.isNew) "New note" else "Edit note",
                    color = AliveColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    sound?.play(Sfx.PIN)
                    viewModel.onPinToggled()
                }) {
                    Icon(
                        if (s.pinned) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (s.pinned) "Unpin" else "Pin",
                        tint = if (s.pinned) AliveColors.Primary else AliveColors.TextSecondary,
                    )
                }
                if (!s.isNew) {
                    IconButton(onClick = {
                        sound?.play(Sfx.DELETE)
                        viewModel.delete(onClose)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete note")
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = s.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = s.body,
                    onValueChange = viewModel::onBodyChange,
                    label = { Text("Write your note") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = s.tag,
                    onValueChange = viewModel::onTagChange,
                    label = { Text("Tag (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Color", color = AliveColors.TextSecondary, fontSize = 13.sp)
                ColorPicker(
                    selected = s.color,
                    onSelect = {
                        sound?.play(Sfx.TOGGLE)
                        viewModel.onColorChange(it)
                    },
                )

                if (s.taskId != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Attached to: ${s.linkedTaskTitle ?: "a task"}",
                            color = AliveColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::onDetach) { Text("Detach") }
                    }
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
            ) { Text("Save note", fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColorPicker(selected: NoteColor, onSelect: (NoteColor) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NoteColor.entries.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.accent)
                    .then(if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                    .clickable { onSelect(color) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "${color.label} selected",
                        tint = Color(0xFF1B1F3B),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}