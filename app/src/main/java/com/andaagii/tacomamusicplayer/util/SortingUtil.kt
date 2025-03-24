package com.andaagii.tacomamusicplayer.util

import com.andaagii.tacomamusicplayer.constants.Const
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
    }
}