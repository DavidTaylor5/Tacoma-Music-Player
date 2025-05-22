package com.andaagii.tacomamusicplayer.util

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves timber logs to a separate file for easier parsing.
 * Will create a log under Android/data/com.andaagii.tacomamusicplayer/files/logs
 */
class FileLoggingTree(logDir: File?): Timber.DebugTree() {

    private lateinit var logFile: File

    init {
        //TODO delete previous files in the logs folder
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        logFile = File(logDir, "logs/${timeStamp}_debug_logs.txt")

        // Create all parent directories if they don't exist
        logFile.parentFile?.let { parentFile ->
            if (!parentFile.exists()) {
                parentFile.mkdirs() // Creates all non-existing parent directories
            } else {
                deletePreviousLogs()
            }
        }
    }

    private fun deletePreviousLogs() {
        logFile.parentFile?.let { parentFile ->
            for(file in parentFile.listFiles()) {
                if(file.absoluteFile != logFile.absoluteFile) {
                    file.delete()
                }
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm::ss", Locale.getDefault()).format(Date())
        val priorityStr = getPriorityString(priority)
        val logMessage = "$timeStamp $priorityStr/${tag.orEmpty()}: $message\n"

        try {
            FileWriter(logFile, true).use { it.append(logMessage) }
        } catch (e: IOException) {
            Log.e("FileLoggingTree", "Error writing log to file", e)
        }

        super.log(priority, tag, message, t)
    }



    private fun getPriorityString(priority: Int): String {
        return when(priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "U"
        }
    }
}