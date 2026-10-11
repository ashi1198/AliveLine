package com.aliveline.app.feature.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.model.Note
import com.aliveline.app.domain.model.displayTitle
import com.aliveline.app.domain.repository.NoteRepository
import com.aliveline.app.ui.cardColor
import com.aliveline.app.ui.theme.AliveColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TaskNotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: NoteRepository,
) : ViewModel() {
    // Same nav entry as the task detail screen, so the "taskId" argument is available here too.
    private val taskId: Long = checkNotNull(savedStateHandle.get<Long>("taskId"))

    val notes: StateFlow<List<Note>> = repo.observeNotesForTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun TaskNotesSection(
    onOpenNote: (Long) -> Unit,
    onAddNote: () -> Unit,
    viewModel: TaskNotesViewModel = hiltViewModel(),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Notes",
                color = AliveColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAddNote) { Text("+ Add note") }
        }
        if (notes.isEmpty()) {
            Text("No notes attached yet.", color = AliveColors.TextSecondary, fontSize = 13.sp)
        }
        notes.forEach { note ->
            val shape = RoundedCornerShape(14.dp)
            Surface(
                modifier = Modifier.fillMaxWidth().clip(shape).clickable { onOpenNote(note.id) },
                shape = shape,
                color = note.color.cardColor(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(note.displayTitle, color = AliveColors.TextPrimary, fontWeight = FontWeight.Medium)
                    if (note.title.isNotBlank() && note.body.isNotBlank()) {
                        Text(
                            note.body,
                            color = AliveColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}