package com.andaagii.tacomamusicplayer

import android.app.Application
import com.andaagii.tacomamusicplayer.util.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TacomaMusicPlayerApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG) {
            Timber.plant(FileLoggingTree(
                logDir = getExternalFilesDir(null),
            ))
        }
    }
}