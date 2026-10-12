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

    @Query("UPDATE tasks SET startMinute = :startMinute, durationMin = :durationMin WHERE id = :id")
    abstract suspend fun setSchedule(id: Long, startMinute: Int?, durationMin: Int?)

    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    abstract suspend fun setSubtaskDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    abstract suspend fun deleteTask(id: Long)
}