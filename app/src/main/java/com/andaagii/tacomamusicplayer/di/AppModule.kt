package com.andaagii.tacomamusicplayer.di

import com.andaagii.tacomamusicplayer.repository.MusicProviderRepository
import com.andaagii.tacomamusicplayer.repository.MusicRepository
import com.andaagii.tacomamusicplayer.repository.MusicRepositoryImpl
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their single concrete implementation.
 *
 * Both [MusicRepository] and [MusicProviderRepository] resolve to the same
 * [MusicRepositoryImpl] singleton, so callers that only need read access (e.g.
 * [com.andaagii.tacomamusicplayer.service.MusicService]) receive a narrower type without
 * exposing write operations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        impl: MusicRepositoryImpl
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindMusicProviderRepository(
        impl: MusicRepositoryImpl
    ): MusicProviderRepository
}