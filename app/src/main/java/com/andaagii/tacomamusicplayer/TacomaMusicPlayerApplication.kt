package com.andaagii.tacomamusicplayer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.andaagii.tacomamusicplayer.util.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point, annotated with [HiltAndroidApp] to trigger Hilt's code generation.
 *
 * Implements [Configuration.Provider] so WorkManager uses the Hilt-injected [HiltWorkerFactory],
 * which is required for [androidx.hilt.work.HiltWorker]-annotated workers such as
 * [com.andaagii.tacomamusicplayer.worker.CatalogMusicWorker].
 *
 * In debug builds, plants a [com.andaagii.tacomamusicplayer.util.FileLoggingTree] so that Timber
 * log output is written to a file on external storage for post-session inspection.
 */
@HiltAndroidApp
class TacomaMusicPlayerApplication: Application(), androidx.work.Configuration.Provider  { // , Configuration.Provider

    // Required for Hilt-injected workers; must be injected at the Application level
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // File-based logging is only active in debug builds to avoid leaking logs in production
        if(BuildConfig.DEBUG) {
            Timber.plant(FileLoggingTree(
                logDir = getExternalFilesDir(null),
            ))
        }
    }
}