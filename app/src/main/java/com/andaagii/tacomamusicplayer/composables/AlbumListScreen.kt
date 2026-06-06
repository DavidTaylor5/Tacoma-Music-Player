package com.andaagii.tacomamusicplayer.composables

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
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl

/**
 * Full-screen albums browsing page.
 *
 * Stateful — manages the sort-options dropdown and the per-row overflow menu. Displays albums in
 * either a [LazyColumn] (list) or [LazyVerticalGrid] (grid) depending on [layoutType]. Sorting
 * is applied locally via [SortingUtil.sortAlbums] so the screen reacts instantly to [sorting]
 * changes without a round-trip through DataStore.
 *
 * Replaces `AlbumListFragment` + `AlbumListAdapter` + `AlbumGridAdapter` + `fragment_albumlist.xml`.
 *
 * @param modifier Modifier applied to the root [Box].
 * @param albums All available albums from the repository, unsorted.
 * @param layoutType Whether to render a linear list or a two-column grid.
 * @param sorting The active sort order applied to [albums] before display.
 * @param onAlbumClick Called with the tapped [MediaItem] to drill into the album's track list.
 * @param onPlayClick Called with the tapped [MediaItem] to start album playback immediately.
 * @param onMenuOption Called when the user selects an item from the per-row overflow menu.
 *   Receives the chosen [MenuOptionUtil.MenuOption], the [MediaItem], and the optional custom
 *   image base name needed by the [MenuOptionUtil.MenuOption.ADD_ALBUM_IMAGE] action.
 * @param onLayoutToggle Called when the user taps the layout-toggle icon in the toolbar.
 * @param onSortingSelected Called with the chosen [SortingUtil.SortingOption] when the user
 *   picks a sort order from the settings dropdown.
 */
