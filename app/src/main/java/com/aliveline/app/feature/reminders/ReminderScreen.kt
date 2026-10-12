package com.aliveline.app.feature.reminders

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.ui.components.EmptyHint
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors

@Composable
fun RemindersScreen(
    onTaskClick: (Long) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Column {
                Text("Reminders", color = AliveColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "A gentle nudge, only when a task isn't done yet.",
                    color = AliveColors.TextSecondary,
                )
            }
        }

        if (!notificationsOn) {
            item(key = "permission") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AliveColors.Surface,
                    border = BorderStroke(1.dp, AliveColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Notifications are off",
                            color = AliveColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Turn them on so your bots can remind you.",
                            color = AliveColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                        ) { Text("Open settings") }
                    }
                }
            }
        }

        if (state.items.isEmpty()) {
            item(key = "empty") {
                EmptyHint("No reminders yet. Add one when you create or edit a task.")
            }
        } else {
            items(state.items, key = { it.taskId }) { item ->
                val accent = item.bot.palette().base
                val shape = RoundedCornerShape(18.dp)
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(shape).clickable { onTaskClick(item.taskId) },
                    shape = shape,
                    color = AliveColors.Surface,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.timeLabel, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                color = AliveColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                            )
                            Text(
                                "${item.whenLabel} \u00B7 ${item.bot.displayName}",
                                color = AliveColors.TextSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}