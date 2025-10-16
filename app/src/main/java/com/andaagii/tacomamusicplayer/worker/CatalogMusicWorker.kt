package com.andaagii.tacomamusicplayer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaBrowser
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MediaStoreUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * A class that queries the medialibraryservice, saving found songs and album art into a database.
 */
@HiltWorker
class CatalogMusicWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaStoreUtil: MediaStoreUtil,
    private val songDao: SongDao,
    private val songGroupDao: SongGroupDao
): CoroutineWorker(appContext, workerParams) { //TODO find another way to pass in these dependencies...

    override suspend fun doWork(): Result {
        Timber.d("doWork: Catalog user music!")

        // Start cataloging the music
        catalogMusic()

        Timber.d("doWork: return success!")
        return Result.success()
    }

    /**
     * Catalog all of the music on a user's phone
     */
    private fun catalogMusic() {
        Timber.d("catalogMusic: ")

        val albums = mediaStoreUtil.queryAvailableAlbums(appContext)
        catalogAlbums(albums)

        for(album in albums) {
            catalogAlbumSongs(album.mediaId)
        }
    }

    private fun catalogAlbums(albums: List<MediaItem>) {
        Timber.d("catalogAlbums: album amount=${albums.size}")
        val albumEntityList: MutableList<SongGroupEntity> = mutableListOf()
        for(album in albums) {
            val albumInfo = album.mediaMetadata
            val description = "${albumInfo.albumTitle}_${albumInfo.albumArtist}"

            val savedAlbum = songGroupDao.findSongGroupByDescription(description)

            val songGroupEntity = SongGroupEntity(
                songGroupType = SongGroupType.ALBUM,
                artFile = if(savedAlbum!=null) savedAlbum.artFile else albumInfo.artworkUri.toString(),
                groupTitle = albumInfo.albumTitle.toString(),
                groupArtist = albumInfo.albumArtist.toString(),
                searchDescription = description,
                groupDuration = if(savedAlbum!=null) savedAlbum.groupDuration else "0",
                creationTimestamp = "0",
                lastModificationTimestamp = "0"
            )

            albumEntityList.add(songGroupEntity)
        }

        if(albumEntityList.isNotEmpty()) {
            songGroupDao.insertSongGroups(*albumEntityList.toTypedArray())
        }
    }

    /**
     * Takes an album and adds all of it's songs to the
     */
    private fun catalogAlbumSongs(albumName: String) {
        Timber.d("catalogAlbumSongs: albumName=$albumName")

        val songs = mediaStoreUtil.querySongsFromAlbum(appContext, albumName)
        val songEntityList: MutableList<SongEntity> = mutableListOf()

        //After parsing all the songs, update album duration
        var albumDuration: Long = 0

        for(song in songs) {
            val songInfo = song.mediaMetadata
            val songEntity = SongEntity(
                albumTitle = songInfo.albumTitle.toString(),
                artist = songInfo.artist.toString(),
                searchDescription = "${songInfo.title}_${songInfo.albumTitle}_${songInfo.artist}",
                name = songInfo.title.toString(),
                uri = song.mediaId,
                songDuration = songInfo.description.toString()
            )
            songEntityList.add(songEntity)

            songInfo.description.toString().toLongOrNull()?.let { duration ->
                albumDuration += duration
            }
        }

        val albumWithDuration = songGroupDao.findSongGroupByName(albumName)?.copy(
            groupDuration = albumDuration.toString()
        )

        //Update the album's duration
        albumWithDuration?.let {
            songGroupDao.updateSongGroups(albumWithDuration)
        }

        if(songEntityList.isNotEmpty()) {
            songDao.insertItems(*songEntityList.toTypedArray())
        }
    }
}

/*
One work is defined, it must be scheduled with the WorkManager service in order to run.
WorkManager offers a lot of flexibility in how you schedule your work. You can schedule it to run
periodically over an interval of time, or you can schedule it to run only one time.

However you choose to schedule it, you will always use a WorkRequest. While a Worker defines the unit
of work, a WorkRequest (and its subclasses) define how and when it should be run. In the simplest case,
you can use a OneTimeWorkRequest.
ex.
val uploadWorkRequest: WorkRequest =
  OneTimeWorkRequestBuilder<UploadWorker>()
    .build()

Finally, you need to submit your WorkRequest to WorkManager using the enqueue() method.
ex.
WorkManager
  .getInstance(myContext)
  .enqueue(uploadWorkRequest)

 */