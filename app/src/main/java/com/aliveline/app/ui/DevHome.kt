package com.aliveline.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.ui.theme.AliveColors

/** Temporary stand-in for the real Home screen (Phase 3). */
@Composable
fun DevHome(onReplayOnboarding: () -> Unit) {
    var state by rememberSaveable { mutableStateOf(BotState.IDLE) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            "Onboarding complete \u2013 Home comes in Phase 3",
            color = AliveColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotType.entries.forEach { BotAvatar(it, state, Modifier.weight(1f).aspectRatio(1f)) }
        }

        Text("State: ${state.name}", color = AliveColors.TextPrimary)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BotState.entries.forEach { s ->
                FilterChip(selected = s == state, onClick = { state = s }, label = { Text(s.name) })
            }
        }

        OutlinedButton(onClick = onReplayOnboarding) { Text("Replay onboarding (dev only)") }
    }
}
