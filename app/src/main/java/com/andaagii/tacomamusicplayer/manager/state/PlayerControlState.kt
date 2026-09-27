package com.andaagii.tacomamusicplayer.manager.state

import androidx.media3.common.Player
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType

/**
 * Determines the Player Control State.
 */
data class PlayerControlState(
    val isPlaying: Boolean = false,
    val shuffleMode: ShuffleType = ShuffleType.NOT_SHUFFLED,
    val loopMode: Int = Player.REPEAT_MODE_OFF
)