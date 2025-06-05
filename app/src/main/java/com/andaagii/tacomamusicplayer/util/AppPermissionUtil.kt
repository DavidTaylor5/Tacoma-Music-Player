package com.andaagii.tacomamusicplayer.util

import android.content.Context
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import timber.log.Timber

class AppPermissionUtil {

    companion object {
        const val externalRequestCode = 3
        const val readMediaAudioRequestCode = 4
        const val readExternalStorageCode = 5
    }

//    private val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
    private val externalPermission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
    private val readMediaAudioPermission = Manifest.permission.READ_MEDIA_AUDIO

    //api < 33
    private val readExternalMediaPermission = Manifest.permission.READ_EXTERNAL_STORAGE

//    private val readMediaImagesPermission = Manifest.permission.READ_MEDIA_IMAGES

    /**
     * Determine if permission "MANAGE_EXTERNAL_STORAGE" is granted on the device.
     * This permission is deprecated, I don't want to use this permission as media store is the new
     * recommended way to interact with external files.
     */
    fun verifyExternalPermission(context: Context): Boolean {
        var readExternalPermission: Int = ContextCompat.checkSelfPermission(context, externalPermission)
        Timber.d("verifyExternalPermission: ${readExternalPermission == PackageManager.PERMISSION_GRANTED}")
        return readExternalPermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests "MANAGE_EXTERNAL_STORAGE" permission.
     */
    fun requestExternalPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(externalPermission), externalRequestCode)
    }

    /**
     * Determine if permission "READ_MEDIA_AUDIO" is granted by the user.
     */
    fun verifyReadMediaAudioPermission(context: Context): Boolean {
        val readReadMediaAudioPermission: Int = ContextCompat.checkSelfPermission(context, readMediaAudioPermission)
        Timber.d("verifyReadMediaAudioPermission: ${readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED}")
        return readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request permission "READ_MEDIA_AUDIO" which is necessary to read audio files in music folder
     * of user's device. If sdk is < 33, I need the read_external_storage permission
     */
    fun requestReadMediaAudioPermission(context: Context) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(readExternalMediaPermission), readExternalStorageCode)
        } else {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(readMediaAudioPermission), readMediaAudioRequestCode)
        }
    }
}