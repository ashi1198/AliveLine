package com.aliveline.app.feature.bot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.components.EmptyHint
import com.aliveline.app.ui.components.SectionTitle
import com.aliveline.app.ui.components.TaskCard
import com.aliveline.app.ui.conditionMessage
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.label
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun BotDetailScreen(
    onBack: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onAddTask: (BotType) -> Unit,
    viewModel: BotDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = state.bot.palette().base

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "top") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    state.bot.displayName,
                    color = AliveColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item(key = "hero") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BotAvatar(state.bot, state.botState, Modifier.size(200.dp))
                Text(state.condition.label, color = accent, fontWeight = FontWeight.SemiBold)
                Text(
                    state.bot.conditionMessage(state.condition),
                    color = AliveColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        item(key = "add") {
            Button(
                onClick = { onAddTask(state.bot) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
            ) { Text("+ Add a task for ${state.bot.displayName}") }
        }

        item(key = "todos_title") { SectionTitle("Needs") }
        if (state.todos.isEmpty()) {
            item(key = "todos_empty") { EmptyHint("Nothing waiting. Enjoy the calm!") }
        } else {
            items(state.todos, key = { "todo_${it.id}" }) { item ->
                TaskCard(
                    item = item,
                    onClick = { onTaskClick(item.id) },
                    onComplete = { viewModel.onCompleteClicked(item.id) },
                )
            }
        }

        item(key = "done_title") { SectionTitle("Done today") }
        if (state.doneToday.isEmpty()) {
            item(key = "done_empty") { EmptyHint("Nothing finished yet today. No rush.") }
        } else {
            items(state.doneToday, key = { "done_${it.id}" }) { item ->
                TaskCard(item = item, onClick = { onTaskClick(item.id) })
            }
        }
    }
}