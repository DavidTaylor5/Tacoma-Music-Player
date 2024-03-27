package com.example.tacomamusicplayer.util

import android.content.Context
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat

class AppPermissionUtil {

    companion object {
        val notificationRequestCode = 2
        val externalRequestCode = 3
        val readMediaAudioRequestCode = 4
    }
    
    val TAG = AppPermissionUtil::class.java.simpleName

    val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
//    val notificationRequestCode = 2

    val externalPermission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
//    val externalRequestCode = 3

    val readMediaAudioPermission = Manifest.permission.READ_MEDIA_AUDIO
//    val readMediaAudioRequestCode = 4

    fun verifyExternalPermission(context: Context): Boolean {
        var readExternalPermission: Int = ContextCompat.checkSelfPermission(context, externalPermission)
        Log.d(TAG, "verifyExternalPermission: ${readExternalPermission == PackageManager.PERMISSION_GRANTED}")
        return readExternalPermission == PackageManager.PERMISSION_GRANTED
    }

    fun requestExternalPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(externalPermission), externalRequestCode)
    }

    fun verifyReadMediaAudioPermission(context: Context): Boolean {
        var readReadMediaAudioPermission: Int = ContextCompat.checkSelfPermission(context, readMediaAudioPermission)
        Log.d(TAG, "verifyReadMediaAudioPermission: ${readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED}")
        return readReadMediaAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    fun requestReadMediaAudioPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(readMediaAudioPermission), readMediaAudioRequestCode)
    }

    fun verifyNotificationPermission(context: Context): Boolean {
        var readNotificationPermission: Int = ContextCompat.checkSelfPermission(context, notificationPermission)
        Log.d(TAG, "verifyNotificationPermission: ${readNotificationPermission == PackageManager.PERMISSION_GRANTED}")
        return readNotificationPermission == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(context: Context) {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(notificationPermission), notificationRequestCode)
    }

}