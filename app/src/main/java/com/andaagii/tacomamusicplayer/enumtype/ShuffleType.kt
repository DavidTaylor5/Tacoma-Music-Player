package com.andaagii.tacomamusicplayer.enumtype

import com.andaagii.tacomamusicplayer.constants.Const
import timber.log.Timber

/**
 * Shuffle state for the playback queue.
 *
 * The selected value is serialised to DataStore as the `Const` string returned by [type]
 * and deserialised back via [determineShuffleTypeFromString]. The default state when no
 * preference has been saved — or when an unrecognised string is read — is [NOT_SHUFFLED].
 */
enum class ShuffleType {

    /** Tracks play in their original cataloged or user-defined order. */
    NOT_SHUFFLED {
        override fun type(): String {
            return Const.NOT_SHUFFLED
        }
    },

    /** Tracks play in a randomised order built when shuffle is activated. */
    SHUFFLED {
        override fun type(): String {
            return Const.SHUFFLED
        }
    };

    /** Returns the DataStore string key that identifies this shuffle state. */
    abstract fun type(): String

    companion object {

        /**
         * Deserialises a DataStore string back to a [ShuffleType].
         *
         * Matches against the `Const` string for each entry. Logs a debug warning and falls
         * back to [NOT_SHUFFLED] if [shuffleTypeStr] does not match any known value, ensuring
         * playback always starts in a predictable state after a corrupt or missing preference.
         *
         * @param shuffleTypeStr The string previously written to DataStore via [type].
         * @return The matching [ShuffleType], or [NOT_SHUFFLED] if unrecognised.
         */
        fun determineShuffleTypeFromString(shuffleTypeStr: String): ShuffleType {
            return when (shuffleTypeStr) {
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
