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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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

/**
 * Linear list row for a single song entry in the all-songs list or a song-group detail view.
 *
 * Stateless — all display data and callbacks are provided by the caller. Renders a 60 dp card
 * with a drag handle, album art with an optional favourite indicator overlay, song metadata
 * text, an add-to-playlist button, and an overflow menu button.
 *
 * The drag handle is conditionally visible — hidden for album contexts (fixed MediaStore order)
 * and shown only for playlists (where reordering is supported).
 *
 * @param modifier Modifier applied to the root [Card].
 * @param title The song display title.
 * @param artist The track artist.
 * @param duration Pre-formatted duration string (e.g. "3:45").
 * @param artworkUri Content URI for the album artwork, or `null` for the placeholder.
 * @param showDragHandle Whether to render the reorder drag-handle icon. Defaults to `true`.
 * @param isSelected Whether this song is selected in multi-select mode. Drives the animated
 *   green selection border that sweeps in around the artwork. Defaults to `false`.
 * @param onSongClick Called when the user taps the text area to play the song.
 * @param onArtworkClick Called when the user taps the artwork / favourite overlay area.
 * @param onAddClick Called when the user taps the add-to-playlist button.
 * @param onMenuClick Called when the user taps the overflow menu button.
 * @param dragHandleModifier Modifier applied to the drag-handle icon. In a reorderable
 *   `LazyColumn`, the caller passes `Modifier.draggableHandle(...)` from within a
 *   `ReorderableItem` scope so the library can intercept the gesture. Defaults to no-op.
 */
@Composable
fun SongItem(
    modifier: Modifier = Modifier,
    title: String,
    artist: String,
    duration: String,
    artworkUri: Uri?,
    showDragHandle: Boolean = true,
    isSelected: Boolean = false,
    onSongClick: () -> Unit,
    onArtworkClick: () -> Unit,
    onAddClick: () -> Unit,
    onMenuClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    // The tap gesture detectors below run inside pointerInput(Unit), which never restarts and
    // would otherwise capture the first callback instances forever. rememberUpdatedState keeps
    // them pointing at the latest lambdas so artwork taps toggle the current selection state.
    val currentArtworkClick by rememberUpdatedState(onArtworkClick)
    val currentSongClick by rememberUpdatedState(onSongClick)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ListItemCardBackground),
        border = BorderStroke(1.dp, ListItemCardStroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle — conditionally visible; caller applies draggableHandle modifier
            if (showDragHandle) {
                Icon(
                    painter = painterResource(R.drawable.baseline_reorder_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .width(40.dp)
                        .height(60.dp)
                        .then(dragHandleModifier)
                )
            }

            // Artwork area with selection-border overlay — 60×60 dp ConstraintLayout equivalent.
            // The border layer sits behind the 50 dp art (offset 5 dp top/start) so the green
            // frames it, matching the original viewholder_song.xml z-order.
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { currentArtworkClick() })
                    },
                contentAlignment = Alignment.Center
            ) {
                SelectionBorder(
                    isActive = isSelected,
                    modifier = Modifier.size(60.dp)
                )

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
            }

            // Song metadata — weight=10 taking the remaining row space, clickable to play
            Column(
                modifier = Modifier
                    .weight(10f)
                    .padding(top = 10.dp, start = 10.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { currentSongClick() })
                    }
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

            IconButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.add_icon),
                    contentDescription = null,
                    modifier = Modifier.size(35.dp),
                    tint = Color.Unspecified
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
private fun SongItemPreview() {
    SongItem(
        title = "Comfortably Numb",
        artist = "Pink Floyd",
        duration = "6:22",
        artworkUri = null,
        showDragHandle = true,
        isSelected = false,
        onSongClick = {},
        onArtworkClick = {},
        onAddClick = {},
        onMenuClick = {},
        dragHandleModifier = Modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SongItemFavouritePreview() {
    SongItem(
        title = "Wish You Were Here",
        artist = "Pink Floyd",
        duration = "5:34",
        artworkUri = null,
        showDragHandle = false,
        isSelected = true,
        onSongClick = {},
        onArtworkClick = {},
        onAddClick = {},
        onMenuClick = {}
    )
}
