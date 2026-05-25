package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Generic two-button dialog with a single text input field.
 *
 * Stateful — the typed text is managed internally. Both callbacks receive the current
 * input text so callers do not need to hold separate input state. Commonly used for naming
 * new playlists or renaming existing ones.
 *
 * @param modifier Modifier applied to the outermost [Column].
 * @param hint Placeholder / label text shown inside the text field.
 * @param option1Text Label for the first (typically cancel/dismiss) button.
 * @param option2Text Label for the second (typically confirm/submit) button.
 * @param onOption1Click Called with the current input text when the first button is tapped.
 * @param onOption2Click Called with the current input text when the second button is tapped.
 */
@Composable
fun InputTextPrompt(
    modifier: Modifier = Modifier,
    hint: String = "",
    option1Text: String = "",
    option2Text: String = "",
    onOption1Click: (inputText: String) -> Unit = {},
    onOption2Click: (inputText: String) -> Unit = {}
) {
    // Owned locally so the caller does not need to manage the transient input string
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier.width(250.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(text = hint, color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { onOption1Click(text) }) {
                Text(text = option1Text, color = Color.White)
            }
            TextButton(onClick = { onOption2Click(text) }) {
                Text(text = option2Text, color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InputTextPromptPreview() {
    InputTextPrompt(
        hint = "Playlist name",
        option1Text = "CANCEL",
        option2Text = "CREATE"
    )
}
