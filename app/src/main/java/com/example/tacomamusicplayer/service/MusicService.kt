package com.example.tacomamusicplayer.service

import android.content.Intent
import android.net.Uri
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
import com.example.tacomamusicplayer.util.MediaStoreUtil
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber

class MusicService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private var session: MediaLibrarySession? = null
    private val mediaStoreUtil: MediaStoreUtil = MediaStoreUtil()

    //TODO I want to map Album MediaItems to Song MediaItems [albums contain songs...]
    private var albumToSongMap: HashMap<String, MutableList<MediaItem>> = HashMap() //album titles to list of mediaItems
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

    //TODO why is the music being set up twice?

    /**
     * Query Music in background coroutine, I don't want this causing stuttering on UI.
     */
    private fun queryMusicOnDevice() {
        Timber.d("queryMusicOnDevice: ")

        mediaStoreUtil.queryAvailableAlbums(this) //test to show I can query available albums
        //queryAvailableAlbums()
        Timber.d("queryMusicOnDevice: =============================================================")
        val albumName = "Liquid Swords [Explicit]"
        mediaStoreUtil.querySongsFromAlbum(this, albumName)
        //querySongsFromAlbum(albumName)
        Timber.d("GZA: =============================================================")
        albumToSongMap = mediaStoreUtil.queryAllMediaItems(this)
        albumList = createAlbumMediaItems()
    }

    fun getListOfSongMediaItemsFromAlbum(albumTitle: String): List<MediaItem>? {
        return albumToSongMap[albumTitle]
    }

    //TODO remove this function, replace with mediaStoreUtil.queryAvailableAlbums() ....
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

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeMediaSession()
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