@Composable
fun AlbumListScreen(
    modifier: Modifier = Modifier,
    albums: List<MediaItem>,
    layoutType: LayoutType,
    sorting: SortingUtil.SortingOption,
    onAlbumClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onMenuOption: (MenuOptionUtil.MenuOption, MediaItem, String?) -> Unit,
    onLayoutToggle: () -> Unit,
    onSortingSelected: (SortingUtil.SortingOption) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    // mediaId of the album whose overflow menu is currently open; null = none
    var menuOpenAlbumId by remember { mutableStateOf<String?>(null) }
    val sortedAlbums = remember(albums, sorting) { SortingUtil.sortAlbums(albums, sorting) }

    // Icon flips to indicate the layout the user will switch TO, matching the old fragment behaviour
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
                    text = stringResource(R.string.albums),
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
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
                            text = { Text(stringResource(R.string.order_by_newest_release)) },
                            onClick = {
                                onSortingSelected(SortingUtil.SortingOption.SORTING_NEWEST_RELEASE)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_by_oldest_release)) },
                            onClick = {
                                onSortingSelected(SortingUtil.SortingOption.SORTING_OLDEST_RELEASE)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_by_title_alphabetical)) },
                            onClick = {
                                onSortingSelected(SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_by_artist_alphabetical)) },
                            onClick = {
                                onSortingSelected(SortingUtil.SortingOption.SORTING_ARTIST_ALPHABETICAL)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────────────
            if (sortedAlbums.isEmpty()) {
                InformationScreen(
                    firstIcon = painterResource(R.drawable.browse_album_icon),
                    firstInfo = stringResource(R.string.no_albums_found),
                    onFirstClick = {},
                    secondIcon = painterResource(R.drawable.album_icon),
                    secondInfo = stringResource(R.string.no_albums_found),
                    onSecondClick = {}
                )
            } else when (layoutType) {
                LayoutType.LINEAR_LAYOUT -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(sortedAlbums, key = { it.mediaId }) { album ->
                            val meta = album.mediaMetadata
                            AlbumListItem(
                                albumTitle = meta.albumTitle?.toString().orEmpty(),
                                albumArtist = meta.albumArtist?.toString().orEmpty(),
                                releaseYear = meta.releaseYear?.takeIf { it > 0 },
                                artworkUri = meta.artworkUri,
                                onAlbumClick = { onAlbumClick(album) },
                                onPlayClick = { onPlayClick(album) },
                                onMenuClick = { menuOpenAlbumId = album.mediaId },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            DropdownMenu(
                                expanded = menuOpenAlbumId == album.mediaId,
                                onDismissRequest = { menuOpenAlbumId = null }
                            ) {
                                AlbumMenuItems(
                                    album = album,
                                    onMenuOption = onMenuOption,
                                    onDismiss = { menuOpenAlbumId = null }
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
                        items(sortedAlbums, key = { it.mediaId }) { album ->
                            val meta = album.mediaMetadata
                            val year = meta.releaseYear?.takeIf { it > 0 }
                            val description = if (year != null)
                                "$year | ${meta.albumArtist}"
                            else
                                meta.albumArtist?.toString().orEmpty()
                            Box {
                                AlbumGridItem(
                                    albumTitle = meta.albumTitle?.toString().orEmpty(),
                                    description = description,
                                    artworkUri = meta.artworkUri,
                                    onAlbumClick = { onAlbumClick(album) },
                                    onLongClick = { menuOpenAlbumId = album.mediaId }
                                )
                                DropdownMenu(
                                    expanded = menuOpenAlbumId == album.mediaId,
                                    onDismissRequest = { menuOpenAlbumId = null }
                                ) {
                                    AlbumMenuItems(
                                        album = album,
                                        onMenuOption = onMenuOption,
                                        onDismiss = { menuOpenAlbumId = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The three album overflow menu items shared between the list and grid layouts.
 *
 * Computes the custom image base name from [album] metadata only when the user taps
 * "Add Custom Album Image", avoiding the [UtilImpl.getImageBaseNameFromExternalStorage]
 * call on every recomposition.
 *
 * @param album The album this menu is anchored to.
 * @param onMenuOption Forwarded from [AlbumListScreen]; see its KDoc for the full contract.
 * @param onDismiss Called after any item is tapped so the caller can close the menu.
 */
@Composable
private fun AlbumMenuItems(
    album: MediaItem,
    onMenuOption: (MenuOptionUtil.MenuOption, MediaItem, String?) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.play_album)) },
        onClick = {
            onMenuOption(MenuOptionUtil.MenuOption.PLAY_ALBUM, album, null)
            onDismiss()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.add_to_queue)) },
        onClick = {
            onMenuOption(MenuOptionUtil.MenuOption.ADD_TO_QUEUE, album, null)
            onDismiss()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.add_custom_album_image)) },
        onClick = {
            val meta = album.mediaMetadata
            val imgName = UtilImpl.getImageBaseNameFromExternalStorage(
                groupTitle = meta.albumTitle?.toString().orEmpty(),
                artist = meta.albumArtist?.toString().orEmpty(),
                songGroupType = SongGroupType.ALBUM
            )
            onMenuOption(MenuOptionUtil.MenuOption.ADD_ALBUM_IMAGE, album, imgName)
            onDismiss()
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AlbumListScreenLinearPreview() {
    val fakeAlbum = MediaItem.Builder()
        .setMediaId("1")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setAlbumTitle("The Dark Side of the Moon")
                .setAlbumArtist("Pink Floyd")
                .setReleaseYear(1973)
                .build()
        )
        .build()
    AlbumListScreen(
        albums = listOf(fakeAlbum, fakeAlbum.buildUpon().setMediaId("2").build()),
        layoutType = LayoutType.LINEAR_LAYOUT,
        sorting = SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL,
        onAlbumClick = {},
        onPlayClick = {},
        onMenuOption = { _, _, _ -> },
        onLayoutToggle = {},
        onSortingSelected = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AlbumListScreenGridPreview() {
    val fakeAlbum = MediaItem.Builder()
        .setMediaId("1")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setAlbumTitle("Abbey Road")
                .setAlbumArtist("The Beatles")
                .setReleaseYear(1969)
                .build()
        )
        .build()
    AlbumListScreen(
        albums = listOf(fakeAlbum, fakeAlbum.buildUpon().setMediaId("2").build()),
        layoutType = LayoutType.TWO_GRID_LAYOUT,
        sorting = SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL,
        onAlbumClick = {},
        onPlayClick = {},
        onMenuOption = { _, _, _ -> },
        onLayoutToggle = {},
        onSortingSelected = {}
    )
}
