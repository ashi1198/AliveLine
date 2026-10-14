package com.aliveline.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Handles the Done and Snooze buttons on reminder notifications and alarms. */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return
        val action = intent.action
        val app = context.applicationContext

        // Stop the ringing straight away.
        NotificationManagerCompat.from(app).cancel(ReminderNotifications.notificationId(taskId))

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(app, ReminderEntryPoint::class.java)
                val tasks = entry.taskRepository()
                val task = tasks.observeTask(taskId).first() ?: return@launch
                when (action) {
                    ReminderNotifications.ACTION_DONE ->
                        tasks.setTaskCompletedAt(taskId, System.currentTimeMillis())
                    ReminderNotifications.ACTION_SNOOZE ->
                        entry.reminderScheduler().snooze(task)
                }
            } finally {
                pending.finish()
            }
        }
    }
}