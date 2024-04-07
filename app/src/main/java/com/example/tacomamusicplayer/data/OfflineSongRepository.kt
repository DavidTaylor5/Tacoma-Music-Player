package com.example.tacomamusicplayer.data

import kotlinx.coroutines.flow.Flow

class OfflineSongRepository(private val songEntryDao: SongEntryDao): SongRepository {
    override fun getAllSongsStream(): Flow<List<SongEntry>> = songEntryDao.getAllSongs()

    override fun getSongStream(id: Int): Flow<SongEntry>  = songEntryDao.getSong(id)

    override suspend fun insertSong(song: SongEntry) = songEntryDao.insert(song)

    override suspend fun deleteSong(song: SongEntry) = songEntryDao.delete(song)

    override suspend fun updateSong(song: SongEntry) = songEntryDao.update(song)
}

/*
* When I want to use this OfflineSongRepository / Database
*
* override val itemsRepository: ItemsRepository by lazy {
*   OfflineItemsRepository(InventoryDatabase.getDatabase(context).itemDao())
* }
* */