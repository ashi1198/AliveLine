package com.aliveline.app.data.note

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aliveline.app.data.task.TaskEntity

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL, // deleting a task keeps its notes, just detached
        ),
    ],
    indices = [Index("taskId")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val tag: String?,
    val pinned: Boolean,
    val taskId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val color: String = "DEFAULT",
)