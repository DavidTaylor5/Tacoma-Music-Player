package com.andaagii.tacomamusicplayer.composables

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.andaagii.tacomamusicplayer.R

/** Green stroke color matching `#4CAF50` shown on the currently playing queue row. */
private val QueuePlayingGreen = Color(0xFF4CAF50)

/**
 * Linear list row for a single song entry in the current playback queue.
 *
 * Stateless — all display data and callbacks are provided by the caller. Mirrors the
 * `viewholder_queue_song.xml` / `QueueListAdapter` pair. Identical structure to
 * [SongItem] except:
 * - The drag handle is always shown (queue reordering is always available).
 * - There is no add-to-playlist button; only the overflow menu icon is present.
 * - The card border switches between green ([QueuePlayingGreen]) when [isCurrentlyPlaying]
 *   is `true` and white when `false`, giving a clear visual indicator of the active track.
 *
 * @param modifier Modifier applied to the root [Card].
 * @param title The song display title.
 * @param artist The track artist.
 * @param duration Pre-formatted duration string (e.g. "3:45").
 * @param artworkUri Content URI for the album artwork, or `null` for the placeholder.
 * @param isCurrentlyPlaying Whether this song is the active track. `true` renders a green
 *   card border; `false` renders a white border. Corresponds to [DisplaySong.showPlayIndicator].
 * @param onSongClick Called when the user taps the row to jump to this song in the queue.
 * @param onMenuClick Called when the user taps the overflow menu button.
 * @param dragHandleModifier Modifier applied to the drag-handle icon. In a reorderable
 *   `LazyColumn`, the caller passes `Modifier.draggableHandle(...)` from within a
 *   `ReorderableItem` scope so the library can intercept the gesture.
 */
@Composable
fun QueueSongItem(
    modifier: Modifier = Modifier,
    title: String,
    artist: String,
    duration: String,
    artworkUri: Uri?,
    isCurrentlyPlaying: Boolean,
    onSongClick: () -> Unit,
    onMenuClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    // Border color distinguishes the active track from the rest of the queue
    val borderColor = if (isCurrentlyPlaying) QueuePlayingGreen else Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(top = 2.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onSongClick() })
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle — always visible in the queue, unlike in SongItem
            Icon(
                painter = painterResource(R.drawable.baseline_reorder_24),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .width(40.dp)
                    .height(60.dp)
                    .padding(horizontal = 10.dp)
                    .then(dragHandleModifier)
            )

            // Artwork area with playing indicator overlay — same 60×60 dp structure as SongItem
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.white_note),
                    error = painterResource(R.drawable.white_note),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .padding(top = 5.dp, start = 5.dp)
                        .align(Alignment.TopStart)
                )

                // Playing indicator — shown when this is the active track.
                // The original used AnimationDrawable; full animation can be wired in later.
                if (isCurrentlyPlaying) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_star_24_green),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            // Song metadata
            Column(
                modifier = Modifier
                    .weight(10f)
                    .padding(top = 10.dp, start = 10.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = duration,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(R.drawable.menu_icon),
                    contentDescription = null,
                    modifier = Modifier.size(35.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun QueueSongItemIdlePreview() {
    QueueSongItem(
        title = "Money",
        artist = "Pink Floyd",
        duration = "6:22",
        artworkUri = null,
        isCurrentlyPlaying = false,
        onSongClick = {},
        onMenuClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun QueueSongItemPlayingPreview() {
    QueueSongItem(
        title = "Time",
        artist = "Pink Floyd",
        duration = "7:05",
        artworkUri = null,
        isCurrentlyPlaying = true,
        onSongClick = {},
        onMenuClick = {}
    )
}
