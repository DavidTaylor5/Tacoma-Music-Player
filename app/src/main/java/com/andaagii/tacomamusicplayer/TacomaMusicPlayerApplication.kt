package com.andaagii.tacomamusicplayer

import android.app.Application
import com.andaagii.tacomamusicplayer.util.FileLoggingTree
import timber.log.Timber
import java.io.File

class TacomaMusicPlayerApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(
                logDir = getExternalFilesDir(null),
            ))
        }
    }
}