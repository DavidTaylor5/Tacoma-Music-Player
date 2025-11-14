package com.andaagii.tacomamusicplayer.data

import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

data class AndroidAutoPlayData(
    val songGroupType: SongGroupType? = null,
    val groupTitle: String = "",
    val position: Int = 0,
    val songTitle: String = ""
)
