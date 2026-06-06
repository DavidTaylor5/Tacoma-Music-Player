package com.andaagii.tacomamusicplayer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andaagii.tacomamusicplayer.R

/**
 * Full-screen empty-state or informational view split into two tappable halves.
 *
 * Stateless — the caller supplies icons, info strings, and click callbacks for each half.
 * A white horizontal divider separates the two sections. Commonly used when a list page
 * has no content and needs to prompt the user toward two different actions.
 *
 * @param modifier Modifier applied to the root [Column].
 * @param firstIcon [Painter] for the icon displayed in the top half.
 * @param firstInfo Descriptive text shown below the first icon.
 * @param onFirstClick Called when the user taps anywhere in the top half.
 * @param secondIcon [Painter] for the icon displayed in the bottom half.
 * @param secondInfo Descriptive text shown below the second icon.
 * @param onSecondClick Called when the user taps anywhere in the bottom half.
 */
@Composable
fun InformationScreen(
    modifier: Modifier = Modifier,
    firstIcon: Painter,
    firstInfo: String,
    onFirstClick: () -> Unit,
    secondIcon: Painter,
    secondInfo: String,
    onSecondClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top half — tappable area for the first action
        InformationHalf(
            modifier = Modifier.weight(1f),
            icon = firstIcon,
            info = firstInfo,
            onClick = onFirstClick
        )

        // White divider matching the original layout separator
        HorizontalDivider(color = Color.White, thickness = 1.dp)

        // Bottom half — tappable area for the second action
        InformationHalf(
            modifier = Modifier.weight(1f),
            icon = secondIcon,
            info = secondInfo,
            onClick = onSecondClick
        )
    }
}

/**
 * Single tappable half of [InformationScreen] containing a centred icon and label.
 *
 * @param modifier Modifier applied to the [Box] container.
 * @param icon [Painter] for the icon shown at the top of the half.
 * @param info Descriptive text below the icon.
 * @param onClick Called when the user taps this half.
 */
@Composable
private fun InformationHalf(
    modifier: Modifier = Modifier,
    icon: Painter,
    info: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InformationScreenPreview() {
    InformationScreen(
        firstIcon = painterResource(R.drawable.album_icon),
        firstInfo = "No albums found. Tap to scan your library.",
        onFirstClick = {},
        secondIcon = painterResource(R.drawable.playlist_icon),
        secondInfo = "No playlists yet. Tap to create one.",
        onSecondClick = {}
    )
}
