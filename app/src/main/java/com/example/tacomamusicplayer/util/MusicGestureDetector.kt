package com.example.tacomamusicplayer.util

import android.view.GestureDetector
import android.view.MotionEvent
import timber.log.Timber

class MusicGestureDetector: GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    override fun onDown(event: MotionEvent): Boolean {
        Timber.d("onDown: $event")
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        Timber.d("onLongPress: $event")
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        Timber.d("onFling: $e1 $e2")
        return true
    }

    override fun onShowPress(event: MotionEvent) {
        Timber.d("onShowPress: $event")
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        Timber.d("onSingleTapUp: $event")
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        Timber.d("onScroll: $e1 $e2")
        return true
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        Timber.d("onDoubleTap: $event")
        return true
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        Timber.d("onDoubleTapEvent: $event")
        return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        Timber.d("onSingleTapConfirmed: $event")
        return true
    }



}