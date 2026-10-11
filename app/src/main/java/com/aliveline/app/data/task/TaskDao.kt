package com.aliveline.app.data.task

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("UPDATE tasks SET completedAt = :completedAt WHERE id = :id")
    abstract suspend fun setCompletedAt(id: Long, completedAt: Long?)

    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    abstract suspend fun setSubtaskDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    abstract suspend fun deleteTask(id: Long)
}