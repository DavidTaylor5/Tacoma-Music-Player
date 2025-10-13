package com.andaagii.tacomamusicplayer.repository

import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    val songDao: SongDao,
    val songGroupDao: SongGroupDao
): MusicRepository {

}