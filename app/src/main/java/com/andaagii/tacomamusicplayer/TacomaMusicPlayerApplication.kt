package com.andaagii.tacomamusicplayer

import android.app.Application
import timber.log.Timber

class TacomaMusicPlayerApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}