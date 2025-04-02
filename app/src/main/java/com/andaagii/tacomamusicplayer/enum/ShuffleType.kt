package com.andaagii.tacomamusicplayer.enum

import com.andaagii.tacomamusicplayer.constants.Const
import timber.log.Timber

enum class ShuffleType {
    NOT_SHUFFLED {
        override fun type(): String {
            return Const.NOT_SHUFFLED
        }
    },
    SHUFFLED {
        override fun type(): String {
            return Const.SHUFFLED
        }
    };

    abstract fun type(): String

    companion object {
        fun determineShuffleTypeFromString(shuffleTypeStr: String): ShuffleType {
            return when(shuffleTypeStr) {
                Const.NOT_SHUFFLED -> NOT_SHUFFLED
                Const.SHUFFLED -> SHUFFLED
                else -> {
                    Timber.d("determineShuffleTypeFromString: unknown shuffleTypeStr, setting as default NOT_SHUFFLED")
                    NOT_SHUFFLED
                }
            }
        }
    }
}