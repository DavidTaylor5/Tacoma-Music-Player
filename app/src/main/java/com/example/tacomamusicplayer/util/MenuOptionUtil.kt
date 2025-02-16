package com.example.tacomamusicplayer.util

import com.example.tacomamusicplayer.constants.Const

class MenuOptionUtil {

    /**
     * MenuOptions that can be handled in the app. Usually interactions with the media player.
     */
    enum class MenuOption {

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
            return when(title) {
                Const.ADD_TO_PLAYLIST ->  MenuOption.ADD_TO_PLAYLIST
                Const.REMOVE_FROM_PLAYLIST -> MenuOption.REMOVE_FROM_PLAYLIST
                Const.RENAME_PLAYLIST -> MenuOption.RENAME_PLAYLIST
                Const.ADD_PLAYLIST_IMAGE -> MenuOption.ADD_PLAYLIST_IMAGE
                Const.REMOVE_PLAYLIST -> MenuOption.REMOVE_PLAYLIST

                Const.CLEAR_QUEUE -> MenuOption.CLEAR_QUEUE
                Const.ADD_TO_QUEUE -> MenuOption.ADD_TO_QUEUE
                Const.REMOVE_FROM_QUEUE -> MenuOption.REMOVE_FROM_QUEUE

                Const.CHECK_STATS -> MenuOption.CHECK_STATS
                else -> MenuOption.UNKNOWN
            }
        }
    }
}