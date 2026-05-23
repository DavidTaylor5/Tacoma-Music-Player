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

/*
* Android Auto fixes
*
* TODO REMOVE SUBTITLE FROM ARTIST
*  TODO Add artists instead of SUBTITLE on ALBUM
*   TODO Add image for album
*    TODO add image for song
*     TODO Make subtitle Album / Artist on song
*
* */

//TODO if no song is playing don't show the mini player...

//TODO Low priority, fix for multi select blocking the bottom song...

//TODO allow user to add album to playlist from album tab

//TODO Create custom listings for each country.

//TODO Update the description to maximize ASO

//TODO when the queue is empty and not playing anything, I shouldn't let the user click the play button, mini player shouldn't be present.

//TODO add back information on the playlist songgroup, album songgroup. Can I finally display duration?
// On SongGroupHeader I want to display "X tracks | 33:02"

//TODO Error on adding some images as playlist covers...

//TODO Allow user to choose the crop uCrop - github.com/Yalantis/uCrop

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
 * Foreground [MediaLibraryService] that owns the [ExoPlayer] instance and exposes the app's
 * media library to Android Auto, Google Assistant, and the in-app UI via a [MediaLibrarySession].
 *
 * Responsibilities:
 * - Creates and manages the [ExoPlayer] and [MediaLibrarySession] lifecycle.
 * - Serves the browsable hierarchy (Root → Artists/Albums/Playlists → songs) through
 *   [librarySessionCallback].
 * - Handles the Android Auto playback-initiation flow via the [pendingSeek] mechanism.
 * - Executes all repository calls on [Dispatchers.IO] via [serviceScope].
 *
 * Lifecycle: created by the Android OS when the first controller binds or the notification
 * is shown; destroyed after [onTaskRemoved] when no playback is active. All coroutine work is
 * tied to [serviceJob], which is cancelled in [onDestroy].
 *
 * Threading: [librarySessionCallback] overrides are invoked on the main thread by Media3;
 * blocking work is always dispatched to [serviceScope] and returned as a [ListenableFuture].
 *
 * Injected via Hilt: [MediaStoreUtil], [MusicProviderRepository], [MediaItemUtil].
 */
@AndroidEntryPoint
class MusicService : MediaLibraryService() {
    /**
     * Queue index to seek to after the next [Player.EVENT_MEDIA_ITEM_TRANSITION] event fires.
     *
     * Android Auto triggers [onAddMediaItems] before the new queue is loaded into [player].
     * Because [ExoPlayer.seekTo] requires the queue to be populated first, the desired index is
     * stored here and consumed in [PlayerEventListener.onEvents] on the first item-transition
     * event after the queue is set. Reset to `null` immediately after the seek to prevent
     * re-seeking on subsequent transitions.
     *
     * Must only be written from the main thread (Media3 callback thread).
     */
    private var pendingSeek: Int? = null

    /**
     * The [ExoPlayer] instance responsible for all audio decoding and playback.
     *
     * Initialised in [initializePlayer] during [onCreate]; released in [onDestroy]. Must not be
     * accessed before [onCreate] completes.
     */
    private lateinit var player: ExoPlayer

    /**
     * The active [MediaLibrarySession] that connects [player] to all Media3 controllers.
     *
     * `null` before [initializeMediaSession] runs and after [onDestroy] cleans up. The session
     * is registered with the service via [addSession] so the OS can route controller connections.
     */
    private var session: MediaLibrarySession? = null

    /** Queries on-device audio via [MediaStore]; used by [getListOfSongMediaItemsFromAlbum]. */
    @Inject
    lateinit var mediaStoreUtil: MediaStoreUtil

    /**
     * Repository that fetches albums, artists, playlists, and songs as [MediaItem] lists for
     * use by [librarySessionCallback] and Android Auto / Google Assistant browsing.
     */
    @Inject
    lateinit var musicProvider: MusicProviderRepository

    /**
     * Builds and parses [MediaItem] instances, including decoding the encoded Android Auto
     * media-ID format used by [pendingSeek] resolution.
     */
    @Inject
    lateinit var mediaItemUtil: MediaItemUtil

    /**
     * Root [SupervisorJob] that scopes all coroutines to the service lifetime.
     *
     * Cancelled in [onDestroy] so that in-flight repository calls are abandoned when the
     * service is torn down without leaking coroutines into the process.
     */
    private val serviceJob = SupervisorJob()

