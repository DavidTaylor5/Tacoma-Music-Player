package com.andaagii.tacomamusicplayer.repository

import android.content.Context
import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupCrossRefEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val mediaItemUtil: MediaItemUtil,
    private val songDao: SongDao,
    private val songGroupDao: SongGroupDao
): MusicRepository, MusicProviderRepository {

    override suspend fun searchMusic(search: String): List<MediaItem> {
        val matchingSongGroups = songGroupDao.findDescriptionFromSearchStr(search)
        val matchingAlbums = matchingSongGroups.filter { it.songGroupType == SongGroupType.ALBUM }
            .map { mediaItemUtil.createAlbumMediaItemFromSongGroupEntity(it) }
        val matchingPlaylists = matchingSongGroups.filter { it.songGroupType == SongGroupType.PLAYLIST }
            .map { mediaItemUtil.createPlaylistMediaItemFromSongGroupEntity(it) }
        val matchingSongs = songDao.findDescriptionFromSearchStr(search)
            .map { mediaItemUtil.createMediaItemFromSongEntity(it) }

        //TODO now I need to combine the lists, and sort by substring position first, then reduce size to 20 total.
        val combinedData = matchingAlbums + matchingPlaylists + matchingSongs
        val searchData = combinedData.sortedBy {
            it.mediaMetadata.subtitle.toString().indexOf(string = search, ignoreCase = true)
        }

        return searchData
    }

//TODO ADD LOGIC TO BLOCK TWO PLAYLISTS WITH THE SAME NAME! Probably using UI
    override suspend fun createPlaylist(playlistName: String) {
        withContext(Dispatchers.IO) {
            Timber.d("createNamedPlaylist: playlistName=$playlistName")

            val playlist = SongGroupEntity(
                groupTitle = playlistName,
                artFileOriginal = "",
                artFileCustom = "",
                useCustomArt = true,
                creationTimestamp = LocalDateTime.now().toString(),
                lastModificationTimestamp = LocalDateTime.now().toString(),
                songGroupType = SongGroupType.PLAYLIST,
                searchDescription = playlistName,
                groupDuration = "0",
                groupArtist = Const.USER_PLAYLIST
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
                artFileCustom = artFileName,
            )

            songGroupDao.updateSongGroups(updatedPlaylist)
        }
    }

    override suspend fun  createInitialQueueIfEmpty(title: String) {
        var queue = songGroupDao.findSongGroupByName(title)
        if(queue == null) {
            queue = SongGroupEntity(
                songGroupType = SongGroupType.QUEUE,
                groupTitle = title,
                groupArtist = "QUEUE",
                searchDescription = "QUEUE",
                groupDuration = ""
            )

            songGroupDao.insertSongGroups(queue)
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

    override suspend fun getAllAlbums(useFileProviderUri: Boolean): List<MediaItem> = withContext(Dispatchers.IO) {
         songGroupDao.getSongGroupsByType(SongGroupType.ALBUM).map { songGroup ->
            mediaItemUtil.createAlbumMediaItemFromSongGroupEntity(
                album = songGroup,
                artUri = if(useFileProviderUri)
                    mediaItemUtil.determineArtUri(songGroup, true)
                else null
            )
        }
    }

    override suspend fun getAllArtists(): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.getAllArtists().map { artist ->
            //TODO Grab first album from artist, assign it to the artist uri...

            mediaItemUtil.createMediaItemFromArtist(artist)
        }
    }

    override suspend fun getAllPlaylists(useFileProviderUri: Boolean): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.getSongGroupsByType(SongGroupType.PLAYLIST).map { songGroup ->
            mediaItemUtil.createPlaylistMediaItemFromSongGroupEntity(
                playlist = songGroup,
                artUri = if(useFileProviderUri)
                mediaItemUtil.determineArtUri(songGroup, true)
                else null
            )
        }
    }

    override suspend fun getAlbumsFromArtist(artist: String): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.findAllSongGroupsByArtist(artist).map { songGroup ->
            mediaItemUtil.createAlbumMediaItemFromSongGroupEntity(songGroup)
        }
    }

    override suspend fun getSongsFromAlbum(
        albumTitle: String
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.findSongGroupByName(albumTitle)?.let { album ->
            Timber.d("getSongsFromAlbum: album=$album")
            songDao.getAllSongsFromAlbum(albumTitle).mapIndexed { position, songEntity ->
                mediaItemUtil.createMediaItemFromSongEntity(
                    song = songEntity,
                    position = position,
                    songGroupType = SongGroupType.ALBUM,
                )
            }
        } ?: listOf()
    }

    override suspend fun getSongsFromPlaylist(
        playlistTitle: String
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        songGroupDao.findSongGroupByName(playlistTitle)?.let { playlist ->
            songDao.selectAllSongsFromPlaylist(playlist.groupId).mapIndexed { position, songEntity ->
                mediaItemUtil.createMediaItemFromSongEntity(
                    song = songEntity,
                    position = position,
                    songGroupType = SongGroupType.PLAYLIST,
                    playlistTitle = playlistTitle,
                )
            }
        } ?: listOf()
    }

    override suspend fun getSongFromName(songTitle: String): List<MediaItem> = withContext(Dispatchers.IO) {
        songDao.queryAllSongsWithSongName(songTitle).map { songEntity ->
            mediaItemUtil.createMediaItemFromSongEntity(songEntity)
        }
    }


}