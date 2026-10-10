package com.andaagii.tacomamusicplayer.di

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.SessionToken
import com.andaagii.tacomamusicplayer.service.MusicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicModule {
    @Provides
    @Singleton
    fun provideSessionToken(
        @ApplicationContext context: Context
    ): SessionToken =
        SessionToken(context, ComponentName(context, MusicService::class.java))
}