package com.andaagii.tacomamusicplayer.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import timber.log.Timber

/**
 * [ContentObserver] registered on the MediaStore audio URI that triggers a library re-catalog
 * whenever the device's music files change (e.g. a file is added or deleted).
 *
 * To prevent hammering the database on rapid successive MediaStore events (common when copying
 * multiple files), updates are debounced with a 30-second delay. Only one catalog job runs at
 * a time; additional [onChange] calls while a job is in flight are ignored.
 *
 * Must be registered and unregistered by the caller (currently [MainActivity]) to match the
 * appropriate lifecycle scope.
 *
 * @param handler [Handler] passed to [ContentObserver] for thread scheduling.
 * @param context Application context used to display the update toast.
 * @param onContentChange Callback invoked after the debounce delay to trigger re-cataloging.
 */
class MusicContentObserver(
    handler: Handler,
    val context: Context,
    val onContentChange: () -> Unit
    ): ContentObserver(handler) {

    private val delayCheckHandler = Handler(Looper.getMainLooper())
    private var currentlyLoadingSongs: Boolean = false

    /**
     * Called when the observed MediaStore URI changes.
     *
     * Ignores the event if a catalog job is already queued or running. Otherwise, sets the
     * in-flight flag immediately to block further triggers, shows a toast, and posts a delayed
     * call to [onContentChange] so rapid file-system events are collapsed into a single update.
     *
     * @param selfChange `true` if the change was triggered by this observer's own write.
     * @param uri The URI of the content that changed, or `null` if unknown.
     */
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Timber.d("onChange: selfChange=$selfChange, uri=$uri")

        if(!currentlyLoadingSongs) {
            currentlyLoadingSongs = true
            Toast.makeText(context, "Updating Album List...", Toast.LENGTH_SHORT).show()

            // 30-second debounce: collapses bursts of MediaStore events (e.g. bulk file copies)
            // into a single catalog run
            delayCheckHandler.postDelayed({
                //TODO only update album list if new album list != old album list
                //query new albums
                onContentChange()

                currentlyLoadingSongs = false
            }, 30_000)
        }
    }
}