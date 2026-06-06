package com.andaagii.tacomamusicplayer.screen

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
import androidx.compose.foundation.lazy.grid.GridCells.Fixed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.composables.InputTextPrompt
import com.andaagii.tacomamusicplayer.composables.PlaylistGridItem
import com.andaagii.tacomamusicplayer.composables.PlaylistListItem
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl

/**
 * Full-screen playlists browsing page.
 *
 * Stateful — manages the sort-options dropdown, the per-row overflow menu, and two
 * [com.andaagii.tacomamusicplayer.composables.InputTextPrompt] overlays for creating and renaming playlists. Displays playlists in either
 * a [LazyColumn] (list) or [LazyVerticalGrid] (grid) depending on [layoutType]. Sorting is
 * applied locally via [SortingUtil.sortPlaylists] for instant UI updates.
 *
 * The rename flow is handled entirely inside this composable: tapping "Rename Playlist" in the
 * per-row menu sets [isShowingRenamePrompt] and captures [renameTargetTitle], then the overlay
 * calls [onRenamePlaylist] when the user confirms.
 *
 * Replaces `PlaylistFragment` + `PlaylistAdapter` + `PlaylistGridAdapter` + `fragment_playlist.xml`.
 *
 * @param modifier Modifier applied to the root [Box].
 * @param playlists All available playlists from the repository, unsorted.
 * @param layoutType Whether to render a linear list or a two-column grid.
 * @param sorting The active sort order applied to [playlists] before display.
 * @param onPlaylistClick Called with the tapped [MediaItem] to drill into the playlist's track list.
 * @param onPlayClick Called with the playlist title to start playback immediately.
 * @param onAddToQueue Called with the playlist title to append it to the end of the current queue.
 * @param onAddPlaylistImage Called with the playlist title to launch the image-picker flow
 *   via [rememberLauncherForActivityResult] in [MusicChooserScreen].
 * @param onDeletePlaylist Called with the playlist title to permanently remove it.
 * @param onCreatePlaylist Called with the new playlist name when the user confirms the create prompt.
 * @param onRenamePlaylist Called with the old title and the new title when the user confirms
 *   the rename prompt.
 * @param onLayoutToggle Called when the user taps the layout-toggle icon in the toolbar.
 * @param onSortingSelected Called with the chosen [SortingUtil.SortingOption] when the user
 *   picks a sort order from the settings dropdown.
 */
