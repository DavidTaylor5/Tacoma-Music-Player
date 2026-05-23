package com.andaagii.tacomamusicplayer.util

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import timber.log.Timber

/**
 * Pure sorting utility for album and playlist [MediaItem] lists.
 *
 * Contains the [SortingOption] enum defining the available sort orders and the static
 * [sortAlbums] / [sortPlaylists] functions that apply them. This class holds no state;
 * all functions are accessed via the [Companion] object.
 */
class SortingUtil {

    /**
     * Available sort orders for album and playlist [MediaItem] lists.
     *
     * Each entry is serialised to/from DataStore as the [Const] string returned by [type].
     * [determineSortingOptionFromTitle] maps those strings back to entries; unknown strings
     * fall back to [SORTING_NEWEST_RELEASE].
     */
    enum class SortingOption {

        /** Sorts items A→Z by title. Available for both albums and playlists. */
        SORTING_TITLE_ALPHABETICAL {
            override fun type(): String = Const.SORTING_TITLE_ALPHABETICAL
        },

        /** Sorts albums A→Z by album artist. Not applicable to playlists. */
        SORTING_ARTIST_ALPHABETICAL {
            override fun type(): String = Const.SORTING_ARTIST_ALPHABETICAL
        },

        /** Sorts albums descending by release year (newest first). */
        SORTING_NEWEST_RELEASE {
            override fun type(): String = Const.SORTING_NEWEST_RELEASE
        },

        /** Sorts albums ascending by release year (oldest first). */
        SORTING_OLDEST_RELEASE {
            override fun type(): String = Const.SORTING_OLDEST_RELEASE
        },

        /** Sorts playlists descending by creation timestamp (newest created first). Playlist only. */
        SORTING_BY_CREATION_DATE {
            override fun type(): String = Const.SORTING_CREATION_DATE
        },

        /** Sorts playlists descending by last-modification timestamp (recently edited first). Playlist only. */
        SORTING_BY_MODIFICATION_DATE {
            override fun type(): String = Const.SORTING_MODIFICATION_DATE
        };

        /** Returns the [Const] string used to persist and restore this sort option via DataStore. */
        abstract fun type(): String
    }

