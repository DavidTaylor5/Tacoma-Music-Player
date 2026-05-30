package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Playlist modification dialog — not yet implemented.
 *
 * This is a stub composable matching the empty [com.andaagii.tacomamusicplayer.view.CustomPlaylistModPrompt]
 * class. Implement the body here when the playlist modification feature is built out.
 *
 * @param modifier Modifier applied to the root [Box].
 */
@Composable
fun PlaylistModPrompt(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier)
}

@Preview
@Composable
private fun PlaylistModPromptPreview() {
    PlaylistModPrompt()
}
