package com.andaagii.tacomamusicplayer.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.composables.InputTextPrompt
import com.andaagii.tacomamusicplayer.composables.MultiSelectPrompt
import com.andaagii.tacomamusicplayer.composables.PlaylistPrompt
import com.andaagii.tacomamusicplayer.composables.SongGroupInfoView
import com.andaagii.tacomamusicplayer.composables.SongItem
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Sentinel index used to distinguish the header [DropdownMenu] from per-row menus. */
private const val HEADER_MENU_INDEX = -2

/**
 * Full-screen song list page that handles album browsing, playlist browsing, and inline search.
 *
 * Stateful — manages per-row and header menu state, playlist-prompt visibility, the
 * create-playlist dialog, search query text, and a local drag-reorder shadow list for
 * immediate visual feedback. Drag-to-reorder is enabled only when [songGroupType] is
 * [SongGroupType.PLAYLIST]; in other modes the drag handle is hidden.
 *
 * Replaces `SongListFragment` + `SongListAdapter` + `fragment_songlist.xml`.
 *
 * @param modifier Modifier applied to the root [Box].
 * @param songs Already-resolved list of [MediaItem]s to display. The caller maps ViewModel
 *   state to this list: search results when search mode is active, otherwise the group songs.
 * @param songGroupType Display mode — [SongGroupType.ALBUM], [SongGroupType.PLAYLIST], or
 *   [SongGroupType.SEARCH_LIST]. Controls drag handles, per-row menu options, and click routing.
 * @param showHeader Whether to show the [com.andaagii.tacomamusicplayer.composables.SongGroupInfoView] header above the list. Callers
 *   hide it when search mode is active or when there are no songs.
 * @param songGroupArtUri Artwork URI for the header [com.andaagii.tacomamusicplayer.composables.SongGroupInfoView], or `null` for the
 *   default placeholder.
 * @param songGroupTitle Title displayed in the [com.andaagii.tacomamusicplayer.composables.SongGroupInfoView] header.
 * @param isShowingSearchMode Whether the inline search bar is active.
 * @param selectedSongs Songs currently selected via artwork-tap multi-select.
 * @param availablePlaylists All known playlists, pre-filtered by the caller to exclude internal
 *   queue playlists. Passed to [com.andaagii.tacomamusicplayer.composables.PlaylistPrompt].
 * @param onSongClick Called with the row index to play a song (non-search mode).
 * @param onArtworkClick Called with the [MediaItem] and the new selection state when the user
 *   taps the artwork area to toggle multi-select.
 * @param onAddToQueue Called with a list of songs to append to the playback queue.
 * @param onRemoveFromPlaylist Called with the songs to remove from the current playlist.
 * @param onHeaderPlayClick Called when the user taps the play button in [com.andaagii.tacomamusicplayer.composables.SongGroupInfoView].
 * @param onSearchQueryChanged Called with the current query string as the user types.
 * @param onToggleSearch Called when the user taps the search icon to enter or exit search mode.
 * @param onCancelSearch Called when the user taps Cancel in the search bar.
 * @param onMovePlaylistItem Called with `(from, to)` when a drag completes, so the caller
 *   can persist the new order.
 * @param onDismissMultiSelect Called when the user taps the close icon on [com.andaagii.tacomamusicplayer.composables.MultiSelectPrompt].
 * @param onConfirmAddToPlaylists Called with the selected playlist titles and the songs to add
 *   when the user confirms [com.andaagii.tacomamusicplayer.composables.PlaylistPrompt].
 * @param onCreatePlaylist Called with the new playlist name when the user confirms the create
 *   playlist dialog.
 * @param onSearchAlbumClick Called with the album [MediaItem] when the user taps an album
 *   search result.
 * @param onSearchPlaylistClick Called with the playlist [MediaItem] when the user taps a
 *   playlist search result.
 * @param onSearchSongClick Called with the song [MediaItem] when the user taps a song in
 *   search results.
 */
