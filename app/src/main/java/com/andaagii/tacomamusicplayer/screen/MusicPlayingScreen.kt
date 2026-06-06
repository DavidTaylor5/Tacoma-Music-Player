package com.andaagii.tacomamusicplayer.screen

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType

/**
 * Full-screen player page shown at [com.andaagii.tacomamusicplayer.enumtype.PageType.PLAYER_PAGE]
 * in [MusicChooserScreen]'s [HorizontalPager].
 *
 * Stateless — all playback state is passed as parameters and all user interactions are surfaced
 * via lambda callbacks. When no song is active the screen shows a placeholder illustration;
 * once a song is playing the artwork, metadata, seek bar, and controls become visible.
 *
 * The seek bar is rendered via an [AndroidView]-wrapped [PlayerView] because Media3 has
 * no Compose-native equivalent yet. All other controls are native Composables.
 *
 * @param modifier Modifier applied to the root [Box].
 * @param songInfo Metadata for the currently playing track, or `null` / a null-sentinel when
 *   nothing is queued. Controls whether the placeholder or the active player UI is shown.
 * @param isPlaying Whether the player is actively outputting audio; drives the play/pause icon.
 * @param loopMode Current [Player] repeat mode (`REPEAT_MODE_OFF`, `REPEAT_MODE_ONE`,
 *   or `REPEAT_MODE_ALL`); drives the loop-toggle icon.
 * @param shuffleMode Current shuffle state; drives the shuffle-toggle icon.
 * @param mediaController Active [MediaController], bound to the embedded [PlayerView] so
 *   the seek bar reflects live playback position. May be `null` before the media session connects.
 * @param onPreviousSong Called when the user taps the previous-track button.
 * @param onSeekBack Called when the user taps the seek-back (−15 s) button.
 * @param onTogglePlay Called when the user taps the play/pause button.
 * @param onSeekForward Called when the user taps the seek-forward (+15 s) button.
 * @param onNextSong Called when the user taps the next-track button.
 * @param onFlipLoopMode Called when the user taps the loop-toggle button; the caller cycles the
 *   repeat mode through off → one → all → off.
 * @param onFlipShuffleState Called when the user taps the shuffle-toggle button.
 */
@OptIn(UnstableApi::class)
@Composable
fun MusicPlayingScreen(
    modifier: Modifier = Modifier,
    songInfo: SongData?,
    isPlaying: Boolean,
    loopMode: Int?,
    shuffleMode: ShuffleType?,
    mediaController: MediaController?,
    onPreviousSong: () -> Unit,
    onSeekBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekForward: () -> Unit,
    onNextSong: () -> Unit,
    onFlipLoopMode: () -> Unit,
    onFlipShuffleState: () -> Unit
) {
    val showActivePlayer = songInfo != null && !SongData.isNullSong(songInfo)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!showActivePlayer) {
            Image(
                painter = painterResource(R.drawable.chopper_default),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(40.dp))

                AsyncImage(
                    model = songInfo?.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(7f)
                )

                SongInfoRow(
                    songInfo = songInfo,
                    loopMode = loopMode,
                    shuffleMode = shuffleMode,
                    onFlipLoopMode = onFlipLoopMode,
                    onFlipShuffleState = onFlipShuffleState
                )

                // Seek bar — PlayerView has no Compose equivalent; AndroidView is the
                // supported interop path. Inflate from XML so controller_layout_id /
                // player_layout_id (XML-only attributes) restrict the surface to just the
                // progress bar, matching the pre-Compose Views UI. The transport controls
                // below are the real ones; the PlayerView contributes only the timebar.
                AndroidView(
                    modifier = Modifier.fillMaxWidth()
                        .padding(10.dp),
                    factory = { ctx ->
                        (LayoutInflater.from(ctx)
                            .inflate(R.layout.view_progress_only_player, null) as PlayerView)
                            .apply {
                                player = mediaController
                                showController()
                            }
                    },
                    update = { playerView -> playerView.player = mediaController }
                )

                PlaybackControlRow(
                    isPlaying = isPlaying,
                    onPreviousSong = onPreviousSong,
                    onSeekBack = onSeekBack,
                    onTogglePlay = onTogglePlay,
                    onSeekForward = onSeekForward,
                    onNextSong = onNextSong
                )
            }
        }
    }
}

