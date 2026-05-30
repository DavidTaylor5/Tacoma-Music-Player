package com.andaagii.tacomamusicplayer.di

import android.content.Context
import androidx.media3.session.MediaBrowser
import com.andaagii.tacomamusicplayer.database.PlayerDatabase
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and its DAOs as injectable singletons.
 *
 * The [PlayerDatabase] instance is created once per process via
 * [PlayerDatabase.getDatabase]; the DAOs are extracted from it and provided
 * individually so injection sites only depend on the interface they actually need.
 */
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