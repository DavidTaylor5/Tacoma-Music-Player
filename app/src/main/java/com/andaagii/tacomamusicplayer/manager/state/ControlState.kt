package com.andaagii.tacomamusicplayer.manager.state

import androidx.media3.common.Player
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType

data class ControlState(
    val shuffleMode: ShuffleType = ShuffleType.NOT_SHUFFLED,
    val loopMode: Int = Player.REPEAT_MODE_OFF,
    val isPlaying: Boolean = false
)
