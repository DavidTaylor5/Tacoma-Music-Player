package com.andaagii.tacomamusicplayer.di

import com.andaagii.tacomamusicplayer.manager.PlaybackManager
import com.andaagii.tacomamusicplayer.manager.PlaybackManagerImpl
import com.andaagii.tacomamusicplayer.repository.MusicProviderRepository
import com.andaagii.tacomamusicplayer.repository.MusicRepository
import com.andaagii.tacomamusicplayer.repository.MusicRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Binds
    @Singleton
    abstract fun bindMusicManager(
        impl: PlaybackManagerImpl
    ): PlaybackManager
}