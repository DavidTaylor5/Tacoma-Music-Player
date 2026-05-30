package com.andaagii.tacomamusicplayer.composables

import android.net.Uri
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.andaagii.tacomamusicplayer.R

/**
 * Header view displayed above a song list when browsing an album or playlist.
 *
 * Stateless — all data and actions are supplied by the caller. Displays artwork loaded
 * asynchronously via Coil, a title, and play/menu action buttons.
 *
 * @param modifier Modifier applied to the root [Row].
 * @param imageUri Content URI pointing to the album or playlist artwork, or `null` to show
 *   the placeholder.
 * @param title The album or playlist title displayed in the centre of the header.
 * @param onPlayClick Called when the user taps the play button.
 * @param onMenuClick Called when the user taps the overflow menu button.
 */
@Composable
fun SongGroupInfoView(
    modifier: Modifier = Modifier,
    imageUri: Uri?,
    title: String,
    onPlayClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork — fixed width matching the original 80 dp XML dimension
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            placeholder = painterResource(R.drawable.album_icon),
            error = painterResource(R.drawable.album_icon),
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
        )

        // Title fills remaining horizontal space
        Text(
            text = title,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SongGroupInfoViewPreview() {
    SongGroupInfoView(
        imageUri = null,
        title = "Dark Side of the Moon",
        onPlayClick = {},
        onMenuClick = {}
    )
}
