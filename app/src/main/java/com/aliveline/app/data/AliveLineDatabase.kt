package com.aliveline.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aliveline.app.data.note.NoteDao
import com.aliveline.app.data.note.NoteEntity
import com.aliveline.app.data.task.SubtaskEntity
import com.aliveline.app.data.task.TaskDao
import com.aliveline.app.data.task.TaskEntity

@Database(
    entities = [TaskEntity::class, SubtaskEntity::class, NoteEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AliveLineDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
}