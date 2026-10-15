package com.aliveline.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.TaskItem
import com.aliveline.app.ui.components.EmptyHint
import com.aliveline.app.ui.components.RecapCard
import com.aliveline.app.ui.components.SectionTitle
import com.aliveline.app.ui.components.SpeechBubble
import com.aliveline.app.ui.components.TaskCard
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.label
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun HomeScreen(
    onBotClick: (BotType) -> Unit,
    onTaskClick: (Long) -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var recapHidden by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.greeting,
                        color = AliveColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(state.dateLabel, color = AliveColors.TextSecondary)
                    if (state.streak >= 2) {
                        Text(
                            "\uD83D\uDD25 ${state.streak}-day streak",
                            color = AliveColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                IconButton(onClick = onOpenAchievements) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Achievements",
                        tint = AliveColors.Primary,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AliveColors.TextSecondary,
                    )
                }
            }
        }

        state.chatter?.let { chatter ->
            item(key = "chatter") {
                SpeechBubble(
                    text = chatter.text,
                    speaker = chatter.bot.displayName,
                    accent = chatter.bot.palette().base,
                    fillWidth = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::onChatterTapped,
                )
            }
        }
        val recap = state.recap
        if (recap != null && !recapHidden) {
            item(key = "recap") {
                RecapCard(text = recap, onDismiss = { recapHidden = true })
            }
        }

        item(key = "bots") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.bots.forEach { summary ->
                    BotTile(
                        summary = summary,
                        onClick = { onBotClick(summary.bot) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "todos_title") { SectionTitle("Today's todos") }
        if (state.todos.isEmpty()) {
            item(key = "todos_empty") {
                EmptyHint(
                    if (state.hasAnyTasks) {
                        "All clear for now. Nice rhythm!"
                    } else {
                        "Your bots are waiting. Tap + to add your first task."
                    },
                )
            }
        } else {
            items(state.todos, key = { "todo_${it.id}" }) { item ->
                TaskCard(
                    item = item,
                    onClick = { onTaskClick(item.id) },
                    onComplete = { viewModel.onCompleteClicked(item.id, item.bot) },
                )
            }
        }

        item(key = "activity_title") { SectionTitle("Activity") }
        if (state.doneToday.isEmpty()) {
            item(key = "activity_empty") {
                EmptyHint("Nothing yet today. One small step is enough.")
            }
        } else {
            items(state.doneToday, key = { "done_${it.id}" }) { item ->
                ActivityRow(item = item, onClick = { onTaskClick(item.id) })
            }
        }
    }
}

@Composable
private fun BotTile(summary: BotSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = summary.bot.palette().base
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = AliveColors.Surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BotAvatar(summary.bot, summary.state, Modifier.fillMaxWidth().aspectRatio(1f))
            Text(
                summary.bot.displayName.removeSuffix(" Bot"),
                color = AliveColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(summary.condition.label, color = accent, fontSize = 12.sp)
            if (summary.pendingCount > 0) {
                Text("${summary.pendingCount} to do", color = AliveColors.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ActivityRow(item: TaskItem, onClick: () -> Unit) {
    val accent = item.bot.palette().base
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(accent, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${item.bot.displayName} finished", color = AliveColors.TextSecondary, fontSize = 12.sp)
            Text(item.title, color = AliveColors.TextPrimary)
        }
        item.doneAtLabel?.let { Text(it, color = AliveColors.TextSecondary, fontSize = 12.sp) }
    }
}