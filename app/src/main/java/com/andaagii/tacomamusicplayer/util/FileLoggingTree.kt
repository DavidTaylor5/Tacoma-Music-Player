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
 * A Timber [Timber.DebugTree] that mirrors every log entry to a text file on disk.
 *
 * On construction, a new log file is created at
 * `<externalFilesDir>/logs/<yyyy-MM-dd HH:mm>_debug_logs.txt`. If the `logs/` directory
 * already exists, all log files from previous sessions are deleted before the new file is
 * opened — this prevents unbounded disk growth while keeping the current session's output
 * intact for device-side inspection.
 *
 * Each entry is appended in the format `yyyy-MM-dd HH:mm::ss P/TAG: message` followed by a
 * newline, and Logcat output is preserved via `super.log()`.
 *
 * The full log directory path is:
 * `Android/data/com.andaagii.tacomamusicplayer/files/logs`
 *
 * @param logDir The parent directory for the `logs/` sub-folder, typically the value of
 *   [android.content.Context.getExternalFilesDir] called with `null`.
 */
class FileLoggingTree(logDir: File?) : Timber.DebugTree() {

    private lateinit var logFile: File

    init {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        logFile = File(logDir, "logs/${timeStamp}_debug_logs.txt")

        // If the directory is new, mkdirs() creates it along with any missing parents.
        // If it already exists, delete stale log files from previous sessions before opening
        // the new log — we only ever need the current session's output on device.
        logFile.parentFile?.let { parentFile ->
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            } else {
                deletePreviousLogs()
            }
        }
    }

    /**
     * Deletes all log files in the same directory except the current session's file.
     *
     * Identifies the current log by absolute path equality so the newly created file is
     * never removed even if its timestamp matches a stale file name.
     */
    private fun deletePreviousLogs() {
        logFile.parentFile?.let { parentFile ->
            for (file in parentFile.listFiles()) {
                if (file.absoluteFile != logFile.absoluteFile) {
                    file.delete()
                }
            }
        }
    }

    /**
     * Formats and appends [message] to the log file, then forwards the call to Logcat.
     *
     * The entry format is: `yyyy-MM-dd HH:mm::ss P/TAG: message\n` where `P` is the
     * single-letter priority code returned by [getPriorityString]. Using `append = true` in
     * [FileWriter] ensures entries accumulate across multiple log calls within the same session.
     *
     * @param priority The Android log priority constant (e.g., [Log.DEBUG]).
     * @param tag The tag derived by Timber from the calling class name, or `null`.
     * @param message The formatted log message.
     * @param t An optional [Throwable] accompanying this log entry.
     */
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

    /**
     * Maps an Android [Log] priority integer to a single-letter code for compact log output.
     *
     * | Priority | Code |
     * |----------|------|
     * | VERBOSE  | V    |
     * | DEBUG    | D    |
     * | INFO     | I    |
     * | WARN     | W    |
     * | ERROR    | E    |
     * | ASSERT   | A    |
     * | unknown  | U    |
     *
     * @param priority An Android `Log.*` priority constant.
     * @return The corresponding single-letter code, or `"U"` for unrecognised values.
     */
    private fun getPriorityString(priority: Int): String {
        return when (priority) {
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
