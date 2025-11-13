package com.andaagii.tacomamusicplayer.service

import android.content.Intent
import android.os.Bundle
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
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.andaagii.tacomamusicplayer.constants.Const.Companion.ALBUM_ID
import com.andaagii.tacomamusicplayer.constants.Const.Companion.ALBUM_PREFIX
import com.andaagii.tacomamusicplayer.constants.Const.Companion.ARTIST_ID
import com.andaagii.tacomamusicplayer.constants.Const.Companion.ARTIST_PREFIX
import com.andaagii.tacomamusicplayer.constants.Const.Companion.PLAYLIST_ID
import com.andaagii.tacomamusicplayer.constants.Const.Companion.PLAYLIST_PREFIX
import com.andaagii.tacomamusicplayer.constants.Const.Companion.ROOT_ID
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.repository.MusicProviderRepository
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.MediaStoreUtil
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.guava.asListenableFuture

//TODO GET GENRE FROM THE MEDIA ITEM?

//TODO ALLOW USER TO DELETE MULTIPLE SONGS!

//TODO CLEAN UP THE ANDROID AUTO IMPLEMENTATION, ADD BITMAPS, FIX TITLE AND SUBTITLE.

//TODO Android Auto has a search functionality, is this expected to be apart of my app?

//TODO Create custom listings for each country.

//TODO Update the description to maximize ASO

//TODO Allow user to remove multiple songs from playlist.

/*
* TODO add all of Android's expected well-known root IDs
*  2️⃣ Android’s expected well-known root IDs
*
*
* KEY IDEA
* So for full integration:

Implement onGetChildren() for Auto browsing

Implement onSearch() + onGetSearchResult() for Assistant voice commands

They both share the same MediaLibrarySession and can reuse your MusicRepository for actual song data.
*
*

Google doesn’t document every single ID, but Media3 samples and Android Auto / Assistant guidelines follow this pattern:

const val ROOT_ID = "root"
const val ALBUMS_ID = "albums"
const val ARTISTS_ID = "artists"
const val PLAYLISTS_ID = "playlists"
const val GENRES_ID = "genres"
const val RECENTLY_ADDED_ID = "recently_added"


onGetLibraryRoot() should return LibraryResult.ofRoot(ROOT_ID)

onGetChildren(ROOT_ID) → returns all top-level categories (albums, artists, playlists, etc.)

onGetChildren(ALBUMS_ID) → returns list of albums

onGetChildren("album_<albumId>") → returns songs in that album

Key point: Assistant expects these consistent IDs. If your service uses "album" instead of "albums", the Assistant may fail to find albums because it looks for "albums" specifically.
* */

/**
 * MusicService serves up a way to query albums and songs for the UI.
 */
@AndroidEntryPoint
class MusicService : MediaLibraryService() {
    /**
     * In scenarios such as Android Auto, I need a way to communicate with the app to seek
     * based on which song the user has clicked.
     */
    private var pendingSeek: Int? = null

    private lateinit var player: ExoPlayer
    private var session:MediaLibrarySession? = null
    @Inject
    lateinit var mediaStoreUtil: MediaStoreUtil
    @Inject
    lateinit var musicProvider: MusicProviderRepository
    @Inject
    lateinit var mediaItemUtil: MediaItemUtil

    // Gives my service the ability to run coroutines
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

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

            /*
            * This function can be called in two scenarios, when I manually add songs to the controller,
            * and in scenario such as android auto, this function will also call with user's click.
            * 1) Manually add items to controller -> app takes care of the logic
            * 2) Android Auto click -> sends the mediaItems with just the ID -> query songs and set pending seek.
            * */

            if(mediaItems.size == 1 && mediaItems[0].mediaId.contains("groupTitle=")) {
                val androidAutoPlayData = mediaItemUtil.getAndroidAutoPlayDataFromMediaItem(mediaItems[0])

                pendingSeek = androidAutoPlayData.position

                if(androidAutoPlayData.songGroupType == SongGroupType.PLAYLIST) {
                    return serviceScope.async {
                        musicProvider.getSongsFromPlaylist(
                            androidAutoPlayData.groupTitle
                        ).toMutableList()
                    }.asListenableFuture()
                } else if(androidAutoPlayData.songGroupType == SongGroupType.ALBUM) {
                    return serviceScope.async {
                        musicProvider.getSongsFromAlbum(
                            androidAutoPlayData.groupTitle
                        ).toMutableList()
                    }.asListenableFuture()
                }
            }

