package com.andaagii.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.QueueListAdapter
import com.andaagii.tacomamusicplayer.data.DisplaySong
import com.andaagii.tacomamusicplayer.databinding.FragmentCurrentQueueBinding
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Current playback queue page hosted at index 0 in `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Displays the queue as a draggable list using [QueueListAdapter] with [ItemTouchHelper].
 * Two observers drive adapter rebuilds:
 * - [MainViewModel.showLoadingScreen] — initialises the adapter after the music service
 *   has finished loading on first launch.
 * - [MainViewModel.currentlyPlayingSongs] — re-syncs the list whenever the queue changes
 *   externally (e.g., from Android Auto or a ViewModel operation).
 *
 * A third observer on [MainViewModel.currentPlayingSongInfo] updates the playing indicator
 * on the active row without rebuilding the entire adapter.
 */
@AndroidEntryPoint
class CurrentQueueFragment : Fragment() {
    private lateinit var binding: FragmentCurrentQueueBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private var queueAdapter: QueueListAdapter? = null

    /**
     * Provides drag-to-reorder behaviour for the queue RecyclerView.
     *
     * 1. All four directions (UP, DOWN, START, END) are enabled so dragging feels organic —
     *    START/END allow the item to track diagonal finger movements naturally.
     * 2. While dragging, the item's alpha is reduced to 0.5 to indicate it is being moved.
     * 3. On drop ([onMove]), the adapter's backing model and the [MediaController][androidx.media3.session.MediaController]
     *    queue are both updated, then [QueueListAdapter.notifyItemMoved] triggers the
     *    visual animation.
     * 4. Horizontal swipe ([onSwiped]) is intentionally a no-op — swiping is not used for
     *    removal in the queue.
     */
    private var startDrag = -1
    private var endDrag = -1
    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    Timber.d("onSelectedChanged: actionState=$actionState")
                    if (actionState == ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = 0.5f
                    } else if(actionState == ACTION_STATE_IDLE) {
                        if (startDrag != -1) {
                            parentViewModel.mediaController.value?.moveMediaItem(startDrag, endDrag)
                        }
                        startDrag = -1
                        endDrag = -1
                    }
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)
                    viewHolder.itemView.alpha = 1.0f
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {

                    val adapter = recyclerView.adapter as QueueListAdapter
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition

                    if(startDrag == -1) {
                        startDrag = from
                    }

                    endDrag = to

                    Timber.d("onMove: from=$from, to=$to")

                    adapter.moveItem(from, to)

                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // Horizontal swipe is not used for queue row removal; intentionally ignored.
                }
            }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: ")
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_down)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCurrentQueueBinding.inflate(inflater)

        // Initial load — build the adapter once the music service has finished initialising.
        parentViewModel.showLoadingScreen.observe(viewLifecycleOwner) { loadingMusic ->
            Timber.d("onCreateView: loadingMusic=$loadingMusic")
            if (!loadingMusic) {
                parentViewModel.mediaController.value?.let { controller ->
                    val songs = UtilImpl.getSongListFromMediaController(controller)
                    Timber.d("onCreateView: queueSongs=$songs")
                    val displaySongs = songs.map { song -> DisplaySong(song, song == controller.currentMediaItem) }
                    getOrCreateQueueAdapter().submitList(displaySongs)
                    determineIfShowingEmptyPlaylistScreen(songs)
                }
            }
        }

        // Subsequent updates — re-sync the adapter whenever the queue changes externally
        // (e.g., Android Auto adds tracks, or a ViewModel operation replaces the queue).
        parentViewModel.currentlyPlayingSongs.observe(viewLifecycleOwner) { _ ->
            parentViewModel.mediaController.value?.let { controller ->
                val songs = UtilImpl.getSongListFromMediaController(controller)
                Timber.d("onCreateView: queueSongs=$songs")
                val displaySongs = songs.map { song -> DisplaySong(song, song == controller.currentMediaItem) }
                getOrCreateQueueAdapter().submitList(displaySongs)
                determineIfShowingEmptyPlaylistScreen(songs)
            }
        }

        parentViewModel.currentPlayingSongInfo.observe(viewLifecycleOwner) { currSong ->
            queueAdapter?.updateCurrentSongIndicator(currSong)
        }

        parentViewModel.clearQueue.observe(viewLifecycleOwner) { shouldClear ->
            if (shouldClear) {
                queueAdapter?.clearQueue()
                parentViewModel.handledClearningQueue()
            }
        }

        binding.clearQueue.setOnClickListener {
            parentViewModel.clearQueue()
        }

        // Cache 30 off-screen ViewHolders to reduce bind calls during fast scrolling.
        // This trades a small amount of memory for significantly smoother list performance.
        binding.displayRecyclerview.setItemViewCacheSize(30)
        itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)

        setupPage()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
    }

    /**
     * Syncs the [MediaController] queue after the adapter has already removed the item at
     * [songPosition] via [submitList].
     *
     * @param songPosition The former adapter position of the removed track.
     */
    private fun handleRemoveSong(songPosition: Int) {
        parentViewModel.mediaController.value?.removeMediaItem(songPosition)
    }

    /**
     * Shows or hides the empty-state message based on whether the queue has any tracks.
     *
     * Displayed when the queue is empty (e.g., on first launch before any song is played);
     * hidden as soon as at least one track is present.
     *
     * @param songs The current list of tracks in the queue.
     */
    private fun determineIfShowingEmptyPlaylistScreen(songs: List<MediaItem>) {
        if (songs.isEmpty()) {
            binding.noMusicAddedText.visibility = View.VISIBLE
        } else {
            binding.noMusicAddedText.visibility = View.GONE
        }
    }

    /**
     * Seeks the [MediaController] to [position] and begins playback.
     *
     * @param position The zero-based queue index to seek to.
     */
    private fun playSongAtPosition(position: Int) {
        parentViewModel.mediaController.value?.let { controller ->
            controller.seekTo(position, 0L)
            controller.play()
        }
    }

    /** Initiates a drag operation for [viewHolder] via [itemTouchHelper]. */
    private fun handleViewHolderHandleDrag(viewHolder: ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    /**
     * Routes queue-level menu actions to the appropriate handler.
     *
     * Note: [MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST] is not yet implemented for the queue page.
     *
     * @param menuOption The action selected from the popup menu.
     * @param mediaItems The media items associated with the action (may be empty).
     */
    private fun handleSongSetting(menuOption: MenuOptionUtil.MenuOption, mediaItems: List<MediaItem> = listOf()) {
        when (menuOption) {
            MenuOptionUtil.MenuOption.CLEAR_QUEUE -> {
                parentViewModel.clearQueue()
                queueAdapter?.clearQueue()
            }
            MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST -> {
                // Not yet implemented for the queue page.
            }
            else -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
        }
    }

    /** Returns the existing [QueueListAdapter], creating and attaching it to the RecyclerView if needed. */
    private fun getOrCreateQueueAdapter(): QueueListAdapter =
        queueAdapter ?: QueueListAdapter(
            this::handleSongSetting,
            this::handleViewHolderHandleDrag,
            this::handleRemoveSong,
            this::playSongAtPosition
        ).also {
            queueAdapter = it
            binding.displayRecyclerview.adapter = it
        }

    /** Initialises the RecyclerView with a vertical [LinearLayoutManager]. */
    private fun setupPage() {
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
