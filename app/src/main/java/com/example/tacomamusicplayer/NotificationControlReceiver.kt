package com.example.tacomamusicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationControlReceiver(): BroadcastReceiver() {

    val TAG = NotificationControlReceiver::class.java.simpleName

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(TAG, "onReceive: ")
    }

}