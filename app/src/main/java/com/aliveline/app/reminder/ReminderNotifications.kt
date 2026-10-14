package com.aliveline.app.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aliveline.app.MainActivity
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.ReminderStyle
import com.aliveline.app.domain.model.Task

object ReminderNotifications {
    private const val CHANNEL_SOFT = "reminders"
    private const val CHANNEL_ALARM = "alarms"
    private const val NOTIFICATION_BASE = 200_000
    private const val DONE_BASE = 400_000
    private const val SNOOZE_BASE = 500_000
    private const val ALARM_TIMEOUT_MS = 120_000L

    const val ACTION_DONE = "com.aliveline.app.REMINDER_DONE"
    const val ACTION_SNOOZE = "com.aliveline.app.REMINDER_SNOOZE"

    fun notificationId(taskId: Long): Int = NOTIFICATION_BASE + taskId.toInt()

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SOFT, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT),
        )

        val alarmChannel = NotificationChannel(CHANNEL_ALARM, "Task alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings until you mark the task done or snooze it"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
        }
        manager.createNotificationChannel(alarmChannel)
    }

    /** Supportive, never scolding. */
    fun messageFor(bot: BotType, alarm: Boolean): String = if (alarm) {
        when (bot) {
            BotType.BRAIN -> "Brain Bot: time to focus. You've got this."
            BotType.HEALTH -> "Health Bot: time to look after yourself."
            BotType.SOCIAL -> "Social Bot: time for a friendly moment."
        }
    } else {
        when (bot) {
            BotType.BRAIN -> "Brain Bot: ready for a little focus?"
            BotType.HEALTH -> "Health Bot: a small step for your body today?"
            BotType.SOCIAL -> "Social Bot: a friendly moment today?"
        }
    }

    private fun actionIntent(context: Context, action: String, requestBase: Int, taskId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestBase + taskId.toInt(),
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    @SuppressLint("MissingPermission")
    fun show(context: Context, task: Task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannels(context)
        val isAlarm = task.reminderStyle == ReminderStyle.ALARM

        val open = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, if (isAlarm) CHANNEL_ALARM else CHANNEL_SOFT)
            .setSmallIcon(if (isAlarm) android.R.drawable.ic_lock_idle_alarm else android.R.drawable.ic_popup_reminder)
            .setContentTitle(task.title)
            .setContentText(messageFor(task.bot, isAlarm))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Done", actionIntent(context, ACTION_DONE, DONE_BASE, task.id))

        if (isAlarm) {
            builder
                .addAction(
                    0,
                    "Snooze ${ReminderScheduler.SNOOZE_MINUTES} min",
                    actionIntent(context, ACTION_SNOOZE, SNOOZE_BASE, task.id),
                )
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setTimeoutAfter(ALARM_TIMEOUT_MS)
        }

        val notification = builder.build()
        // Keep repeating the sound until the notification is dismissed (or times out).
        if (isAlarm) notification.flags = notification.flags or Notification.FLAG_INSISTENT

        NotificationManagerCompat.from(context).notify(notificationId(task.id), notification)
    }
}