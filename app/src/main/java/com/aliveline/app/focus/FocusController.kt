package com.aliveline.app.focus

import com.aliveline.app.domain.model.FocusSession
import com.aliveline.app.domain.repository.FocusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** The one place that starts and stops focus sessions: saves the session and (un)schedules notifications together. */
@Singleton
class FocusController @Inject constructor(
    private val repo: FocusRepository,
    private val scheduler: FocusScheduler,
) {
    val session: Flow<FocusSession?> = repo.session

    suspend fun start(session: FocusSession) {
        repo.start(session)
        scheduler.schedule(session)
    }

    suspend fun stop() {
        scheduler.cancel()
        repo.clear()
    }
}