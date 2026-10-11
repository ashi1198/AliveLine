package com.aliveline.app.domain.repository

import com.aliveline.app.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

interface FocusRepository {
    val session: Flow<FocusSession?>
    suspend fun start(session: FocusSession)
    suspend fun clear()
}