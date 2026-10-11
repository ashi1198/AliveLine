package com.aliveline.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.feature.settings.SettingsViewModel
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = AliveColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = AliveColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        content()
    }
}

/** Reminders are still a placeholder; the sound switch and dev reset live here until a Settings screen exists. */
@Composable
fun RemindersScreen(
    onReplayOnboarding: () -> Unit,
    settings: SettingsViewModel = hiltViewModel(),
) {
    val soundOn by settings.soundEnabled.collectAsStateWithLifecycle()

    PlaceholderScreen(
        title = "Reminders",
        message = "Alarms and reminders are coming in a later batch.",
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sound effects", color = AliveColors.TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = soundOn, onCheckedChange = settings::onSoundToggled)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onReplayOnboarding) { Text("Replay onboarding (dev only)") }
    }
}