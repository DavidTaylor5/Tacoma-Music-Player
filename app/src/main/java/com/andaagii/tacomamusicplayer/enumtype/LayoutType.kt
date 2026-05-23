package com.andaagii.tacomamusicplayer.enumtype

import timber.log.Timber

/**
 * RecyclerView layout mode for the album and playlist browsing pages.
 *
 * The selected value is serialised to DataStore as the string returned by [type] and
 * deserialised back via [determineLayoutFromString]. The default layout when no preference
 * has been saved — or when an unrecognised string is read — is [LINEAR_LAYOUT].
 */
enum class LayoutType {

    /** Single-column vertical list layout, backed by `AlbumListAdapter` / `PlaylistAdapter`. */
    LINEAR_LAYOUT {
        override fun type(): String {
            return "Linear"
        }
    },

    /** Two-column grid layout, backed by `AlbumGridAdapter` / `PlaylistGridAdapter`. */
    TWO_GRID_LAYOUT {
        override fun type(): String {
            return "2x2 Grid"
        }
    };

    /** Returns the DataStore string key that identifies this layout mode. */
    abstract fun type(): String

    companion object {

        /**
         * Deserialises a DataStore string back to a [LayoutType].
         *
         * Matches against each entry's [type] string. Logs a debug warning and falls back
         * to [LINEAR_LAYOUT] if [layout] does not match any known value, ensuring the UI
         * always has a valid layout mode even after a schema change or corrupt preference.
         *
         * @param layout The string previously written to DataStore via [type].
         * @return The matching [LayoutType], or [LINEAR_LAYOUT] if unrecognised.
         */
        fun determineLayoutFromString(layout: String): LayoutType {
            return when (layout) {
                LINEAR_LAYOUT.type() -> LINEAR_LAYOUT
                TWO_GRID_LAYOUT.type() -> TWO_GRID_LAYOUT
                else -> {
                    Timber.d("determineLayoutFromString: UNKNOWN LAYOUT TYPE, returning LINEAR_LAYOUT")
                    LINEAR_LAYOUT
                }
            }
        }
    }
}
