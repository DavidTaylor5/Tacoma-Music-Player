package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andaagii.tacomamusicplayer.R

/**
 * Settings/options dialog shell.
 *
 * Stateless — shell for future settings options. The only functional element is "Set alternate Art".
 * Additional options should be
 * added here as the settings feature is fleshed out.
 *
 * @param modifier Modifier applied to the root [Column].
 * @param onApplyClick Called when the user taps APPLY to confirm settings changes.
 * @param onCloseClick Called when the user taps CLOSE to dismiss without changes.
 */
@Composable
fun SettingsPrompt(
    modifier: Modifier = Modifier,
    onApplyClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sorting_options_title),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        // Placeholder setting option — expand this section when settings are implemented
        Text(
            text = stringResource(R.string.set_alternate_art),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider(color = Color.DarkGray)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCloseClick) {
                Text(text = stringResource(R.string.close_button), color = Color.White)
            }
            TextButton(onClick = onApplyClick) {
                Text(text = stringResource(R.string.apply_button), color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun SettingsPromptPreview() {
    SettingsPrompt(
        onApplyClick = {},
        onCloseClick = {}
    )
}
