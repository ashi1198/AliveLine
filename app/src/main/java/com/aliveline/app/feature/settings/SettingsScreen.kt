package com.aliveline.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aliveline.app.domain.model.AccentColor
import com.aliveline.app.domain.model.AppSettings
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.ThemeStyle
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors
import com.aliveline.app.ui.theme.backgroundColor
import com.aliveline.app.ui.theme.color
import com.aliveline.app.ui.theme.surfaceColor
import com.aliveline.app.ui.theme.title
import kotlin.math.roundToInt
import androidx.compose.material3.FilterChip
import com.aliveline.app.domain.model.ReminderStyle
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = settings

    if (current == null) {
        Box(Modifier.fillMaxSize())
    } else {
        SettingsContent(
            settings = current,
            onBack = onBack,
            onNameChange = viewModel::onNameChange,
            onSoundEnabledChange = viewModel::onSoundEnabledChange,
            onVolumeChange = viewModel::onVolumeChange,
            onThemeChange = viewModel::onThemeChange,
            onAccentChange = viewModel::onAccentChange,
            onReminderStyleChange = viewModel::onReminderStyleChange,
            onReplayOnboarding = onReplayOnboarding,
        )
    }
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onThemeChange: (ThemeStyle) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onReminderStyleChange: (ReminderStyle) -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    val sound = LocalSoundPlayer.current
    // Local copies keep typing and dragging smooth; they are saved as the user goes.
    var name by remember { mutableStateOf(settings.userName) }
    var volume by remember { mutableFloatStateOf(settings.soundVolume) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Settings",
                color = AliveColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("Profile") {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(24)
                        onNameChange(name.trim())
                    },
                    label = { Text("Your name") },
                    supportingText = { Text("Shown in your Home greeting") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Sound") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sound effects", color = AliveColors.TextPrimary, modifier = Modifier.weight(1f))
                    Switch(checked = settings.soundEnabled, onCheckedChange = onSoundEnabledChange)
                }
                Text(
                    "Volume  ${(volume * 100).roundToInt()}%",
                    color = AliveColors.TextSecondary,
                    fontSize = 13.sp,
                )
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = {
                        onVolumeChange(volume)
                        sound?.play(Sfx.TOGGLE, volume = volume)
                    },
                    enabled = settings.soundEnabled,
                )
                Text("Hear the bots", color = AliveColors.TextSecondary, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotType.entries.forEach { bot ->
                        OutlinedButton(
                            onClick = { sound?.play(Sfx.CELEBRATE, bot) },
                            enabled = settings.soundEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(bot.displayName.removeSuffix(" Bot"), color = bot.palette().base)
                        }
                    }
                }
            }
            Section("Reminders") {
                Text("New reminders start as:", color = AliveColors.TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.reminderStyle == ReminderStyle.NOTIFICATION,
                        onClick = { onReminderStyleChange(ReminderStyle.NOTIFICATION) },
                        label = { Text("Gentle nudge") },
                    )
                    FilterChip(
                        selected = settings.reminderStyle == ReminderStyle.ALARM,
                        onClick = { onReminderStyleChange(ReminderStyle.ALARM) },
                        label = { Text("Alarm") },
                    )
                }
                Text(
                    if (settings.reminderStyle == ReminderStyle.ALARM) {
                        "Rings with your alarm sound until you mark the task done or snooze it."
                    } else {
                        "A soft notification with a Done button."
                    },
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Section("Theme") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ThemeStyle.entries.forEach { theme ->
                        ThemeSwatch(
                            theme = theme,
                            selected = theme == settings.theme,
                            accent = settings.accent.color,
                            onClick = {
                                sound?.play(Sfx.TOGGLE)
                                onThemeChange(theme)
                            },
                        )
                    }
                }
            }

            Section("Accent color") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AccentColor.entries.forEach { accent ->
                        val selected = accent == settings.accent
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .then(if (selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                                .clickable {
                                    sound?.play(Sfx.TOGGLE)
                                    onAccentChange(accent)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "${accent.title} selected",
                                    tint = Color(0xFF10131F),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            Section("Advanced") {
                OutlinedButton(onClick = onReplayOnboarding) { Text("Replay onboarding") }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AliveColors.Surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = AliveColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ThemeStyle, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(width = 84.dp, height = 56.dp)
                .clip(shape)
                .background(theme.backgroundColor)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) accent else Color.White.copy(alpha = 0.2f),
                    shape = shape,
                ),
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.surfaceColor),
            )
        }
        Text(
            theme.title,
            color = if (selected) AliveColors.TextPrimary else AliveColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}