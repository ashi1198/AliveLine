package com.aliveline.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aliveline.app.MainActivity
import com.aliveline.app.domain.ReminderRules
import com.aliveline.app.domain.model.ReminderStyle
import com.aliveline.app.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one alarm per task with a reminder, always pointing at the NEXT time it should fire.
 * - NOTIFICATION style: an inexact alarm (no special permission); may be a few minutes late in battery saver.
 * - ALARM style: an "alarm clock" alarm. Android treats these as exact and needs no extra permission;
 *   a small alarm icon also shows in the status bar while one is set.
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
            if (at == null) cancel(task.id) else schedule(task, at, REQUEST_BASE)
        }
    }

    /** Called right after a reminder fired, to line up the following one. */
    fun scheduleNext(task: Task) {
        val at = ReminderRules.nextTrigger(task, System.currentTimeMillis(), ZoneId.systemDefault())
        if (at == null) cancel(task.id) else schedule(task, at, REQUEST_BASE)
    }

    /** One extra reminder a few minutes from now (the Snooze button). */
    fun snooze(task: Task, minutes: Int = SNOOZE_MINUTES) {
        schedule(task, System.currentTimeMillis() + minutes * 60_000L, SNOOZE_BASE)
    }

    private fun schedule(task: Task, atMs: Long, requestBase: Int) {
        val operation = pendingIntent(task.id, requestBase, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (task.reminderStyle == ReminderStyle.ALARM) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(atMs, openAppIntent()), operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
        }
    }

    private fun cancel(taskId: Long) {
        pendingIntent(taskId, REQUEST_BASE, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(taskId: Long, requestBase: Int, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_TASK_ID, taskId)
        // The request code is what tells tasks apart (extras are ignored when matching).
        return PendingIntent.getBroadcast(
            context,
            requestBase + taskId.toInt(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION = "com.aliveline.app.REMINDER"
        const val EXTRA_TASK_ID = "task_id"
        const val SNOOZE_MINUTES = 10
        private const val REQUEST_BASE = 100_000
        private const val SNOOZE_BASE = 300_000
    }
}