    companion object {
        /**
         * Maps a DataStore sort-option string back to a [SortingOption].
         *
         * The strings compared here are the [Const] values returned by [SortingOption.type],
         * which DataStore writes directly. Returns [SortingOption.SORTING_NEWEST_RELEASE] for
         * any unrecognised string, including the `"default"` sentinel emitted by
         * [DataStoreUtil.getAlbumSortingPreference] before the user has set a preference.
         *
         * @param sorting The raw sort-option string read from DataStore.
         * @return The corresponding [SortingOption], or [SortingOption.SORTING_NEWEST_RELEASE]
         *   as the fallback.
         */
        fun determineSortingOptionFromTitle(sorting: String): SortingOption {
            Timber.d("determineSortingOptionFromTitle: sorting=$sorting")
            return when (sorting) {
                Const.SORTING_TITLE_ALPHABETICAL -> SortingOption.SORTING_TITLE_ALPHABETICAL
                Const.SORTING_ARTIST_ALPHABETICAL -> SortingOption.SORTING_ARTIST_ALPHABETICAL
                Const.SORTING_NEWEST_RELEASE -> SortingOption.SORTING_NEWEST_RELEASE
                Const.SORTING_OLDEST_RELEASE -> SortingOption.SORTING_OLDEST_RELEASE
                Const.SORTING_CREATION_DATE -> SortingOption.SORTING_BY_CREATION_DATE
                Const.SORTING_MODIFICATION_DATE -> SortingOption.SORTING_BY_MODIFICATION_DATE
                else -> {
                    Timber.d("determineSortingOptionFromTitle: Unknown sorting string, defaulting to newest release first.")
                    SortingOption.SORTING_NEWEST_RELEASE
                }
            }
        }

        /**
         * Sorts [playlists] according to [sorting] and returns the sorted list.
         *
         * Playlist timestamps are encoded in each [MediaItem]'s `description` field as
         * `"<creationTimestamp>:<lastModificationTimestamp>"` by
         * [MediaItemUtil.createPlaylistMediaItemFromSongGroupEntity]. [getCreationTimestamp]
         * and [getModificationTimestamp] split on `":"` to extract the relevant value.
         *
         * Unrecognised sort options (including album-only options like
         * [SortingOption.SORTING_ARTIST_ALPHABETICAL]) fall through to the `else` branch,
         * which defaults to descending modification date.
         *
         * @param playlists The unsorted list of playlist [MediaItem] objects.
         * @param sorting The [SortingOption] to apply.
         * @return A new sorted list; the original [playlists] list is not mutated.
         */
        fun sortPlaylists(playlists: List<MediaItem>, sorting: SortingOption): List<MediaItem> {
            return when (sorting) {
                SortingOption.SORTING_TITLE_ALPHABETICAL -> {
                    playlists.sortedBy { playlist ->
                        playlist.mediaMetadata.albumTitle.toString()
                    }
                }
                SortingOption.SORTING_BY_CREATION_DATE -> {
                    playlists.sortedByDescending { playlist ->
                        getCreationTimestamp(playlist.mediaMetadata.description.toString())
                    }
                }
                SortingOption.SORTING_BY_MODIFICATION_DATE -> {
                    playlists.sortedByDescending { playlist ->
                        getModificationTimestamp(playlist.mediaMetadata.description.toString())
                    }
                }
                else -> {
                    // Default to most-recently modified first.
                    playlists.sortedByDescending { playlist ->
                        getModificationTimestamp(playlist.mediaMetadata.description.toString())
                    }
                }
            }
        }

        /**
         * Extracts the creation timestamp from a playlist description string.
         *
         * The description is encoded as `"<creationTimestamp>:<lastModificationTimestamp>"` by
         * [MediaItemUtil.createPlaylistMediaItemFromSongGroupEntity]. After splitting on `":"`,
         * index `[0]` holds the creation timestamp.
         *
         * @param playlistDescription The raw `description` string from a playlist [MediaItem].
         * @return The creation timestamp string, or `"Unknown"` if the format is unrecognised.
         */
        private fun getCreationTimestamp(playlistDescription: String): String {
            val timestamps = playlistDescription.split(":")
            return if (timestamps.isNotEmpty()) timestamps[0] else "Unknown"
        }

        /**
         * Extracts the last-modification timestamp from a playlist description string.
         *
         * The description is encoded as `"<creationTimestamp>:<lastModificationTimestamp>"` by
         * [MediaItemUtil.createPlaylistMediaItemFromSongGroupEntity]. After splitting on `":"`,
         * index `[1]` holds the modification timestamp.
         *
         * @param playlistDescription The raw `description` string from a playlist [MediaItem].
         * @return The last-modification timestamp string, or `"Unknown"` if the format is
         *   unrecognised or the description contains only one segment.
         */
        private fun getModificationTimestamp(playlistDescription: String): String {
            val timestamps = playlistDescription.split(":")
            return if (timestamps.size >= 2) timestamps[1] else "Unknown"
        }

        /**
         * Sorts [albums] according to [sorting] and returns the sorted list.
         *
         * Album metadata is read from [MediaItem.mediaMetadata]: `albumTitle`, `albumArtist`,
         * and `releaseYear`. Unrecognised sort options (including playlist-only options) fall
         * through to the `else` branch, which defaults to ascending release year.
         *
         * @param albums The unsorted list of album [MediaItem] objects.
         * @param sorting The [SortingOption] to apply.
         * @return A new sorted list; the original [albums] list is not mutated.
         */
        fun sortAlbums(albums: List<MediaItem>, sorting: SortingOption): List<MediaItem> {
            return when (sorting) {
                SortingOption.SORTING_TITLE_ALPHABETICAL -> {
                    albums.sortedBy { album ->
                        album.mediaMetadata.albumTitle.toString()
                    }
                }
                SortingOption.SORTING_ARTIST_ALPHABETICAL -> {
                    albums.sortedBy { album ->
                        album.mediaMetadata.albumArtist.toString()
                    }
                }
                SortingOption.SORTING_NEWEST_RELEASE -> {
                    albums.sortedByDescending { album ->
                        album.mediaMetadata.releaseYear
                    }
                }
                SortingOption.SORTING_OLDEST_RELEASE -> {
                    albums.sortedBy { album ->
                        album.mediaMetadata.releaseYear
                    }
                }
                else -> {
                    // Default to ascending release year for unrecognised options.
                    albums.sortedBy { album ->
                        album.mediaMetadata.releaseYear
                    }
                }
            }
        }
    }
}
