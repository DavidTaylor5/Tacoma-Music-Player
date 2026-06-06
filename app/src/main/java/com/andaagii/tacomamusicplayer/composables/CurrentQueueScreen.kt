package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.andaagii.tacomamusicplayer.R

/**
 * Full-screen page for the current playback queue, hosted at index 0 in
 * `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Stateful — manages a local copy of [songs] for immediate drag-reorder visual feedback, and
 * tracks which row's dropdown menu is open. Drag reorder is backed by `sh.calvin.reorderable`;
 * the final item position is committed to the [MediaController] via [onMoveItem] when the
 * user releases the drag handle.
 *
 * @param modifier Modifier applied to the root [Column].
 * @param songs The current queue from `MainViewModel.currentlyPlayingSongs`. Drives a local
 *   shadow list that updates instantly on each drag step.
 * @param currentlyPlayingTitle Title string of the track currently playing, used to highlight
 *   the active row with a green border. `null` when nothing is playing.
 * @param onSongClick Called with the tapped row's index to seek to and play that position.
 * @param onRemoveSong Called with the row index to remove that track from the queue.
 * @param onMoveItem Called with `(from, to)` indices when a drag completes, so the caller
 *   can persist the reorder to the [MediaController] via `moveMediaItem`.
 * @param onClearQueue Called when the user taps the "Clear Queue" button.
 */
@Composable
fun CurrentQueueScreen(
    modifier: Modifier = Modifier,
    songs: List<MediaItem>,
    currentlyPlayingTitle: String?,
    onSongClick: (position: Int) -> Unit,
    onRemoveSong: (position: Int) -> Unit,
    onMoveItem: (from: Int, to: Int) -> Unit,
    onClearQueue: () -> Unit
) {
    // Local shadow list for immediate drag-reorder feedback; reset when the external queue changes.
    var localSongs by remember(songs) { mutableStateOf(songs) }

    // Index of the row whose dropdown menu is currently open; -1 = none visible.
    var menuOpenIndex by remember { mutableIntStateOf(-1) }

    // Drag tracking — start index captured on drag-handle press, cleared on release.
    var dragStartIndex by remember { mutableIntStateOf(-1) }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            localSongs = localSongs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header row — mirrors the ConstraintLayout header in fragment_current_queue.xml
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.queue),
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClearQueue) {
                Text(
                    text = stringResource(R.string.clear_queue),
                    color = Color.White
                )
            }
        }

        if (localSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_music_in_queue),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(localSongs, key = { _, item -> item.mediaId }) { index, song ->
                    ReorderableItem(reorderState, key = song.mediaId) { _ ->
                        QueueSongItem(
                            title              = song.mediaMetadata.title?.toString().orEmpty(),
                            artist             = song.mediaMetadata.artist?.toString().orEmpty(),
                            duration           = song.mediaMetadata.description?.toString().orEmpty(),
                            artworkUri         = song.mediaMetadata.artworkUri,
                            isCurrentlyPlaying = song.mediaMetadata.title?.toString() == currentlyPlayingTitle,
                            onSongClick        = { onSongClick(index) },
                            onMenuClick        = { menuOpenIndex = index },
                            dragHandleModifier = Modifier.draggableHandle(
                                onDragStarted = { dragStartIndex = index },
                                onDragStopped = {
                                    val finalIndex = localSongs.indexOfFirst { it.mediaId == song.mediaId }
                                    if (dragStartIndex >= 0 && finalIndex >= 0) {
                                        onMoveItem(dragStartIndex, finalIndex)
                                    }
                                    dragStartIndex = -1
                                }
                            )
                        )

                        DropdownMenu(
                            expanded = menuOpenIndex == index,
                            onDismissRequest = { menuOpenIndex = -1 }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove_from_queue)) },
                                onClick = {
                                    onRemoveSong(index)
                                    menuOpenIndex = -1
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CurrentQueueScreenWithSongsPreview() {
    val fakeSong = MediaItem.Builder()
        .setMediaId("1")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Money")
                .setArtist("Pink Floyd")
                .setDescription("6:22")
                .build()
        )
        .build()
    CurrentQueueScreen(
        songs = listOf(fakeSong, fakeSong.buildUpon().setMediaId("2").build()),
        currentlyPlayingTitle = "Money",
        onSongClick = {},
        onRemoveSong = {},
        onMoveItem = { _, _ -> },
        onClearQueue = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CurrentQueueScreenEmptyPreview() {
    CurrentQueueScreen(
        songs = emptyList(),
        currentlyPlayingTitle = null,
        onSongClick = {},
        onRemoveSong = {},
        onMoveItem = { _, _ -> },
        onClearQueue = {}
    )
}
