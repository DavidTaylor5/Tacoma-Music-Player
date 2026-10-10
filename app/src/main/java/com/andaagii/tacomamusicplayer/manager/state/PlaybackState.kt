package com.andaagii.tacomamusicplayer.manager.state

import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType

/**
 * Determines the Player Control State.
 */
data class PlaybackState(
    val controlState: ControlState = ControlState(),
    val positionState: PositionState = PositionState()
)