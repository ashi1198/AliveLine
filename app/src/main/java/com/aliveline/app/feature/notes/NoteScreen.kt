package com.aliveline.app.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.model.NoteColor
import com.aliveline.app.domain.model.displayTitle
import com.aliveline.app.ui.accent
import com.aliveline.app.ui.cardColor
import com.aliveline.app.ui.components.EmptyHint
import com.aliveline.app.ui.components.SectionTitle
import com.aliveline.app.ui.theme.AliveColors
import com.aliveline.app.domain.model.DecorationType
@Composable
fun NotesScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sound = LocalSoundPlayer.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Text("Notes", color = AliveColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search notes") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.tags.isNotEmpty()) {
                item(key = "tags") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.selectedTag == null,
                            onClick = { viewModel.onTagSelected(null) },
                            label = { Text("All") },
                        )
                        state.tags.forEach { tag ->
                            FilterChip(
                                selected = state.selectedTag.equals(tag, ignoreCase = true),
                                onClick = { viewModel.onTagSelected(tag) },
                                label = { Text("#$tag") },
                            )
                        }
                    }
                }
            }

            if (state.pinned.isEmpty() && state.others.isEmpty()) {
                item(key = "empty") {
                    EmptyHint(
                        if (state.totalCount == 0) {
                            "No notes yet. Tap + to capture a thought."
                        } else {
                            "No notes match your search."
                        },
                    )
                }
            }

            if (state.pinned.isNotEmpty()) {
                item(key = "pinned_title") { SectionTitle("Pinned") }
                items(state.pinned, key = { "p_${it.note.id}" }) { item ->
                    NoteCard(
                        item = item,
                        onClick = { onNoteClick(item.note.id) },
                        onTogglePin = {
                            sound?.play(Sfx.PIN)
                            viewModel.onTogglePin(item.note)
                        },
                    )
                }
            }

            if (state.others.isNotEmpty()) {
                if (state.pinned.isNotEmpty()) item(key = "others_title") { SectionTitle("Others") }
                items(state.others, key = { "o_${it.note.id}" }) { item ->
                    NoteCard(
                        item = item,
                        onClick = { onNoteClick(item.note.id) },
                        onTogglePin = {
                            sound?.play(Sfx.PIN)
                            viewModel.onTogglePin(item.note)
                        },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onNewNote,
            containerColor = AliveColors.Primary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New note")
        }
    }
}

@Composable
private fun NoteCard(item: NoteItem, onClick: () -> Unit, onTogglePin: () -> Unit) {
    val note = item.note
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = note.color.cardColor(),
        border = if (note.color != NoteColor.DEFAULT) {
            BorderStroke(1.dp, note.color.accent.copy(alpha = 0.5f))
        } else {
            null
        },
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 14.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.displayTitle,
                    color = AliveColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onTogglePin) {
                    Icon(
                        if (note.pinned) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (note.pinned) "Unpin note" else "Pin note",
                        tint = if (note.pinned) AliveColors.Primary else AliveColors.TextSecondary,
                    )
                }
            }
            if (note.title.isNotBlank() && note.body.isNotBlank()) {
                Text(
                    note.body,
                    color = AliveColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            if (note.tag != null || item.taskTitle != null) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    note.tag?.let { Text("#$it", color = AliveColors.Primary, fontSize = 12.sp) }
                    item.taskTitle?.let {
                        Text("\u21B3 $it", color = AliveColors.TextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            if (note.decorations.isNotEmpty()) {
                val photoCount = note.decorations.count { it.type == DecorationType.PHOTO }
                val stickerCount = note.decorations.size - photoCount
                Text(
                    listOfNotNull(
                        if (photoCount > 0) "\uD83D\uDDBC $photoCount" else null,
                        if (stickerCount > 0) "\u2728 $stickerCount" else null,
                    ).joinToString("  \u00B7  "),
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}