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
 * Grid cell for a single album entry.
 *
 * Stateless — all data and actions are supplied by the caller. Mirrors the
 * `viewholder_album_grid_layout.xml` / `AlbumGridAdapter` pair: a centred card containing
 * a 150 dp artwork thumbnail, album title, and a secondary description (artist or year).
 * Short tap opens the album; long press triggers the overflow menu.
 *
 * @param modifier Modifier applied to the outermost [Card] container.
 * @param albumTitle Album display title shown below the artwork, truncated to one line.
 * @param description Secondary text (artist / year) shown beneath the title in grey.
 * @param artworkUri Content URI for the album artwork, or `null` to show the placeholder.
 * @param onAlbumClick Called when the user short-taps the cell to open the album.
 * @param onLongClick Called when the user long-presses the cell to reveal the options menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumGridItem(
    modifier: Modifier = Modifier,
    albumTitle: String,
    description: String,
    artworkUri: Uri?,
    onAlbumClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = modifier
            .wrapContentSize()
            .padding(top = 10.dp)
            // combinedClickable replaces the XML short-click / long-click listeners
            .combinedClickable(
                onClick = onAlbumClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Artwork thumbnail card — 150×150 dp matching the XML album_frame dimensions
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

            // Album title — 150 dp fixed width to match the XML
            Text(
                text = albumTitle,
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(150.dp)
                    .padding(top = 3.dp)
            )

            // Description (artist / year) — 100 dp wide in the album variant, grey tint
            Text(
                text = description,
                color = Color(0xFFABA4A4),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(100.dp)
                    .padding(top = 2.dp, bottom = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AlbumGridItemPreview() {
    AlbumGridItem(
        albumTitle = "Abbey Road",
        description = "The Beatles",
        artworkUri = null,
        onAlbumClick = {},
        onLongClick = {}
    )
}
