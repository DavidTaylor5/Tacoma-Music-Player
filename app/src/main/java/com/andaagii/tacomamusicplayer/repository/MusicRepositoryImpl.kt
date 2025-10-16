package com.andaagii.tacomamusicplayer.repository

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.andaagii.tacomamusicplayer.database.PlayerDatabase
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.factory.MediaBrowserFactory
import com.andaagii.tacomamusicplayer.worker.CatalogMusicWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    val mediaBrowserFactory: MediaBrowserFactory,
    val songDao: SongDao,
    val songGroupDao: SongGroupDao
): MusicRepository {

    init {
        //Catalog all of the music on the user's device to a database in the background
        val catalogWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<CatalogMusicWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(catalogWorkRequest)
    }

    override suspend fun createPlaylist(playlistName: String) {
        withContext(Dispatchers.IO) {
            Timber.d("createNamedPlaylist: playlistName=$playlistName")

            val playlist = SongGroupEntity(
                groupTitle = playlistName,
                artFile = "",
                creationTimestamp = LocalDateTime.now().toString(),
                lastModificationTimestamp = LocalDateTime.now().toString(),
                songGroupType = SongGroupType.PLAYLIST,
                searchDescription = playlistName,
                groupDuration = "0",
                groupArtist = "USER"
            )

            songGroupDao.insertSongGroups(playlist)
        }
    }

    override suspend fun removeSongsFromPlaylist(playlistTitle: String, songs: List<MediaItem>) {
        withContext(Dispatchers.IO) {
            //TODO how can I remove the song ids from the cross ref?
            //TODO I need to the songs in the database first...
        }
    }

    //TODO how can I go from MediaItem to songId?
}