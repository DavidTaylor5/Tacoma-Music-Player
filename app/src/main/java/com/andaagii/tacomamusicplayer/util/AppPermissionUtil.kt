package com.andaagii.tacomamusicplayer.util

import android.content.Context
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import timber.log.Timber

/**
 * Checks and requests audio-related runtime permissions.
 *
 * Handles the API 33 split: [Manifest.permission.READ_MEDIA_AUDIO] is used on API ≥ 33
 * (Android 13 / Tiramisu); [Manifest.permission.READ_EXTERNAL_STORAGE] is used on older
 * devices. [Manifest.permission.MANAGE_EXTERNAL_STORAGE] is retained for legacy reference
 * only and is no longer actively used — MediaStore is the recommended approach for
 * external storage access on modern Android.
 */
class AppPermissionUtil {

    companion object {
        /** Identifies the [requestExternalPermission] callback in `onRequestPermissionsResult`. */
        const val externalRequestCode = 3

        /** Identifies the [requestReadMediaAudioPermission] callback on API ≥ 33. */
        const val readMediaAudioRequestCode = 4

        /** Identifies the [requestReadMediaAudioPermission] callback on API < 33. */
        const val readExternalStorageCode = 5
    }

    /**
     * Deprecated legacy permission kept for reference only. `MANAGE_EXTERNAL_STORAGE` grants
     * broad file-system access that Google Play no longer approves for general media apps;
     * [readMediaAudioPermission] is the active permission used for audio access.
     */
    private val externalPermission = Manifest.permission.MANAGE_EXTERNAL_STORAGE

    /** Active audio-read permission required on API ≥ 33 (Android 13 / Tiramisu). */
    private val readMediaAudioPermission = Manifest.permission.READ_MEDIA_AUDIO

    /** Fallback audio-read permission for API < 33 devices. */
    private val readExternalMediaPermission = Manifest.permission.READ_EXTERNAL_STORAGE

    /**
     * Returns `true` if the deprecated `MANAGE_EXTERNAL_STORAGE` permission is currently
     * granted. Kept for legacy diagnostic purposes; prefer [verifyReadMediaAudioPermission]
     * for all active permission checks.
     *
     * @param context The application or activity context used to check the grant state.
     * @return `true` if `MANAGE_EXTERNAL_STORAGE` is granted, `false` otherwise.
     */
    fun verifyExternalPermission(context: Context): Boolean {
        val readExternalPermission: Int = ContextCompat.checkSelfPermission(context, externalPermission)
        Timber.d("verifyExternalPermission: ${readExternalPermission == PackageManager.PERMISSION_GRANTED}")
        return readExternalPermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests the deprecated `MANAGE_EXTERNAL_STORAGE` permission. Not used in the active
     * permission flow; kept for legacy reference only.
     *
     * @param context The activity context. Cast to [Activity] internally because
     *   [ActivityCompat.requestPermissions] requires an `Activity`.
     */
    fun requestExternalPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(externalPermission), externalRequestCode)
    }

    /**
     * Returns `true` if `READ_MEDIA_AUDIO` is currently granted on this device. This is the
     * primary permission check before querying [android.provider.MediaStore] for audio tracks.
     *
     * @param context The application or activity context used to check the grant state.
     * @return `true` if `READ_MEDIA_AUDIO` is granted, `false` otherwise.
     */
    fun verifyReadMediaAudioPermission(context: Context): Boolean {
        val readReadMediaAudioPermission: Int = ContextCompat.checkSelfPermission(context, readMediaAudioPermission)
        Timber.d("verifyReadMediaAudioPermission: ${readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED}")
        return readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests the runtime permission required to read audio files from device storage.
     *
     * On API ≥ 33 (Android 13 / Tiramisu) the granular [Manifest.permission.READ_MEDIA_AUDIO]
     * permission is requested, identified in `onRequestPermissionsResult` by
     * [readMediaAudioRequestCode]. On older APIs, the broader
     * [Manifest.permission.READ_EXTERNAL_STORAGE] is requested instead, identified by
     * [readExternalStorageCode].
     *
     * @param context The activity context. Cast to [Activity] internally because
     *   [ActivityCompat.requestPermissions] requires an `Activity`.
     */
    fun requestReadMediaAudioPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(readExternalMediaPermission), readExternalStorageCode)
        } else {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(readMediaAudioPermission), readMediaAudioRequestCode)
        }
    }
}
