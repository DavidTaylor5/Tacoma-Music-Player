package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.andaagii.tacomamusicplayer.R
import kotlinx.coroutines.delay

/**
 * Shared visual styling for the linear list-item rows (songs, albums, playlists, queue).
 *
 * These values reproduce the original `Widget.MyApp.CardView` theme style and the
 * `favorite_animation` AnimationDrawable so the Compose rows match the pre-migration look.
 */

/** Card background matching `Widget.MyApp.CardView` `cardBackgroundColor` `#141118`. */
internal val ListItemCardBackground = Color(0xFF141118)

/** Card stroke matching `Widget.MyApp.CardView` `strokeColor` `#48454C`. */
internal val ListItemCardStroke = Color(0xFF48454C)

/** Selection-border frames; index 0 = inactive (black), 12 = active (full green). */
private val SelectionFrames = listOf(
    R.drawable.fav_00, R.drawable.fav_01, R.drawable.fav_02, R.drawable.fav_03,
    R.drawable.fav_04, R.drawable.fav_05, R.drawable.fav_06, R.drawable.fav_07,
    R.drawable.fav_08, R.drawable.fav_09, R.drawable.fav_10, R.drawable.fav_11,
    R.drawable.fav_12,
)

/** Per-frame duration matching the original `favorite_animation` animation-list item. */
private const val SELECTION_FRAME_MS = 30L

/**
 * Animated green selection/active border drawn behind a 50 dp album-art thumbnail.
 *
 * Reproduces the original `favorite_animation` / `unfavorite_animation` AnimationDrawables by
 * cycling through [SelectionFrames]. The green sweeps in when [isActive] becomes `true` and back
 * out when it becomes `false`. The frame state initialises to the end frame so a row scrolled
 * back into view in its active state shows the steady border without replaying — mirroring the
 * original adapter's `selectDrawable(0)` static bind.
 *
 * Used for both song multi-select (in [SongItem]) and the currently-playing queue track (in
 * [QueueSongItem]). Place it behind the artwork in a 60 dp box so the green frames the art.
 *
 * @param isActive Whether the border should be shown in its green (selected/playing) state.
 * @param modifier Modifier applied to the frame [Image]; sized 60 dp by the caller.
 */
@Composable
internal fun SelectionBorder(isActive: Boolean, modifier: Modifier = Modifier) {
    val last = SelectionFrames.lastIndex
    var frame by remember { mutableIntStateOf(if (isActive) last else 0) }

    LaunchedEffect(isActive) {
        val target = if (isActive) last else 0
        // Step one frame at a time toward the target; no-op on initial composition (already there)
        val step = if (target > frame) 1 else -1
        while (frame != target) {
            delay(SELECTION_FRAME_MS)
            frame += step
        }
    }

    Image(
        painter = painterResource(SelectionFrames[frame]),
        contentDescription = null,
        modifier = modifier
    )
}
