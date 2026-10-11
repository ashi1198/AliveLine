package com.aliveline.app.di

import com.aliveline.app.data.focus.DataStoreFocusRepository
import com.aliveline.app.domain.repository.FocusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FocusModule {
    @Binds
    @Singleton
    abstract fun bindFocusRepository(impl: DataStoreFocusRepository): FocusRepository
}