/**
 * Horizontal row showing the loop toggle, centred song metadata, and shuffle toggle.
 *
 * @param songInfo Song metadata displayed in the centre column.
 * @param loopMode Current repeat mode; determines which loop icon is shown.
 * @param shuffleMode Current shuffle state; determines which shuffle icon is shown.
 * @param onFlipLoopMode Called when the loop-toggle icon button is tapped.
 * @param onFlipShuffleState Called when the shuffle-toggle icon button is tapped.
 */
@Composable
private fun SongInfoRow(
    songInfo: SongData?,
    loopMode: Int?,
    shuffleMode: ShuffleType?,
    onFlipLoopMode: () -> Unit,
    onFlipShuffleState: () -> Unit
) {
    val loopIconRes = when (loopMode) {
        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
        Player.REPEAT_MODE_ALL -> R.drawable.repeat
        else                   -> R.drawable.one_x
    }
    val shuffleIconRes = if (shuffleMode == ShuffleType.SHUFFLED) R.drawable.shuffle
                         else R.drawable.right_arrow

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onFlipLoopMode,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                painter = painterResource(loopIconRes),
                contentDescription = null,
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = songInfo?.songTitle.orEmpty(),
                color = Color.White,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = songInfo?.albumTitle.orEmpty(),
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = songInfo?.artist.orEmpty(),
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = onFlipShuffleState,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                painter = painterResource(shuffleIconRes),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

/**
 * Fixed-height row of playback transport controls: seek-back, previous, play/pause, next,
 * seek-forward.
 *
 * @param isPlaying Whether audio is currently playing; toggles the play/pause icon.
 * @param onPreviousSong Called on previous-track tap.
 * @param onSeekBack Called on seek-back (−15 s) tap.
 * @param onTogglePlay Called on play/pause tap.
 * @param onSeekForward Called on seek-forward (+15 s) tap.
 * @param onNextSong Called on next-track tap.
 */
@Composable
private fun PlaybackControlRow(
    isPlaying: Boolean,
    onPreviousSong: () -> Unit,
    onSeekBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekForward: () -> Unit,
    onNextSong: () -> Unit
) {
    val playPauseIconRes = if (isPlaying) R.drawable.baseline_pause_24
                           else R.drawable.white_play_arrow

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSeekBack,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_replay_15),
                contentDescription = null,
                tint = Color.White
            )
        }

        IconButton(
            onClick = onPreviousSong,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_keyboard_double_arrow_left_24),
                contentDescription = null,
                tint = Color.White
            )
        }

        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(75.dp)
        ) {
            Icon(
                painter = painterResource(playPauseIconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(75.dp)
            )
        }

        IconButton(
            onClick = onNextSong,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_keyboard_double_arrow_right_24),
                contentDescription = null,
                tint = Color.White
            )
        }

        IconButton(
            onClick = onSeekForward,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_forward_15),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MusicPlayingScreenActivePrevew() {
    MusicPlayingScreen(
        songInfo = SongData(
            songUri = "content://media/external/audio/media/1",
            songTitle = "Song Title",
            albumTitle = "Album Title",
            artist = "Artist Name",
            artworkUri = "",
            duration = "3:45"
        ),
        isPlaying = true,
        loopMode = Player.REPEAT_MODE_OFF,
        shuffleMode = ShuffleType.NOT_SHUFFLED,
        mediaController = null,
        onPreviousSong = {},
        onSeekBack = {},
        onTogglePlay = {},
        onSeekForward = {},
        onNextSong = {},
        onFlipLoopMode = {},
        onFlipShuffleState = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MusicPlayingScreenPlaceholderPreview() {
    MusicPlayingScreen(
        songInfo = null,
        isPlaying = false,
        loopMode = null,
        shuffleMode = null,
        mediaController = null,
        onPreviousSong = {},
        onSeekBack = {},
        onTogglePlay = {},
        onSeekForward = {},
        onNextSong = {},
        onFlipLoopMode = {},
        onFlipShuffleState = {}
    )
}
