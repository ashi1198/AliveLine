package com.aliveline.app.data.task

import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomTaskRepository @Inject constructor(
    private val dao: TaskDao,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTask(id: Long): Flow<Task?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun addTask(task: Task): Long =
        dao.insertWithSubtasks(task.toEntity(), task.subtasks.map { it.toEntity(taskId = 0) })

    override suspend fun setTaskCompletedAt(id: Long, completedAt: Long?) =
        dao.setCompletedAt(id, completedAt)

    override suspend fun setSubtaskDone(subtaskId: Long, done: Boolean) =
        dao.setSubtaskDone(subtaskId, done)

    override suspend fun deleteTask(id: Long) = dao.deleteTask(id)
}