    /**
     * Coroutine scope for all asynchronous repository and search operations.
     *
     * Uses [Dispatchers.IO] because every operation involves either database queries or
     * [MediaStore] cursor reads — both are blocking I/O that must not run on Main.
     */
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    /**
     * Root [MediaItem] returned by [onGetLibraryRoot] to all browsing controllers.
     *
     * Not browsable and not playable; it exists only as the logical top of the tree required
     * by the [MediaLibrarySession] protocol. The title is an internal sentinel value — it is
     * never displayed to users. Controllers use the media ID `"root"` ([ROOT_ID]) to request
     * children via [onGetChildren].
     */
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

    /**
     * [MediaLibrarySession.Callback] implementation that serves the browsing hierarchy and
     * handles playback initiation from Android Auto and Google Assistant.
     *
     * All override methods are invoked on the main thread by Media3. Blocking repository calls
     * are dispatched to [serviceScope] and returned as a [ListenableFuture] via
     * `async { }.asListenableFuture()`. Synchronous responses use [Futures.immediateFuture].
     *
     * Browse hierarchy served:
     * - Root → Artists (`artists`), Albums (`albums`), Playlists (`playlists`)
     * - `artist:<name>` → albums belonging to that artist
     * - `album:<title>` → songs belonging to that album
     * - `playlist:<title>` → songs belonging to that playlist
     */
    private val librarySessionCallback: MediaLibrarySession.Callback = object : MediaLibrarySession.Callback {


        /**
         * Resolves incoming [MediaItem] requests into fully-populated items before they are
         * added to [player]'s queue.
         *
         * Called in two distinct scenarios:
         *
         * 1. **In-app queue management** — the UI sends complete [MediaItem] objects already
         *    containing URIs. These are returned unchanged via [Futures.immediateFuture].
         * 2. **Android Auto tap** — Auto sends a single [MediaItem] containing only the encoded
         *    media ID (format: `songGroupType=…|||groupTitle=…|||position=…|||songTitle=…`).
         *    The service decodes the ID via [MediaItemUtil.getAndroidAutoPlayDataFromMediaItem],
         *    stores the target queue index in [pendingSeek], then fetches the full song list from
         *    [MusicProviderRepository] asynchronously. The seek is applied once
         *    [PlayerEventListener.onEvents] fires [Player.EVENT_MEDIA_ITEM_TRANSITION].
         *
         * All async work runs on [serviceScope] and is returned as a [ListenableFuture].
         *
         * @param mediaSession The active [MediaSession].
         * @param controller The controller that issued the add-items request.
         * @param mediaItems Items to resolve; may be partial (ID-only) in the Android Auto scenario.
         * @return A future that resolves to the fully-populated item list for the player queue.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Android Auto encodes playback context in the media ID using the "groupTitle=" marker.
            // A plain in-app add passes fully-populated items that do not use this encoding.
            if(mediaItems.size == 1 && mediaItems[0].mediaId.contains("groupTitle=")) {
                val androidAutoPlayData = mediaItemUtil.getAndroidAutoPlayDataFromMediaItem(mediaItems[0])

                // Store the desired queue index; the actual seek happens in PlayerEventListener.onEvents
                // after the new queue is committed to the player.
                pendingSeek = androidAutoPlayData.position

                if(androidAutoPlayData.songGroupType == SongGroupType.PLAYLIST) {
                    Timber.d("onAddMediaItems: Playback for Playlist!")
                    // Fetch playlist songs with file-provider URIs so Android Auto can read artwork
                    return serviceScope.async {
                        musicProvider.getSongsFromPlaylist(
                            androidAutoPlayData.groupTitle,
                            useFileProviderUri = true
                        ).toMutableList()
                    }.asListenableFuture()
                } else if(androidAutoPlayData.songGroupType == SongGroupType.ALBUM) {
                    Timber.d("onAddMediaItems: Playback for Album! title=${androidAutoPlayData.groupTitle}")
                    // Fetch album songs with file-provider URIs so Android Auto can read artwork
                    return serviceScope.async {
                        musicProvider.getSongsFromAlbum(
                            androidAutoPlayData.groupTitle, //TODO This title isn't coming in correct... good kid,
                            useFileProviderUri = true
                        ).toMutableList()
                    }.asListenableFuture()
                }
            }

            return Futures.immediateFuture(mediaItems)
        }

        /**
         * Handles custom [SessionCommand] requests sent by any bound controller.
         *
         * Currently delegates entirely to the default Media3 implementation. Override this
         * method to add application-specific commands (for example, triggering shuffle from
         * a widget).
         *
         * @param session The active [MediaLibrarySession].
         * @param controller The controller issuing the command.
         * @param customCommand The command identifier and any extra data.
         * @param args Optional arguments bundle accompanying the command.
         * @return A [SessionResult] future; returns [SessionResult.RESULT_ERROR_NOT_SUPPORTED]
         *   by default.
         */
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return super.onCustomCommand(session, controller, customCommand, args)
        }


