package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Green accent matching the `#4CAF50` used in the playlist prompt dialog. */
private val PromptCheckboxGreen = Color(0xFF4CAF50)

/**
 * Single row inside the add-to-playlist dialog showing one playlist name with a checkbox.
 *
 * Stateless — the caller (typically [PlaylistPrompt]) owns the checked state and passes
 * it down. Tapping anywhere in the row triggers [onCheckedChange] so users are not forced
 * to hit the small checkbox target. Mirrors `viewholder_playlist_prompt.xml`.
 *
 * @param modifier Modifier applied to the root [Row].
 * @param playlistName The name of the playlist to display.
 * @param isChecked Whether this playlist is currently selected.
 * @param onCheckedChange Called with the new checked state when the row or checkbox is tapped.
 */
@Composable
fun PlaylistPromptItem(
    modifier: Modifier = Modifier,
    playlistName: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Whole row is tappable to toggle the checkbox
            .clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = playlistName,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(end = 20.dp, top = 20.dp, bottom = 20.dp)
        )

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PromptCheckboxGreen,
                uncheckedColor = Color.Gray
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PlaylistPromptItemCheckedPreview() {
    PlaylistPromptItem(
        playlistName = "Favourites",
        isChecked = true,
        onCheckedChange = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PlaylistPromptItemUncheckedPreview() {
    PlaylistPromptItem(
        playlistName = "Road Trip",
        isChecked = false,
        onCheckedChange = {}
    )
}
