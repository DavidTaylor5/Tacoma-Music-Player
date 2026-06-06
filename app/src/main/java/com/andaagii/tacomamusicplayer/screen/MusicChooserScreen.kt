package com.andaagii.tacomamusicplayer.screen

import android.app.Activity.RESULT_OK
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.composables.MiniPlayer
import com.andaagii.tacomamusicplayer.composables.NavigationControl
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.AlbumTabViewModel
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.viewmodel.PlaylistTabViewModel
import com.andaagii.tacomamusicplayer.viewmodel.SongListViewModel
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full-screen container that hosts the five-page [HorizontalPager] and the [com.andaagii.tacomamusicplayer.composables.NavigationControl]
 * tab bar. Replaces [com.andaagii.tacomamusicplayer.fragment.PlayerDisplayFragment] and
 * [com.andaagii.tacomamusicplayer.adapter.ScreenSlidePagerAdapter].
 *
 * Stateful — receives the three Activity-scoped ViewModels from [com.andaagii.tacomamusicplayer.composables.TacomaMusicPlayerApp] so
 * that it shares the same [MainViewModel] instance that owns the [MediaController]. Page
 * navigation events from [MainViewModel.navigateToPage] are consumed via [LaunchedEffect] to
 * call [androidx.compose.foundation.pager.PagerState.animateScrollToPage]. Album and playlist
 * image-picker flows are handled via [rememberLauncherForActivityResult] so that no Fragment
 * lifecycle owner is needed.
 *
 * The [HorizontalPager] keeps all five pages alive simultaneously via [beyondViewportPageCount]
 * = 4, matching the old `ViewPager2.offscreenPageLimit = 4` behaviour.
 *
 * @param mainViewModel Activity-scoped [MainViewModel]; owns the [MediaController] and all
 *   playback state.
 * @param albumViewModel Activity-scoped [AlbumTabViewModel]; provides album list and layout prefs.
 * @param playlistViewModel Activity-scoped [PlaylistTabViewModel]; provides playlist list and
 *   layout prefs.
 */
