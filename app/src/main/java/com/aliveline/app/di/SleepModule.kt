package com.aliveline.app.di

import com.aliveline.app.data.sleep.RoomSleepRepository
import com.aliveline.app.domain.repository.SleepRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SleepModule {
    @Binds
    @Singleton
    abstract fun bindSleepRepository(impl: RoomSleepRepository): SleepRepository
}