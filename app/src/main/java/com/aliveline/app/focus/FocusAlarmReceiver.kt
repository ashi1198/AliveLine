package com.aliveline.app.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FocusAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Focus"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Your focus timer has an update."
        FocusNotifications.show(context, title, message)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
    }
}