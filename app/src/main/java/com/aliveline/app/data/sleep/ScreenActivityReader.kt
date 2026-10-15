package com.aliveline.app.data.sleep

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.aliveline.app.domain.model.ScreenEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads WHEN the screen turned on or off (never which apps you used) from Android's usage history.
 * Needs the "Usage access" special permission, which you switch on in system settings.
 */
@Singleton
class ScreenActivityReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Screen events are only reported on Android 9 (API 28) and newer. */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    @Suppress("DEPRECATION")
    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow("android:get_usage_stats", Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun events(startMs: Long, endMs: Long): List<ScreenEvent> {
        if (!supported || !hasAccess()) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usage = manager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()
        val result = mutableListOf<ScreenEvent>()
        while (usage.hasNextEvent()) {
            usage.getNextEvent(event)
            when (event.eventType) {
                SCREEN_INTERACTIVE -> result += ScreenEvent(event.timeStamp, screenOn = true)
                SCREEN_NON_INTERACTIVE -> result += ScreenEvent(event.timeStamp, screenOn = false)
            }
        }
        return result
    }

    private companion object {
        // UsageEvents.Event.SCREEN_INTERACTIVE and SCREEN_NON_INTERACTIVE (added in API 28).
        const val SCREEN_INTERACTIVE = 15
        const val SCREEN_NON_INTERACTIVE = 16
    }
}