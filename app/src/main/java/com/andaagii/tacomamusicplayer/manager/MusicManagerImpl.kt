package com.andaagii.tacomamusicplayer.manager

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andaagii.tacomamusicplayer.service.MusicService
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class MusicManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
): MusicManager {

    /**
     * Reference to the app's mediaController.
     */
    val mediaController: LiveData<MediaController>
        get() = _mediaController
    private val _mediaController: MutableLiveData<MediaController> = MutableLiveData()

    private lateinit var mediaBrowser: MediaBrowser
    private var rootMediaItem: MediaItem? = null
    private lateinit var sessionToken: SessionToken

    override fun initialize() {
        Timber.d("initialize: ")
        sessionToken = createSessionToken()
        setupMediaController(sessionToken)
        setupMediaBrowser(sessionToken)
    }

    override fun release() {
        Timber.d("release: ")
        mediaController.value?.removeListener(playerListener)
        mediaController.value?.release()

        if(this::mediaBrowser.isInitialized) {
            mediaBrowser.release()
        }
    }

    /**
     * Returns a mediaController, used to interact with the music session.
     * @param session The session token associated with this app. [Should only be one]
     */
    private fun setupMediaController(session: SessionToken) {
        Timber.d("setupMediaController: session=$session")
        val controllerFuture = MediaController.Builder(context, session).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()

            _mediaController.value = controller

            //TODO Figure out this Logic
//            determineLoopingPref(context)
//
//            //Add old queue to the mediaController
//            restoreQueue()
//
//            //Restore the original ordering for current songs in mediaController
//            restoreQueueOrder()
//
//            _loopMode.postValue(controller.repeatMode)

            controller.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    /**
     * A session token is needed to connect to the music service. [And start the service?]
     */
    private fun createSessionToken(): SessionToken {
        Timber.d("createSessionToken: ")
        return SessionToken(context, ComponentName(context, MusicService::class.java))
    }

    /**
     * Sets up the MediaBrowser, which is used to browse music on the app.
     * @param session The session token associated with this app. [Should only be one]
     */
    private fun setupMediaBrowser(session: SessionToken) {
        Timber.d("DT>>> setupMediaBrowser: session=$session")
        val browserFuture = MediaBrowser.Builder(context, sessionToken)
            .buildAsync()
        browserFuture.addListener({
            browserFuture.get().let { browser ->
                mediaBrowser = browser
                getRoot()
                Timber.d("setupMediaBrowser: sessionToken=${mediaBrowser.connectedToken}")
            }
            mediaBrowser = browserFuture.get()
        }, MoreExecutors.directExecutor())
    }

    /**
     * The root is the top most node returned from the MediaLibraryService, media is organized as
     * a tree of MediaItems.
     */
    private fun getRoot() {
        Timber.d("getRoot: ")
        mediaBrowser.let { browser ->
            val rootFuture = browser.getLibraryRoot(null)
            rootFuture.addListener({
                rootMediaItem = rootFuture.get().value
            }, MoreExecutors.directExecutor())
        }
    }

    private val playerListener = object: Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Timber.d("onMediaMetadataChanged: artist=${mediaMetadata.artist}, title=${mediaMetadata.title}, albumTitle=${mediaMetadata.albumTitle}")
            //TODO Figure out this logic
//            _currentPlayingSongInfo.postValue(
//                SongData(
//                    songUri = "UNKNOWN",
//                    songTitle = mediaMetadata.title.toString(),
//                    albumTitle = mediaMetadata.albumTitle.toString(),
//                    artist = mediaMetadata.artist.toString(),
//                    artworkUri = mediaMetadata.artworkUri.toString(),
//                    duration = mediaMetadata.description.toString()
//                )
//            )
            super.onMediaMetadataChanged(mediaMetadata)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            //TODO Figure out this Logic
//            _isPlaying.postValue(isPlaying)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Timber.d("onRepeatModeChanged: ")
            super.onRepeatModeChanged(repeatMode)
            //TODO Figure out this Logic
//            _loopMode.postValue(repeatMode)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            //TODO Figure out this Logic
//            _currentlyPlayingSongs.value = mediaController.value?.let { controller ->
//                UtilImpl.getSongListFromMediaController(controller)
//            }
        }
    }
}