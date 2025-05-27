package com.andaagii.tacomamusicplayer.util

import android.view.Menu
import com.andaagii.tacomamusicplayer.constants.Const
import timber.log.Timber

class MenuOptionUtil {

    /**
     * MenuOptions that can be handled in the app. Usually interactions with the media player.
     */
    enum class MenuOption {

        //Song Group Options
        PLAY_SONG_GROUP {
            override fun type(): String {
                return Const.PLAY_SONG_GROUP
            }
        },

        //PLAYLIST OPTIONS
        ADD_TO_PLAYLIST {
            override fun type(): String {
                return Const.ADD_TO_PLAYLIST
            }
        },
        REMOVE_FROM_PLAYLIST {
            override fun type(): String {
                return Const.REMOVE_FROM_PLAYLIST
            }
        },
        RENAME_PLAYLIST {
            override fun type(): String {
                return Const.RENAME_PLAYLIST
            }
        },
        ADD_PLAYLIST_IMAGE {
            override fun type(): String {
                return Const.ADD_PLAYLIST_IMAGE
            }
        },
        REMOVE_PLAYLIST {
            override fun type(): String {
                return Const.REMOVE_PLAYLIST
            }
        },
        PLAY_PLAYLIST_ONLY {
            override fun type(): String {
                return Const.PLAY_PLAYLIST_ONLY
            }
        },

        //QUEUE OPTIONS
        REMOVE_FROM_QUEUE {
            override fun type(): String {
                return Const.REMOVE_FROM_QUEUE
            }
        },
        CLEAR_QUEUE {
            override fun type(): String {
                return Const.CLEAR_QUEUE
            }
        },
        ADD_TO_QUEUE {
            override fun type(): String {
                return Const.ADD_TO_QUEUE
            }
        },

        PLAY_ALBUM {
            override fun type(): String {
                return Const.PLAY_ALBUM
            }
        },
        ADD_ALBUM_IMAGE {
          override fun type(): String {
              return Const.ADD_ALBUM_IMAGE
          }
        },
        CHECK_STATS {
            override fun type(): String {
                return Const.CHECK_STATS
            }
        },
        UNKNOWN {
            override fun type(): String {
                return Const.UNKNOWN
            }
        };

        abstract fun type(): String
    }

    companion object {
        /**
         * UI Menu options return a string, which I use to determine specific actions
         * within the app. I use MenuOption enum for simplicity.
         */
        fun determineMenuOptionFromTitle(title: String): MenuOption {
            Timber.d("determineMenuOptionFromTitle: title=$title")
            return when(title) {
                Const.PLAY_SONG_GROUP -> MenuOption.PLAY_SONG_GROUP

                Const.PLAY_PLAYLIST_ONLY -> MenuOption.PLAY_PLAYLIST_ONLY
                Const.ADD_TO_PLAYLIST ->  MenuOption.ADD_TO_PLAYLIST
                Const.REMOVE_FROM_PLAYLIST -> MenuOption.REMOVE_FROM_PLAYLIST
                Const.RENAME_PLAYLIST -> MenuOption.RENAME_PLAYLIST
                Const.ADD_PLAYLIST_IMAGE -> MenuOption.ADD_PLAYLIST_IMAGE
                Const.REMOVE_PLAYLIST -> MenuOption.REMOVE_PLAYLIST

                Const.PLAY_ALBUM -> MenuOption.PLAY_ALBUM
                Const.ADD_ALBUM_IMAGE -> MenuOption.ADD_ALBUM_IMAGE

                Const.CLEAR_QUEUE -> MenuOption.CLEAR_QUEUE
                Const.ADD_TO_QUEUE -> MenuOption.ADD_TO_QUEUE
                Const.REMOVE_FROM_QUEUE -> MenuOption.REMOVE_FROM_QUEUE

                Const.CHECK_STATS -> MenuOption.CHECK_STATS
                else -> MenuOption.UNKNOWN
            }
        }
    }
}