@Composable
fun SongListScreen(
    modifier: Modifier = Modifier,
    songs: List<MediaItem>,
    songGroupType: SongGroupType,
    showHeader: Boolean,
    songGroupArtUri: Uri?,
    songGroupTitle: String,
    isShowingSearchMode: Boolean,
    selectedSongs: List<MediaItem>,
    availablePlaylists: List<MediaItem>,
    onSongClick: (position: Int) -> Unit,
    onArtworkClick: (song: MediaItem, isNowSelected: Boolean) -> Unit,
    onAddToQueue: (songs: List<MediaItem>) -> Unit,
    onRemoveFromPlaylist: (songs: List<MediaItem>) -> Unit,
    onHeaderPlayClick: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onCancelSearch: () -> Unit,
    onMovePlaylistItem: (from: Int, to: Int) -> Unit,
    onDismissMultiSelect: () -> Unit,
    onConfirmAddToPlaylists: (playlistNames: List<String>, songs: List<MediaItem>) -> Unit,
    onCreatePlaylist: (name: String) -> Unit,
    onSearchAlbumClick: (MediaItem) -> Unit,
    onSearchPlaylistClick: (MediaItem) -> Unit,
    onSearchSongClick: (MediaItem) -> Unit,
) {
    // -1 = no row menu open; HEADER_MENU_INDEX = header menu open; ≥0 = that row's menu
    var menuOpenIndex by remember { mutableIntStateOf(-1) }

    var isShowingCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Songs staged for PlaylistPrompt; non-empty means the prompt should be shown
    var songsForPlaylistPrompt by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val isShowingPlaylistPrompt = songsForPlaylistPrompt.isNotEmpty()

    // Local shadow list gives immediate visual feedback during playlist drag
    var localSongs by remember(songs) { mutableStateOf(songs) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }

    // Search query is local; callers receive the string via onSearchQueryChanged
    var searchQuery by rememberSaveable(isShowingSearchMode) { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        if (isShowingSearchMode) onSearchQueryChanged(searchQuery)
    }

    val listState = rememberLazyListState()
    val isDraggable = songGroupType == SongGroupType.PLAYLIST
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            localSongs = localSongs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    )
    val displayList = if (isDraggable) localSongs else songs

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Toolbar row ──────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.songs),
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        painter = painterResource(
                            if (isShowingSearchMode) R.drawable.baseline_search_off_24
                            else R.drawable.baseline_search_24
                        ),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // ── Header: search bar or SongGroupInfoView ───────────────────────────────
            if (isShowingSearchMode) {
                SongListSearchBar(
                    query = searchQuery,
                    onQueryChanged = { searchQuery = it },
                    onCancel = {
                        searchQuery = ""
                        onCancelSearch()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            } else if (showHeader) {
                SongGroupInfoView(
                    imageUri = songGroupArtUri,
                    title = songGroupTitle,
                    onPlayClick = onHeaderPlayClick,
                    onMenuClick = { menuOpenIndex = HEADER_MENU_INDEX },
                    modifier = Modifier.height(80.dp)
                )

                // Header dropdown menu
                DropdownMenu(
                    expanded = menuOpenIndex == HEADER_MENU_INDEX,
                    onDismissRequest = { menuOpenIndex = -1 }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.play_song_group)) },
                        onClick = {
                            onHeaderPlayClick()
                            menuOpenIndex = -1
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_playlist)) },
                        onClick = {
                            songsForPlaylistPrompt = songs
                            menuOpenIndex = -1
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_queue)) },
                        onClick = {
                            onAddToQueue(songs)
                            menuOpenIndex = -1
                        }
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────────────
            if (displayList.isEmpty() && !isShowingSearchMode) {
                InformationScreen(
                    firstIcon = painterResource(R.drawable.playlist_icon),
                    firstInfo = stringResource(R.string.choose_a_playlist_to_view),
                    onFirstClick = {},
                    secondIcon = painterResource(R.drawable.browse_album_icon),
                    secondInfo = stringResource(R.string.choose_an_album_to_view),
                    onSecondClick = {}
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(displayList, key = { _, item -> item.mediaId }) { index, song ->
                        val isSelected = selectedSongs.any { it.mediaId == song.mediaId }
                        if (isDraggable) {
                            ReorderableItem(reorderState, key = song.mediaId) { _ ->
                                SongItemRow(
                                    index = index,
                                    song = song,
                                    songGroupType = songGroupType,
                                    isSelected = isSelected,
                                    dragHandleModifier = Modifier.draggableHandle(
                                        onDragStarted = { dragStartIndex = index },
                                        onDragStopped = {
                                            val finalIndex = localSongs.indexOfFirst { it.mediaId == song.mediaId }
                                            if (dragStartIndex >= 0 && finalIndex >= 0) {
                                                onMovePlaylistItem(dragStartIndex, finalIndex)
                                            }
                                            dragStartIndex = -1
                                        }
                                    ),
                                    onSongClick = { onSongClick(index) },
                                    onArtworkClick = { onArtworkClick(song, !isSelected) },
                                    onAddToQueue = { onAddToQueue(listOf(song)) },
                                    onAddToPlaylist = { songsForPlaylistPrompt = listOf(song) },
                                    onRemoveFromPlaylist = { onRemoveFromPlaylist(listOf(song)) },
                                    onMenuClick = { menuOpenIndex = index },
                                    menuExpanded = menuOpenIndex == index,
                                    onMenuDismiss = { menuOpenIndex = -1 },
                                    onSearchAlbumClick = onSearchAlbumClick,
                                    onSearchPlaylistClick = onSearchPlaylistClick,
                                    onSearchSongClick = onSearchSongClick,
                                )
                            }
                        } else {
                            SongItemRow(
                                index = index,
                                song = song,
                                songGroupType = songGroupType,
                                isSelected = isSelected,
                                dragHandleModifier = Modifier,
                                onSongClick = { onSongClick(index) },
                                onArtworkClick = { onArtworkClick(song, !isSelected) },
                                onAddToQueue = { onAddToQueue(listOf(song)) },
                                onAddToPlaylist = { songsForPlaylistPrompt = listOf(song) },
                                onRemoveFromPlaylist = { onRemoveFromPlaylist(listOf(song)) },
                                onMenuClick = { menuOpenIndex = index },
                                menuExpanded = menuOpenIndex == index,
                                onMenuDismiss = { menuOpenIndex = -1 },
                                onSearchAlbumClick = onSearchAlbumClick,
                                onSearchPlaylistClick = onSearchPlaylistClick,
                                onSearchSongClick = onSearchSongClick,
                            )
                        }
                    }
                }
            }
        }

        // ── Overlays ──────────────────────────────────────────────────────────────────
        if (isShowingPlaylistPrompt) {
            PlaylistPrompt(
                playlists = availablePlaylists,
                onAddClick = { names ->
                    onConfirmAddToPlaylists(names.toList(), songsForPlaylistPrompt)
                    songsForPlaylistPrompt = emptyList()
                },
                onCloseClick = { songsForPlaylistPrompt = emptyList() },
                onCreateNewPlaylistClick = { isShowingCreatePlaylistDialog = true }
            )
        }

        if (isShowingCreatePlaylistDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                InputTextPrompt(
                    hint = Const.NEW_PLAYLIST_HINT,
                    option1Text = Const.CANCEL,
                    option2Text = Const.ADD,
                    onOption1Click = { _ -> isShowingCreatePlaylistDialog = false },
                    onOption2Click = { name ->
                        onCreatePlaylist(name)
                        isShowingCreatePlaylistDialog = false
                    }
                )
            }
        }

        if (selectedSongs.isNotEmpty()) {
            MultiSelectPrompt(
                descriptionText = "${selectedSongs.size} ${stringResource(R.string.songs).lowercase()} selected",
                onMenuIconClick = { songsForPlaylistPrompt = selectedSongs },
                onCloseIconClick = {
                    onDismissMultiSelect()
                    songsForPlaylistPrompt = emptyList()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

/**
 * Inline search bar for the song list toolbar area.
 *
 * Renders a white [BasicTextField] with a cancel button. The clear icon wipes the query
 * text; the cancel button calls [onCancel] so the caller can exit search mode.
 */
@Composable
private fun SongListSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* keyboard dismiss handled by OS */ }),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_for_albums_and_songs),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChanged("") }) {
                Icon(
                    painter = painterResource(R.drawable.baseline_clear_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        IconButton(onClick = onCancel) {
            Icon(
                painter = painterResource(R.drawable.baseline_search_off_24),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Per-row helper that resolves display data, routing logic, and the per-row [DropdownMenu]
 * before delegating to [com.andaagii.tacomamusicplayer.composables.SongItem]. Keeps the [LazyColumn] item block readable.
 */
@Composable
private fun SongItemRow(
    index: Int,
    song: MediaItem,
    songGroupType: SongGroupType,
    isSelected: Boolean,
    dragHandleModifier: Modifier,
    onSongClick: () -> Unit,
    onArtworkClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onSearchAlbumClick: (MediaItem) -> Unit,
    onSearchPlaylistClick: (MediaItem) -> Unit,
    onSearchSongClick: (MediaItem) -> Unit,
) {
    val meta = song.mediaMetadata

    // Search result type detection — browsable items are albums or playlists
    val isPlaylistResult = songGroupType == SongGroupType.SEARCH_LIST &&
        meta.isBrowsable == true &&
        meta.albumArtist?.toString() == Const.USER_PLAYLIST
    val isAlbumResult = songGroupType == SongGroupType.SEARCH_LIST &&
        meta.isBrowsable == true && !isPlaylistResult
    val isSongResult = songGroupType == SongGroupType.SEARCH_LIST && !isPlaylistResult && !isAlbumResult

    // Display fields vary by type; search results show type label instead of duration
    val title = when {
        isPlaylistResult || isAlbumResult -> meta.albumTitle?.toString().orEmpty()
        else -> meta.title?.toString().orEmpty()
    }
    val artist = when {
        isPlaylistResult || isAlbumResult -> meta.albumArtist?.toString().orEmpty()
        else -> meta.artist?.toString().orEmpty()
    }
    val durationLabel = when {
        isPlaylistResult -> stringResource(R.string.playlist)
        isAlbumResult    -> stringResource(R.string.album)
        else             -> meta.description?.toString().orEmpty()
    }

    val rowClickAction: () -> Unit = when {
        isPlaylistResult -> { { onSearchPlaylistClick(song) } }
        isAlbumResult    -> { { onSearchAlbumClick(song) } }
        isSongResult     -> { { onSearchSongClick(song) } }
        else             -> onSongClick
    }

    SongItem(
        title = title,
        artist = artist,
        duration = durationLabel,
        artworkUri = meta.artworkUri,
        showDragHandle = songGroupType == SongGroupType.PLAYLIST,
        isSelected = isSelected,
        onSongClick = rowClickAction,
        onArtworkClick = onArtworkClick,
        onAddClick = onAddToPlaylist,
        onMenuClick = onMenuClick,
        dragHandleModifier = dragHandleModifier,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = onMenuDismiss
    ) {
        if (songGroupType != SongGroupType.SEARCH_LIST || isSongResult) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_to_playlist)) },
                onClick = {
                    onAddToPlaylist()
                    onMenuDismiss()
                }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_to_queue)) },
            onClick = {
                onAddToQueue()
                onMenuDismiss()
            }
        )
        if (songGroupType == SongGroupType.PLAYLIST) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_playlist)) },
                onClick = {
                    onRemoveFromPlaylist()
                    onMenuDismiss()
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SongListScreenAlbumPreview() {
    val fakeSong = MediaItem.Builder()
        .setMediaId("1")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Comfortably Numb")
                .setArtist("Pink Floyd")
                .setDescription("374000")
                .build()
        )
        .build()
    SongListScreen(
        songs = listOf(fakeSong, fakeSong.buildUpon().setMediaId("2").build()),
        songGroupType = SongGroupType.ALBUM,
        showHeader = true,
        songGroupArtUri = null,
        songGroupTitle = "The Wall",
        isShowingSearchMode = false,
        selectedSongs = emptyList(),
        availablePlaylists = emptyList(),
        onSongClick = {},
        onArtworkClick = { _, _ -> },
        onAddToQueue = {},
        onRemoveFromPlaylist = {},
        onHeaderPlayClick = {},
        onSearchQueryChanged = {},
        onToggleSearch = {},
        onCancelSearch = {},
        onMovePlaylistItem = { _, _ -> },
        onDismissMultiSelect = {},
        onConfirmAddToPlaylists = { _, _ -> },
        onCreatePlaylist = {},
        onSearchAlbumClick = {},
        onSearchPlaylistClick = {},
        onSearchSongClick = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SongListScreenEmptyPreview() {
    SongListScreen(
        songs = emptyList(),
        songGroupType = SongGroupType.ALBUM,
        showHeader = false,
        songGroupArtUri = null,
        songGroupTitle = "",
        isShowingSearchMode = false,
        selectedSongs = emptyList(),
        availablePlaylists = emptyList(),
        onSongClick = {},
        onArtworkClick = { _, _ -> },
        onAddToQueue = {},
        onRemoveFromPlaylist = {},
        onHeaderPlayClick = {},
        onSearchQueryChanged = {},
        onToggleSearch = {},
        onCancelSearch = {},
        onMovePlaylistItem = { _, _ -> },
        onDismissMultiSelect = {},
        onConfirmAddToPlaylists = { _, _ -> },
        onCreatePlaylist = {},
        onSearchAlbumClick = {},
        onSearchPlaylistClick = {},
        onSearchSongClick = {},
    )
}
