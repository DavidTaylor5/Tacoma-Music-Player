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
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupCrossRefEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.factory.MediaBrowserFactory
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.worker.CatalogMusicWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val mediaItemUtil: MediaItemUtil,
    private val mediaBrowserFactory: MediaBrowserFactory,
    private val songDao: SongDao,
    private val songGroupDao: SongGroupDao
): MusicRepository, MusicProviderRepository {

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

    override suspend fun removeSongsFromPlaylist(playlistTitle: String, songs: List<SongEntity>) {
        Timber.d("removeSongsFromPlaylist: playlistTitle=$playlistTitle, songs=$songs")
        withContext(Dispatchers.IO) {
            val playlistRefs = songs.map { song ->
                SongGroupCrossRefEntity(
                    playlistTitle,
                    song.searchDescription
                )
            }

            songGroupDao.deleteSongsFromPlaylist(*playlistRefs.toTypedArray())
        }
    }

    override fun getAllAvailableAlbumsFlow(): Flow<List<SongGroupEntity>> {
        Timber.d("getAllAvailableAlbumsFlow: ")
        return songGroupDao.getSongGroupsByTypeFlow(SongGroupType.ALBUM)
    }

    override fun getAllAvailablePlaylistFlow(): Flow<List<SongGroupEntity>> {
        Timber.d("getAllAvailablePlaylistFlow: ")
        return songGroupDao.getSongGroupsByTypeFlow(SongGroupType.PLAYLIST)
    }

    override suspend fun getAllAlbums(): List<MediaItem> = withContext(Dispatchers.IO) {
         songGroupDao.getSongGroupsByType(SongGroupType.ALBUM).map { songGroup ->
            mediaItemUtil.createMediaItemFromAlbum(songGroup.groupTitle)
        }
    }


    override suspend fun getAllArtists(): List<MediaItem> = withContext(Dispatchers.IO) {
        songDao.getAllArtists().map { artist ->
            mediaItemUtil.createMediaItemFromArtist(artist)
        }
    }


    override suspend fun getAllPlaylists(): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.getSongGroupsByType(SongGroupType.PLAYLIST).map { songGroup ->
            mediaItemUtil.createMediaItemFromPlaylist(songGroup.groupTitle)
        }
    }


    override suspend fun getAlbumsFromArtist(artist: String): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.findAllSongGroupsByArtist(artist).map { songGroup ->
            mediaItemUtil.createMediaItemFromAlbum(songGroup.groupTitle)
        }
    }

    override suspend fun getSongsFromAlbum(albumTitle: String): List<MediaItem> = withContext(Dispatchers.IO){
        songDao.getAllSongsFromAlbum(albumTitle).map { songEntity ->
            mediaItemUtil.createMediaItemFromSongEntity(songEntity)
        }
    }

    override suspend fun getSongsFromPlaylist(playlistTitle: String): List<MediaItem> = withContext(Dispatchers.IO){
        songDao.selectAllSongsFromPlaylist(playlistTitle).map { songEntity ->
            mediaItemUtil.createMediaItemFromSongEntity(songEntity)
        }
    }

    override suspend fun getSongFromName(songTitle: String): List<MediaItem> = withContext(Dispatchers.IO){
        songDao.queryAllSongsWithSongName(songTitle).map { songEntity ->
            mediaItemUtil.createMediaItemFromSongEntity(songEntity)
        }
    }


}