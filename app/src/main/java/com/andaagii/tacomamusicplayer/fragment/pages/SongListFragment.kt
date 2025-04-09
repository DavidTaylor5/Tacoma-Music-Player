package com.andaagii.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
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
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.SongListAdapter
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.databinding.FragmentSonglistBinding
import com.andaagii.tacomamusicplayer.enum.PageType
import com.andaagii.tacomamusicplayer.enum.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.PLAY_SONG_GROUP
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.ADD_TO_QUEUE
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.CHECK_STATS
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.viewmodel.SongListViewModel
import timber.log.Timber

class SongListFragment(

): Fragment() {
    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: SongListViewModel by viewModels()

    private var currentSongGroup:  SongGroup? = null

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
                    val adapter = recyclerView.adapter as SongListAdapter
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition

                    Timber.d("onMove: from=$from, to=$to")

                    /*
                    2. Update the backing model. Custom implementation in SongListAdapter. You need to
                    implement reordering of the backing model inside the method.
                     */
                    adapter.moveItem(from, to)
                    //parentViewModel.swapAdjacentSongsInQueue(from, to) //TODO this code is somewhat redundant...

                    // Update the mediaController playlist
                    //TODO what should this update?
                    //parentViewModel.mediaController.value?.moveMediaItem(from, to)

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

    override fun onPause() {
        super.onPause()

        //If it's a playlist, save the order to the database [it could have changed.]
        currentSongGroup?.let { songGroup ->

            val finalSongOrder = (binding.displayRecyclerview.adapter as SongListAdapter).getSongOrder()

            if(determineIfPlaylistSongsHaveChanged(songGroup.songs, finalSongOrder)
                && songGroup.type == SongGroupType.PLAYLIST) {
                songGroup.songs = finalSongOrder
                parentViewModel.updatePlaylistOrder(songGroup)
            }
        }
    }

    private fun determineIfPlaylistSongsHaveChanged(originalSongOrder: List<MediaItem>, finalSongOrder: List<MediaItem>): Boolean {
        return originalSongOrder != finalSongOrder
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSonglistBinding.inflate(inflater)

        parentViewModel.currentSongList.observe(viewLifecycleOwner) { songGroup ->
            Timber.d("onCreateView: title=${songGroup.title}, songs.size=${songGroup.songs.size}")

            currentSongGroup = songGroup

            binding.displayRecyclerview.adapter = SongListAdapter(
                songGroup.songs,
                this::handleSongSetting,
                this::handleSongClicked,
                this::handleSongSelected,
                songGroup.type,
                this::handleViewHolderHandleDrag
            )
            determineIfShowingInformationScreen(songGroup.songs, songGroup.type)

            binding.songGroupInfo.setSongGroupTitleText(songGroup.title)

            // Determine what icon to display for song group
            if(songGroup.type == SongGroupType.ALBUM && songGroup.songs.isNotEmpty()) {
                songGroup.songs[0].mediaMetadata.artworkUri?.let { songArt ->
                    val ableToDraw = UtilImpl.drawUriOntoImageView(
                        binding.songGroupInfo.getSongGroupImage(),
                        songArt,
                        Size(200, 200)
                    )

                    if(!ableToDraw) {
                        binding.songGroupInfo.getSongGroupImage()
                            .setImageResource(R.drawable.white_note)
                    }
                }

                //Remove drag ability from songs in an album.
                itemTouchHelper.attachToRecyclerView(null)

            } else { // Playlist icon
                UtilImpl.setPlaylistImageFromAppStorage(binding.songGroupInfo.getSongGroupImage(), songGroup.title)

                //Adds drag ability to songs in a playlist.
                itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)
            }
        }

        parentViewModel.isShowingSearchMode.observe(viewLifecycleOwner) { isShowing ->
            if(isShowing) {
                activateSearchMode()
            } else {
                deactivateSearchMode()
            }
        }

        parentViewModel.availablePlaylists.observe(viewLifecycleOwner) { playlists ->
            binding.playlistPrompt.setPlaylistData(playlists)
        }

        viewModel.isShowingPlaylistPrompt.observe(viewLifecycleOwner) { isShowing ->
            if(isShowing) {
                binding.playlistPrompt.visibility = View.VISIBLE
            } else {
                binding.playlistPrompt.visibility = View.GONE
            }
        }

        binding.songGroupInfo.setOnPlayIconPressed {
            handlePlaySongGroup()
        }

        binding.songGroupInfo.setOnMenuIconPressed {
            val menu = PopupMenu(binding.root.context, binding.songGroupInfo.getMenuIconView())

            menu.menuInflater.inflate(R.menu.songlist_songgroup_options, menu.menu)
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

        viewModel.currentlySelectedSongs.observe(viewLifecycleOwner) { currentlySelectedSongs ->
            binding.multiSelectPrompt.setPromptText("${currentlySelectedSongs.size} songs selected")
        }

        binding.multiSelectPrompt.setOnMenuIconClick {
            val menu = PopupMenu(this.context, binding.multiSelectPrompt)

            menu.menuInflater.inflate(R.menu.songlist_songgroup_options, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(this.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                //I don't need to add any more songs here, already added when selected.
                handleSongSetting(MenuOptionUtil.determineMenuOptionFromTitle(it.title.toString()), listOf())
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        setupCreatePlaylistPrompt()
        setupPlaylistPrompt()

        setupPage()

        return binding.root
    }

    private fun activateSearchMode() {
        //TODO setup a search mode in my app
        // Update the song group to display song search info
        // Update the adapter to use the searchListAdapter
    }

    private fun deactivateSearchMode() {
        //Essentially set this back to a clean slate...
    }

    private fun handleViewHolderHandleDrag(viewHolder: ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun setupCreatePlaylistPrompt() {
        //set playlist prompt hint
        binding.createPlaylistPrompt.setTextInputHint(Const.NEW_PLAYLIST_HINT)

        //Option 1 Button will be Cancel the prompt
        binding.createPlaylistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.createPlaylistPrompt.setOption1ButtonOnClick {
            binding.createPlaylistPrompt.closePrompt()
            viewModel.clearPreparedSongsForPlaylists()
        }

        //Option 2 Button will be add a new playlist
        binding.createPlaylistPrompt.setOption2ButtonText(Const.ADD)
        binding.createPlaylistPrompt.setOption2ButtonOnClick {
            parentViewModel.createNamedPlaylist(binding.createPlaylistPrompt.getUserInputtedText())
        }
    }

    /**
     * Sets up the adding a song to a Playlist Prompt functionality.
     */
    private fun setupPlaylistPrompt() {
        //When add button is clicked, I should add songs into playlists
        binding.playlistPrompt.onAddButtonClick {
            val checkedPlaylists: List<String> = viewModel.checkedPlaylists.value ?: listOf()
            val playlistAddSongs: List<MediaItem> = viewModel.currentlySelectedSongs.value ?: listOf()

            parentViewModel.addSongsToAPlaylist(
                checkedPlaylists,
                playlistAddSongs
            )

            viewModel.clearPreparedSongsForPlaylists()
            binding.playlistPrompt.closePrompt()
        }

        binding.playlistPrompt.onCreateNewPlaylistClicked {
            binding.createPlaylistPrompt.showPrompt()
        }
        binding.playlistPrompt.onCloseButtonClicked {
            binding.createPlaylistPrompt.closePrompt()
            binding.playlistPrompt.closePrompt()
        }

        binding.playlistPrompt.setPlaylistCheckedHandler { playlistTitle, isChecked ->
            viewModel.updateCheckedPlaylists(playlistTitle, isChecked)
        }

        viewModel.isShowingMultiSelectPrompt.observe(viewLifecycleOwner) { isShowing ->
            if(isShowing) {
                binding.multiSelectPrompt.visibility = View.VISIBLE
            } else {
                binding.multiSelectPrompt.visibility = View.GONE
            }
        }

        viewModel.isPlaylistPromptAddClickable.observe(viewLifecycleOwner) { isClickable ->
            binding.playlistPrompt.updateAddButtonClickability(isClickable)
        }
    }

    //TODO move this logic ?
    private fun handleSongSetting(menuOption: MenuOptionUtil.MenuOption, mediaItems: List<MediaItem>) {
        when (menuOption) {
            PLAY_SONG_GROUP -> handlePlaySongGroup()
            ADD_TO_PLAYLIST -> {
                viewModel.prepareSongsForPlaylists()
                handleAddToPlaylist(mediaItems)
            }
            MenuOptionUtil.MenuOption.REMOVE_FROM_PLAYLIST -> {
               // parentViewModel.removeSongsFromPlaylist(currentSongGroup?.title ?: "Unknown Playlist", mediaItems)
                if(mediaItems.isNotEmpty()) {
                    val posOfDeletedSong = (binding.displayRecyclerview.adapter as SongListAdapter).removeSong(mediaItems[0].mediaId)
                    (binding.displayRecyclerview.adapter as SongListAdapter).notifyItemRemoved(posOfDeletedSong)
                }
            }
            ADD_TO_QUEUE -> handleAddToQueue(mediaItems)
            CHECK_STATS -> handleCheckStats()
            else -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
        }
    }

    private fun handlePlaySongGroup() {
        currentSongGroup?.let { songGroup ->
            parentViewModel.playSongGroupAtPosition(songGroup, 0)
        }
    }

    /**
     * When a song is clicked, I should clear the queue and start playing the current song group
     * at the position specified.
     */
    private fun handleSongClicked(position: Int) {
        currentSongGroup?.let { songGroup ->
            parentViewModel.playSongGroupAtPosition(songGroup, position)
        }
    }

    private fun handleSongSelected(mediaItem: MediaItem, isSelected:Boolean) {
        if(isSelected) {
            viewModel.selectSongs(listOf(mediaItem), showPrompt = true)
        } else {
            viewModel.unselectSong(mediaItem)
        }
    }

    private fun handleAddToPlaylist(mediaItems: List<MediaItem>) {
        viewModel.selectSongs(mediaItems, showPrompt = false)
        binding.playlistPrompt.showPrompt()
    }

    private fun handleAddToQueue(mediaItems: List<MediaItem>) {
        parentViewModel.addSongsToEndOfQueue(mediaItems)
    }

    private fun handleCheckStats() {
        //TODO...
    }

    /**
     * Shows a prompt for the user to choose a playlist or album.
     * Should show when there is no songs in the current song list, not an empty playlist.
     */
    private fun determineIfShowingInformationScreen(songs: List<MediaItem>, songGroupType: SongGroupType) {
        //Only show user information screen on app startup [?]
        if( songGroupType != SongGroupType.PLAYLIST && songs.isEmpty()) {
            binding.songListInformationScreen.visibility = View.VISIBLE
            binding.songGroupInfo.visibility = View.GONE
        } else {
            binding.songListInformationScreen.visibility = View.GONE
            binding.songGroupInfo.visibility = View.VISIBLE
        }
    }

    private fun setupPage() {
        binding.songGroupInfo.setSongGroupTitleText("PARTICULAR ALBUM - ARTIST")

        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        //First Icon will be the playlists
        binding.songListInformationScreen.setFirstInfo("Choose a playlist to View")
        binding.songListInformationScreen.setFirstIcon(resources.getDrawable(R.drawable.playlist_icon)) //TODO add theme here?
        binding.songListInformationScreen.setFirstIconCallback { parentViewModel.setPage(PageType.PLAYLIST_PAGE) }

        //Second Icon will be the Albums
        binding.songListInformationScreen.setSecondInfo("Choose an album to View")
        binding.songListInformationScreen.setSecondIcon(resources.getDrawable(R.drawable.browse_album_icon)) //TODO add theme here?
        binding.songListInformationScreen.setSecondIconCallback { parentViewModel.setPage(PageType.ALBUM_PAGE) }
    }
}