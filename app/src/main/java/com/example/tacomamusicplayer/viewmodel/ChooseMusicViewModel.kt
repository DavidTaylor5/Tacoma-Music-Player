package com.example.tacomamusicplayer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel

class ChooseMusicViewModel: ViewModel() {

    val TAG: String =  ChooseMusicViewModel::class.java.simpleName

    init {
        Log.d(TAG, "init: ")
    }

}