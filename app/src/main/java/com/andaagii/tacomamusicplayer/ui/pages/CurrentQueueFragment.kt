package com.andaagii.tacomamusicplayer.ui.pages

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
import com.andaagii.tacomamusicplayer.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class CurrentQueueFragment: Fragment() {
    private lateinit var binding: FragmentCurrentQueueBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    private val itemTouchHelper by lazy {
        /*
        1. Specify all 4 directions, specifying START and END also allows more organic dragging
        than just specifying UP and DOWN.
         */
        val simpleItemTouchCallback =
        object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {
            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                target: ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition

                Timber.d("onMove: from=$from, to=$to")
                parentViewModel.moveInQueue(from, to)
                return true
            }

            override fun onSwiped(
                viewHolder: ViewHolder,
                direction: Int
            ) {
                TODO("Not yet implemented")
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

        binding.displayRecyclerview.setItemViewCacheSize(30)
        itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)
        setupPage()
        return binding.root
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