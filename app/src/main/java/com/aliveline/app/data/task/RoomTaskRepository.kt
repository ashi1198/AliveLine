package com.aliveline.app.data.task

import com.aliveline.app.domain.model.BotType
import com.aliveline.app.domain.model.Completion
import com.aliveline.app.domain.model.Task
import com.aliveline.app.domain.repository.TaskRepository
import java.time.Instant
import java.time.ZoneId
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

    override fun observeCompletions(): Flow<List<Completion>> =
        dao.observeCompletions().map { list -> list.map { it.toDomain() } }

    override suspend fun addTask(task: Task): Long =
        dao.insertWithSubtasks(task.toEntity(), task.subtasks.map { it.toEntity(taskId = 0) })

    override suspend fun updateTask(task: Task) =
        dao.updateWithSubtasks(task.toEntity(), task.subtasks.map { it.toEntity(task.id) })

    override suspend fun setTaskCompletedAt(id: Long, completedAt: Long?) {
        val zone = ZoneId.systemDefault()
        fun dayOf(ms: Long): Long = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

        val previous = dao.completedAtOf(id)
        dao.completeTask(
            id = id,
            completedAt = completedAt,
            epochDay = completedAt?.let { dayOf(it) },
            previousEpochDay = previous?.let { dayOf(it) },
        )
    }

    override suspend fun setSchedule(id: Long, startMinute: Int?, durationMin: Int?) =
        dao.setSchedule(id, startMinute, durationMin)

    override suspend fun setSubtaskDone(subtaskId: Long, done: Boolean) =
        dao.setSubtaskDone(subtaskId, done)

    override suspend fun deleteTask(id: Long) = dao.deleteTask(id)
}

private fun CompletionEntity.toDomain(): Completion = Completion(
    taskId = taskId,
    bot = runCatching { BotType.valueOf(bot) }.getOrDefault(BotType.BRAIN),
    epochDay = epochDay,
    completedAt = completedAt,
)