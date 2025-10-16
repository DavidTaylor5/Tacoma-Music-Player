package com.andaagii.tacomamusicplayer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.andaagii.tacomamusicplayer.util.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TacomaMusicPlayerApplication: Application(), androidx.work.Configuration.Provider  { // , Configuration.Provider

    // So I can use hilt with my workers
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG) {
            Timber.plant(FileLoggingTree(
                logDir = getExternalFilesDir(null),
            ))
        }
    }
}