            return Futures.immediateFuture(mediaItems)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return super.onCustomCommand(session, controller, customCommand, args)
        }


        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        //Usually used to start async search
        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val foundMatches = musicProvider.searchMusic(query)
                //This code triggers the onGetSearchResult callback
                session.notifySearchResultChanged(browser, query, foundMatches.size, null)
            }

            return super.onSearch(session, browser, query, params)
        }

        //Used by Google assistant to get the result of a search
        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String, //ex GNX Kendrick Lamar
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

            //TODO This will work for android auto but will it work with google assistant...

            return serviceScope.async {
                val foundMatches = musicProvider.searchMusic(query)
                Timber.d("onGetSearchResult: foundMatches=$foundMatches")
                LibraryResult.ofItemList(foundMatches, params)
            }.asListenableFuture()


//            return super.onGetSearchResult(session, browser, query, page, pageSize, params)
        }

        //Used by Android Auto to browse the user's media
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return when {
                parentId == ROOT_ID -> {
                    Futures.immediateFuture(
                        LibraryResult.ofItemList(
                            listOf(
                                MediaItem.Builder().setMediaId(ARTIST_ID).setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(ARTIST_ID)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .build()
                                ).build(),
                                MediaItem.Builder().setMediaId(ALBUM_ID).setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(ALBUM_ID)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .build()
                                ).build(),
                                MediaItem.Builder().setMediaId(PLAYLIST_ID).setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(PLAYLIST_ID)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .build()
                                ).build()
                            ),
                            params
                        )
                    )
                }
                parentId == ALBUM_ID -> {
                    serviceScope.async {
                        val a = musicProvider.getAllAlbums()
                        Timber.d("onGetChildren: a=$a")
                        LibraryResult.ofItemList(musicProvider.getAllAlbums(), params)
                    }.asListenableFuture()
                }
                parentId == ARTIST_ID -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(musicProvider.getAllArtists(), params) //TODO too many artists!!!
                    }.asListenableFuture()
                }
                parentId == PLAYLIST_ID -> {
                    serviceScope.async {
                        val a = musicProvider.getAllPlaylists()
                        Timber.d("onGetChildren: a=$a")
                        LibraryResult.ofItemList(musicProvider.getAllPlaylists(), params)
                    }.asListenableFuture()
                }
                parentId.contains(ALBUM_PREFIX) -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(
                            musicProvider.getSongsFromAlbum(
                                mediaItemUtil.removeMediaItemPrefix(parentId) //TODO return a modified list of songs ALBUM:ALBUM_TITLE:SONG_TITLE:POSITION
                            ),
                            params
                        )
                    }.asListenableFuture()
                }
                parentId.contains(ARTIST_PREFIX) -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(
                            musicProvider.getAlbumsFromArtist(
                                mediaItemUtil.removeMediaItemPrefix(parentId)
                            ),
                            params
                        )
                    }.asListenableFuture()
                }
                parentId.contains(PLAYLIST_PREFIX) -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(
                            musicProvider.getSongsFromPlaylist(
                                mediaItemUtil.removeMediaItemPrefix(parentId) //TODO return a modified list of songs PLAYLIST:PLAYLIST_TITLE_SONG_TITLE:POSITION
                            ),
                            params
                        )
                    }.asListenableFuture()
                }
                else ->  {
                    serviceScope.async {

                        //TODO get either the album or the playlist...
                        //TODO set the position=X on all mediaItems so that android auto knows to play song at position.

                        LibraryResult.ofItemList(
                            musicProvider.getSongFromName(parentId), //TODO modify this with a function that returns auto:SONG_TITLE PLAYLIST:PLAYLIST_TITLE:START_POSITION:SONG_TITLE
                            params
                        )
                    }.asListenableFuture()
                }
            }

        }
    }

    fun getListOfSongMediaItemsFromAlbum(albumTitle: String): List<MediaItem> {
        return mediaStoreUtil.querySongsFromAlbum(this, albumTitle)
    }

    override fun onCreate() {
        Timber.d("onCreate: ")
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
        Timber.d("onDestroy: ")
        serviceJob.cancel()
        session?.run {
            player.release()
            session = null
        }
        super.onDestroy()
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d("onGetSession: session=$session, session.token=${session?.token}")
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
            .setId(generateRandomStringId())
            .build()
        Timber.d("initializeMediaSession: DT>>> ADD SESSION")
        addSession(session!!)
        return true
    }

    /**
     * Create a random string for the session id, previously I'm getting a crash because the service
     * doesn't have a unique id.
     */
    private fun generateRandomStringId(): String {
        return "Tacoma Music Player: ${Random.nextDouble()}"
    }


    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            if (playbackState == Player.STATE_ENDED) {
                //TODO SOMETHING Analytics?
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if(events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                pendingSeek?.let { position ->
                    player.seekTo(position, 0)
                    pendingSeek = null
                }
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