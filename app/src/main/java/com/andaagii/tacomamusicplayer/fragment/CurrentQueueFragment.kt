package com.andaagii.tacomamusicplayer.fragment

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
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
import com.andaagii.tacomamusicplayer.enum.ScreenType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class CurrentQueueFragment: Fragment() {
    private lateinit var binding: FragmentCurrentQueueBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    //Adds functionality for moving items around the recyclerview.
    private val itemTouchHelper by lazy {
        /*
        1. Specify all 4 directions, specifying START and END also allows more organic dragging
        than just specifying UP and DOWN.
         */
        val simpleItemTouchCallback =
        object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                //When an item is being dragged, I set alpha to .5
                if(actionState == ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
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

                Timber.d("onMove: from=$from, to=$to")

                /*
                2. Update the backing model. Custom implementation in SongListAdapter. You need to
                implement reordering of the backing model inside the method.
                 */
                adapter.moveItem(from, to)

                // Update the mediaController playlist
                parentViewModel.mediaController.value?.moveMediaItem(from, to)

                // 3. Tell adapter to render the model update.
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

        parentViewModel.mediaController.value?.let { controller ->
            val songs = UtilImpl.getSongListFromMediaController(controller)
            val displaySongs = songs.map {song ->
                if(song == controller.currentMediaItem) {
                    DisplaySong(
                        song,
                        true
                    )
                } else {
                    DisplaySong(
                        song,
                        false
                    )
                }
            }

            binding.displayRecyclerview.adapter = QueueListAdapter(
                displaySongs,
                this::handleSongSetting,
                this::handleViewHolderHandleDrag,
                this::handleRemoveSong,
                this::playSongAtPosition
            )
            determineIfShowingEmptyPlaylistScreen(songs)
        }

        parentViewModel.currentPlayingSongInfo.observe(viewLifecycleOwner) {currSong ->
            (binding.displayRecyclerview.adapter as QueueListAdapter)
                .updateCurrentSongIndicator(currSong)
        }

        parentViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            if(isPlaying) {
                binding.controlButton.setBackgroundResource(R.drawable.baseline_pause_24)
            } else {
                binding.controlButton.setBackgroundResource(R.drawable.white_play_arrow)
            }
        }

        binding.playerSection.setOnClickListener {
            findNavController().navigate(ScreenType.MUSIC_PLAYING_SCREEN.route())
        }

        binding.controlButton.setOnClickListener {
            parentViewModel.flipPlayingState()
        }

        binding.menuIcon.setOnClickListener {
            val menu = PopupMenu(binding.root.context, binding.menuIcon)

            menu.menuInflater.inflate(R.menu.queue_overall_options, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(binding.root.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                handleSongSetting(
                    MenuOptionUtil.determineMenuOptionFromTitle(it.toString()),
                    parentViewModel.currentSongList.value?.songs ?: listOf()
                )
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        //For smoother scrolling I keep 30 viewholders saved in memory offscreen. optimized for ~40
        //TODO Remove this code when I implement coil or glide....
        binding.displayRecyclerview.setItemViewCacheSize(30)
        itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)

        setupPage()

        return binding.root
    }

    private fun handleRemoveSong(songPosition: Int) {
        binding.displayRecyclerview.adapter?.let { adapter ->
            adapter.notifyItemRemoved(songPosition)
            parentViewModel.mediaController.value?.removeMediaItem(songPosition)
        }
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
        parentViewModel.mediaController.value?.let {controller ->
            controller.seekTo(position, 0L)
            controller.play()
        }
    }

    private fun handleViewHolderHandleDrag(viewHolder: ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    //TODO update this later...
    private fun handleSongSetting(menuOption: MenuOptionUtil.MenuOption, mediaItems: List<MediaItem> = listOf()) {
        when (menuOption) {
            MenuOptionUtil.MenuOption.CLEAR_QUEUE -> {
                parentViewModel.clearQueue()
                (binding.displayRecyclerview.adapter as QueueListAdapter).clearQueue()
            }
            MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST -> {
                //TODO ADD to playlist code
            }
            else -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
        }
    }

    private fun setupPage() {
        //binding.sectionTitle.text = "PARTICULAR ALBUM - ARTIST"

        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}