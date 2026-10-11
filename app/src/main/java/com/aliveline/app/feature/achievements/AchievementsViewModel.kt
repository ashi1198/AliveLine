package com.aliveline.app.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.AchievementEvaluator
import com.aliveline.app.domain.model.Achievement
import com.aliveline.app.domain.repository.AchievementRepository
import com.aliveline.app.domain.repository.NoteRepository
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AchievementItem(
    val achievement: Achievement,
    val current: Int,
    val unlocked: Boolean,
)

data class AchievementsUiState(
    val items: List<AchievementItem> = emptyList(),
    val unlockedCount: Int = 0,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    tasks: TaskRepository,
    notes: NoteRepository,
    achievements: AchievementRepository,
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> =
        combine(tasks.observeTasks(), notes.observeNotes(), achievements.unlocked) { t, n, saved ->
            val zone = ZoneId.systemDefault()
            val progress = AchievementEvaluator.progress(t, n, LocalDate.now(zone), zone)
            val items = Achievement.entries.map { a ->
                val current = progress[a] ?: 0
                // Once unlocked it stays unlocked, even if the tasks are deleted later.
                AchievementItem(a, current, unlocked = a in saved || current >= a.target)
            }
            AchievementsUiState(items, items.count { it.unlocked })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())
}