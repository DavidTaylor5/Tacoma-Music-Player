package com.andaagii.tacomamusicplayer.repository

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupCrossRefEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.QueueAddType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.factory.MediaBrowserFactory
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.worker.CatalogMusicWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val mediaItemUtil: MediaItemUtil,
    private val mediaBrowserFactory: MediaBrowserFactory,
    private val songDao: SongDao,
    private val songGroupDao: SongGroupDao
): MusicRepository, MusicProviderRepository {

    private lateinit var currentWorkerId: UUID
    private lateinit var workManager: WorkManager

    init {
        //Catalog all of the music on the user's device to a database in the background
        //TODO I also need to run a workrequest everytime I observe a change in the MUSIC folder
        val catalogWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<CatalogMusicWorker>()
            .build()

        workManager = WorkManager.getInstance(context)

        workManager.enqueue(catalogWorkRequest)
    }

    override fun cancelCatalogWorker() {
        workManager.cancelWorkById(currentWorkerId)
    }

//TODO ADD LOGIC TO BLOCK TWO PLAYLISTS WITH THE SAME NAME! Probably using UI
    override suspend fun createPlaylist(playlistName: String) {
        withContext(Dispatchers.IO) {
            Timber.d("createNamedPlaylist: playlistName=$playlistName")

            val playlist = SongGroupEntity(
                groupTitle = playlistName,
                artFile = "",
                artUri = "",
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
        //TODO How do I remove a song from the certain position, same song can be added more than once?
//        withContext(Dispatchers.IO) {
//            val playlistRefs = songs.map { song ->
//                SongGroupCrossRefEntity(
//                    playlistTitle,
//                    song.searchDescription
//                )
//            }
//
//            songGroupDao.deleteSongsFromPlaylist(*playlistRefs.toTypedArray())
//        }
    }

    override fun getAllAvailableAlbumsFlow(): Flow<List<MediaItem>> {
        Timber.d("getAllAvailableAlbumsFlow: ")
        return songGroupDao.getSongGroupsByTypeFlow(SongGroupType.ALBUM)
            .map { albums -> albums.map { album -> mediaItemUtil.createAlbumMediaItemFromSongGroupEntity(album) } }
    }

    override fun getAllAvailablePlaylistFlow(): Flow<List<MediaItem>> {
        Timber.d("getAllAvailablePlaylistFlow: ")
        return songGroupDao.getSongGroupsByTypeFlow(SongGroupType.PLAYLIST)
            .map { playlists -> playlists.map { playlist -> mediaItemUtil.createPlaylistMediaItemFromSongGroupEntity(playlist) } }
    }

    override suspend fun updatePlaylistImage(playlistTitle: String, artFileName: String) {
        Timber.d("updatePlaylistImage: playlistTitle=$playlistTitle, artFileName=$artFileName")
        withContext(Dispatchers.IO) {
            val playlist = songGroupDao.findSongGroupByName(playlistTitle)

            //If playlist is null I should create one?
            if(playlist == null) {
                Timber.d("addListOfSongMediaItemsToAPlaylist: No playlist found for playlistTitle=$playlistTitle")
                return@withContext
            }

            val updatedPlaylist = playlist.copy(
                artUri = artFileName
            )

            songGroupDao.updateSongGroups(updatedPlaylist)
        }
    }

    override suspend fun updatePlaylistSongOrder(
        playlistTitle: String,
        songDescriptions: List<String>
    ) {
        val playlist = songGroupDao.findSongGroupByName(playlistTitle)
        //val currentPlaylistSongs = songGroupDao.selectSongsFromPlaylist(playlistTitle)
        if(playlist != null) {
            songGroupDao.deleteAllSongsFromPlaylist(playlist.groupId)

            val updatedPlaylist = songDescriptions.mapIndexed { index, desc ->
                SongGroupCrossRefEntity(
                    groupId = playlist.groupId,
                    searchDescription = desc,
                    position = index
                )
            }

            songGroupDao.insertPlaylistSongs(*updatedPlaylist.toTypedArray())
        } else {
            Timber.d("updatePlaylistSongOrder: no playlist found for playlistTitle=$playlistTitle")
        }

        //TODO I should probably update at some point to only delete, swap positions of songs that have changed.

    }

    override suspend fun updatePlaylistTitle(originalTitle: String, newTitle: String) {
        withContext(Dispatchers.IO) {
            val playlist = songGroupDao.findSongGroupByName(originalTitle)
            if(playlist != null) {
                songGroupDao.updateSongGroups(
                    playlist.copy(
                        groupTitle = newTitle
                    )
                )
            } else {
                Timber.d("updatePlaylistTitle: No playlist found with title=$originalTitle")
            }
        }
    }

    override suspend fun addSongsToPlaylist(playlistTitle: String, songDescriptions: List<String>) {
        withContext(Dispatchers.IO) {
            val playlist = songGroupDao.findSongGroupByName(playlistTitle) //TODO playlist is showing up as null
            
            if(playlist == null) {
                Timber.d("addSongsToPlaylist: No playlist found for playlistTitle=$playlistTitle")
                return@withContext
            }
            
            val currentPlaylistSongs = songGroupDao.selectSongsFromPlaylist(playlist.groupId)
            
            val songs: List<SongEntity> = songDescriptions.map {
                val foundSongs = songDao.findSongFromSearchDescription(it)
                if(foundSongs.size > 1) {
                    Timber.e("addSongsToPlaylist: FOUND MULTIPLE SONGS WITH description=$it")
                }
                if(foundSongs.isEmpty()) {
                    Timber.e("addSongsToPlaylist: FOUND NO SONG WITH description=$it")
                }
                foundSongs[0]
            }

            val durationAdded = songs.map { it.songDuration.toLongOrNull() ?:0 }.reduce { acc, l -> acc+l }
            val nextPosition = if(currentPlaylistSongs.isNotEmpty()) currentPlaylistSongs.last().position + 100 else 0

            if(playlist == null) {
                Timber.d("addListOfSongMediaItemsToAPlaylist: No playlist found for playlistTitle=$playlistTitle")
                return@withContext
            }

            //update playlist group duration
            songGroupDao.updateSongGroups(
                playlist.copy(
                    groupDuration = playlist.groupDuration + durationAdded
                )
            )

            val playlistRefs = songs.mapIndexed { index, song ->
                SongGroupCrossRefEntity(
                    groupId = playlist.groupId,
                    searchDescription = song.searchDescription,
                    position = nextPosition + (100 * index)
                )
            }

            //Update the playlist refs
            songGroupDao.insertPlaylistSongs(*playlistRefs.toTypedArray())
        }
    }

    override suspend fun getAllAlbums(): List<MediaItem> = withContext(Dispatchers.IO) {
         songGroupDao.getSongGroupsByType(SongGroupType.ALBUM).map { songGroup ->
            mediaItemUtil.createAlbumMediaItemFromSongGroupEntity(songGroup)
        }
    }

    override suspend fun getAllArtists(): List<MediaItem> = withContext(Dispatchers.IO) {
        songDao.getAllArtists().map { artist ->
            mediaItemUtil.createMediaItemFromArtist(artist)
        }
    }

    override suspend fun getAllPlaylists(): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.getSongGroupsByType(SongGroupType.PLAYLIST).map { songGroup ->
            mediaItemUtil.createPlaylistMediaItemFromSongGroupEntity(songGroup)
        }
    }

    override suspend fun getAlbumsFromArtist(artist: String): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.findAllSongGroupsByArtist(artist).map { songGroup ->
            mediaItemUtil.createAlbumMediaItemFromSongGroupEntity(songGroup)
        }
    }

    override suspend fun getSongsFromAlbum(albumTitle: String): List<MediaItem> = withContext(Dispatchers.IO){
        songDao.getAllSongsFromAlbum(albumTitle).map { songEntity ->
            mediaItemUtil.createMediaItemFromSongEntity(songEntity)
        }
    }

    override suspend fun getSongsFromPlaylist(playlistTitle: String): List<MediaItem> = withContext(Dispatchers.IO){
        val playlist = songGroupDao.findSongGroupByName(playlistTitle)
        if(playlist != null) {
            songDao.selectAllSongsFromPlaylist(playlist.groupId).map { songEntity ->
                mediaItemUtil.createMediaItemFromSongEntity(songEntity)
            }
        } else {
            Timber.d("getSongsFromPlaylist: No playlist with playlistTitle=$playlistTitle found.")
            listOf()
        }
    }

    override suspend fun getSongFromName(songTitle: String): List<MediaItem> = withContext(Dispatchers.IO){
        songDao.queryAllSongsWithSongName(songTitle).map { songEntity ->
            mediaItemUtil.createMediaItemFromSongEntity(songEntity)
        }
    }


}