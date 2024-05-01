package com.example.tacomamusicplayer.util

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class UtilImpl {

    companion object {

        /**
         * Translates a given millisecond value into minutes and seconds.
         * @param msDuration Length in milliseconds.
         */
        fun calculateHumanReadableTimeFromMilliseconds(msDuration: Long): String {
            //duration object from given milliseconds
            val duration = msDuration.toDuration(DurationUnit.MILLISECONDS)
            //calculate in whole minutes
            val minutes = duration.inWholeMinutes
            //subtract whole minutes from original milliseconds to get remaining whole seconds.
            val seconds = duration.minus(minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds
            //Return formatted string
            return "$minutes:$seconds"
        }

        /**
         * Call this function onResume(). Removes the navigation bar from the bottom of the screen.
         */
        fun hideNavigationUI(window: Window) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.navigationBars())
            } else {
                val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

                window.decorView.systemUiVisibility = flags
            }
        }
    }
}