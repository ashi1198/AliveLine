package com.aliveline.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.aliveline.app.domain.repository.SettingsRepository
import com.aliveline.app.domain.repository.TaskRepository
import com.aliveline.app.widget.BotWidget
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltAndroidApp
class AliveLineApplication : Application() {

    @Inject lateinit var tasks: TaskRepository
    @Inject lateinit var settings: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Keep the home screen widget fresh whenever tasks or settings change.
        appScope.launch {
            combine(tasks.observeTasks(), settings.settings) { _, _ -> }
                .collect { BotWidget().updateAll(this@AliveLineApplication) }
        }
    }
}