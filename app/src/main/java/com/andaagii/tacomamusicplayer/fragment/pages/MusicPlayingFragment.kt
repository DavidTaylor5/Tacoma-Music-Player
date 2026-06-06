package com.andaagii.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andaagii.tacomamusicplayer.composables.MusicPlayingScreen
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Thin shell fragment that hosts the [MusicPlayingScreen] composable.
 *
 * This fragment contains no UI logic of its own — all state is collected from [MainViewModel]
 * inside the `setContent` block using `collectAsStateWithLifecycle()`, and all user interactions
 * are forwarded to [MainViewModel] or the active [androidx.media3.session.MediaController].
 *
 * Swipe and double-tap gestures that collapse the player are handled by the parent
 * `PlayerDisplayFragment`.
 */
@AndroidEntryPoint
class MusicPlayingFragment : Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val songInfo     by parentViewModel.currentPlayingSongInfo.collectAsStateWithLifecycle()
                val isPlaying    by parentViewModel.isPlaying.collectAsStateWithLifecycle()
                val loopMode     by parentViewModel.loopMode.collectAsStateWithLifecycle()
                val shuffleMode  by parentViewModel.shuffleMode.collectAsStateWithLifecycle()
                val controller   by parentViewModel.mediaController.collectAsStateWithLifecycle()

                MusicPlayingScreen(
                    songInfo           = songInfo,
                    isPlaying          = isPlaying,
                    loopMode           = loopMode,
                    shuffleMode        = shuffleMode,
                    mediaController    = controller,
                    onPreviousSong     = { controller?.seekToPrevious() },
                    onSeekBack         = { controller?.seekBack() },
                    onTogglePlay       = parentViewModel::flipPlayingState,
                    onSeekForward      = { controller?.seekForward() },
                    onNextSong         = { controller?.seekToNextMediaItem() },
                    onFlipLoopMode     = parentViewModel::flipLoopMode,
                    onFlipShuffleState = parentViewModel::flipShuffleState
                )
            }
        }
    }
}
