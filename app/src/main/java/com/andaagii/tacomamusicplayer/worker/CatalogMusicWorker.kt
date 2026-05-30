package com.andaagii.tacomamusicplayer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.media3.common.MediaItem
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.MediaStoreUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.util.UtilImpl.Companion.saveImageFromMediaStoreUri
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import androidx.core.net.toUri

/**
 * [CoroutineWorker] that scans device storage via [MediaStoreUtil] and upserts discovered songs
 * and albums into Room.
 *
 * Enqueued by [com.andaagii.tacomamusicplayer.activity.MainActivity] as a one-time [androidx.work.WorkRequest]
 * on first launch and on manual refresh. Any previously running catalog work is cancelled first to
 * prevent parallel executions from producing duplicate database entries.
 *
 * Processing steps in [doWork]:
 * 1. Query all albums from MediaStore via [catalogMusic].
 * 2. For each album, extract cover art from the first track and cache it to external storage.
 * 3. Catalog all tracks within the album into [SongEntity] rows.
 * 4. Insert/update the [SongGroupEntity] row for the album if it doesn't already exist.
 * 5. Delete any database albums or tracks that are no longer present in MediaStore.
 *
 * Uses `@HiltWorker` + `@AssistedInject` so Hilt can satisfy the DAOs and util dependencies.
 */