        /**
         * Returns the root [MediaItem] of the browsable tree to any connecting controller.
         *
         * Responds synchronously with [rootItem], which has media ID `"root"`. Controllers
         * then call [onGetChildren] with `"root"` to retrieve the top-level categories.
         *
         * @param browser The controller requesting the root.
         * @param params Optional hints about browsing intent (offline mode, etc.) — not used here.
         * @return An immediate future wrapping [rootItem].
         */
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        /**
         * Begins an asynchronous music search and notifies the browser when results are ready.
         *
         * Media3 separates search into two phases: this method fires the query on [serviceScope]
         * and calls [MediaLibrarySession.notifySearchResultChanged] when complete, which in turn
         * triggers [onGetSearchResult] on the browser side to retrieve the actual items.
         *
         * The search runs on [Dispatchers.IO] inside [serviceScope] because
         * [MusicProviderRepository.searchMusic] performs a database query.
         *
         * @param browser The controller that issued the search.
         * @param query Free-text search string (e.g., "GNX Kendrick Lamar").
         * @param params Optional search hints — passed through to [notifySearchResultChanged] unchanged.
         * @return The default super result (always [LibraryResult.ofVoid]); the real result arrives
         *   asynchronously via the notify callback.
         */
        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val foundMatches = musicProvider.searchMusic(query)
                // Triggers the onGetSearchResult callback on the browser side
                session.notifySearchResultChanged(browser, query, foundMatches.size, null)
            }

            return super.onSearch(session, browser, query, params)
        }

        /**
         * Returns the search results previously computed by [onSearch] to the requesting controller.
         *
         * Called by Google Assistant and Android Auto after [onSearch] emits
         * [MediaLibrarySession.notifySearchResultChanged]. Results use file-provider URIs so
         * that external processes (Assistant, Auto) can securely access artwork files.
         *
         * Note: [page] and [pageSize] are currently ignored — the full result set is returned
         * in one response. Pagination should be implemented if search results grow large.
         *
         * Runs on [serviceScope] ([Dispatchers.IO]) because the repository performs a database query.
         *
         * @param browser The controller requesting results.
         * @param query The same query string that was passed to [onSearch].
         * @param page Requested result page (0-indexed) — not yet honoured.
         * @param pageSize Maximum items per page — not yet honoured.
         * @param params Optional browsing hints.
         * @return A future resolving to the full list of matching [MediaItem] objects.
         */
        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceScope.async {
                val foundMatches = musicProvider.searchMusic(
                    query,
                    useFileProviderUri = true
                )
                Timber.d("onGetSearchResult: foundMatches=$foundMatches")
                LibraryResult.ofItemList(foundMatches, params)
            }.asListenableFuture()
        }

        /**
         * Returns the children of a given [parentId] node in the media browse tree.
         *
         * Called by Android Auto (and any Media3 browser) to populate each level of the
         * browsable hierarchy. The [parentId] string encodes both the node type and — for
         * non-root nodes — the item identifier using the prefix constants from [Const]:
         *
         * | parentId value         | Content returned                            |
         * |------------------------|---------------------------------------------|
         * | `"root"`               | Category nodes: Artists, Albums, Playlists  |
         * | `"artists"`            | All artist [MediaItem] objects              |
         * | `"albums"`             | All album [MediaItem] objects               |
         * | `"playlists"`          | All playlist [MediaItem] objects            |
         * | `"artist:<name>"`      | Albums belonging to the named artist        |
         * | `"album:<title>"`      | Songs belonging to the named album          |
         * | `"playlist:<title>"`   | Songs belonging to the named playlist       |
         * | anything else          | Single-song lookup by title                 |
         *
         * All non-root responses pass `useFileProviderUri = true` to [MusicProviderRepository]
         * so that Android Auto — which runs in a separate process — can read artwork via the
         * app's [AlbumArtFileProvider] content provider.
         *
         * Async branches run on [serviceScope] ([Dispatchers.IO]); the root branch is synchronous.
         *
         * @param browser The controller browsing the library.
         * @param parentId ID of the node whose children are requested.
         * @param page Result page (0-indexed) — not currently used; full lists are returned.
         * @param pageSize Maximum items per page — not currently used.
         * @param params Optional browsing hints from the controller.
         * @return A future resolving to the children list for the given node.
         */
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return when {
                // Top-level categories — synchronous, no I/O needed
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
                // All albums with file-provider artwork URIs for cross-process image access
                parentId == ALBUM_ID -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(musicProvider.getAllAlbums(true), params)
                    }.asListenableFuture()
                }
                // All artists; artwork URI is derived from the first album found for that artist
                parentId == ARTIST_ID -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(musicProvider.getAllArtists(), params) //TODO too many artists!!!
                    }.asListenableFuture()
                }
                // All user-created playlists with file-provider artwork URIs
                parentId == PLAYLIST_ID -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(musicProvider.getAllPlaylists(true), params)
                    }.asListenableFuture()
                }
                // Songs within a specific album; strip the "album:" prefix before querying
                parentId.contains(ALBUM_PREFIX) -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(
                            musicProvider.getSongsFromAlbum(
                                albumTitle = mediaItemUtil.removeMediaItemPrefix(parentId),
                                useFileProviderUri = true
                            ),
                            params
                        )
                    }.asListenableFuture()
                }
                // Albums for a specific artist; strip the "artist:" prefix before querying
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
                // Songs within a specific playlist; strip the "playlist:" prefix before querying
                parentId.contains(PLAYLIST_PREFIX) -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(
                            musicProvider.getSongsFromPlaylist(
                                playlistTitle = mediaItemUtil.removeMediaItemPrefix(parentId),
                                useFileProviderUri = true
                            ),
                            params
                        )
                    }.asListenableFuture()
                }
                // Fallback: treat parentId as a song title for direct single-song lookup
                else -> {
                    serviceScope.async {
                        LibraryResult.ofItemList(
                            musicProvider.getSongFromName(parentId), //TODO modify this with a function that returns auto:SONG_TITLE PLAYLIST:PLAYLIST_TITLE:START_POSITION:SONG_TITLE
                            params
                        )
                    }.asListenableFuture()
                }
            }
        }
    }

    /**
     * Returns a list of [MediaItem] objects for every song in the given album, sourced directly
     * from [MediaStore] rather than the app's Room database.
     *
     * Used by the UI layer to populate an album's song queue without requiring a database lookup.
     * Performs a synchronous [MediaStore] cursor query — callers should invoke this off the main
     * thread.
     *
     * @param albumTitle The album title as stored in [MediaStore]; must match exactly.
     * @return All songs in the album as [MediaItem] objects, or an empty list if none are found.
     */
    fun getListOfSongMediaItemsFromAlbum(albumTitle: String): List<MediaItem> {
        return mediaStoreUtil.querySongsFromAlbum(this, albumTitle)
    }

    /**
     * Initialises the service by building [player] then [session], in that order.
     *
     * [initializeMediaSession] requires [player] to already exist, so the call order must not
     * be reversed. Called once by the Android OS when the first controller binds.
     */
    override fun onCreate() {
        Timber.d("onCreate: ")
        super.onCreate()
        initializePlayer()
        initializeMediaSession()
    }

    /**
     * Stops the service when the user swipes the app away from Recents, unless audio is
     * actively playing.
     *
     * If [player] is paused or the queue is empty, there is no reason to keep the service
     * alive, so [stopSelf] is called. If playback is active, the service continues running
     * as a foreground service until the user explicitly pauses or the queue ends.
     *
     * @param rootIntent The intent that launched the task being removed — not used here.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if(!player.playWhenReady || player.mediaItemCount == 0)
            stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    /**
     * Releases all resources in reverse-initialisation order.
     *
     * Cancels [serviceJob] first to abort any in-flight coroutines before releasing [player],
     * preventing callbacks from firing on a released player. Sets [session] to `null` after
     * release so that [onGetSession] returns `null` and no new controllers can bind during
     * teardown.
     */
    override fun onDestroy() {
        Timber.d("onDestroy: ")
        // Cancel coroutines before releasing the player to prevent post-release callbacks
        serviceJob.cancel()
        session?.run {
            player.release()
            // Null out session so onGetSession returns null during teardown
            session = null
        }
        super.onDestroy()
    }


    /**
     * Returns the [MediaLibrarySession] for incoming controller connections.
     *
     * Returns `null` after [onDestroy] has set [session] to `null`, which prevents new
     * controllers from binding during service teardown. Media3 requires this override to
     * route controllers to the correct session when a service hosts multiple sessions.
     *
     * @param controllerInfo Metadata about the connecting controller — not inspected here.
     * @return The active [session], or `null` if the service is being destroyed.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d("onGetSession: session=$session, session.token=${session?.token}")
        return session
    }

    /**
     * Constructs the [ExoPlayer] instance with audio-focus and noise-transition handling,
     * then prepares it with an empty queue so it is ready to accept items from the UI.
     *
     * Must be called before [initializeMediaSession] because the session requires an existing
     * player reference. Attaches [PlayerEventListener] to handle [pendingSeek] resolution
     * and future playback-state events.
     *
     * @return `true` unconditionally (return value reserved for future error reporting).
     */
    private fun initializePlayer(): Boolean {
        Timber.d("initializePlayer: ")

        val playerBuilder: ExoPlayer.Builder = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            // Request audio focus and duck other apps' audio when playback starts
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            // Pause playback automatically when headphones are unplugged
            .setHandleAudioBecomingNoisy(true)

        player = playerBuilder.build()

        player.addListener(PlayerEventListener())
        // Default to paused; the UI controls when playback begins
        player.playWhenReady = false

        // Prepare with an empty queue so the player is in STATE_READY before the UI adds items
        player.setMediaItems(listOf())
        player.prepare()
        return true
    }

    /**
     * Creates the [MediaLibrarySession] that connects [player] to all Media3 controllers
     * and registers it with the service.
     *
     * Requires [player] to be fully initialised before calling. Uses a random ID via
     * [generateRandomStringId] to avoid a crash caused by duplicate session IDs when the
     * service is restarted without being fully destroyed. Calls [addSession] so the OS can
     * discover and route controller connections to this session.
     *
     * @return `true` unconditionally (return value reserved for future error reporting).
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
     * Generates a unique session ID for each [MediaLibrarySession] instance.
     *
     * Media3 requires session IDs to be unique within a process. Reusing a fixed ID across
     * service restarts (where the old session may not yet be fully released) causes an
     * [IllegalStateException]. A random suffix ensures uniqueness without requiring a
     * counter or timestamp.
     *
     * @return A string in the form `"Tacoma Music Player: <random double>"`.
     */
    private fun generateRandomStringId(): String {
        return "Tacoma Music Player: ${Random.nextDouble()}"
    }


    /**
     * [Player.Listener] that handles playback events for [MusicService].
     *
     * Declared as an inner class so it can access [pendingSeek] directly. All callbacks are
     * invoked on the main thread by Media3.
     *
     * Key responsibility: consumes [pendingSeek] on the first [Player.EVENT_MEDIA_ITEM_TRANSITION]
     * after Android Auto loads a new queue, ensuring the player jumps to the correct song.
     */
    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            if (playbackState == Player.STATE_ENDED) {
                //TODO SOMETHING Analytics?
            }
        }

        /**
         * Consumes [pendingSeek] on the item-transition event that follows an Android Auto
         * queue load.
         *
         * After [onAddMediaItems] populates the queue with Android Auto's chosen album or
         * playlist, the first [Player.EVENT_MEDIA_ITEM_TRANSITION] signals that the new queue
         * is committed. At that point [pendingSeek] holds the index of the tapped song, and
         * [ExoPlayer.seekTo] is safe to call. The seek position is cleared immediately to
         * prevent re-seeking on any subsequent natural track transitions.
         *
         * @param player The player that fired the events.
         * @param events The batch of events that occurred in this player update cycle.
         */
        override fun onEvents(player: Player, events: Player.Events) {
            if(events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                // Apply the deferred Android Auto seek now that the new queue is loaded
                pendingSeek?.let { position ->
                    player.seekTo(position, 0)
                    // Clear immediately so natural track transitions do not re-trigger the seek
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