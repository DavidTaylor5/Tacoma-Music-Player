package com.andaagii.tacomamusicplayer.composables

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.activity.MainActivity
import com.andaagii.tacomamusicplayer.enumtype.ScreenType
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel

/**
 * Root composable for `MainActivity.setContent`. Owns the [NavHost] with two destinations
 * ([ScreenType.MUSIC_CHOOSER_SCREEN] and [ScreenType.PERMISSION_DENIED_SCREEN]), a full-screen
 * loading overlay, and side-effect [LaunchedEffect]s for navigation events, keyboard dismissal,
 * and permission initialisation.
 *
 * Replaces the `ActivityMainBinding` + `NavHostFragment` layer from
 * [com.andaagii.tacomamusicplayer.activity.MainActivity].
 *
 * @param viewModel The activity-scoped [MainViewModel] created in [MainActivity].
 */
@Composable
fun TacomaMusicPlayerApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val showLoadingScreen by viewModel.showLoadingScreen.collectAsStateWithLifecycle()

    // Consume one-shot screen navigation events from the ViewModel.
    LaunchedEffect(Unit) {
        viewModel.screenState.collect { screenType ->
            navController.navigate(screenType.route())
        }
    }

    // Forward keyboard-dismiss requests from the ViewModel.
    LaunchedEffect(Unit) {
        viewModel.notifyHideKeyboard.collect {
            keyboardController?.hide()
        }
    }

    // Observe permission state and trigger music init once permission is granted.
    LaunchedEffect(Unit) {
        viewModel.isAudioPermissionGranted.collect { isGranted ->
            isGranted ?: return@collect
            if (!isGranted) {
                AppPermissionUtil().requestReadMediaAudioPermission(context)
            } else {
                viewModel.initializeMusicPlaying()
                (context as MainActivity).queryMusic()
            }
        }
    }

    // Mirror the old OnBackPressedCallback: pop the nav stack; finish the activity if empty.
    BackHandler {
        if (!navController.popBackStack()) {
            (context as Activity).finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ScreenType.MUSIC_CHOOSER_SCREEN.route()
        ) {
            composable(ScreenType.MUSIC_CHOOSER_SCREEN.route()) {
                MusicChooserScreen()
            }
            composable(ScreenType.PERMISSION_DENIED_SCREEN.route()) {
                PermissionDeniedScreen(
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
                )
            }
        }

        if (showLoadingScreen) {
            AppLoadingScreen()
        }
    }
}

/**
 * Full-screen black loading overlay shown while the app is initialising.
 *
 * Mirrors the `loading_screen` layout from the deleted `activity_main.xml`.
 */
@Composable
private fun AppLoadingScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                modifier = Modifier.wrapContentSize().padding(bottom = 30.dp)
            ) {
                AsyncImage(
                    model = R.drawable.app_play_store_512,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Tacoma Music Player",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}
