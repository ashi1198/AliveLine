package com.aliveline.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aliveline.app.MainActivity
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.repository.SettingsRepository
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.ui.displayName
import com.aliveline.app.ui.label
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AppPalette
import com.aliveline.app.ui.theme.buildPalette
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Lets the widget (which Hilt can't inject directly) reach the app's repositories. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
    fun settingsRepository(): SettingsRepository
}

class BotWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val tasks = entry.taskRepository().observeTasks().first()
        val settings = entry.settingsRepository().settings.first()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        // Prepare all three bots up front; tapping the face only changes which one is shown.
        val perBot = withContext(Dispatchers.Default) {
            BotType.entries.associateWith { bot ->
                val data = WidgetDataBuilder.build(tasks, today, zone, preferredBot = bot)
                data to BotFaceRenderer.render(bot, data.condition, 192)
            }
        }
        val appPalette = buildPalette(settings.theme, settings.accent)

        provideContent {
            val index = currentState<Preferences>()[BotIndexKey] ?: 0
            val bot = BotType.entries[Math.floorMod(index, BotType.entries.size)]
            val (data, face) = perBot.getValue(bot)
            WidgetContent(data, face, appPalette)
        }
    }

    companion object {
        /** Which bot this widget is currently showing (0 = Brain, 1 = Health, 2 = Social). */
        val BotIndexKey = intPreferencesKey("bot_index")
    }
}

class BotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BotWidget()
}

@Composable
private fun WidgetContent(data: WidgetData, face: Bitmap, appPalette: AppPalette) {
    val accent = data.bot.palette().base
    val dots = BotType.entries.joinToString(" ") { if (it == data.bot) "\u25CF" else "\u25CB" }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(appPalette.surface))
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tapping the face switches to the next bot.
        Image(
            provider = ImageProvider(face),
            contentDescription = "${data.bot.displayName} is ${data.condition.label}. Tap to switch bot.",
            modifier = GlanceModifier
                .size(76.dp)
                .clickable(actionRunCallback<CycleBotAction>()),
        )
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "${data.bot.displayName} \u00B7 ${data.condition.label}",
                style = TextStyle(
                    color = ColorProvider(accent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = data.todoTitle ?: data.message,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2,
            )
            data.todoMeta?.let { meta ->
                Text(
                    text = meta,
                    style = TextStyle(color = ColorProvider(Color(0xFF9AA3C0)), fontSize = 12.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = dots,
                style = TextStyle(color = ColorProvider(accent), fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}