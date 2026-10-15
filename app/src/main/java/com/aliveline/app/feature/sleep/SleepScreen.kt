package com.aliveline.app.feature.sleep

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.domain.SleepRules
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.PhoneUse
import com.aliveline.app.domain.model.SleepSession
import com.aliveline.app.ui.BotAvatar
import com.aliveline.app.ui.BotState
import com.aliveline.app.ui.components.EmptyHint
import com.aliveline.app.ui.components.SectionTitle
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

private fun openUsageAccessSettings(context: Context) {
    val base = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // On newer Android versions the "package:" data jumps straight to AliveLine's own switch.
    val direct = Intent(base).setData(Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(direct) }
        .onFailure { runCatching { context.startActivity(base) } }
}

@Composable
fun SleepScreen(
    onBack: () -> Unit,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sound = LocalSoundPlayer.current
    val zone = remember { ZoneId.systemDefault() }
    val accent = BotType.HEALTH.palette().base

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.active?.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    if (!state.loaded) {
        Box(Modifier.fillMaxSize())
        return
    }

    val active = state.active
    val last = state.history.firstOrNull()

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
                    "Sleep",
                    color = AliveColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item(key = "hero") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BotAvatar(
                    type = BotType.HEALTH,
                    state = if (active != null) BotState.SLEEPY else BotState.IDLE,
                    modifier = Modifier.size(160.dp),
                )
            }
        }

        if (active != null) {
            item(key = "active") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AliveColors.Surface,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Sleeping since ${timeFormat.format(Instant.ofEpochMilli(active.startAt).atZone(zone))}",
                            color = AliveColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${SleepRules.durationLabel(now - active.startAt)} so far",
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = {
                                sound?.play(Sfx.WAKE, BotType.HEALTH)
                                viewModel.onWake()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 6.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                        ) { Text("I'm awake", fontSize = 16.sp) }
                        TextButton(onClick = viewModel::onCancel) {
                            Text("Cancel this night", color = AliveColors.TextSecondary)
                        }
                        if (now - active.startAt > 16 * 3_600_000L) {
                            Text(
                                "This night has been running a long time. Tap I'm awake, or cancel it.",
                                color = AliveColors.TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        } else {
            item(key = "start") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            sound?.play(Sfx.TOGGLE)
                            viewModel.onGoingToSleep()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    ) { Text("Going to sleep", fontSize = 16.sp) }
                    Text(
                        "Tap when you put your phone down, and tap I'm awake in the morning.",
                        color = AliveColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (!state.usageSupported) {
            item(key = "unsupported") {
                Text(
                    "Phone-use detection needs Android 9 or newer. Your taps still track your sleep.",
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        } else if (!state.hasUsageAccess) {
            item(key = "access") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AliveColors.Surface,
                    border = BorderStroke(1.dp, AliveColors.Primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "See your phone use at night",
                            color = AliveColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Allow usage access so AliveLine can tell when your screen was on while you slept. " +
                                    "It only sees screen on and off times, never which apps you use, and nothing leaves your phone. " +
                                    "This is optional.",
                            color = AliveColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                        Button(onClick = { openUsageAccessSettings(context) }) { Text("Open usage access settings") }
                    }
                }
            }
        } else {
            item(key = "access_on") {
                Text(
                    "Phone-use detection is on.",
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        if (last != null) {
            item(key = "last") { SummaryCard(last, zone, accent) }
        }

        if (state.history.size > 1) {
            item(key = "earlier_title") { SectionTitle("Earlier nights") }
            items(state.history.drop(1).take(14), key = { "night_${it.id}" }) { night ->
                HistoryRow(night, zone)
            }
        }

        if (last == null && active == null) {
            item(key = "empty") {
                EmptyHint("No nights yet. Tap Going to sleep when you're ready for bed.")
            }
        }
    }
}

@Composable
private fun SummaryCard(session: SleepSession, zone: ZoneId, accent: Color) {
    val end = session.endAt ?: session.startAt
    val phone = if (session.usageTracked) {
        PhoneUse(session.pickups, session.screenOnMs, session.firstHalfHourMs)
    } else {
        null
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AliveColors.Surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Last night \u00B7 ${dayFormat.format(Instant.ofEpochMilli(session.startAt).atZone(zone))}",
                color = AliveColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            InfoRow("Bedtime", timeFormat.format(Instant.ofEpochMilli(session.startAt).atZone(zone)))
            InfoRow("Woke up", timeFormat.format(Instant.ofEpochMilli(end).atZone(zone)))
            InfoRow("Time in bed", SleepRules.durationLabel(session.durationMs), valueColor = accent)

            if (phone != null) {
                InfoRow("Screen on, first 30 min", SleepRules.screenLabel(phone.firstHalfHourMs))
                InfoRow("Phone pickups", if (phone.pickups == 1) "1 time" else "${phone.pickups} times")
                InfoRow("Screen on, total", SleepRules.screenLabel(phone.screenOnMs))
            } else {
                Text(
                    "Phone use wasn't measured. Allow usage access to see it next time.",
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }

            Text(
                SleepRules.message(session.durationMs, phone),
                color = accent,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = AliveColors.TextPrimary) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = AliveColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HistoryRow(session: SleepSession, zone: ZoneId) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AliveColors.Surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                dayFormat.format(Instant.ofEpochMilli(session.startAt).atZone(zone)),
                color = AliveColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (session.usageTracked) {
                Text(
                    "${session.pickups} pickups",
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Text(
                SleepRules.durationLabel(session.durationMs),
                color = AliveColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}