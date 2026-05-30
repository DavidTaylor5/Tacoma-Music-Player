package com.andaagii.tacomamusicplayer.composables

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.andaagii.tacomamusicplayer.R

/**
 * Grid cell for a single playlist entry.
 *
 * Stateless — all data and actions are supplied by the caller. Mirrors the
 * `viewholder_playlist_grid_layout.xml` / `PlaylistGridAdapter` pair. Structure is
 * identical to [AlbumGridItem] except the description text spans 150 dp (the full card
 * width) rather than the 100 dp used by the album variant.
 *
 * Short tap opens the playlist; long press reveals the options menu.
 *
 * @param modifier Modifier applied to the outermost [Card] container.
 * @param playlistName Playlist display name shown below the artwork, truncated to one line.
 * @param description Secondary text (e.g. track count or last modified date) in grey.
 * @param artworkUri Content URI for the playlist artwork, or `null` to show the placeholder.
 * @param onPlaylistClick Called when the user short-taps the cell to open the playlist.
 * @param onLongClick Called when the user long-presses to reveal the options menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistGridItem(
    modifier: Modifier = Modifier,
    playlistName: String,
    description: String,
    artworkUri: Uri?,
    onPlaylistClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = modifier
            .wrapContentSize()
            .padding(top = 10.dp)
            .combinedClickable(
                onClick = onPlaylistClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Artwork thumbnail — 150×150 dp matching the XML picture_frame dimensions
            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.size(150.dp)
            ) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.white_note),
                    error = painterResource(R.drawable.white_note),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(150.dp)
                )
            }

            // Playlist name — 150 dp wide matching the XML
            Text(
                text = playlistName,
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(150.dp)
                    .padding(top = 3.dp)
            )

            // Description — 150 dp wide (wider than the 100 dp album variant)
            Text(
                text = description,
                color = Color(0xFFABA4A4),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(150.dp)
                    .padding(top = 2.dp, bottom = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PlaylistGridItemPreview() {
    PlaylistGridItem(
        playlistName = "Road Trip",
        description = "23 songs",
        artworkUri = null,
        onPlaylistClick = {},
        onLongClick = {}
    )
}
