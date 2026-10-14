package com.aliveline.app.feature.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.data.photo.PhotoStore
import com.aliveline.app.domain.model.DecorationType
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.domain.model.NoteDecoration
import com.aliveline.app.ui.accent
import com.aliveline.app.ui.cardColor
import com.aliveline.app.ui.label
import com.aliveline.app.ui.screenTint
import com.aliveline.app.ui.theme.AliveColors
import kotlin.math.roundToInt

@Composable
fun NoteEditorScreen(
    onClose: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val sound = LocalSoundPlayer.current
    val background by animateColorAsState(s.color.screenTint(), tween(250), label = "noteBackground")
    var showStickers by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            sound?.play(Sfx.PIN)
            viewModel.addPhoto(uri)
        }
    }

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
                    minLines = 6,
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

                Text("Decorate", color = AliveColors.TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStickers = true }) { Text("+ Sticker") }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        enabled = !s.importing,
                    ) { Text(if (s.importing) "Adding..." else "+ Photo") }
                }

                DecorationBoard(
                    decorations = s.decorations,
                    selectedId = s.selectedId,
                    noteColor = s.color,
                    photoStore = viewModel.photoStore,
                    onSelect = viewModel::onSelect,
                    onTransform = viewModel::onTransform,
                )

                if (s.selectedId != null) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = viewModel::onBringToFront) { Text("To front") }
                        OutlinedButton(onClick = viewModel::onDuplicate) { Text("Duplicate") }
                        OutlinedButton(onClick = {
                            sound?.play(Sfx.DELETE)
                            viewModel.onDeleteSelected()
                        }) { Text("Remove") }
                    }
                }
                Text(
                    "Tap to select. Drag to move, pinch to resize and twist to rotate.",
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
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

        if (showStickers) {
            StickerSheet(
                onPick = {
                    sound?.play(Sfx.PIN)
                    viewModel.addSticker(it)
                    showStickers = false
                },
                onDismiss = { showStickers = false },
            )
        }
    }
}

/** The free-placement area. Positions are stored as fractions of the board, so it looks the same on any screen. */
@Composable
private fun DecorationBoard(
    decorations: List<NoteDecoration>,
    selectedId: Long?,
    noteColor: NoteColor,
    photoStore: PhotoStore,
    onSelect: (Long?) -> Unit,
    onTransform: (Long, Float, Float, Float, Float) -> Unit,
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(20.dp)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(shape)
            .background(noteColor.cardColor())
            .border(1.dp, noteColor.accent.copy(alpha = 0.4f), shape)
            .pointerInput(Unit) { detectTapGestures { onSelect(null) } },
    ) {
        val boardW = with(density) { maxWidth.toPx() }
        val boardH = with(density) { maxHeight.toPx() }

        if (decorations.isEmpty()) {
            Text(
                "Add stickers or photos, then place them anywhere here.",
                color = AliveColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }

        decorations.sortedBy { it.z }.forEach { decoration ->
            key(decoration.id) {
                DecorationView(
                    decoration = decoration,
                    selected = decoration.id == selectedId,
                    boardW = boardW,
                    boardH = boardH,
                    photoStore = photoStore,
                    onSelect = onSelect,
                    onTransform = onTransform,
                )
            }
        }
    }
}

@Composable
private fun DecorationView(
    decoration: NoteDecoration,
    selected: Boolean,
    boardW: Float,
    boardH: Float,
    photoStore: PhotoStore,
    onSelect: (Long?) -> Unit,
    onTransform: (Long, Float, Float, Float, Float) -> Unit,
) {
    // This wrapper fills the board and places its content around the item's saved center point.
    Box(
        Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        (decoration.x * constraints.maxWidth - placeable.width / 2f).roundToInt(),
                        (decoration.y * constraints.maxHeight - placeable.height / 2f).roundToInt(),
                    )
                }
            },
    ) {
        Box(
            Modifier
                // Gestures come BEFORE graphicsLayer, so drag amounts are in plain board pixels.
                .pointerInput(decoration.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        onTransform(decoration.id, pan.x / boardW, pan.y / boardH, zoom, rotation)
                    }
                }
                .pointerInput(decoration.id) {
                    detectTapGestures { onSelect(decoration.id) }
                }
                .graphicsLayer {
                    scaleX = decoration.scale
                    scaleY = decoration.scale
                    rotationZ = decoration.rotation
                }
                .then(
                    if (selected) {
                        Modifier.border(2.dp, AliveColors.Primary, RoundedCornerShape(10.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(4.dp),
        ) {
            when (decoration.type) {
                DecorationType.STICKER -> StickerGlyph(decoration.content, emojiSize = 48.sp, botSize = 80.dp)
                DecorationType.PHOTO -> PhotoImage(decoration.content, photoStore)
            }
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