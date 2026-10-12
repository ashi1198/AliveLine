package com.aliveline.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.aliveline.app.domain.repository.SettingsRepository
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.reminder.ReminderScheduler
import com.aliveline.app.widget.BotWidget
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltAndroidApp
class AliveLineApplication : Application() {

    @Inject lateinit var tasks: TaskRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var reminders: ReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Whenever tasks or settings change: keep reminders scheduled and the widget fresh.
        appScope.launch {
            combine(tasks.observeTasks(), settings.settings) { taskList, _ -> taskList }
                .collect { taskList ->
                    reminders.sync(taskList)
                    BotWidget().updateAll(this@AliveLineApplication)
                }
        }
    }
}