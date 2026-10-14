package com.aliveline.app.di

import android.content.Context
import androidx.room.Room
import com.aliveline.app.data.AliveLineDatabase
import com.aliveline.app.data.MIGRATION_1_2
import com.aliveline.app.data.MIGRATION_2_3
import com.aliveline.app.data.MIGRATION_3_4
import com.aliveline.app.data.MIGRATION_4_5
import com.aliveline.app.data.MIGRATION_5_6
import com.aliveline.app.data.note.NoteDao
import com.aliveline.app.data.task.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AliveLineDatabase =
        Room.databaseBuilder(context, AliveLineDatabase::class.java, "aliveline.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    fun provideTaskDao(db: AliveLineDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideNoteDao(db: AliveLineDatabase): NoteDao = db.noteDao()
}