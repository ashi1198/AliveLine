package com.aliveline.app.data.task

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks")
    abstract fun observeAll(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract fun observeById(id: Long): Flow<TaskWithSubtasks?>

    @Insert
    abstract suspend fun insertTask(task: TaskEntity): Long

    @Insert
    abstract suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Transaction
    open suspend fun insertWithSubtasks(task: TaskEntity, subtasks: List<SubtaskEntity>): Long {
        val id = insertTask(task)
        if (subtasks.isNotEmpty()) insertSubtasks(subtasks.map { it.copy(taskId = id) })
        return id
    }

    @Update
    abstract suspend fun updateTask(task: TaskEntity)

    @Upsert
    abstract suspend fun upsertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    abstract suspend fun deleteAllSubtasks(taskId: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId AND id NOT IN (:keepIds)")
    abstract suspend fun deleteSubtasksNotIn(taskId: Long, keepIds: List<Long>)

    /** Edits a task and its subtasks in one transaction. Subtasks with id 0 are new; missing ones are removed. */
    @Transaction
    open suspend fun updateWithSubtasks(task: TaskEntity, subtasks: List<SubtaskEntity>) {
        updateTask(task)
        val keep = subtasks.map { it.id }.filter { it > 0 }
        if (keep.isEmpty()) deleteAllSubtasks(task.id) else deleteSubtasksNotIn(task.id, keep)
        if (subtasks.isNotEmpty()) upsertSubtasks(subtasks.map { it.copy(taskId = task.id) })
    }

    @Query("UPDATE tasks SET completedAt = :completedAt WHERE id = :id")
    abstract suspend fun setCompletedAt(id: Long, completedAt: Long?)

    @Query("SELECT completedAt FROM tasks WHERE id = :id")
    abstract suspend fun completedAtOf(id: Long): Long?

    @Query("SELECT bot FROM tasks WHERE id = :id")
    abstract suspend fun botOf(id: Long): String?

    @Query("SELECT * FROM completions")
    abstract fun observeCompletions(): Flow<List<CompletionEntity>>

    @Insert
    abstract suspend fun insertCompletion(completion: CompletionEntity)

    @Query("DELETE FROM completions WHERE taskId = :taskId AND epochDay = :epochDay")
    abstract suspend fun deleteCompletion(taskId: Long, epochDay: Long)

    /**
     * Marks a task done (completedAt != null) or not done (null) and keeps the history in step.
     * Completing again on the same day never creates a duplicate; undoing removes that day's record.
     */
    @Transaction
    open suspend fun completeTask(id: Long, completedAt: Long?, epochDay: Long?, previousEpochDay: Long?) {
        setCompletedAt(id, completedAt)
        if (completedAt == null) {
            if (previousEpochDay != null) deleteCompletion(id, previousEpochDay)
        } else if (epochDay != null) {
            val bot = botOf(id) ?: return
            deleteCompletion(id, epochDay)
            insertCompletion(CompletionEntity(taskId = id, bot = bot, epochDay = epochDay, completedAt = completedAt))
        }
    }

    @Query("UPDATE tasks SET startMinute = :startMinute, durationMin = :durationMin WHERE id = :id")
    abstract suspend fun setSchedule(id: Long, startMinute: Int?, durationMin: Int?)

    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    abstract suspend fun setSubtaskDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    abstract suspend fun deleteTask(id: Long)
}