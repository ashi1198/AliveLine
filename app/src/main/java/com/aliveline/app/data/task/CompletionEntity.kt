package com.aliveline.app.data.task

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completions",
    indices = [Index("epochDay"), Index("taskId")],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val bot: String,
    val epochDay: Long,
    val completedAt: Long,
)