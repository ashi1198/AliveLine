package com.aliveline.app.domain.repository

import com.aliveline.app.domain.model.Completion
import com.aliveline.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTask(id: Long): Flow<Task?>
    fun observeCompletions(): Flow<List<Completion>>
    suspend fun addTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun setTaskCompletedAt(id: Long, completedAt: Long?)
    suspend fun setSchedule(id: Long, startMinute: Int?, durationMin: Int?)
    suspend fun setSubtaskDone(subtaskId: Long, done: Boolean)
    suspend fun deleteTask(id: Long)
}