package com.andaagii.tacomamusicplayer.composables

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.andaagii.tacomamusicplayer.R

/**
 * Linear list row for a single album entry.
 *
 * Stateless — all data and actions are supplied by the caller. Mirrors the
 * `viewholder_album.xml` / `AlbumListAdapter` pair: a 60 dp card containing album art,
 * title, release year, a play button, and an overflow menu button.
 *
 * @param modifier Modifier applied to the root [Card].
 * @param albumTitle The album display title shown in the centre column.
 * @param albumArtist The primary artist for the album.
 * @param releaseYear Optional release year displayed in the right info column.
 * @param artworkUri Content URI for the album artwork, or `null` to show the placeholder.
 * @param onAlbumClick Called when the user taps the row to open the album.
 * @param onPlayClick Called when the user taps the play button.
 * @param onMenuClick Called when the user taps the overflow menu button.
 */
@Composable
fun AlbumListItem(
    modifier: Modifier = Modifier,
    albumTitle: String,
    albumArtist: String,
    releaseYear: Int?,
    artworkUri: Uri?,
    onAlbumClick: () -> Unit,
    onPlayClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        onClick = onAlbumClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album artwork — fixed 50 dp square with start margin matching the XML
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                placeholder = painterResource(R.drawable.white_note),
                error = painterResource(R.drawable.white_note),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(50.dp)
            )

            // Album title — takes most horizontal space, matches XML weight=3
            Text(
                text = albumTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(3f)
                    .padding(horizontal = 4.dp)
            )

            // Release year info column — weight=1 matching the XML duration_info column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (releaseYear != null) {
                    Text(
                        text = releaseYear.toString(),
                        color = Color.White,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }

            IconButton(onClick = onPlayClick) {
                Icon(
                    painter = painterResource(R.drawable.white_play_arrow),
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
private fun AlbumListItemPreview() {
    AlbumListItem(
        albumTitle = "The Dark Side of the Moon",
        albumArtist = "Pink Floyd",
        releaseYear = 1973,
        artworkUri = null,
        onAlbumClick = {},
        onPlayClick = {},
        onMenuClick = {}
    )
}
