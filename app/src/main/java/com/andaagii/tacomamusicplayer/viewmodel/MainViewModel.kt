package com.andaagii.tacomamusicplayer.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.PlayerDatabase
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.enumtype.QueueAddType
import com.andaagii.tacomamusicplayer.enumtype.ScreenType
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.repository.MusicProviderRepository
import com.andaagii.tacomamusicplayer.repository.MusicRepository
import com.andaagii.tacomamusicplayer.service.MusicService
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.util.DataStoreUtil
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.util.UtilImpl.Companion.deletePicture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Central ViewModel for MainActivity. Manages the Media3 controller lifecycle, playback queue,
 * playback state, screen navigation, search, and runtime permissions.
 *
 * Threading: [playerListener] callbacks arrive on the main thread from Media3; all repository
 * and DataStore reads/writes run on [Dispatchers.IO] via [viewModelScope].
 *
 * Injected via Hilt: [MusicRepository], [MusicProviderRepository], [MediaItemUtil].
 *
 * Exposed StateFlow (continuous state):
 * - [mediaController] — the active [MediaController] once connected; `null` until connected.
 * - [currentSongGroup] — album or playlist currently loaded into the song-list view.
 * - [currentSearchList] — results of the most recent in-app text search.
 * - [isAudioPermissionGranted] — whether `READ_MEDIA_AUDIO` is granted.
 * - [currentlyPlayingSongs] — current player queue as a [MediaItem] list.
 * - [currentPlayingSongInfo] — metadata for the song currently playing.
 * - [isPlaying] — `true` while the player is actively playing audio.
 * - [shuffleMode] — current shuffle state ([ShuffleType]).
 * - [loopMode] — current [Player] repeat mode (one of `Player.REPEAT_MODE_*`).
 * - [originalSongOrder] — pre-shuffle queue; restored when the user turns shuffle off.
 * - [isShowingSearchMode] — whether the search bar is visible.
 * - [showLoadingScreen] — `true` until the persisted queue is restored on startup.
 * - [availablePlaylists] — live list of all user-created playlists.
 *
 * Exposed Flow/Channel (one-shot events):
 * - [navigateToPage] — one-shot event to scroll the [HorizontalPager] to a specific [PageType].
 * - [isPlaylistNameDuplicate] — one-shot event for a duplicate-playlist-name error.
 * - [screenState] — one-shot navigation event to a [ScreenType] destination.
 * - [notifyHideKeyboard] — one-shot keyboard-dismiss trigger.
 * - [clearQueue] — one-shot event indicating the queue was just cleared.
 * - [shouldShowAddPlaylistPromptOnPlaylistPage] — one-shot trigger to show the add-playlist dialog.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val musicRepo: MusicRepository,
    private val musicProvider: MusicProviderRepository,
    private val mediaItemUtil: MediaItemUtil
): AndroidViewModel(application) {

    /** [AppPermissionUtil] instance used by [checkPermissions] to verify audio access. */
    private val permissionManager = AppPermissionUtil()

    /**
     * The active [MediaController] used to control [MusicService] playback.
     *
     * `null` until [setupMediaController] completes asynchronously after [initializeMusicPlaying]
     * is called. UI should guard against null before sending playback commands.
     */
    val mediaController: StateFlow<MediaController?>
        get() = _mediaController
    private val _mediaController = MutableStateFlow<MediaController?>(null)

    /**
     * The album or playlist currently displayed in the song-list view.
     *
     * Updated by [querySongsFromAlbum] and [querySongsFromPlaylist]. Cleared to an empty
     * [SongGroup] at the start of each album query so the UI can show a loading state.
     */
    val currentSongGroup: StateFlow<SongGroup?>
        get() = _currentSongGroup
    private val _currentSongGroup = MutableStateFlow<SongGroup?>(null)

    /**
     * Results of the most recent in-app text search, updated by [querySearchData].
     * Empty until the user performs a search.
     */
    val currentSearchList: StateFlow<List<MediaItem>?>
        get() = _currentSearchList
    private val _currentSearchList = MutableStateFlow<List<MediaItem>?>(null)

    /** `true` when the `READ_MEDIA_AUDIO` (or `READ_EXTERNAL_STORAGE`) permission is granted. */
    val isAudioPermissionGranted: StateFlow<Boolean?>
        get() = _isAudioPermissionGranted
    private val _isAudioPermissionGranted = MutableStateFlow<Boolean?>(null)

    /**
     * One-shot event emitted when the user tries to create a playlist with a name that
     * already exists.
     */
    private val _isPlaylistNameDuplicate = Channel<Boolean>(Channel.BUFFERED)
    val isPlaylistNameDuplicate: Flow<Boolean> = _isPlaylistNameDuplicate.receiveAsFlow()

    //TODO move playlist add prompt to the overall fragment?

    /**
     * In-memory record of the current top-level screen.
     *
     * Updated by [setScreenData] alongside [screenState] so callers can check the current screen
     * synchronously without consuming the one-shot Channel event.
     */
    private var currentScreenType: ScreenType? = null

    /**
     * One-shot navigation event that directs [MainActivity] to navigate to a [ScreenType].
     *
     * Backed by a [Channel] so each destination is delivered exactly once and is not re-delivered
     * on Activity recreation (unlike plain StateFlow, which would replay the last screen).
     */
    private val _screenState = Channel<ScreenType>(Channel.BUFFERED)
    val screenState: Flow<ScreenType> = _screenState.receiveAsFlow()

    /**
     * One-shot event that scrolls the [MusicChooserScreen] [HorizontalPager] to the given [PageType].
     *
     * Backed by a [Channel] so each emission is consumed exactly once and is not re-delivered
     * on Fragment view recreation (unlike plain [MutableLiveData]).
     */
    private val _navigateToPage = Channel<PageType>(Channel.BUFFERED)
    val navigateToPage: Flow<PageType> = _navigateToPage.receiveAsFlow()

    /**
     * In-memory mirror of the currently visible [PageType].
     *
     * Not observed as LiveData; updated via [observeCurrentPage] and queried via [getCurrentPage].
     */
    private var currentPage: PageType? = null

    /**
     * The full player queue as a [MediaItem] list.
     *
     * Updated by [playerListener.onTimelineChanged] whenever the queue is modified (add, remove,
     * or reorder). Reflects the live state of the [MediaController]'s queue.
     */
    val currentlyPlayingSongs: StateFlow<List<MediaItem>>
        get() = _currentlyPlayingSongs
    private val _currentlyPlayingSongs = MutableStateFlow<List<MediaItem>>(emptyList())

    /**
     * Metadata for the song currently playing, including title, artist, album, and artwork URI.
     *
     * Built from [MediaMetadata] inside [playerListener.onMediaMetadataChanged]. Note that
     * `songUri` is always set to `"UNKNOWN"` because the playback URI is not available from
     * metadata callbacks alone.
     */
    val currentPlayingSongInfo: StateFlow<SongData?>
        get() = _currentPlayingSongInfo
    private val _currentPlayingSongInfo = MutableStateFlow<SongData?>(null)

    /** `true` while the player is actively playing audio; updated by [playerListener.onIsPlayingChanged]. */
    val isPlaying: StateFlow<Boolean>
        get() = _isPlaying
    private val _isPlaying = MutableStateFlow(false)

    /**
     * Current shuffle state ([ShuffleType.SHUFFLED] or [ShuffleType.NOT_SHUFFLED]).
     *
     * Toggled by [flipShuffleState] and persisted to DataStore via [saveShufflePref] on every
     * change. Restored from DataStore on startup via [determineShufflePref].
     */
    val shuffleMode: StateFlow<ShuffleType?>
        get() = _shuffleMode
    private val _shuffleMode = MutableStateFlow<ShuffleType?>(null)

    /**
     * Current [Player] repeat mode (`Player.REPEAT_MODE_OFF`, `REPEAT_MODE_ONE`, or
     * `REPEAT_MODE_ALL`).
     *
     * Cycled by [flipLoopMode] and persisted to DataStore via [saveLoopingPref].
     */
    val loopMode: StateFlow<Int?>
        get() = _loopMode
    private val _loopMode = MutableStateFlow<Int?>(null)

    /**
     * Snapshot of the queue order before the most recent shuffle was applied.
     *
     * Populated whenever songs are added with `shouldAddToOriginalList = true` inside
     * [addTracksSaveTrackOrder] and persisted to the database via [saveOriginalOrder] so it
     * survives app restarts. Used by [unshuffleSongs] to restore the pre-shuffle order.
     */
    val originalSongOrder: StateFlow<List<MediaItem>>
        get() = _originalSongOrder
    private val _originalSongOrder = MutableStateFlow<List<MediaItem>>(emptyList())

    /**
     * `true` while the search bar is active.
     *
     * Toggled by [flipSearchButtonState] and reset to `false` by [handleCancelSearchButtonClick].
     */
    val isShowingSearchMode: StateFlow<Boolean>
        get() = _isShowingSearchMode
    private val _isShowingSearchMode = MutableStateFlow(false)

    /**
     * One-shot keyboard-dismiss event. Emitted by [removeVirtualKeyboard]; collectors call
     * `hideSoftInput` in response.
     */
    private val _notifyHideKeyboard = Channel<Unit>(Channel.BUFFERED)
    val notifyHideKeyboard: Flow<Unit> = _notifyHideKeyboard.receiveAsFlow()

    /**
     * `true` from startup until [restoreQueue] completes (with a 500 ms settling delay).
     *
     * Controls the full-screen loading overlay in the UI. Hidden via [loadingHandler] after the
     * queue and playback position are restored.
     */
    val showLoadingScreen: StateFlow<Boolean>
        get() = _showLoadingScreen
    private val _showLoadingScreen = MutableStateFlow(true)

    /**
     * [Handler] used to post a delayed Runnable that hides the loading screen after the queue
     * restore operation settles.
     */
    val loadingHandler = Handler(Looper.getMainLooper())

    /**
     * One-shot event emitted when [clearQueue] empties the player queue, signalling the UI to
     * dismiss the now-playing panel.
     */
    private val _clearQueue = Channel<Unit>(Channel.BUFFERED)
    val clearQueue: Flow<Unit> = _clearQueue.receiveAsFlow()

    /**
     * One-shot trigger to show the add-playlist prompt dialog on the Playlist tab.
     */
    private val _shouldShowAddPlaylistPromptOnPlaylistPage = Channel<Unit>(Channel.BUFFERED)
    val shouldShowAddPlaylistPromptOnPlaylistPage: Flow<Unit> =
        _shouldShowAddPlaylistPromptOnPlaylistPage.receiveAsFlow()

    /**
     * Live list of all user-created playlists as [MediaItem] objects.
     *
     * Backed by a Room [Flow] via [MusicRepository.getAllAvailablePlaylistFlow]; emits a new list
     * whenever the playlist table changes. Shared with `WhileSubscribed(5_000)` so the query stops
     * 5 seconds after the last subscriber disappears.
     */
    val availablePlaylists: StateFlow<List<MediaItem>> = musicRepo.getAllAvailablePlaylistFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            listOf()
        )

    /**
     * [Player.Listener] that bridges ExoPlayer callbacks to this ViewModel's LiveData properties.
     *
     * Registered on the [MediaController] in [setupMediaController] and removed in [onCleared].
     * All callbacks are invoked on the main thread by Media3.
     */
    private val playerListener = object: Player.Listener {
        /**
         * Maps the incoming [MediaMetadata] to a [SongData] and posts it to
         * [currentPlayingSongInfo].
         *
         * Note: [SongData.songUri] is set to `"UNKNOWN"` because the playback URI is not
         * accessible from metadata callbacks — only the metadata fields are available here.
         */
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Timber.d("onMediaMetadataChanged: artist=${mediaMetadata.artist}, title=${mediaMetadata.title}, albumTitle=${mediaMetadata.albumTitle}")
            _currentPlayingSongInfo.value = SongData(
                songUri = "UNKNOWN",
                songTitle = mediaMetadata.title.toString(),
                albumTitle = mediaMetadata.albumTitle.toString(),
                artist = mediaMetadata.artist.toString(),
                artworkUri = mediaMetadata.artworkUri.toString(),
                duration = mediaMetadata.description.toString()
            )
            super.onMediaMetadataChanged(mediaMetadata)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _isPlaying.value = isPlaying
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Timber.d("onRepeatModeChanged: ")
            super.onRepeatModeChanged(repeatMode)
            _loopMode.value = repeatMode
        }

        /**
         * Updates [currentlyPlayingSongs] whenever the player queue changes.
         *
         * [onTimelineChanged] fires for any structural queue modification (add, remove, reorder,
         * or clear) — making it the correct place to keep [currentlyPlayingSongs] in sync with the
         * live queue, not just on track transitions.
         */
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            _currentlyPlayingSongs.value = mediaController.value?.let { controller ->
                UtilImpl.getSongListFromMediaController(controller)
            } ?: emptyList()
        }
    }

    /**
     * Toggles [isShowingSearchMode] between `true` and `false`.
     *
     * Also dismisses the soft keyboard via [removeVirtualKeyboard] to prevent it lingering
     * after the search bar is hidden.
     */
    fun flipSearchButtonState() {
        Timber.d("flipSearchButtonState: isSearchMode=${_isShowingSearchMode.value}")
        _isShowingSearchMode.value = !_isShowingSearchMode.value
        removeVirtualKeyboard()
    }

    /**
     * Explicitly hides search mode by setting [isShowingSearchMode] to `false`.
     *
     * Used when the user taps the "Cancel" button, as opposed to tapping the search icon
     * which goes through [flipSearchButtonState].
     */
    fun handleCancelSearchButtonClick() {
        Timber.d("handleCancelSearchButtonClick: ")
        _isShowingSearchMode.value = false
    }

    /**
     * Posts the next integer to [notifyHideKeyboard] as a one-shot keyboard-dismiss event.
     *
     * Observers detect any value change and call `hideSoftInput`; the value itself is
     * irrelevant — only the change matters.
     */
    fun removeVirtualKeyboard() {
        Timber.d("removeVirtualKeyboard: ")
        _notifyHideKeyboard.trySend(Unit)
    }

    /**
     * Sets the currentSearchList based on user search.
     */
    fun querySearchData(search: String) {
        Timber.d("querySearchData: search=$search")
        viewModelScope.launch(Dispatchers.IO) {
            _currentSearchList.value = musicRepo.searchMusic(search)
        }
    }

    /**
     * Changes between songs being shuffled and songs being in original order.
     */
    fun flipShuffleState() {
        if(_shuffleMode.value == ShuffleType.SHUFFLED) {
            //Set to be original order
            _shuffleMode.value = ShuffleType.NOT_SHUFFLED
            unshuffleSongs()
            saveShufflePref(getApplication<Application>().applicationContext, ShuffleType.NOT_SHUFFLED)
            Timber.d("flipShuffleState: ${ShuffleType.NOT_SHUFFLED}")
        } else {
            //Set to be shuffled
            _shuffleMode.value = ShuffleType.SHUFFLED
            shuffleSongsInMediaController()
            saveShufflePref(getApplication<Application>().applicationContext, ShuffleType.SHUFFLED)
            Timber.d("flipShuffleState: ${ShuffleType.SHUFFLED}")
        }
    }

    /**
     * Cycles the repeat mode through `REPEAT_MODE_OFF` → `REPEAT_MODE_ONE` → `REPEAT_MODE_ALL`
     * → `REPEAT_MODE_OFF` and persists the new value via [saveLoopingPref].
     *
     * Updates [loopMode] indirectly via [playerListener.onRepeatModeChanged].
     */
    fun flipLoopMode() {
        if(_loopMode.value == Player.REPEAT_MODE_OFF) {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_ONE}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_ONE
        } else if(_loopMode.value == Player.REPEAT_MODE_ONE) {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_ALL}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_ALL
        } else {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_OFF}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_OFF
        }

        saveLoopingPref(getApplication<Application>().applicationContext, _mediaController.value?.repeatMode ?: Player.REPEAT_MODE_ONE)
    }

    /**
     * Pauses the [MediaController] if audio is currently playing; plays it if paused.
     *
     * [isPlaying] is updated automatically via [playerListener.onIsPlayingChanged].
     */
    fun flipPlayingState() {
        if(_isPlaying.value) {
            _mediaController.value?.pause()
            Timber.d("flipPlayingState: Pausing!")
        } else {
            _mediaController.value?.play()
            Timber.d("flipPlayingState: Playing!")
        }
    }

    /**
     * Entry point called during [init] to restore all saved playback preferences.
     *
     * Currently restores only the shuffle preference; looping is restored later inside
     * [setupMediaController] once the [MediaController] is connected and can accept a repeat mode.
     *
     * @param context Application context used to access DataStore.
     */
    private fun setMusicPlayingPrefs(context: Context) {
        Timber.d("setMusicPlayingPrefs: ")
        //determineLoopingPref(context)
        determineShufflePref(context)
    }

    /**
     * Reads the looping preference from DataStore and applies it to the [MediaController].
     *
     * Must be called after the controller is connected (inside [setupMediaController]'s callback)
     * so that `repeatMode` can actually be set on the controller instance.
     *
     * @param context Application context used to access DataStore.
     */
    private fun determineLoopingPref(context: Context) {
        Timber.d("determineLoopingPref: ")
        viewModelScope.launch {
            DataStoreUtil.getLoopingPreference(context).collect { loopingPref ->
                _mediaController.value?.repeatMode = loopingPref
            }
        }
    }

    /**
     * Reads the shuffle preference from DataStore and posts the corresponding [ShuffleType] to
     * [shuffleMode].
     *
     * @param context Application context used to access DataStore.
     */
    private fun determineShufflePref(context: Context) {
        Timber.d("determineShufflePref: ")
        viewModelScope.launch {
            DataStoreUtil.getShufflePreference(context).collect { shufflePref ->
                val shuffleType = ShuffleType.determineShuffleTypeFromString(shufflePref)
                _shuffleMode.value = shuffleType
            }
        }
    }

    /**
     * Persists the current [Player] repeat mode integer to DataStore on [Dispatchers.IO].
     *
     * @param context Application context used to access DataStore.
     * @param loopInt One of `Player.REPEAT_MODE_OFF`, `REPEAT_MODE_ONE`, or `REPEAT_MODE_ALL`.
     */
    private fun saveLoopingPref(context: Context, loopInt: Int) {
        Timber.d("saveLoopingPref: loopInt=$loopInt")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setLoopingPreference(context, loopInt)
        }
    }

    /**
     * Persists the current [ShuffleType] to DataStore on [Dispatchers.IO].
     *
     * @param context Application context used to access DataStore.
     * @param shuffleType The shuffle state to save.
     */
    private fun saveShufflePref(context: Context, shuffleType: ShuffleType) {
        Timber.d("saveShufflePref: shuffleType=$shuffleType")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setShufflePreference(context, shuffleType)
        }
    }

    /**
     * Emits [page] as a one-shot navigation event on [navigateToPage], causing the
     * [MusicChooserScreen] [HorizontalPager] to scroll to that page.
     *
     * @param page The [PageType] to navigate to.
     */
    fun setPage(page: PageType) {
        _navigateToPage.trySend(page)
    }

    /**
     * Records [page] in the in-memory [currentPage] field so [getCurrentPage] can be queried
     * synchronously without observing LiveData.
     *
     * @param page The [PageType] that has just become visible.
     */
    fun observeCurrentPage(page: PageType) {
        currentPage = page
    }

    /**
     * Returns the most recently recorded [PageType], or `null` if [observeCurrentPage] has not
     * been called yet.
     */
    fun getCurrentPage(): PageType? {
        return currentPage
    }

    /**
     * [MediaBrowser] connected to [MusicService] for traversing the browsable media library tree.
     *
     * Initialised asynchronously in [setupMediaBrowser]; must not be accessed before that
     * completes.
     */
    private lateinit var mediaBrowser: MediaBrowser

    /**
     * The root node returned by [MusicService.onGetLibraryRoot]; stored for potential future
     * tree traversal.
     */
    private var rootMediaItem: MediaItem? = null

    /**
     * [SessionToken] identifying the [MusicService] instance.
     *
     * Created in [createSessionToken] and shared by both [setupMediaController] and
     * [setupMediaBrowser].
     */
    private lateinit var sessionToken: SessionToken

    init {
        Timber.d("init: ")
        checkPermissions()

        //ex. the layout of the albums / playlist fragments
        checkUserPreferences()
    }

    /**
     * Determine all saved user preferences, loopMode, shuffleMode, layout, sorting.
     */
    private fun checkUserPreferences() {
        Timber.d("checkUserPreferences: ")
        setMusicPlayingPrefs(getApplication<Application>().applicationContext)
    }

    /**
     * Creates a new empty playlist with the given name in the database.
     *
     * [availablePlaylists] updates automatically via the Room → Flow pipeline after creation.
     *
     * @param playlistName The display name for the new playlist.
     */
    fun createNamedPlaylist(playlistName: String) {
        Timber.d("createNamedPlaylist: playlistName=$playlistName")
        viewModelScope.launch {
            musicRepo.createPlaylist(playlistName)
        }
    }

    /**
     * Persists the current song ordering of [albumSongGroup] to the database.
     *
     * Only acts when [albumSongGroup] has type [SongGroupType.PLAYLIST]; album orderings are
     * fixed by the source data and cannot be reordered by the user.
     *
     * @param albumSongGroup The [SongGroup] whose song order should be saved.
     */
    fun updatePlaylistOrder(albumSongGroup: SongGroup) {
        Timber.d("updatePlaylistOrder: albumSongGroup=$albumSongGroup")
        if(albumSongGroup.type == SongGroupType.PLAYLIST) {
            viewModelScope.launch(Dispatchers.IO) {
                musicRepo.updatePlaylistSongOrder(
                    albumSongGroup.group.mediaMetadata.albumTitle.toString(),
                    albumSongGroup.songs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
                )
            }
        }
    }

    /**
     * Removes [songsToRemove] from the current playlist group, updates [currentSongGroup]
     * so the UI re-renders immediately, and persists the new order via [updatePlaylistOrder].
     *
     * @param songsToRemove The tracks to remove, matched by media ID.
     */
    fun removeSongsFromCurrentPlaylist(songsToRemove: List<MediaItem>) {
        val current = _currentSongGroup.value ?: return
        if (current.type != SongGroupType.PLAYLIST) return
        val updated = current.copy(
            songs = current.songs.filter { song -> songsToRemove.none { it.mediaId == song.mediaId } }
        )
        _currentSongGroup.value = updated
        updatePlaylistOrder(updated)
    }

    /**
     * Saves the current songs playing in the queue, to be loaded when the app opens next.
     */
    fun saveQueue() {
        Timber.d("saveQueue: ")
        mediaController.value?.let { controller ->
            //Save current Player state
            savePlayerState(controller)

            val songs = UtilImpl.getSongListFromMediaController(controller)

            if(songs.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    //In case queue has never been initialized
                    musicRepo.createInitialQueueIfEmpty(Const.PLAYLIST_QUEUE_TITLE)

                    musicRepo.updatePlaylistSongOrder(
                        Const.PLAYLIST_QUEUE_TITLE,
                        songs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
                    )
                }
            }
        }
    }

    /**
     * When the user exits the app in shuffled mode, give the user the ability to return to ordered mode.
     */
    fun saveOriginalOrder(songs: List<MediaItem>) {
        Timber.d("saveOriginalOrder: ")
        viewModelScope.launch(Dispatchers.IO) {
            //In case queue has never been initialized
            musicRepo.createInitialQueueIfEmpty(Const.ORIGINAL_QUEUE_ORDER)

            musicRepo.updatePlaylistSongOrder(
                Const.ORIGINAL_QUEUE_ORDER,
                songs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
            )
        }
    }

    /**
     * Persists the current playback position and queue index to DataStore so they can be restored
     * by [restoreQueue] on the next app launch.
     *
     * @param controller The active [MediaController] whose position and index are read.
     */
    private fun savePlayerState(controller: MediaController) {
        val playbackPosition = controller.currentPosition
        val songPosition = controller.currentMediaItemIndex
        Timber.d("savePlayerState: playbackPosition=$playbackPosition, songPosition=$songPosition")

        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaybackPosition(getApplication<Application>().applicationContext, playbackPosition)
            DataStoreUtil.setSongPosition(getApplication<Application>().applicationContext, songPosition)
        }
    }

    /**
     * Restores the persisted queue from the previous session and seeks to the saved position.
     *
     * Startup restore sequence (runs on [Dispatchers.IO], switches to Main for controller calls):
     * 1. Read the saved playback position and queue index from DataStore.
     * 2. Load the persisted queue songs from the database.
     * 3. Add the songs to the controller via [addTracksSaveTrackOrder].
     * 4. Seek to the saved index and position.
     * 5. Hide the loading screen after a 500 ms delay to let the UI settle.
     */
    private fun restoreQueue() {
        Timber.d("restoreQueue: ")
        viewModelScope.launch(Dispatchers.IO) {
            val playbackPosition = DataStoreUtil.getPlaybackPosition(getApplication<Application>().applicationContext).firstOrNull()
            val songPosition = DataStoreUtil.getSongPosition(getApplication<Application>().applicationContext).firstOrNull()

            val queue = musicRepo.getSongsFromPlaylist(Const.PLAYLIST_QUEUE_TITLE)
            Timber.d("restoreQueue: queue=${queue.map { it.mediaMetadata.title }}")

            withContext(Dispatchers.Main) {
                // Restore Playback State
                mediaController.value?.let { controller ->
                    addTracksSaveTrackOrder(
                        mediaItems = queue,
                        clearOriginalSongList = false,
                        clearCurrentSongs = true,
                        shouldAddToOriginalList = false,
                        preventShuffle = true
                    )

                    if(songPosition != null && songPosition < controller.mediaItemCount) {
                        if(playbackPosition != null) {
                            controller.seekTo(songPosition, playbackPosition)
                        } else {
                            controller.seekTo(songPosition, 0)
                        }
                    }

                    loadingHandler.postDelayed({
                        _showLoadingScreen.value = false
                    }, 500)
                }
            }
        }
    }

    /**
     * Loads the pre-shuffle song order from the database into [originalSongOrder].
     *
     * Called during startup (from [setupMediaController]) so that [unshuffleSongs] has the
     * original ordering available if the user turns shuffle off in the current session.
     */
    private fun restoreQueueOrder() {
        Timber.d("restoreQueueOrder: ")
        viewModelScope.launch(Dispatchers.IO) {
            val queueOrdered = musicRepo.getSongsFromPlaylist(Const.ORIGINAL_QUEUE_ORDER)
            Timber.d("restoreQueueOrder: queueOrdered=${queueOrdered.map { it.mediaMetadata.title }}")
            _originalSongOrder.value = queueOrdered
        }
    }

    /**
     * Adds [songs] to every playlist in [playlistTitles].
     *
     * Iterates over each title and delegates to [addListOfSongMediaItemsToAPlaylist], which
     * runs the database write on [Dispatchers.IO]. Multiple playlists are processed sequentially.
     *
     * @param playlistTitles The titles of the playlists to add songs to.
     * @param songs The [MediaItem]s to append to each playlist.
     */
    fun addSongsToAPlaylist(playlistTitles: List<String>, songs: List<MediaItem>) {
        Timber.d("addSongsToAPlaylist: playlistTitles=$playlistTitles, songDescriptions=$songs")
        playlistTitles.forEach { playlist ->
            addListOfSongMediaItemsToAPlaylist(playlist, songs)
        }
    }
    
    /**
     * Renames a playlist from [currentTitle] to [newTitle] in the database.
     *
     * Delegates to [MusicRepository] on [Dispatchers.IO]. [availablePlaylists] updates
     * automatically via the Room → Flow pipeline.
     *
     * @param currentTitle The existing playlist title to rename.
     * @param newTitle The new title to assign.
     */
    fun updatePlaylistTitle(currentTitle: String, newTitle: String ) {
        Timber.d("updatePlaylistTitle: currentTitle=$currentTitle, newTitle=$newTitle")
        viewModelScope.launch(Dispatchers.IO) {
            musicRepo.updatePlaylistTitle(currentTitle, newTitle)
        }
    }

    /**
     * Update the playlist image.
     */
    fun updateSongGroupImage(title: String, artFileName: String, updateSongs: Boolean = false) {
        Timber.d("updateSongGroupImage: title=$title, artFileName=$artFileName")
        viewModelScope.launch(Dispatchers.IO) {
            // Update Song Group
            musicRepo.updateSongGroupImage(title, artFileName)

            // Update an album's songs with it's new custom image
            if(updateSongs) {
                musicRepo.updateAlbumSongsWithCustomImage(title, artFileName)
            }

            //TODO hotfix, refresh the songgroup if updated songgroup == current songGroup
        }
    }

    /**
     * Adds [songs] to the playlist identified by [playlistTitle] in the database.
     *
     * [MediaItem] objects are mapped to their `searchDescription` string keys before being
     * passed to the repository, because the database stores song-playlist associations by
     * the unique search description rather than by URI or media ID.
     *
     * Even when adding a single song, use this function to go through the correct mapping path.
     *
     * @param playlistTitle The title of the target playlist.
     * @param songs The [MediaItem] objects to add.
     */
    private fun addListOfSongMediaItemsToAPlaylist(playlistTitle: String, songs: List<MediaItem>) {
        Timber.d("addListOfSongMediaItemsToAPlaylist: playlistTitle=$playlistTitle, songDescriptions.size=${songs.size}")
        viewModelScope.launch(Dispatchers.IO) {
            val songDescriptions = songs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
            musicRepo.addSongsToPlaylist(playlistTitle, songDescriptions)
        }
    }

    /**
     * Cleans up resources when the ViewModel is destroyed.
     *
     * Removes [playerListener] from the controller to prevent stale callbacks, then releases
     * [mediaBrowser] if it was successfully initialised.
     */
    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared: ")

        mediaController.value?.let { controller ->
            controller.removeListener(playerListener)
        }

        if(this::mediaBrowser.isInitialized) {
            mediaBrowser.release()
        }

        _navigateToPage.close()
        _isPlaylistNameDuplicate.close()
        _screenState.close()
        _notifyHideKeyboard.close()
        _clearQueue.close()
        _shouldShowAddPlaylistPromptOnPlaylistPage.close()
    }

    /**
     * Re-checks audio permissions only when the app is currently on the permission-denied screen.
     *
     * Prevents redundant permission checks from other screens while still allowing the
     * permission-denied screen to re-verify after the user grants permission in Settings.
     */
    fun checkPermissionsIfOnPermissionDeniedScreen() {
        Timber.d("checkPermissionsIfOnPermissionDeniedScreen: ")
        if (currentScreenType == ScreenType.PERMISSION_DENIED_SCREEN) {
            checkPermissions()
        }
    }

    /**
     *  Clear queue and play the song group at a certain position.
     */
    fun playSongGroupAtPosition(songGroup: SongGroup, position: Int) {
        Timber.d("playSongGroupAtPosition: songGroup=$songGroup, position=$position")
        mediaController.value?.let { controller ->
            controller.pause()

            addTracksSaveTrackOrder(
                songGroup.songs,
                clearOriginalSongList = true,
                startingSongPosition = position,
                clearCurrentSongs = true,
                shouldAddToOriginalList = true
            )
            controller.play()
        }
    }

    /**
     * Clear queue and play the specified playlist.
     * @param playlistTitle The groupTitle of a playlist.
     */
    fun playPlaylist(playlistTitle: String) {
        Timber.d("playPlaylist: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            val playlistSongs = musicRepo.getSongsFromPlaylist(playlistTitle = playlistTitle)

            withContext(Dispatchers.Main) {
                addTracksSaveTrackOrder(
                    mediaItems = playlistSongs,
                    clearOriginalSongList = true,
                    startingSongPosition = 0,
                    clearCurrentSongs = true,
                    shouldAddToOriginalList = true
                )

                mediaController.value?.play()
            }
        }
    }

    /**
     * Adds all playlist songs to the back of the current queue.
     * @param playlistTitle The groupTitle of a playlist.
     */
    fun addPlaylistToBackOfQueue(playlistTitle: String) {
        Timber.d("addPlaylistToBackOfQueue: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            val playlistSongs = musicRepo.getSongsFromPlaylist(playlistTitle = playlistTitle)

            withContext(Dispatchers.Main) {
                addTracksSaveTrackOrder(
                    mediaItems = playlistSongs,
                    clearOriginalSongList = false,
                    clearCurrentSongs = false,
                    shouldAddToOriginalList = true
                )
            }
        }
    }

    /**
     * Adds multiple songs to the end of the controller in the queue
     */
    fun addSongsToEndOfQueue(songs: List<MediaItem>) {
        Timber.d("addSongsToEndOfQueue: songs=$songs")
        addTracksSaveTrackOrder(
            mediaItems = songs,
            clearOriginalSongList = false,
            clearCurrentSongs = false,
            shouldAddToOriginalList = true
        )
    }

    /**
     * Clear all songs out of Player.
     */
    fun clearQueue() {
        Timber.d("clearQueue: ")
        addTracksSaveTrackOrder(
            mediaItems = listOf(),
            clearOriginalSongList = true,
            clearCurrentSongs = true,
            shouldAddToOriginalList = false
        )
        _clearQueue.trySend(Unit)
    }

    /**
     * Triggers the add-playlist prompt dialog on the Playlist tab.
     */
    fun showAddPlaylistPromptOnPlaylistPage() {
        _shouldShowAddPlaylistPromptOnPlaylistPage.trySend(Unit)
    }

    /**
     * Sets the current screen of the application.
     * @param nextScreen The next screen to be navigated to.
     */
    private fun setScreenData(nextScreen: ScreenType) {
        Timber.d("setScreenData: nextScreen=$nextScreen")
        if (currentScreenType != nextScreen) {
            currentScreenType = nextScreen
            _screenState.trySend(nextScreen)
        }
    }

    /**
     * Starts [MusicService] and establishes both the [MediaController] and [MediaBrowser]
     * connections using a shared [SessionToken].
     *
     * Must be called from [MainActivity] after permissions are confirmed. Both connections are
     * asynchronous; the ViewModel updates [mediaController] and [mediaBrowser] once they resolve.
     */
    fun initializeMusicPlaying() {
        if (_mediaController.value != null) return
        Timber.d("initializeMusicPlaying: ")
        sessionToken = createSessionToken()
        setupMediaController(sessionToken)
        setupMediaBrowser(sessionToken)
    }

    /**
     * Creates the [SessionToken] that identifies [MusicService] to Media3.
     *
     * The token is required to connect both the [MediaController] and [MediaBrowser]. Creating
     * it also implicitly starts [MusicService] if it is not already running.
     *
     * @return A [SessionToken] bound to [MusicService].
     */
    private fun createSessionToken(): SessionToken {
        Timber.d("createSessionToken: ")
        return SessionToken(getApplication<Application>().applicationContext, ComponentName(getApplication<Application>().applicationContext, MusicService::class.java))
    }

    /**
     * Asynchronously builds a [MediaController] connected to [session] and performs the startup
     * restore sequence once the connection succeeds.
     *
     * Restore sequence (runs inside the future listener):
     * 1. Store the controller and apply the saved looping preference.
     * 2. Restore the persisted queue via [restoreQueue] (which also hides the loading screen).
     * 3. Restore the pre-shuffle original order via [restoreQueueOrder].
     * 4. Post the current repeat mode to [loopMode] and register [playerListener].
     *
     * @param session The [SessionToken] identifying [MusicService].
     */
    private fun setupMediaController(session: SessionToken) {
        Timber.d("setupMediaController: session=$session")
        val controllerFuture = MediaController.Builder(getApplication<Application>().applicationContext, session).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()

            _mediaController.value = controller

            determineLoopingPref(getApplication<Application>().applicationContext)

            // Restore the persisted queue from the previous session
            restoreQueue()

            // Restore the original (pre-shuffle) ordering so unshuffleSongs has data
            restoreQueueOrder()

            _loopMode.value = controller.repeatMode
            controller.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    /**
     * Asynchronously builds a [MediaBrowser] connected to [sessionToken] and fetches the
     * library root once the connection succeeds.
     *
     * [getRoot] is called inside the listener (after the browser is ready) to ensure the
     * browser is in the connected state before issuing the root request.
     *
     * @param session The [SessionToken] identifying [MusicService] — not used directly here;
     *   [sessionToken] field is used instead to match the existing build pattern.
     */
    private fun setupMediaBrowser(session: SessionToken) {
        Timber.d("DT>>> setupMediaBrowser: session=$session")
        val browserFuture = MediaBrowser.Builder(getApplication<Application>().applicationContext, sessionToken)
            .buildAsync()
        browserFuture.addListener({
            browserFuture.get().let { browser ->
                mediaBrowser = browser
                getRoot()
                Timber.d("setupMediaBrowser: sessionToken=${mediaBrowser.connectedToken}")
            }
            // Duplicate assignment — browserFuture.get() returns the same instance as above
            mediaBrowser = browserFuture.get()
        }, MoreExecutors.directExecutor())
    }

    /**
     * Fetches the top-level [MediaItem] from [MusicService] via [mediaBrowser] and stores it in
     * [rootMediaItem].
     *
     * The root is the entry point for any future media library tree traversal. Called from
     * [setupMediaBrowser] once the browser connection is established.
     */
    private fun getRoot() {
        Timber.d("getRoot: ")
        mediaBrowser?.let { browser ->
            val rootFuture = browser.getLibraryRoot(null)
            rootFuture.addListener({
                rootMediaItem = rootFuture.get().value
            }, MoreExecutors.directExecutor())
        }
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on album title.
     * @param albumId The title of an album to be queried.
     */
    fun querySongsFromAlbum(album: MediaItem, queueAddType: QueueAddType = QueueAddType.QUEUE_DONT_ADD) {
        Timber.d("querySongsFromAlbum: album=$album, queueAddType=$queueAddType")

        //clear the previous album
        _currentSongGroup.value = SongGroup(
            type=SongGroupType.ALBUM,
            songs = listOf(),
            group = MediaItem.EMPTY,
        )

        val albumTitle = album.mediaMetadata.albumTitle.toString()
        viewModelScope.launch {
            val albumSongs = musicRepo.getSongsFromAlbum(albumTitle)
            val songGroup = SongGroup(
                type = SongGroupType.ALBUM,
                songs = albumSongs,
                album
            )
            _currentSongGroup.value = songGroup

            if(queueAddType == QueueAddType.QUEUE_CLEAR_ADD) {
                addTracksSaveTrackOrder(
                    mediaItems = songGroup.songs,
                    clearOriginalSongList = true,
                    startingSongPosition = 0,
                    clearCurrentSongs = true,
                    shouldAddToOriginalList = true
                )
                mediaController.value?.let { controller ->
                    controller.play()
                }
            } else if(queueAddType == QueueAddType.QUEUE_END_ADD) {
                addTracksSaveTrackOrder(
                    mediaItems = songGroup.songs,
                    clearOriginalSongList = false,
                    startingSongPosition = null,
                    clearCurrentSongs = false,
                    shouldAddToOriginalList = true
                )
            }
        }
    }

    /**
     * Loads the album that [song] belongs to and starts playback at that song's position.
     *
     * Used when the user taps a song in search results, so playback begins in the context of the
     * full album rather than just the single result. The song's position is found via
     * `indexOfFirst` matching on title; if the song is not found in the album list (returns -1),
     * playback falls back to position 0.
     *
     * @param song The [MediaItem] the user tapped.
     */
    fun playAlbumAtSongPosition(song: MediaItem) {
        viewModelScope.launch {
            val album = musicProvider.getSongsFromAlbum(
                song.mediaMetadata.albumTitle.toString(), //TODO This title isn't coming in correct... good kid,
                useFileProviderUri = true
            ).toMutableList()

            var position = album.indexOfFirst { it.mediaMetadata.title == song.mediaMetadata.title }
            if(position == -1) position = 0

            //TODO error when I try to play song from search list into a shuffled player [idealy chosen song would be at position 0...]

            addTracksSaveTrackOrder(
                mediaItems = album,
                clearOriginalSongList = true,
                clearCurrentSongs = true,
                startingSongPosition = position,
                shouldAddToOriginalList = true
            )

            mediaController.value?.play()
        }
    }

    /**
     * Central queue-management function that adds [mediaItems] to the player while maintaining
     * the pre-shuffle [originalSongOrder] snapshot.
     *
     * All queue mutations in this ViewModel go through this function so that shuffle and unshuffle
     * work correctly and the original order is always persisted.
     *
     * @param mediaItems The songs to add to the queue.
     * @param clearOriginalSongList If `true`, resets [originalSongOrder] to empty before appending.
     *   Use when replacing the queue entirely (e.g., playing a new album).
     * @param startingSongPosition The queue index to seek to after adding; `null` to stay at the
     *   current position. Ignored when [shuffleMode] is [ShuffleType.SHUFFLED] (shuffled queues
     *   always start at index 0 with the chosen song pinned first).
     * @param clearCurrentSongs If `true`, clears the player queue before adding [mediaItems].
     * @param shouldAddToOriginalList If `true`, appends [mediaItems] to [originalSongOrder] and
     *   persists it via [saveOriginalOrder].
     * @param preventShuffle If `true`, adds items in their original order even when shuffle is
     *   active. Used during queue restoration to avoid re-shuffling a previously shuffled queue.
     */
    private fun addTracksSaveTrackOrder(
        mediaItems: List<MediaItem>,
        clearOriginalSongList: Boolean = false,
        startingSongPosition: Int? = null,
        clearCurrentSongs: Boolean = false,
        shouldAddToOriginalList: Boolean = false,
        preventShuffle: Boolean = false
    ) {
        Timber.d("addTracksSaveTrackOrder: originalSongOrder=${_originalSongOrder.value.map { it.mediaMetadata.title }}, mediaItems=${mediaItems.map { it.mediaMetadata.title }}, " +
                "clearOriginalSongList=$clearOriginalSongList, startingSongPosition=$startingSongPosition, " +
                "clearCurrentSongs=$clearCurrentSongs, shouldAddToOriginalList=$shouldAddToOriginalList")

        // 1. Clear the existing queue if requested
        if(clearCurrentSongs) {
            _mediaController.value?.clearMediaItems()
        }

        // 2. Reset the original-order snapshot if requested
        if(clearOriginalSongList) {
            Timber.d("addTracksSaveTrackOrder: Setting Clear Original Song List!")
            _originalSongOrder.value = emptyList()
        }

        // 3. Append the new items to the original-order snapshot and persist it
        val songOrder = originalSongOrder.value.toMutableList()
        songOrder.addAll(mediaItems)

        if(shouldAddToOriginalList) {
            Timber.d("addTracksSaveTrackOrder: songOrder=${songOrder.map { it.mediaMetadata.title }}, mediaItems=${mediaItems.map { it.mediaMetadata.title }}, clearOriginalSongList=$clearOriginalSongList")
            _originalSongOrder.value = songOrder

            Timber.d("addTracksSaveTrackOrder: Save Original Order songOrder=${songOrder.map { it.mediaMetadata.title }}")
            saveOriginalOrder(songOrder)
        }

        // 4. Add items to the controller — shuffle if active, preserve order otherwise
        _mediaController.value?.let { controller ->
            if(_shuffleMode.value == ShuffleType.SHUFFLED && !preventShuffle) {

                //TODO I should add a feature where when I shuffle an entire playlist or album, first song isn't preserved...
                val shuffledSongs = shuffleSongs(mediaItems, startingSongPosition)

                if(controller.mediaItemCount == 0) {
                    controller.setMediaItems(shuffledSongs)
                } else {
                    controller.addMediaItems(shuffledSongs)
                }
            } else {
                //TODO update all places where I set / add mediaItems
                if(controller.mediaItemCount == 0) {
                    controller.setMediaItems(mediaItems)
                } else {
                    controller.addMediaItems(mediaItems)
                }
            }
        }

        // 5. Seek to the starting position (only when not shuffled; shuffled queues pin the chosen
        //    song at index 0 via shuffleSongs, so a separate seek is unnecessary)
        if(_shuffleMode.value != ShuffleType.SHUFFLED) {
            startingSongPosition?.let { position ->
                _mediaController.value?.seekTo(position, 0L)
            }
        }
    }

    /**
     * Returns [mediaItems] in a shuffled order.
     *
     * When [startingSongPosition] is provided, the song at that index is placed first in the
     * result and the remaining songs are shuffled behind it — preserving the user's
     * "play this song now" intent while randomising the rest of the queue.
     *
     * @param mediaItems The songs to shuffle.
     * @param startingSongPosition The index of the song that should remain at position 0 in the
     *   shuffled result; `null` to shuffle all songs with no pinned starting song.
     * @return A new list with the songs in shuffled order.
     */
    private fun shuffleSongs(mediaItems: List<MediaItem>, startingSongPosition: Int? = null): List<MediaItem> {
        Timber.d("shuffleSongs: mediaItems=${mediaItems.map { it.mediaMetadata.title }} startingSongPosition=$startingSongPosition")
        if(startingSongPosition == null) {
            return mediaItems.shuffled()
        } else {

            val songOrder = mutableListOf<MediaItem>()
            songOrder.add(mediaItems[startingSongPosition])

            val songsMinusFirstSong = mediaItems.toMutableList()
            songsMinusFirstSong.removeAt(startingSongPosition)
            songsMinusFirstSong.shuffle()

            songOrder.addAll(songsMinusFirstSong)
            return songOrder
        }
    }

    /**
     * Shuffles the current live player queue in-place.
     *
     * Reads the current queue from the [MediaController], shuffles it via [shuffleSongs], then
     * replaces the queue using [addTracksSaveTrackOrder] with `shouldAddToOriginalList = false`
     * so that [originalSongOrder] is not overwritten by the shuffled order.
     */
    private fun shuffleSongsInMediaController() {
        Timber.d("shuffleSongsInMediaController: ")
        _mediaController.value?.let { controller ->
            val currentSongs = UtilImpl.getSongListFromMediaController(controller)

            val shuffledSongs = shuffleSongs(currentSongs)

            if(shuffledSongs.isNotEmpty()) {
                addTracksSaveTrackOrder(
                    mediaItems = shuffledSongs,
                    clearOriginalSongList = false,
                    startingSongPosition = 0,
                    clearCurrentSongs = true,
                    shouldAddToOriginalList = false
                )
            }
        }
    }

    /**
     * Replaces the current queue with [originalSongOrder] starting at position 0, effectively
     * reversing the most recent shuffle.
     *
     * Uses `shouldAddToOriginalList = false` inside [addTracksSaveTrackOrder] so that the
     * original order list is not modified during the restore.
     */
    private fun unshuffleSongs() {
        Timber.d("unshuffleSongs: ")
        val originalSongs = _originalSongOrder.value
        if (_mediaController.value != null && originalSongs.isNotEmpty()) {
            Timber.d("restoreOriginalSongOrder: originalSongs.size=${originalSongs.size}, originalSongs=${originalSongs.map { it.mediaMetadata.title }}")
            addTracksSaveTrackOrder(
                mediaItems = originalSongs,
                clearOriginalSongList = false,
                startingSongPosition = 0,
                clearCurrentSongs = true,
                shouldAddToOriginalList = false
            )
        }
    }

    /**
     * Clears the current queue and starts playing the chosen album.
     */
    fun playAlbum(album: MediaItem) { //TODO I don't think this is working...
        Timber.d("playAlbum: album=$album")
        querySongsFromAlbum(
            album,
            queueAddType = QueueAddType.QUEUE_CLEAR_ADD
        )
    }

    /**
     * Adds to album to the back of the current queue.
     */
    fun addAlbumToBackOfQueue(album: MediaItem) {
        Timber.d("addAlbumToBackOfQueue: album=$album")
        querySongsFromAlbum(
            album,
            queueAddType = QueueAddType.QUEUE_END_ADD
        )
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on a playlist.
     * @param albumId The title of an playlist to be queried.
     */
    fun querySongsFromPlaylist(playlist: MediaItem) {
        Timber.d("querySongsFromPlaylist: playlistId=${playlist.mediaMetadata.albumTitle}")
        viewModelScope.launch(Dispatchers.IO) {
            val playlistSongs = musicRepo.getSongsFromPlaylist(playlist.mediaMetadata.albumTitle.toString())
            val songGroupType = SongGroupType.PLAYLIST
            _currentSongGroup.value = SongGroup(
                songGroupType,
                playlistSongs,
                playlist
            )
        }
    }

    /**
     * Remove a list of of playlists
     * @param playlists A list of the playlist titles to be removed.
     */
    fun removePlaylists(playlists: List<String>) {
        Timber.d("removePlaylists: playlist=$playlists")
        playlists.forEach { playlistTitle ->
            removePlaylist(playlistTitle)
        }
    }

    /**
     * Removes a single playlist based on its title.
     */
    private fun removePlaylist(playlistTitle: String) {
        Timber.d("removePlaylist: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlayerDatabase.getDatabase(getApplication<Application>().applicationContext)
                .songGroupDao()
                .findSongGroupByName(playlistTitle)

            if(playlist != null) {
                PlayerDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .songGroupDao()
                    .deleteSongGroups(playlist)

                //remove associated image
                deletePicture(getApplication<Application>().applicationContext, "$playlistTitle.jpg")
            }
        }
    }

    /**
     * Checks whether `READ_MEDIA_AUDIO` is currently granted and updates [isAudioPermissionGranted].
     *
     * Only updates the LiveData when the state has actually changed to avoid redundant observer
     * callbacks. Called at startup and after returning from the permission denied screen.
     */
    private fun checkPermissions() {
        val isAudioPermissionGranted = permissionManager.verifyReadMediaAudioPermission(getApplication<Application>().applicationContext)
        Timber.d("checkPermissions: isAudioPermissionGranted=$isAudioPermissionGranted")
        if (_isAudioPermissionGranted.value != isAudioPermissionGranted) {
            _isAudioPermissionGranted.value = isAudioPermissionGranted
            // When returning from system settings with permission now granted, navigate home
            if (isAudioPermissionGranted && currentScreenType == ScreenType.PERMISSION_DENIED_SCREEN) {
                setScreenData(ScreenType.MUSIC_CHOOSER_SCREEN)
            }
        }
    }

    /**
     * Based on results from asking user for permission, determine how to proceed. The app requires
     * that read media audio permission is granted for functionality.
     * If permission is not granted, send the user to a permission denied screen.
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.d("handlePermissionResult: requestCode=$requestCode, permissions=$permissions, grantResults=$grantResults")
        if(requestCode == AppPermissionUtil.readMediaAudioRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d("handlePermissionResult: read audio granted!")
                _isAudioPermissionGranted.value = true
            } else {
                Timber.d("handlePermissionResult: read audio NOT granted!")
                setScreenData(ScreenType.PERMISSION_DENIED_SCREEN)
                _showLoadingScreen.value = false
            }
        } else if(requestCode == AppPermissionUtil.readExternalStorageCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d("handlePermissionResult: read audio granted!")
                _isAudioPermissionGranted.value = true
            } else {
                Timber.d("handlePermissionResult: read audio NOT granted!")
                setScreenData(ScreenType.PERMISSION_DENIED_SCREEN)
                _showLoadingScreen.value = false
            }
        }
    }
}