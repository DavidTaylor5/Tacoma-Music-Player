package com.example.tacomamusicplayer.util

import android.content.Context
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import timber.log.Timber

class AppPermissionUtil {

    companion object {
        const val notificationRequestCode = 2
        const val externalRequestCode = 3
        const val readMediaAudioRequestCode = 4
        const val readMediaImages = 5
    }

    private val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
    private val externalPermission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
    private val readMediaAudioPermission = Manifest.permission.READ_MEDIA_AUDIO
    private val readMediaImagesPermission = Manifest.permission.READ_MEDIA_IMAGES

    fun verifyExternalPermission(context: Context): Boolean {
        var readExternalPermission: Int = ContextCompat.checkSelfPermission(context, externalPermission)
        Timber.d("verifyExternalPermission: ${readExternalPermission == PackageManager.PERMISSION_GRANTED}")
        return readExternalPermission == PackageManager.PERMISSION_GRANTED
    }

    fun requestExternalPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(externalPermission), externalRequestCode)
    }

    fun verifyReadMediaAudioPermission(context: Context): Boolean {
        val readReadMediaAudioPermission: Int = ContextCompat.checkSelfPermission(context, readMediaAudioPermission)
        Timber.d("verifyReadMediaAudioPermission: ${readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED}")
        return readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    fun requestReadMediaAudioPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(readMediaAudioPermission), readMediaAudioRequestCode)
    }

    fun verifyNotificationPermission(context: Context): Boolean {
        var readNotificationPermission: Int = ContextCompat.checkSelfPermission(context, notificationPermission)
        Timber.d("verifyNotificationPermission: ${readNotificationPermission == PackageManager.PERMISSION_GRANTED}")
        return readNotificationPermission == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(notificationPermission), notificationRequestCode)
    }

}