package com.aliveline.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aliveline.app.domain.ReminderRules
import com.aliveline.app.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

/**
 * Keeps one alarm per task with a reminder, always pointing at the NEXT time it should fire.
 * Uses inexact alarms (no special permission), so a reminder can be a few minutes late in battery saver.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Schedules or cancels the alarm of every task. Safe to call as often as you like. */
    fun sync(tasks: List<Task>) {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        tasks.forEach { task ->
            val at = ReminderRules.nextTrigger(task, now, zone)
            if (at == null) cancel(task.id) else schedule(task.id, at)
        }
    }

    /** Called right after a reminder fired, to line up the following one. */
    fun scheduleNext(task: Task) {
        val at = ReminderRules.nextTrigger(task, System.currentTimeMillis(), ZoneId.systemDefault())
        if (at == null) cancel(task.id) else schedule(task.id, at)
    }

    private fun schedule(taskId: Long, atMs: Long) {
        val operation = pendingIntent(taskId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
    }

    private fun cancel(taskId: Long) {
        pendingIntent(taskId, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(taskId: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_TASK_ID, taskId)
        // The request code is what tells tasks apart (extras are ignored when matching).
        return PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + taskId.toInt(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION = "com.aliveline.app.REMINDER"
        const val EXTRA_TASK_ID = "task_id"
        private const val REQUEST_BASE = 100_000
    }
}