@OptIn(UnstableApi::class)
@Composable
fun MusicChooserScreen(
    mainViewModel: MainViewModel,
    albumViewModel: AlbumTabViewModel,
    playlistViewModel: PlaylistTabViewModel,
) {
    val songListViewModel: SongListViewModel = viewModel()

    val context = LocalContext.current

    // ── State collection ─────────────────────────────────────────────────────

    val songInfo       by mainViewModel.currentPlayingSongInfo.collectAsStateWithLifecycle()
    val isPlaying      by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val loopMode       by mainViewModel.loopMode.collectAsStateWithLifecycle()
    val shuffleMode    by mainViewModel.shuffleMode.collectAsStateWithLifecycle()
    val controller     by mainViewModel.mediaController.collectAsStateWithLifecycle()
    val queueSongs     by mainViewModel.currentlyPlayingSongs.collectAsStateWithLifecycle()
    val songGroup      by mainViewModel.currentSongGroup.collectAsStateWithLifecycle()
    val searchResults  by mainViewModel.currentSearchList.collectAsStateWithLifecycle()
    val isSearchMode   by mainViewModel.isShowingSearchMode.collectAsStateWithLifecycle()
    val allPlaylists   by mainViewModel.availablePlaylists.collectAsStateWithLifecycle()
    val selectedSongs  by songListViewModel.currentlySelectedSongs.collectAsStateWithLifecycle()

    val albumState     by albumViewModel.albumTabState.collectAsStateWithLifecycle()
    val playlistState  by playlistViewModel.playlistTabState.collectAsStateWithLifecycle()

    // ── Pager ─────────────────────────────────────────────────────────────────

    val pagerState = rememberPagerState(initialPage = PageType.PLAYER_PAGE.ordinal) { PageType.entries.size }
    val scope = rememberCoroutineScope()

    // Consume one-shot page navigation events from the ViewModel.
    LaunchedEffect(Unit) {
        mainViewModel.navigateToPage.collect { page ->
            pagerState.animateScrollToPage(page.type())
        }
    }

    val currentPage = PageType.determinePageFromPosition(pagerState.currentPage)

    LaunchedEffect(pagerState.currentPage) {
        mainViewModel.observeCurrentPage(currentPage)
    }

    // Clear multi-select state when the user swipes away from the song page.
    DisposableEffect(currentPage) {
        onDispose {
            if (currentPage == PageType.SONG_PAGE) {
                songListViewModel.clearMultiSelectSongs()
            }
        }
    }

    // ── Image-picker launchers ────────────────────────────────────────────────
    // These replace the registerForActivityResult calls in AlbumListFragment and PlaylistFragment.

    var selectedAlbumName by remember { mutableStateOf("") }
    var selectedPlaylistName by remember { mutableStateOf("") }

    val albumCropLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                UCrop.getOutput(data)?.path?.let { path ->
                    mainViewModel.updateSongGroupImage(
                        title = selectedAlbumName,
                        artFileName = path,
                        updateSongs = true
                    )
                }
            }
        }
    }

    val albumPickerLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val saveUri = UtilImpl.getSaveFileUri(context, selectedAlbumName, isCustom = true)
        albumCropLauncher.launch(
            UCrop.of(uri, saveUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(700, 700)
                .getIntent(context)
        )
    }

    val playlistCropLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                UCrop.getOutput(data)?.path?.let { path ->
                    mainViewModel.updateSongGroupImage(
                        title = selectedPlaylistName,
                        artFileName = path
                    )
                }
            }
        }
    }

    val playlistPickerLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val saveUri = UtilImpl.getSaveFileUri(context, selectedPlaylistName, isCustom = true)
        playlistCropLauncher.launch(
            UCrop.of(uri, saveUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(700, 700)
                .getIntent(context)
        )
    }

    // ── Derived state for SongListScreen ──────────────────────────────────────

    val playlists = allPlaylists.filter {
        it.mediaMetadata.albumTitle != Const.PLAYLIST_QUEUE_TITLE &&
            it.mediaMetadata.albumTitle != Const.ORIGINAL_QUEUE_ORDER
    }

    val displaySongs = if (isSearchMode) searchResults.orEmpty() else songGroup?.songs.orEmpty()
    val displayType: SongGroupType = if (isSearchMode) SongGroupType.SEARCH_LIST else (songGroup?.type ?: SongGroupType.ALBUM)
    val showHeader = !isSearchMode && songGroup != null && displaySongs.isNotEmpty()

    val artFile = songGroup?.group?.mediaMetadata?.artworkUri?.toString()?.let { File(it) }
    val artUri: Uri? = if (artFile?.exists() == true) songGroup?.group?.mediaMetadata?.artworkUri else null
    val groupTitle: String = songGroup?.group?.mediaMetadata?.albumTitle?.toString().orEmpty()

    // ── Mini-player visibility ────────────────────────────────────────────────

    val showMiniPlayer = currentPage != PageType.PLAYER_PAGE
        && songInfo != null && !SongData.isNullSong(songInfo)

    // ── Layout ────────────────────────────────────────────────────────────────

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = PageType.entries.size - 1,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            when (PageType.determinePageFromPosition(pageIndex)) {
                PageType.QUEUE_PAGE -> CurrentQueueScreen(
                    songs = queueSongs,
                    currentlyPlayingTitle = songInfo?.songTitle,
                    onSongClick = { pos ->
                        controller?.seekTo(pos, 0L)
                        controller?.play()
                    },
                    onRemoveSong = { pos -> controller?.removeMediaItem(pos) },
                    onMoveItem = { from, to -> controller?.moveMediaItem(from, to) },
                    onClearQueue = mainViewModel::clearQueue
                )

                PageType.PLAYER_PAGE -> MusicPlayingScreen(
                    songInfo = songInfo,
                    isPlaying = isPlaying,
                    loopMode = loopMode,
                    shuffleMode = shuffleMode,
                    mediaController = controller,
                    onPreviousSong = { controller?.seekToPrevious() },
                    onSeekBack = { controller?.seekBack() },
                    onTogglePlay = mainViewModel::flipPlayingState,
                    onSeekForward = { controller?.seekForward() },
                    onNextSong = { controller?.seekToNextMediaItem() },
                    onFlipLoopMode = mainViewModel::flipLoopMode,
                    onFlipShuffleState = mainViewModel::flipShuffleState
                )

                PageType.PLAYLIST_PAGE -> PlaylistScreen(
                    playlists = playlistState.playlists,
                    layoutType = playlistState.layout,
                    sorting = playlistState.sorting,
                    onPlaylistClick = { playlist ->
                        mainViewModel.querySongsFromPlaylist(playlist)
                        mainViewModel.setPage(PageType.SONG_PAGE)
                    },
                    onPlayClick = mainViewModel::playPlaylist,
                    onAddToQueue = mainViewModel::addPlaylistToBackOfQueue,
                    onAddPlaylistImage = { title ->
                        selectedPlaylistName = title
                        playlistPickerLauncher.launch("image/*")
                    },
                    onDeletePlaylist = { title -> mainViewModel.removePlaylists(listOf(title)) },
                    onCreatePlaylist = mainViewModel::createNamedPlaylist,
                    onRenamePlaylist = { old, new -> mainViewModel.updatePlaylistTitle(old, new) },
                    onLayoutToggle = {
                        val next = if (playlistState.layout == LayoutType.LINEAR_LAYOUT)
                            LayoutType.TWO_GRID_LAYOUT
                        else
                            LayoutType.LINEAR_LAYOUT
                        playlistViewModel.savePlaylistLayout(context, next)
                    },
                    onSortingSelected = { option ->
                        playlistViewModel.savePlaylistSorting(context, option)
                    }
                )

                PageType.ALBUM_PAGE -> AlbumListScreen(
                    albums = albumState.albums,
                    layoutType = albumState.layout,
                    sorting = albumState.sorting,
                    onAlbumClick = { album ->
                        mainViewModel.querySongsFromAlbum(album)
                        mainViewModel.setPage(PageType.SONG_PAGE)
                    },
                    onPlayClick = mainViewModel::playAlbum,
                    onMenuOption = { option, album, customImageName ->
                        when (option) {
                            MenuOptionUtil.MenuOption.PLAY_ALBUM ->
                                mainViewModel.playAlbum(album)
                            MenuOptionUtil.MenuOption.ADD_TO_QUEUE ->
                                mainViewModel.addAlbumToBackOfQueue(album)
                            MenuOptionUtil.MenuOption.ADD_ALBUM_IMAGE -> {
                                customImageName?.let {
                                    selectedAlbumName = album.mediaMetadata.albumTitle.toString()
                                    albumPickerLauncher.launch("image/*")
                                }
                            }
                            else -> Unit
                        }
                    },
                    onLayoutToggle = {
                        val next = if (albumState.layout == LayoutType.LINEAR_LAYOUT)
                            LayoutType.TWO_GRID_LAYOUT
                        else
                            LayoutType.LINEAR_LAYOUT
                        albumViewModel.saveAlbumLayout(context, next)
                    },
                    onSortingSelected = { option ->
                        albumViewModel.saveAlbumSorting(context, option)
                    }
                )

                PageType.SONG_PAGE -> SongListScreen(
                    songs = displaySongs,
                    songGroupType = displayType,
                    showHeader = showHeader,
                    songGroupArtUri = artUri,
                    songGroupTitle = groupTitle,
                    isShowingSearchMode = isSearchMode,
                    selectedSongs = selectedSongs,
                    availablePlaylists = playlists,
                    onSongClick = { pos ->
                        songGroup?.let { mainViewModel.playSongGroupAtPosition(it, pos) }
                        mainViewModel.removeVirtualKeyboard()
                    },
                    onArtworkClick = { song, isNowSelected ->
                        if (isNowSelected) songListViewModel.selectSongs(listOf(song), showPrompt = true)
                        else songListViewModel.unselectSong(song)
                    },
                    onAddToQueue = { songs -> mainViewModel.addSongsToEndOfQueue(songs) },
                    onRemoveFromPlaylist = { songs ->
                        mainViewModel.removeSongsFromCurrentPlaylist(songs)
                        songListViewModel.clearMultiSelectSongs()
                    },
                    onHeaderPlayClick = {
                        songGroup?.let { mainViewModel.playSongGroupAtPosition(it, 0) }
                    },
                    onSearchQueryChanged = { query -> mainViewModel.querySearchData(query) },
                    onToggleSearch = { mainViewModel.flipSearchButtonState() },
                    onCancelSearch = {
                        mainViewModel.handleCancelSearchButtonClick()
                        mainViewModel.removeVirtualKeyboard()
                    },
                    onMovePlaylistItem = { from, to ->
                        songGroup?.let { sg ->
                            val newSongs = sg.songs.toMutableList().apply { add(to, removeAt(from)) }
                            sg.songs = newSongs
                            mainViewModel.updatePlaylistOrder(sg)
                        }
                    },
                    onDismissMultiSelect = { songListViewModel.clearMultiSelectSongs() },
                    onConfirmAddToPlaylists = { names, songs ->
                        mainViewModel.addSongsToAPlaylist(names, songs)
                        songListViewModel.clearMultiSelectSongs()
                    },
                    onCreatePlaylist = { name -> mainViewModel.createNamedPlaylist(name) },
                    onSearchAlbumClick = { album ->
                        mainViewModel.querySongsFromAlbum(album)
                        mainViewModel.removeVirtualKeyboard()
                        mainViewModel.handleCancelSearchButtonClick()
                    },
                    onSearchPlaylistClick = { playlist ->
                        mainViewModel.querySongsFromPlaylist(playlist)
                        mainViewModel.removeVirtualKeyboard()
                        mainViewModel.handleCancelSearchButtonClick()
                    },
                    onSearchSongClick = { song ->
                        mainViewModel.playAlbumAtSongPosition(song)
                        mainViewModel.removeVirtualKeyboard()
                    },
                )
            }
        }

        if (showMiniPlayer && songInfo != null) {
            MiniPlayer(
                songInfo = songInfo!!,
                isPlaying = isPlaying,
                onNavigateToPlayer = {
                    scope.launch { pagerState.animateScrollToPage(PageType.PLAYER_PAGE.type()) }
                },
                onPreviousSong = { controller?.seekToPrevious() },
                onTogglePlay = mainViewModel::flipPlayingState,
                onNextSong = { controller?.seekToNextMediaItem() }
            )
        }

        NavigationControl(
            currentPage = currentPage,
            queueIconRes = R.drawable.queue_icon,
            playerIconRes = R.drawable.play_circle_outline,
            playlistIconRes = R.drawable.playlist_icon,
            albumIconRes = R.drawable.browse_album_icon,
            songIconRes = R.drawable.album_icon,
            onPageSelected = { page -> mainViewModel.setPage(page) }
        )

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
