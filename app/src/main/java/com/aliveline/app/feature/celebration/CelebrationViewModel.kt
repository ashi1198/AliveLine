package com.aliveline.app.feature.celebration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliveline.app.domain.AchievementEvaluator
import com.aliveline.app.domain.model.Achievement
import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.completedOn
import com.aliveline.app.domain.model.currentTodos
import com.aliveline.app.domain.repository.AchievementRepository
import com.aliveline.app.domain.repository.NoteRepository
import com.aliveline.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class CelebrationLevel { NORMAL, BIG }

data class Celebration(val id: Long, val level: CelebrationLevel, val bot: BotType?)

/**
 * Watches the data (not the buttons), so any screen that completes a task gets the same celebration.
 * - A task newly completed today -> confetti (BIG if it was the last todo of the day).
 * - An achievement newly unlocked -> banner + BIG confetti.
 */
@HiltViewModel
class CelebrationViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val notes: NoteRepository,
    private val achievements: AchievementRepository,
) : ViewModel() {

    private val _celebrations = MutableSharedFlow<Celebration>(extraBufferCapacity = 4)
    val celebrations: SharedFlow<Celebration> = _celebrations.asSharedFlow()

    private val _banner = MutableStateFlow<Achievement?>(null)
    val banner: StateFlow<Achievement?> = _banner.asStateFlow()

    private val queue = ArrayDeque<Achievement>()
    private val announced = mutableSetOf<Achievement>()
    private var bannerJob: Job? = null
    private var nextId = 0L
    private var previousDone: Set<Long>? = null

    init {
        viewModelScope.launch { watchCompletions() }
        viewModelScope.launch { watchAchievements() }
    }

    private suspend fun watchCompletions() {
        tasks.observeTasks().collect { list ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val doneNow = list.completedOn(today, zone)
            val ids = doneNow.map { it.id }.toSet()

            val before = previousDone
            previousDone = ids
            if (before == null) return@collect // first load is just the baseline

            val newlyDone = ids - before
            if (newlyDone.isEmpty()) return@collect

            val bot = doneNow.first { it.id in newlyDone }.bot
            val level = if (list.currentTodos(today, zone).isEmpty()) CelebrationLevel.BIG else CelebrationLevel.NORMAL
            emit(level, bot)
        }
    }

    private suspend fun watchAchievements() {
        combine(tasks.observeTasks(), notes.observeNotes(), achievements.unlocked) { t, n, saved ->
            Triple(t, n, saved)
        }.collect { (taskList, noteList, saved) ->
            val zone = ZoneId.systemDefault()
            val unlockedNow = AchievementEvaluator.unlocked(taskList, noteList, LocalDate.now(zone), zone)
            val fresh = unlockedNow - saved - announced
            if (fresh.isEmpty()) return@collect

            announced += fresh
            achievements.markUnlocked(fresh)
            queue.addAll(fresh.sortedBy { it.ordinal })
            emit(CelebrationLevel.BIG, null)
            showNextBanner()
        }
    }

    private fun emit(level: CelebrationLevel, bot: BotType?) {
        _celebrations.tryEmit(Celebration(nextId++, level, bot))
    }

    private fun showNextBanner() {
        if (bannerJob?.isActive == true) return
        bannerJob = viewModelScope.launch {
            while (queue.isNotEmpty()) {
                _banner.value = queue.removeFirst()
                delay(3_500)
                _banner.value = null
                delay(300)
            }
        }
    }
}