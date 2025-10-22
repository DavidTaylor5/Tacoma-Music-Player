package com.andaagii.tacomamusicplayer.util

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import timber.log.Timber

class SortingUtil {

    enum class SortingOption {

        //Song Group Options
        SORTING_TITLE_ALPHABETICAL {
            override fun type(): String {
                return Const.SORTING_TITLE_ALPHABETICAL
            }
        },

        //PLAYLIST OPTIONS
        SORTING_ARTIST_ALPHABETICAL {
            override fun type(): String {
                return Const.SORTING_ARTIST_ALPHABETICAL
            }
        },
        SORTING_NEWEST_RELEASE {
            override fun type(): String {
                return Const.SORTING_NEWEST_RELEASE
            }
        },
        SORTING_OLDEST_RELEASE {
            override fun type(): String {
                return Const.SORTING_OLDEST_RELEASE
            }
        },

        SORTING_BY_CREATION_DATE {
            override fun type(): String {
                return Const.SORTING_CREATION_DATE
            }
        },

        SORTING_BY_MODIFICATION_DATE {
            override fun type(): String {
                return Const.SORTING_MODIFICATION_DATE
            }
        };

        abstract fun type(): String
    }

    companion object {
        /**
         * UI Menu options return a string, which I use to determine specific actions
         * within the app. I use MenuOption enum for simplicity.
         */
        fun determineSortingOptionFromTitle(sorting: String): SortingOption {
            Timber.d("determineSortingOptionFromTitle: sorting=$sorting")
            return when(sorting) {
                Const.SORTING_TITLE_ALPHABETICAL -> SortingOption.SORTING_TITLE_ALPHABETICAL
                Const.SORTING_ARTIST_ALPHABETICAL -> SortingOption.SORTING_ARTIST_ALPHABETICAL
                Const.SORTING_NEWEST_RELEASE ->  SortingOption.SORTING_NEWEST_RELEASE
                Const.SORTING_OLDEST_RELEASE -> SortingOption.SORTING_OLDEST_RELEASE
                Const.SORTING_CREATION_DATE -> SortingOption.SORTING_BY_CREATION_DATE
                Const.SORTING_MODIFICATION_DATE -> SortingOption.SORTING_BY_MODIFICATION_DATE
                else -> {
                    Timber.d("determineSortingOptionFromTitle: Unknown Sorting String, defaulting to newest release first.")
                    SortingOption.SORTING_NEWEST_RELEASE
                }
            }
        }

        fun sortPlaylists(playlists: List<MediaItem>, sorting: SortingOption): List<MediaItem> {
            return when(sorting) {
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
                else -> { //Default to most recently created.
                    playlists.sortedByDescending { playlist ->
                        getModificationTimestamp(playlist.mediaMetadata.description.toString())
                    }
                }
            }
        }

        private fun getCreationTimestamp(playlistDescription: String): String {
            val timestamps = playlistDescription.split(":")
            return if(timestamps.isNotEmpty()) timestamps[0] else "Unknown"
        }

        private fun getModificationTimestamp(playlistDescription: String): String {
            val timestamps = playlistDescription.split(":")
            return if(timestamps.size >= 2) timestamps[1] else "Unknown"
        }

        fun sortAlbums(albums: List<MediaItem>, sorting: SortingOption): List<MediaItem> {
            return when(sorting) {
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
                else -> { //Default to most recent release year.
                    albums.sortedBy { album ->
                        album.mediaMetadata.releaseYear
                    }
                }
            }
        }
    }
}