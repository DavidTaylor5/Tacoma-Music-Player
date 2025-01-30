package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.tacomamusicplayer.adapter.QueueListAdapter
import com.example.tacomamusicplayer.databinding.FragmentCurrentQueueBinding
import com.example.tacomamusicplayer.util.SongSettingsUtil
import com.example.tacomamusicplayer.viewmodel.CurrentQueueViewModel
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class CurrentQueueFragment: Fragment() {

    //TODO what to do if the current song list is empty?

    private lateinit var binding: FragmentCurrentQueueBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: CurrentQueueViewModel by viewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCurrentQueueBinding.inflate(inflater)

        //TODO I need to also update mediaController when the queue items are moved around.
        //TODO I need to also update the UI for song handle so it doesn't look terrible...
        //TODO I shouldn't have the handle on albums but I can have the handle on playlists...

        //TODO I'll instead query the current mediaItem list -> this can be a playlist or an album of songs
        parentViewModel.songQueue.observe(viewLifecycleOwner) {songs ->
            Timber.d("onCreateView: songs.size=${songs.size}")
            binding.displayRecyclerview.adapter = QueueListAdapter( //TODO I need a different adapter and viewholder for queue fragment
                songs,
                this::handleSongSetting,
                this::handleViewHolderHandleDrag
            )
            determineIfShowingEmptyPlaylistScreen(songs)
        }

        itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)

        //TODO update with queue information... [sometimes I will play a queue, a playlist, or random songs]
        //Maybe I should just remove this in general?
//        parentViewModel.songListTitle.observe(viewLifecycleOwner) { title ->
//            binding.sectionTitle.text = title
//        }

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

    private fun handleViewHolderHandleDrag(viewHolder: ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    //TODO update this later...
    private fun handleSongSetting(setting: SongSettingsUtil.Setting, mediaItems: List<MediaItem> = listOf()) {
//        when (setting) {
//            SongSettingsUtil.Setting.ADD_TO_PLAYLIST -> handleAddToPlaylist()
//            SongSettingsUtil.Setting.ADD_TO_QUEUE -> handleAddToQueue(mediaItem)
//            SongSettingsUtil.Setting.CHECK_STATS -> handleCheckStats()
//            SongSettingsUtil.Setting.UNKNOWN -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
//        }
    }

    private fun setupPage() {
        //binding.sectionTitle.text = "PARTICULAR ALBUM - ARTIST"

        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}