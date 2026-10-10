package com.andaagii.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class CurrentQueueFragment: Fragment() {
    private lateinit var binding: FragmentCurrentQueueBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    //TODO have the queue exist in the viewmodel, have the fragment observe the queue and update adapter

    //Adds functionality for moving items around the recyclerview.
    private val itemTouchHelper by lazy {
        /*
        1. Specify all 4 directions, specifying START and END also allows more organic dragging
        than just specifying UP and DOWN.
         */
        val simpleItemTouchCallback =
        object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

            var currFrom: Int? = null
            var currTo: Int? = null

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                //When an item is being dragged, I set alpha to .5
                if(actionState == ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }

                if(actionState == ACTION_STATE_IDLE) {
                    currFrom?.let { from ->
                        currTo?.let { to ->
                            parentViewModel.moveInQueue(from, to)
                            currFrom = null
                            currTo = null
                        }
                    }
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

                if(currFrom == null) currFrom = from
                currTo = to

                Timber.d("onMove: from=$from, to=$to")
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                /*
                4. Code block for horizontal swipe. ItemTouchHelper handles horizontal swipes
                as well, but it is not relevant to reordering. Ignore
                 */
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
        binding.displayRecyclerview.adapter = QueueListAdapter(
            handleSongSetting = this::handleSongSetting,
            onHandleDrag = this::handleViewHolderHandleDrag,
            playSongAtPosition = this::playSongAtPosition
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.positionStateFlow.collect { state ->
                    (binding.displayRecyclerview.adapter as QueueListAdapter).submitList(
                        state.queue.mapIndexed { index, mediaItem ->
                            DisplaySong(
                                mediaItem = mediaItem,
                                showPlayIndicator = index == state.songPosition
                            )
                        }
                    )
                }
            }
        }

        binding.clearQueue.setOnClickListener {
            parentViewModel.clearQueue()
        }

        //For smoother scrolling I keep 30 viewholders saved in memory offscreen. optimized for ~40
        //TODO Remove this code when I implement coil or glide....
        binding.displayRecyclerview.setItemViewCacheSize(30)
        itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)

        setupPage()

        return binding.root
    }

    /**
     * Shows a prompt for the user to choose a playlist or album.
     * Should show when there is no songs in the current song list, not an empty playlist.
     */
    private fun determineIfShowingEmptyPlaylistScreen(songs: List<MediaItem>) {
        if(songs.isEmpty()){
            binding.noMusicAddedText.visibility = View.VISIBLE
        } else {
            binding.noMusicAddedText.visibility = View.GONE
        }
    }

    private fun playSongAtPosition(position: Int) {
        parentViewModel.playQueueAtPosition(
            position
        )
    }

    private fun handleViewHolderHandleDrag(viewHolder: ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    //TODO update this later...
    private fun handleSongSetting(menuOption: MenuOptionUtil.MenuOption, mediaItems: List<MediaItem> = listOf()) {
        when (menuOption) {
            MenuOptionUtil.MenuOption.CLEAR_QUEUE -> {
                parentViewModel.clearQueue()
            }
            MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST -> {
                //TODO ADD to playlist code
                //parentViewModel.addSongsToAPlaylist()
            }
            else -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
        }
    }

    private fun setupPage() {
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}