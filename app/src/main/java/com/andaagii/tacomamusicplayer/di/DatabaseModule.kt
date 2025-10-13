package com.andaagii.tacomamusicplayer.di

import android.content.Context
import com.andaagii.tacomamusicplayer.database.PlayerDatabase
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PlayerDatabase = PlayerDatabase.getDatabase(context)

    @Provides
    fun provideSongDao(
        db: PlayerDatabase
    ): SongDao = db.songDao()

    @Provides
    fun provideSongGroupDao(
        db: PlayerDatabase
    ): SongGroupDao = db.songGroupDao()
}