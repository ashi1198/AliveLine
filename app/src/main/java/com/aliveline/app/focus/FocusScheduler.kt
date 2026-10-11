package com.aliveline.app.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aliveline.app.domain.FocusCalculator
import com.aliveline.app.domain.model.FocusSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules a notification for every phase change of a session.
 * Uses inexact alarms (no special permission), so a notification can arrive a little late in battery saver.
 */
@Singleton
class FocusScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(session: FocusSession) {
        cancel()
        val now = System.currentTimeMillis()
        FocusCalculator.boundaries(session).forEachIndexed { index, boundary ->
            if (boundary.atMs <= now || index >= MAX_ALARMS) return@forEachIndexed
            val operation = pendingIntent(index, session.title, boundary.message, PendingIntent.FLAG_UPDATE_CURRENT)
                ?: return@forEachIndexed
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, boundary.atMs, operation)
        }
    }

    fun cancel() {
        repeat(MAX_ALARMS) { index ->
            pendingIntent(index, null, null, PendingIntent.FLAG_NO_CREATE)?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun pendingIntent(index: Int, title: String?, message: String?, flags: Int): PendingIntent? {
        val intent = Intent(context, FocusAlarmReceiver::class.java)
        if (title != null) intent.putExtra(FocusAlarmReceiver.EXTRA_TITLE, title)
        if (message != null) intent.putExtra(FocusAlarmReceiver.EXTRA_MESSAGE, message)
        return PendingIntent.getBroadcast(context, REQUEST_BASE + index, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val REQUEST_BASE = 4200
        const val MAX_ALARMS = 24
    }
}