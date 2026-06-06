package com.andaagii.tacomamusicplayer.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andaagii.tacomamusicplayer.R

/**
 * Full-screen notice shown when the `READ_MEDIA_AUDIO` runtime permission has been denied.
 *
 * Stateless — all interaction is surfaced through [onOpenSettings]. Displays a title, an
 * illustration, an explanation of why the permission is needed, and a button that lets
 * the user navigate directly to the system app-settings page to grant the permission.
 *
 * @param modifier Modifier applied to the root [Column].
 * @param onOpenSettings Called when the user taps "Open Settings"; the caller should launch
 *   the system app-settings page for this app (e.g. via [android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS]).
 */
@Composable
fun PermissionDeniedScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.permission_denied_screen_title),
            color = Color.White,
            fontSize = 35.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(R.drawable.file_icon),
            contentDescription = null,
            modifier = Modifier.size(300.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.read_media_audio_denied_explanation),
            color = Color.White,
            fontSize = 25.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onOpenSettings) {
            Text(text = "Open Settings")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PermissionDeniedScreenPreview() {
    PermissionDeniedScreen(onOpenSettings = {})
}
