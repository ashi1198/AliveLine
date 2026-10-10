package com.aliveline.app.di

import com.aliveline.app.data.onboarding.DataStoreOnboardingRepository
import com.aliveline.app.domain.repository.OnboardingRepository
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
}
