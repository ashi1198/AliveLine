package com.aliveline.app.di

import com.aliveline.app.data.achievement.DataStoreAchievementRepository
import com.aliveline.app.domain.repository.AchievementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AchievementModule {
    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: DataStoreAchievementRepository): AchievementRepository
}