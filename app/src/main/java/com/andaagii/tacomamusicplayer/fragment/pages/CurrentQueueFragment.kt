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
import com.andaagii.tacomamusicplayer.composables.CurrentQueueScreen
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Thin shell fragment that hosts the [CurrentQueueScreen] composable.
 *
 * This fragment contains no UI logic of its own — all state is collected from [MainViewModel]
 * inside the `setContent` block using `collectAsStateWithLifecycle()`. Playback commands
 * (seek, remove, reorder) are forwarded directly to the active
 * [androidx.media3.session.MediaController]; queue clearing is delegated to [MainViewModel].
 */
@AndroidEntryPoint
class CurrentQueueFragment : Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val songs      by parentViewModel.currentlyPlayingSongs.collectAsStateWithLifecycle()
                val songInfo   by parentViewModel.currentPlayingSongInfo.collectAsStateWithLifecycle()
                val controller by parentViewModel.mediaController.collectAsStateWithLifecycle()

                CurrentQueueScreen(
                    songs                 = songs,
                    currentlyPlayingTitle = songInfo?.songTitle,
                    onSongClick           = { pos ->
                        controller?.seekTo(pos, 0L)
                        controller?.play()
                    },
                    onRemoveSong          = { pos -> controller?.removeMediaItem(pos) },
                    onMoveItem            = { from, to -> controller?.moveMediaItem(from, to) },
                    onClearQueue          = parentViewModel::clearQueue
                )
            }
        }
    }
}