@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    playlists: List<MediaItem>,
    layoutType: LayoutType,
    sorting: SortingUtil.SortingOption,
    onPlaylistClick: (MediaItem) -> Unit,
    onPlayClick: (playlistTitle: String) -> Unit,
    onAddToQueue: (playlistTitle: String) -> Unit,
    onAddPlaylistImage: (playlistTitle: String) -> Unit,
    onDeletePlaylist: (playlistTitle: String) -> Unit,
    onCreatePlaylist: (name: String) -> Unit,
    onRenamePlaylist: (oldTitle: String, newTitle: String) -> Unit,
    onLayoutToggle: () -> Unit,
    onSortingSelected: (SortingUtil.SortingOption) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    // mediaId of the playlist whose overflow menu is currently open; null = none
    var menuOpenPlaylistId by remember { mutableStateOf<String?>(null) }
    var isShowingCreatePrompt by remember { mutableStateOf(false) }
    var isShowingRenamePrompt by remember { mutableStateOf(false) }
    // Captured when the user taps "Rename Playlist" and read when they confirm the prompt
    var renameTargetTitle by remember { mutableStateOf("") }
    val sortedPlaylists = remember(playlists, sorting) { SortingUtil.sortPlaylists(playlists, sorting) }

    // Icon flips to indicate the layout the user will switch TO
    val layoutToggleIcon = if (layoutType == LayoutType.LINEAR_LAYOUT)
        R.drawable.baseline_grid_view_24
    else
        R.drawable.baseline_table_rows_24

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
                    text = stringResource(R.string.playlists),
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isShowingCreatePrompt = true }) {
                    Icon(
                        painter = painterResource(R.drawable.add_icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(onClick = onLayoutToggle) {
                    Icon(
                        painter = painterResource(layoutToggleIcon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                // Box anchors the sort DropdownMenu below the settings icon
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_settings_24),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_by_title_alphabetical)) },
                            onClick = {
                                onSortingSelected(SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_by_creation_date)) },
                            onClick = {
                                onSortingSelected(SortingUtil.SortingOption.SORTING_BY_CREATION_DATE)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_by_recently_modified)) },
                            onClick = {
                                onSortingSelected(SortingUtil.SortingOption.SORTING_BY_MODIFICATION_DATE)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────────────
            if (sortedPlaylists.isEmpty()) {
                InformationScreen(
                    firstIcon = painterResource(R.drawable.playlist_icon),
                    firstInfo = stringResource(R.string.choose_a_playlist_to_view),
                    onFirstClick = {},
                    secondIcon = painterResource(R.drawable.add_icon),
                    secondInfo = stringResource(R.string.add_playlist),
                    onSecondClick = { isShowingCreatePrompt = true }
                )
            } else when (layoutType) {
                LayoutType.LINEAR_LAYOUT -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(sortedPlaylists, key = { it.mediaId }) { playlist ->
                            val meta = playlist.mediaMetadata
                            val title = meta.albumTitle?.toString().orEmpty()
                            PlaylistListItem(
                                playlistName = title,
                                // Track count and total duration are not yet available in MediaItem
                                // metadata — these fields will be populated in a future task.
                                tracksInfo = "",
                                totalDuration = "",
                                artworkUri = meta.artworkUri,
                                onPlaylistClick = { onPlaylistClick(playlist) },
                                onPlayClick = { onPlayClick(title) },
                                onMenuClick = { menuOpenPlaylistId = playlist.mediaId },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            DropdownMenu(
                                expanded = menuOpenPlaylistId == playlist.mediaId,
                                onDismissRequest = { menuOpenPlaylistId = null }
                            ) {
                                PlaylistMenuItems(
                                    playlistTitle = title,
                                    onPlayClick = onPlayClick,
                                    onAddToQueue = onAddToQueue,
                                    onAddPlaylistImage = onAddPlaylistImage,
                                    onDeletePlaylist = onDeletePlaylist,
                                    onShowRename = {
                                        renameTargetTitle = title
                                        isShowingRenamePrompt = true
                                    },
                                    onDismiss = { menuOpenPlaylistId = null }
                                )
                            }
                        }
                    }
                }
                LayoutType.TWO_GRID_LAYOUT -> {
                    LazyVerticalGrid(
                        columns = Fixed(UtilImpl.determineGridSize()),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedPlaylists, key = { it.mediaId }) { playlist ->
                            val meta = playlist.mediaMetadata
                            val title = meta.albumTitle?.toString().orEmpty()
                            Box {
                                PlaylistGridItem(
                                    playlistName = title,
                                    // Track/duration info not yet available in MediaItem metadata
                                    description = "",
                                    artworkUri = meta.artworkUri,
                                    onPlaylistClick = { onPlaylistClick(playlist) },
                                    onLongClick = { menuOpenPlaylistId = playlist.mediaId }
                                )
                                DropdownMenu(
                                    expanded = menuOpenPlaylistId == playlist.mediaId,
                                    onDismissRequest = { menuOpenPlaylistId = null }
                                ) {
                                    PlaylistMenuItems(
                                        playlistTitle = title,
                                        onPlayClick = onPlayClick,
                                        onAddToQueue = onAddToQueue,
                                        onAddPlaylistImage = onAddPlaylistImage,
                                        onDeletePlaylist = onDeletePlaylist,
                                        onShowRename = {
                                            renameTargetTitle = title
                                            isShowingRenamePrompt = true
                                        },
                                        onDismiss = { menuOpenPlaylistId = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Overlays ──────────────────────────────────────────────────────────────────
        // InputTextPrompt owns its text state via plain `remember`, so the field clears
        // automatically each time the overlay leaves and re-enters the composition.
        if (isShowingCreatePrompt) {
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
                    onOption1Click = { _ -> isShowingCreatePrompt = false },
                    onOption2Click = { name ->
                        onCreatePlaylist(name)
                        isShowingCreatePrompt = false
                    }
                )
            }
        }

        if (isShowingRenamePrompt) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                InputTextPrompt(
                    hint = Const.RENAME_PLAYLIST_HINT,
                    option1Text = Const.CANCEL,
                    option2Text = Const.UPDATE,
                    onOption1Click = { _ -> isShowingRenamePrompt = false },
                    onOption2Click = { newName ->
                        onRenamePlaylist(renameTargetTitle, newName)
                        isShowingRenamePrompt = false
                    }
                )
            }
        }
    }
}

/**
 * The five playlist overflow menu items shared between the list and grid layouts.
 *
 * Rename is handled by calling [onShowRename] rather than bubbling up as a typed callback —
 * the overlay state lives inside [PlaylistScreen] itself.
 *
 * @param playlistTitle The title of the playlist this menu is anchored to.
 * @param onPlayClick Forwarded from [PlaylistScreen]; starts playback of the playlist.
 * @param onAddToQueue Forwarded from [PlaylistScreen]; appends the playlist to the queue.
 * @param onAddPlaylistImage Forwarded from [PlaylistScreen]; launches the image picker.
 * @param onDeletePlaylist Forwarded from [PlaylistScreen]; permanently removes the playlist.
 * @param onShowRename Called to open the rename [InputTextPrompt] overlay for this playlist.
 * @param onDismiss Called after any item is tapped so the caller can close the menu.
 */
@Composable
private fun PlaylistMenuItems(
    playlistTitle: String,
    onPlayClick: (String) -> Unit,
    onAddToQueue: (String) -> Unit,
    onAddPlaylistImage: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onShowRename: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.play_playlist)) },
        onClick = {
            onPlayClick(playlistTitle)
            onDismiss()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.add_to_queue)) },
        onClick = {
            onAddToQueue(playlistTitle)
            onDismiss()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.rename_playlist)) },
        onClick = {
            onShowRename()
            onDismiss()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.add_playlist_image)) },
        onClick = {
            onAddPlaylistImage(playlistTitle)
            onDismiss()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.remove_playlist)) },
        onClick = {
            onDeletePlaylist(playlistTitle)
            onDismiss()
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlaylistScreenLinearPreview() {
    val fakePlaylist = MediaItem.Builder()
        .setMediaId("1")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setAlbumTitle("Late Night Chill")
                .setAlbumArtist("Various Artists")
                .build()
        )
        .build()
    PlaylistScreen(
        playlists = listOf(fakePlaylist, fakePlaylist.buildUpon().setMediaId("2").build()),
        layoutType = LayoutType.LINEAR_LAYOUT,
        sorting = SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL,
        onPlaylistClick = {},
        onPlayClick = {},
        onAddToQueue = {},
        onAddPlaylistImage = {},
        onDeletePlaylist = {},
        onCreatePlaylist = {},
        onRenamePlaylist = { _, _ -> },
        onLayoutToggle = {},
        onSortingSelected = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlaylistScreenGridPreview() {
    val fakePlaylist = MediaItem.Builder()
        .setMediaId("1")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setAlbumTitle("Road Trip")
                .build()
        )
        .build()
    PlaylistScreen(
        playlists = listOf(fakePlaylist, fakePlaylist.buildUpon().setMediaId("2").build()),
        layoutType = LayoutType.TWO_GRID_LAYOUT,
        sorting = SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL,
        onPlaylistClick = {},
        onPlayClick = {},
        onAddToQueue = {},
        onAddPlaylistImage = {},
        onDeletePlaylist = {},
        onCreatePlaylist = {},
        onRenamePlaylist = { _, _ -> },
        onLayoutToggle = {},
        onSortingSelected = {}
    )
}
