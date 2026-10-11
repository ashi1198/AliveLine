package com.aliveline.app.di

import com.aliveline.app.data.note.RoomNoteRepository
import com.aliveline.app.data.onboarding.DataStoreOnboardingRepository
import com.aliveline.app.data.settings.DataStoreSettingsRepository
import com.aliveline.app.data.task.RoomTaskRepository
import com.aliveline.app.domain.repository.NoteRepository
import com.aliveline.app.domain.repository.OnboardingRepository
import com.aliveline.app.domain.repository.SettingsRepository
import com.aliveline.app.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: DataStoreOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: RoomTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: RoomNoteRepository): NoteRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}