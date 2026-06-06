package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.data.SongData

/**
 * Compact mini-player strip shown between the [HorizontalPager] and [NavigationControl]
 * whenever the user is on any page other than the full player and a song is loaded.
 *
 * Stateless — all values are passed in and all interactions surface via lambdas. Visibility
 * is controlled by the caller ([MusicChooserScreen]); this composable is always visible when
 * composed.
 *
 * @param modifier Modifier applied to the root [Card].
 * @param songInfo Metadata for the currently playing track.
 * @param isPlaying Whether the player is actively playing; drives the play/pause icon.
 * @param onNavigateToPlayer Called when the user taps anywhere on the strip to open the full player.
 * @param onPreviousSong Called when the user taps the previous-track button.
 * @param onTogglePlay Called when the user taps the play/pause button.
 * @param onNextSong Called when the user taps the next-track button.
 */
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    songInfo: SongData,
    isPlaying: Boolean,
    onNavigateToPlayer: () -> Unit,
    onPreviousSong: () -> Unit,
    onTogglePlay: () -> Unit,
    onNextSong: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(5.dp)
            .clickable(onClick = onNavigateToPlayer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = songInfo.artworkUri.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp),
                placeholder = painterResource(R.drawable.chopper_default),
                error = painterResource(R.drawable.chopper_default),
            )

            Text(
                text = "${songInfo.songTitle} - ${songInfo.artist}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )

            IconButton(onClick = onPreviousSong) {
                Icon(
                    painter = painterResource(R.drawable.baseline_keyboard_double_arrow_left_24),
                    contentDescription = null
                )
            }

            IconButton(onClick = onTogglePlay) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.baseline_pause_24
                        else R.drawable.white_play_arrow
                    ),
                    contentDescription = null
                )
            }

            IconButton(onClick = onNextSong) {
                Icon(
                    painter = painterResource(R.drawable.baseline_keyboard_double_arrow_right_24),
                    contentDescription = null
                )
            }
        }
    }
}

@Preview
@Composable
private fun MiniPlayerPreview() {
    MiniPlayer(
        songInfo = SongData(
            songUri = "",
            songTitle = "My Song",
            artist = "My Artist",
            albumTitle = "My Album",
            artworkUri = "",
            duration = "3:45"
        ),
        isPlaying = true,
        onNavigateToPlayer = {},
        onPreviousSong = {},
        onTogglePlay = {},
        onNextSong = {},
    )
}
