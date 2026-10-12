package com.aliveline.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aliveline.app.domain.ReminderRules
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Lets broadcast receivers (which Hilt can't inject directly) reach the app's classes. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun taskRepository(): TaskRepository
    fun reminderScheduler(): ReminderScheduler
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return

        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(app, ReminderEntryPoint::class.java)
                // A deleted task simply isn't found, so a leftover alarm does nothing.
                val task = entry.taskRepository().observeTask(taskId).first()
                if (task != null) {
                    val zone = ZoneId.systemDefault()
                    if (ReminderRules.shouldNotify(task, LocalDate.now(zone), zone)) {
                        ReminderNotifications.show(app, task)
                    }
                    entry.reminderScheduler().scheduleNext(task)
                }
            } finally {
                pending.finish()
            }
        }
    }
}