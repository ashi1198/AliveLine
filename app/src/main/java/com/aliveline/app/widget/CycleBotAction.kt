package com.aliveline.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.aliveline.app.domain.model.BotType

/** Runs when the bot face on the widget is tapped: moves to the next bot and redraws. */
class CycleBotAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[BotWidget.BotIndexKey] ?: 0
            prefs[BotWidget.BotIndexKey] = (current + 1) % BotType.entries.size
        }
        BotWidget().update(context, glanceId)
    }
}