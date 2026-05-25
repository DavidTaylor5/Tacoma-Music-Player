package com.andaagii.tacomamusicplayer.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.enumtype.PageType

/**
 * Bottom navigation tab bar for moving between the five main music chooser pages.
 *
 * Stateless — the caller owns [currentPage] and handles page transitions via [onPageSelected].
 * Each tab is highlighted with a unique per-page focus color; inactive tabs use the
 * unfocused gray from the app palette.
 *
 * @param modifier Modifier applied to the root [Row].
 * @param currentPage The page that should appear highlighted as active.
 * @param queueIconRes Drawable resource for the Queue tab icon.
 * @param playerIconRes Drawable resource for the Player tab icon.
 * @param playlistIconRes Drawable resource for the Playlist tab icon.
 * @param albumIconRes Drawable resource for the Album tab icon.
 * @param songIconRes Drawable resource for the Song tab icon.
 * @param onPageSelected Called with the tapped [PageType] when the user selects a tab.
 */
@Composable
fun NavigationControl(
    modifier: Modifier = Modifier,
    currentPage: PageType,
    @DrawableRes queueIconRes: Int,
    @DrawableRes playerIconRes: Int,
    @DrawableRes playlistIconRes: Int,
    @DrawableRes albumIconRes: Int,
    @DrawableRes songIconRes: Int,
    onPageSelected: (PageType) -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        NavigationTab(
            modifier = Modifier.weight(1f),
            iconRes = queueIconRes,
            page = PageType.QUEUE_PAGE,
            currentPage = currentPage,
            // Queue page uses the app accent color when focused
            focusedColor = colorResource(R.color.accent),
            onPageSelected = onPageSelected
        )
        NavigationTab(
            modifier = Modifier.weight(1f),
            iconRes = playerIconRes,
            page = PageType.PLAYER_PAGE,
            currentPage = currentPage,
            focusedColor = colorResource(R.color.light_green),
            onPageSelected = onPageSelected
        )
        NavigationTab(
            modifier = Modifier.weight(1f),
            iconRes = playlistIconRes,
            page = PageType.PLAYLIST_PAGE,
            currentPage = currentPage,
            focusedColor = colorResource(R.color.playlist_button_focused),
            onPageSelected = onPageSelected
        )
        NavigationTab(
            modifier = Modifier.weight(1f),
            iconRes = albumIconRes,
            page = PageType.ALBUM_PAGE,
            currentPage = currentPage,
            focusedColor = colorResource(R.color.albumlist_button_focused),
            onPageSelected = onPageSelected
        )
        NavigationTab(
            modifier = Modifier.weight(1f),
            iconRes = songIconRes,
            page = PageType.SONG_PAGE,
            currentPage = currentPage,
            focusedColor = colorResource(R.color.songlist_button_focused),
            onPageSelected = onPageSelected
        )
    }
}

/**
 * Single tab button inside [NavigationControl].
 *
 * Applies [focusedColor] as the icon background when [page] matches [currentPage],
 * otherwise falls back to the unfocused gray. Stateless.
 *
 * @param modifier Modifier applied to the [IconButton].
 * @param iconRes Drawable resource used as the tab icon.
 * @param page The [PageType] this tab represents.
 * @param currentPage The currently active [PageType].
 * @param focusedColor Background color shown when this tab is active.
 * @param onPageSelected Callback invoked with [page] when the tab is tapped.
 */
@Composable
private fun NavigationTab(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    page: PageType,
    currentPage: PageType,
    focusedColor: Color,
    onPageSelected: (PageType) -> Unit
) {
    // Use the page-specific focus color when active, gray otherwise
    val backgroundColor = if (page == currentPage) focusedColor
    else colorResource(R.color.unfocused_button)

    IconButton(
        onClick = { onPageSelected(page) },
        modifier = modifier.background(backgroundColor)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun NavigationControlPreview() {
    NavigationControl(
        currentPage = PageType.PLAYLIST_PAGE,
        queueIconRes = R.drawable.queue_icon,
        playerIconRes = R.drawable.play_circle_outline,
        playlistIconRes = R.drawable.playlist_icon,
        albumIconRes = R.drawable.browse_album_icon,
        songIconRes = R.drawable.album_icon,
        onPageSelected = {}
    )
}
