package com.example.tacomamusicplayer.service

import android.content.Intent
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
import com.example.tacomamusicplayer.util.MediaStoreUtil
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber

class MusicService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private var session: MediaLibrarySession? = null
    private val mediaStoreUtil: MediaStoreUtil = MediaStoreUtil()

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

    // MediaLibrarySession callback determines what information is going to be returned when
    // UI queries music from the service.
    private val librarySessionCallback: MediaLibrarySession.Callback = object : MediaLibrarySession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {

            //Technically this is a security breach... exposes on device uri to mediacontrollers...
            val updatedMediaItems = mediaItems.map { it -> it.buildUpon().setUri(it.mediaId).build() }.toMutableList()
            return Futures.immediateFuture(updatedMediaItems)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
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
                            mediaStoreUtil.queryAvailableAlbums(this@MusicService)
                        }
                        else ->  {
                            getListOfSongMediaItemsFromAlbum(parentId)
                        }
                    },
                    params
                )
            )
        }
    }

    fun getListOfSongMediaItemsFromAlbum(albumTitle: String): List<MediaItem> {
        return mediaStoreUtil.querySongsFromAlbum(this, albumTitle)
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

    /**
     * Setups up the exoplayer instance that is core to the mp3 functionality. UI classes will
     * use the mediacontroller to request changes to music content.
     */
    private fun initializePlayer(): Boolean {
        Timber.d("initializePlayer: ")

        var playerBuilder: ExoPlayer.Builder = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .setAudioAttributes(AudioAttributes.DEFAULT, true) //coordinate audio //fade out other audio from other apps
            .setHandleAudioBecomingNoisy(true) // disconnect headphones , switch to speakers mode
            //.setWakeMode(C.WAKE_MODE_LOCAL) //device doesn't sleep when playing audio with screen off. //TODO wake lock is running error...

        player = playerBuilder.build()

        val a = player.availableCommands

        player.addListener(PlayerEventListener())
        player.playWhenReady = false //this can be a variable

        //Test code that sets media Items with three of the same -> ui should choose music instead.
        player.setMediaItems(listOf())
        player.prepare()
        return true
    }

    /**
     * A media session is required for mp3 functionality, this will generate the default music
     * notification.
     */
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