package com.example.tacomamusicplayer.service

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentUris.withAppendedId
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.data.SongModel
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executors

class MusicService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private var session: MediaLibrarySession? = null

    //TODO I want to map Album MediaItems to Song MediaItems [albums contain songs...]
    private val albumToSongMap: HashMap<String, MutableList<MediaItem>> = HashMap() //album titles to list of mediaItems
    private lateinit var albumList: List<MediaItem>

    val rootItem = MediaItem.Builder()
        .setMediaId("root")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("musicapprootwhichisnotvisibletocontrollers")
                .build()
        )
        .build()

    private val librarySessionCallback: MediaLibrarySession.Callback = object : MediaLibrarySession.Callback {
        //TODO

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {

            //Because I'm getting the library root, I should actually start querying the songs in the background
            queryMusicOnDevice()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

            return Futures.immediateFuture(
                LibraryResult.ofItemList(
                    when(parentId) {
                        "root" -> {
                            albumList //TODO sometimes this isn't ready yet... How do I stop initializing if this is empty...
                        }
                        //"libraryItem" -> albumList
                        else ->  {
                            //Get list of songs or if album doesn't exist return empty...
                            getListOfSongMediaItemsFromAlbum(parentId) ?: listOf()
                        }
                    },
                    params
                )
            )
        }
    }

    private val serviceIOScope = CoroutineScope(Dispatchers.IO)

    /**
     * Query Music in background coroutine, I don't want this causing stuttering on UI.
     */
    private fun queryMusicOnDevice() {
        Timber.d("queryMusicOnDevice: ")
//        serviceIOScope.launch {
//            readAudioFromStorage()
//            albumList = createAlbumMediaItems()
//        }
        //TODO should I make this run in a background thread? Optimize this!
        readAudioFromStorage()
        albumList = createAlbumMediaItems()
    }

    /**
     * Use MediaStore to query music in android/music file. Requires permission \[permission...]
     */
    private fun readAudioFromStorage(): List<SongModel> {
        Timber.d("readAudioFromStorage: ")

        val tempAudioList: MutableList<SongModel> = ArrayList()

        val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.AudioColumns.DATA, // 0 -> url
            MediaStore.Audio.AudioColumns.TITLE, //1 -> song title
            MediaStore.Audio.AudioColumns.ALBUM, //2 -> album title
            MediaStore.Audio.ArtistColumns.ARTIST, //3 -> artist
            MediaStore.Audio.AudioColumns.DURATION, //4 -> duration in  milliseconds
            MediaStore.Audio.AudioColumns.TRACK, //5 -> track # in album
            MediaStore.Audio.Media.ALBUM_ID, //6 -> what the hell is this?
            MediaStore.Audio.Albums.ALBUM, //7 -> album name again
            MediaStore.Audio.Albums.ARTIST, //8 -> artist again...
            MediaStore.Audio.AudioColumns._ID, //9 id
        )

        val resolver = this.contentResolver

        this.contentResolver.query(
            uriExternal,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while(cursor.moveToNext()) {
                Timber.d("readAudioFromStorage: ${cursor.getString(0)}, ${cursor.getString(1)}, ${cursor.getString(2)}, ${cursor.getString(3)}, ${cursor.getString(4)}, ${cursor.getString(5)}, ${cursor.getString(6)}, ${cursor.getString(7)}, ${cursor.getString(8)}, ${cursor.getString(9)}") //setMedia items here?

                val songUrl = cursor.getString(0)
                val album = cursor.getString(2)
                val artist = cursor.getString(3)
                val songTitle = cursor.getString(1)
                val songId = cursor.getLong(9)  //used when I'm getting the album art...

                try {

                    //Create URI from MediaStore location and returned ID [Keeps actual file location protected?]
                    val imageUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

                    Timber.d("readAudioFromStorage: Getting album art from URI=${imageUri.toString()}")

                    //Album art as a bitmap, I need to work on what to do when this is blank / null?
                    val albumArt = resolver.loadThumbnail(imageUri, Size(100, 100), null)

                    Timber.d("readAudioFromStorage: SUCCESSFUL! ALBUM ART FOUND!")

                } catch (e: Exception) {
                    Timber.d("readAudioFromStorage: ERROR ON LOADING ALBUM ART e=$e")
                }

                val songMediaItem = MediaItem.fromUri(songUrl)
                val updatedSongMediaItem = songMediaItem.buildUpon().setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setTitle(songTitle)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .build()
                ).setMediaId(songTitle)
                    .build()


                if(!albumToSongMap.containsKey(album)) {
                    albumToSongMap[album] = mutableListOf(updatedSongMediaItem)
                } else {
                    albumToSongMap[album]?.add(updatedSongMediaItem)
                }
            }
        }
        Timber.d("readAudioFromStorage: DONE SEARCHING!")

        return tempAudioList
    }

    fun getListOfSongMediaItemsFromAlbum(albumTitle: String): List<MediaItem>? {
        return albumToSongMap[albumTitle]
    }

    private fun createAlbumMediaItems(): MutableList<MediaItem> {

        val albums = mutableListOf<MediaItem>()

        //created from existing albumToSongMap
        for(key in albumToSongMap.keys) {

            //add album to my list...
            albums.add(
                MediaItem.Builder().setMediaId(key)
                    .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setTitle(key)
                        .build()
                    )
                    .build()
            )
        }

        return albums
    }

    private fun createSongMediaItem(
        songTitle: String = "UNKONWN SONG TITLE",
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
        songDuration: Long,
        trackNumber: Int
        ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("libraryItem")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle("musicapprootwhichisnotvisibletocontrollers")
                    .setAlbumTitle("ALBUM TITLE")
                    .setArtist("ARTIST")
                    .setDescription("Description I'll just pass song length here... TODO calculate song minutes and seconds")
                    .setTrackNumber(0)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    private fun createAlbumMediaItem(
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
        ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("libraryItem")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setAlbumArtist("ARTIST")
                    .setAlbumTitle("ALBUM TITLE")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .build()
            )
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeMediaSession()
        queryMusicOnDevice()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if(!player.playWhenReady || player.mediaItemCount == 0)
            stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            session = null
        }
        super.onDestroy()
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d("onGetSession: ")
        return session
    }

    private fun initializePlayer(): Boolean {
        Timber.d("initializePlayer: ")

        var playerBuilder: ExoPlayer.Builder = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .setAudioAttributes(AudioAttributes.DEFAULT, true) //coordinate audio //fade out other audio from other apps
            .setHandleAudioBecomingNoisy(true) // disconnect headphones , switch to speakers mode
            //.setWakeMode(C.WAKE_MODE_LOCAL) //device doesn't sleep when playing audio with screen off. //TODO wake lock is running error...

        player = playerBuilder.build()

        player.addListener(PlayerEventListener())
        player.playWhenReady = false //this can be a variable

        val pkgName = applicationContext.packageName
        //path for local file...
        val path = Uri.parse("android.resource://" + pkgName + "/" + R.raw.earth)

        //Test code that sets media Items with three of the same -> ui should choose music instead.
        player.setMediaItems(listOf(MediaItem.fromUri(path), MediaItem.fromUri(path), MediaItem.fromUri(path)))
        player.prepare()
        //player.play()
        return true
    }

    private fun initializeMediaSession(): Boolean {
        Timber.d("initializeMediaSession: ")
        session = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .build()
        addSession(session!!)

        return true
    }


    private class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            if (playbackState == Player.STATE_ENDED) {
                //TODO SOMETHING
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                //TODO type of error
            } else {
                //TODO other type of error
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            //TODO a track has changed....
        }
    }
}