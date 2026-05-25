package com.andaagii.tacomamusicplayer.composables

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.andaagii.tacomamusicplayer.R

/** Green accent color matching the original `#4CAF50` used for the + button and footer. */
private val PromptGreen = Color(0xFF4CAF50)

/** Dark card background matching the original `#121212` used inside the dialog card. */
private val PromptCardBackground = Color(0xFF121212)

/**
 * Full-screen overlay dialog that lets the user select one or more playlists to add songs to.
 *
 * Stateful — checkbox state for each playlist is managed internally via [remember].
 * Tapping the semi-transparent scrim outside the card triggers [onCloseClick]. The ADD
 * button is disabled until at least one playlist is checked.
 *
 * Replaces [com.andaagii.tacomamusicplayer.view.CustomPlaylistPrompt] and the associated
 * [com.andaagii.tacomamusicplayer.adapter.PlaylistPromptAdapter] RecyclerView setup.
 *
 * @param modifier Modifier applied to the outermost full-screen [Box].
 * @param playlists The list of available playlists to display. Each item's `albumTitle`
 *   metadata field is shown as the playlist name.
 * @param onAddClick Called with the set of checked playlist titles when the user taps ADD.
 * @param onCloseClick Called when the user taps CLOSE or the background scrim.
 * @param onCreateNewPlaylistClick Called when the user taps the "+" create button.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaylistPrompt(
    modifier: Modifier = Modifier,
    playlists: List<MediaItem>,
    onAddClick: (checkedTitles: Set<String>) -> Unit,
    onCloseClick: () -> Unit,
    onCreateNewPlaylistClick: () -> Unit
) {
    // Internal map tracking which playlists are checked; key is the playlist title string
    val checkedState = remember { mutableStateMapOf<String, Boolean>() }
    val anyChecked = checkedState.values.any { it }

    // Full-screen semi-transparent scrim; tapping outside the card closes the prompt
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onCloseClick),
        contentAlignment = Alignment.Center
    ) {
        // Card container — consume clicks so they don't propagate to the scrim
        Column(
            modifier = Modifier
                .width(300.dp)
                .height(600.dp)
                .background(PromptCardBackground)
                .clickable(enabled = false, onClick = {})
        ) {
            // Header row: label + create-new-playlist button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.create_new_playlist),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onCreateNewPlaylistClick) {
                    Text(text = "+", color = PromptGreen, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color.DarkGray)

            // Playlist list — takes the remaining vertical space above the footer
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playlists, key = { it.mediaId }) { playlist ->
                    val title = playlist.mediaMetadata.albumTitle?.toString() ?: ""
                    val isChecked = checkedState[title] == true

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Tapping the row toggles the checkbox
                            .clickable { checkedState[title] = !isChecked }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked -> checkedState[title] = checked },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PromptGreen,
                                uncheckedColor = Color.Gray
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.DarkGray)

            // Footer row: close and add buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onCloseClick) {
                    Text(text = stringResource(R.string.close_button), color = PromptGreen)
                }
                TextButton(
                    onClick = {
                        // Collect titles of all checked playlists before surfacing to caller
                        val checked = checkedState.filterValues { it }.keys
                        onAddClick(checked)
                    },
                    enabled = anyChecked
                ) {
                    Text(text = stringResource(R.string.add_button), color = PromptGreen)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlaylistPromptPreview() {
    PlaylistPrompt(
        playlists = emptyList(),
        onAddClick = {},
        onCloseClick = {},
        onCreateNewPlaylistClick = {}
    )
}
