package com.andaagii.tacomamusicplayer.composables

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * Linear list row for a single playlist entry.
 *
 * Stateless — all data and actions are supplied by the caller. Mirrors the
 * `viewholder_playlist.xml` / `PlaylistAdapter` pair: a 60 dp card containing playlist
 * art, name, track count, total duration, a play button, and an overflow menu button.
 *
 * Structure mirrors [AlbumListItem] with the title column weighted 2f instead of 3f to
 * accommodate the two-line duration info column.
 *
 * @param modifier Modifier applied to the root [Card].
 * @param playlistName The playlist display name shown in the centre column.
 * @param tracksInfo Short string indicating the number of tracks (e.g. "12 songs").
 * @param totalDuration Formatted total runtime of the playlist (e.g. "47:23").
 * @param artworkUri Content URI for the playlist artwork, or `null` to show the placeholder.
 * @param onPlaylistClick Called when the user taps the row to open the playlist.
 * @param onPlayClick Called when the user taps the play button.
 * @param onMenuClick Called when the user taps the overflow menu button.
 */
@Composable
fun PlaylistListItem(
    modifier: Modifier = Modifier,
    playlistName: String,
    tracksInfo: String,
    totalDuration: String,
    artworkUri: Uri?,
    onPlaylistClick: () -> Unit,
    onPlayClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        onClick = onPlaylistClick,
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
            // Playlist artwork — 50 dp square with start margin
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

            // Playlist name — weight=2 matching the XML
            Text(
                text = playlistName,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(2f)
                    .padding(horizontal = 4.dp)
            )

            // Duration info column — two lines: track count and total runtime
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tracksInfo,
                    color = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
                Text(
                    text = totalDuration,
                    color = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
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
private fun PlaylistListItemPreview() {
    PlaylistListItem(
        playlistName = "Late Night Chill",
        tracksInfo = "18 songs",
        totalDuration = "1:12:04",
        artworkUri = null,
        onPlaylistClick = {},
        onPlayClick = {},
        onMenuClick = {}
    )
}
