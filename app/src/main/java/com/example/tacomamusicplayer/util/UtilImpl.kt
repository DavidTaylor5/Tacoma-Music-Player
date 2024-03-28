package com.example.tacomamusicplayer.util

import kotlin.time.DurationUnit
import kotlin.time.toDuration

class UtilImpl {

    companion object {
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
    }
}