@HiltWorker
class CatalogMusicWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaStoreUtil: MediaStoreUtil,
    private val songDao: SongDao,
    private val songGroupDao: SongGroupDao,
    private val mediaItemUtil: MediaItemUtil
): CoroutineWorker(appContext, workerParams) { //TODO find another way to pass in these dependencies...

    override suspend fun doWork(): Result {
        Timber.d("doWork: Catalog user music!")

        // Start cataloging the music
        catalogMusic()

        Timber.d("doWork: return success!")
        return Result.success()
    }

    /**
     * Entry point for the catalog scan.
     *
     * Queries all albums from MediaStore, loads the current database state, and delegates
     * per-album processing (art extraction, song cataloging, upsert) to [catalogAlbums].
     */
    private suspend fun catalogMusic() {
        Timber.d("catalogMusic: ")

        val albums = mediaStoreUtil.queryAvailableAlbums(appContext)
        val dbAlbums = songGroupDao.getSongGroupsByType(SongGroupType.ALBUM)
        catalogAlbums(albums, dbAlbums)
    }

    /**
     * Syncs the Room database with the [albums] returned by MediaStore.
     *
     * For each album: extracts cover art, catalogs its tracks, then upserts the album's
     * [SongGroupEntity] row if it doesn't already exist. Albums present in the database but
     * absent from [albums] are deleted.
     *
     * @param albums Albums discovered from MediaStore.
     * @param dbAlbums Albums currently stored in Room.
     */
    private suspend fun catalogAlbums(albums: List<MediaItem>, dbAlbums: List<SongGroupEntity>) {
        Timber.d("catalogAlbums: album amount=${albums.size}")

        val dbAlbumTitles = dbAlbums.map { it.groupTitle }
        val albumTitles = albums.map { it.mediaMetadata.albumTitle }

        //Determine if I need to add any albums
        for(album in albums) {

            val albumInfo = album.mediaMetadata
            val description = "${albumInfo.albumTitle}_${albumInfo.albumArtist}"

            val firstSongUri = getFirstSongUri(album.mediaId)

            val filePath = saveImageFromMediaStoreUri(
                context = appContext,
                uri = firstSongUri.toUri(),
                fileName = UtilImpl.getImageBaseNameFromExternalStorage(
                    groupTitle = albumInfo.albumTitle.toString(),
                    artist = albumInfo.albumArtist.toString(),
                    songGroupType = SongGroupType.ALBUM
                )
            )

            //First catalog the songs in an album, before displaying the album to the user [don't want album to appear but not it's songs]
            catalogAlbumSongs(album.mediaId, filePath)

            // Don't need to add album if it already exists
            if(!dbAlbumTitles.contains(albumInfo.albumTitle)) {
                val savedAlbum = songGroupDao.findSongGroupByDescription(description)

                // Because SongGroups are now saved by groupId rather than groupTitle, I need to make sure I'm not saving twice.
                val songGroupEntity = if(savedAlbum != null) {
                    Timber.d("catalogAlbums: album=${album.mediaMetadata.albumTitle} copying album, new info")
                    savedAlbum.copy(
                        artFileOriginal = filePath,
                        groupTitle = albumInfo.albumTitle.toString(),
                        groupArtist = albumInfo.albumArtist.toString(),
                        releaseYear = albumInfo.releaseYear.toString()
                    )
                } else {
                    Timber.d("catalogAlbums: album=${album.mediaMetadata.albumTitle} Creating new album entry!")
                    SongGroupEntity(
                        songGroupType = SongGroupType.ALBUM,
                        artFileOriginal = filePath,
                        artFileCustom = "",
                        groupTitle = albumInfo.albumTitle.toString(),
                        groupArtist = albumInfo.albumArtist.toString(),
                        searchDescription = description,
                        groupDuration =  "0",
                        creationTimestamp = "0",
                        lastModificationTimestamp = "0",
                        releaseYear = albumInfo.releaseYear.toString()
                    )
                }

                //albumEntityList.add(songGroupEntity)
                songGroupDao.insertSongGroups(songGroupEntity)
            } else {
                Timber.d("catalogAlbums: database already has album=${album.mediaMetadata.albumTitle}")
            }
        }

        //MediaStore no longer finds the album, meaning it needs to be deleted from the database? Or Should it be greyed out.
        val deleteAlbumTitles = dbAlbumTitles.filter { !albumTitles.contains(it) }

        val deleteAlbums = dbAlbums.filter { deleteAlbumTitles.contains(it.groupTitle) }

//        if(albumEntityList.isNotEmpty()) {
//            songGroupDao.insertSongGroups(*albumEntityList.toTypedArray())
//        }

        if(deleteAlbums.isNotEmpty()) {
            songGroupDao.deleteSongGroups(*deleteAlbums.toTypedArray())
        }
    }

    /**
     * Returns the MediaStore URI of the first track in [albumName], or an empty string if the
     * album has no tracks.
     *
     * Used to extract cover art from the first track's embedded artwork before the album entity
     * is inserted into the database.
     *
     * @param albumName The album name as it appears in MediaStore.
     * @return The media ID (content URI string) of the first track, or `""` if none found.
     */
    private fun getFirstSongUri(albumName: String): String {
        val foundSongs = mediaStoreUtil.querySongsFromAlbum(appContext, albumName)
        var firstSongUri = ""

        if(foundSongs.isNotEmpty()) {
            val firstSong = foundSongs.first()
            firstSongUri =  firstSong.mediaId
        }

        return firstSongUri
    }

    /**
     * Syncs the Room database with the tracks in [albumName] as reported by MediaStore.
     *
     * Inserts [SongEntity] rows for tracks not yet in the database and deletes rows for tracks
     * that are no longer found. Skips tracks that already exist to avoid unnecessary writes.
     *
     * @param albumName The album name used to query MediaStore and Room.
     * @param albumArtFile The file path of the cached cover art to associate with new song rows.
     */
    private suspend fun catalogAlbumSongs(albumName: String, albumArtFile: String) {
        Timber.d("catalogAlbumSongs: albumName=$albumName, albumArtFile=$albumArtFile")

        val foundSongs = mediaStoreUtil.querySongsFromAlbum(appContext, albumName)
        val foundSongTitles = foundSongs.map { it.mediaMetadata.title }
        val dbSongs = songDao.getAllSongsFromAlbum(albumName)
        val dbSongTitles = dbSongs.map { it.name }
        val songEntityList: MutableList<SongEntity> = mutableListOf()

        //After parsing all the songs, update album duration
        var albumDuration: Long = 0

        //TODO update with mediaItemUtil createSongEntityFromMediaItem

        for((index, song) in foundSongs.withIndex()) {
            val songInfo = song.mediaMetadata
            val songDescription = mediaItemUtil.getSongSearchDescriptionFromMediaItem(song)

            if (!dbSongTitles.contains(songInfo.title)) {
                val songEntity = SongEntity(
                    albumTitle = songInfo.albumTitle.toString(),
                    artist = songInfo.artist.toString(),
                    searchDescription = songDescription,
                    name = songInfo.title.toString(),
                    uri = song.mediaId,
                    songDuration = songInfo.description.toString(),
                    artFileOriginal = albumArtFile,
                    useCustomArt = false
                )
                songEntityList.add(songEntity)
            }
        }

        if(songEntityList.isNotEmpty()) {
            songDao.insertItems(*songEntityList.toTypedArray())
        }

        // Delete songs that are no longer found
        val deleteSongs = dbSongs.filter { !foundSongTitles.contains(it.name) }
        if(deleteSongs.isNotEmpty()) {
            songDao.deleteItems(*deleteSongs.toTypedArray())
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