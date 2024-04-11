package com.example.tacomamusicplayer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    //TODO add timber to the project!

    val TAG = MainViewModel::class.java.simpleName

    init {
        Log.d(TAG, "init: ")
    }

    fun doSomething() {
        Log.d(TAG, "doSomething: ")
    }



}