package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.andaagii.tacomamusicplayer.R

/** Green accent color matching the original `#4CAF50` CardView tint. */
private val MultiSelectGreen = Color(0xFF4CAF50)

/**
 * Compact toolbar overlay shown during multi-select mode.
 *
 * Stateless — the caller owns visibility and the selection count text. Displays a close
 * icon on the left, a centred description, and a menu icon on the right, matching the
 * 200 × 40 dp `CardView` in the original layout.
 *
 * @param modifier Modifier applied to the root [Card].
 * @param descriptionText Text shown in the centre, typically indicating the selection count.
 * @param onMenuIconClick Called when the user taps the menu (confirm) icon.
 * @param onCloseIconClick Called when the user taps the close icon to exit multi-select mode.
 */
@Composable
fun MultiSelectPrompt(
    modifier: Modifier = Modifier,
    descriptionText: String,
    onMenuIconClick: () -> Unit,
    onCloseIconClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MultiSelectGreen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseIconClick) {
                Icon(
                    painter = painterResource(R.drawable.baseline_clear_24),
                    contentDescription = null,
                    modifier = Modifier.size(35.dp),
                    tint = Color.White
                )
            }

            // Description occupies the space between both icon buttons
            Text(
                text = descriptionText,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onMenuIconClick) {
                Icon(
                    painter = painterResource(R.drawable.menu_icon),
                    contentDescription = null,
                    modifier = Modifier.size(35.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MultiSelectPromptPreview() {
    MultiSelectPrompt(
        descriptionText = "3 selected",
        onMenuIconClick = {},
        onCloseIconClick = {}
    )
}
