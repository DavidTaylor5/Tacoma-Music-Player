package com.example.tacomamusicplayer.enum

//position determines what I'm showing, the ordering shall be as follows
//0 playlists, 1 albums, 2 songs
enum class PageType {
    PLAYLIST_PAGE {
        override fun type(): Int {
            return 0
        }
    },
    ALBUM_PAGE {
        override fun type(): Int {
            return 1
        }
    },
    SONG_PAGE {
        override fun type(): Int {
            return 2
        }
    };

    abstract fun type(): Int
}