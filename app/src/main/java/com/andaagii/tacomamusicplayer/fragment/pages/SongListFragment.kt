package com.andaagii.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Size
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.ADD_TO_QUEUE
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.CHECK_STATS
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.PLAY_SONG_GROUP
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil.MenuOption.REMOVE_FROM_PLAYLIST
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.viewmodel.SongListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SongListFragment(): Fragment() {
    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: SongListViewModel by viewModels()

    private var currentSongGroup:  SongGroup? = null
    private var lastDisplaySongGroup: SongGroup? =  null

    private var songsToAddToPlaylistPrompt: List<MediaItem>? = null

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

                    //Save playlist change.
                    savePlaylistChanges()

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
        Timber.d("onPause: ")
        super.onPause()
        //Remove multi select when I leave this fragment
        viewModel.clearMultiSelectSongs()
    }

    private fun savePlaylistChanges() {
        currentSongGroup?.let {  songGroup ->
            if(songGroup.type == SongGroupType.PLAYLIST) {
                val finalSongOrder = (binding.displayRecyclerview.adapter as SongListAdapter).getSongOrder()

                if(determineIfPlaylistSongsHaveChanged(songGroup.songs, finalSongOrder)) {
                    songGroup.songs = finalSongOrder
                    parentViewModel.updatePlaylistOrder(songGroup)
                }
            } else {
                Timber.d("savePlaylistChanges: songGroup=$songGroup is not of type PLAYLIST, therefore no save.")
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

        parentViewModel.currentSongGroup.observe(viewLifecycleOwner) { songGroup ->
            Timber.d("onCreateView: title=${songGroup.group.mediaMetadata.albumTitle}")

            currentSongGroup = songGroup
            lastDisplaySongGroup = songGroup

            parentViewModel.handleCancelSearchButtonClick()

            //TODO pass in just the songGroup
            binding.displayRecyclerview.adapter = SongListAdapter(
                songGroup.songs,
                this::handleSongSetting,
                this::handleSongClicked,
                this::handleAlbumClicked,
                this::handlePlaylistClicked,
                this::handleSongSelected,
                songGroup.type,
                this::handleViewHolderHandleDrag
            )
            determineIfShowingInformationScreen(songGroup)

            initializeSongGroupInfo()
        }

        parentViewModel.currentSearchList.observe(viewLifecycleOwner) { searchItems ->
            currentSongGroup?.let { songGroup ->
                if(songGroup.type != SongGroupType.SEARCH_LIST) {
                    Timber.d("onCreateView: saving currentSongGroup to lastDisplaySongGroup")
                    lastDisplaySongGroup = songGroup
                }
            }

            val searchMediaItem = MediaItem.Builder().setMediaId("Search").setMediaMetadata(
                MediaMetadata.Builder().setTitle("Search").build()
            ).build()

            //if(binding.displayRecyclerview.adapter)
            //TODO set the currentSongGroup to be the search data...
            currentSongGroup = SongGroup(
                type = SongGroupType.SEARCH_LIST,
                songs = searchItems,
                group = searchMediaItem,
            )

            if(binding.displayRecyclerview.adapter == null) {
                currentSongGroup?.let { songGroup ->
                    binding.displayRecyclerview.adapter = SongListAdapter(
                        songGroup.songs,
                        this::handleSongSetting,
                        this::handleSongClicked,
                        this::handleAlbumClicked,
                        this::handlePlaylistClicked,
                        this::handleSongSelected,
                        songGroup.type,
                        this::handleViewHolderHandleDrag
                    )
                    determineIfShowingInformationScreen(songGroup)
                }
            } else {
                (binding.displayRecyclerview.adapter as SongListAdapter).setSongs(searchItems, SongGroupType.SEARCH_LIST)
            }
        }

        parentViewModel.isShowingSearchMode.observe(viewLifecycleOwner) { isShowing ->
            if(isShowing) {
                activateSearchMode()
                deactivateDisplayMode()
                removeInformationScreen()
            } else {
                deactivateSearchMode()
                activateDisplayMode()
            }

            //Clear the favorite list when changing modes
            binding.displayRecyclerview.adapter?.let { adapter ->
                (adapter as SongListAdapter).clearAllSelected()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.availablePlaylists.collect { playlists ->
                    val playlistsWithoutQueue = playlists.filter { playlist ->
                        playlist.mediaMetadata.albumTitle != Const.PLAYLIST_QUEUE_TITLE && playlist.mediaMetadata.albumTitle != Const.ORIGINAL_QUEUE_ORDER
                    }
                    binding.playlistPrompt.setPlaylistData(playlistsWithoutQueue)
                }
            }
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
            val menu = PopupMenu(
                binding.root.context,
                binding.songGroupInfo.getMenuIconView(),
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )

            menu.menuInflater.inflate(
                R.menu.songlist_songgroup_options,
                menu.menu
            )

            menu.setOnMenuItemClickListener {
                Toast.makeText(binding.root.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                handleSongSetting(
                    MenuOptionUtil.determineMenuOptionFromTitle(it.toString()),
                    parentViewModel.currentSongGroup.value?.songs ?: listOf()
                )
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        viewModel.currentlySelectedSongs.observe(viewLifecycleOwner) { currentlySelectedSongs ->
            binding.multiSelectPrompt.setPromptText("${currentlySelectedSongs.size} songs selected")

            if(currentlySelectedSongs.isEmpty()) {
                Timber.d("onCreateView: set multiselectPrompt to GONE")
                binding.multiSelectPrompt.visibility = View.INVISIBLE //it appears for custom views, setting invisible is better than gone?
            } else {
                Timber.d("onCreateView: set multiselectPrompt to VISIBLE")
                binding.multiSelectPrompt.visibility = View.VISIBLE
            }

            binding.displayRecyclerview.adapter?.let { adapter ->
                if(currentlySelectedSongs.isEmpty()) {
                    (adapter as SongListAdapter).clearAllSelected()
                }
            }
        }

        binding.multiSelectPrompt.setOnMenuIconClick {
            val menu = PopupMenu(
                this.context,
                binding.multiSelectPrompt,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )

            //Different multi-select options for Playlists versus Albums
            if(currentSongGroup?.type == SongGroupType.PLAYLIST) {
                menu.menuInflater.inflate(R.menu.multi_select_playlist_options, menu.menu)
            } else {
                menu.menuInflater.inflate(R.menu.multi_select_album_options, menu.menu)
            }

            menu.setOnMenuItemClickListener {
                Toast.makeText(this.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                //I don't need to add any more songs here, already added when selected.
                handleSongSetting(MenuOptionUtil.determineMenuOptionFromTitle(it.title.toString()),  viewModel.currentlySelectedSongs.value ?: listOf())
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        binding.multiSelectPrompt.setOnCloseIconClick {
            binding.displayRecyclerview.adapter?.let { adapter ->
                (adapter as SongListAdapter).clearAllSelected()
                viewModel.clearMultiSelectSongs()
                binding.createPlaylistPrompt.closePrompt()
                binding.playlistPrompt.closePrompt()
            }
        }

        binding.searchEditText.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN )) {

                parentViewModel.removeVirtualKeyboard()
                binding.searchEditText.clearFocus()
                true
            } else {
                false
            }
        }
        
        binding.searchEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Timber.d("onTextChanged: User is typing: $s")
                if(parentViewModel.isShowingSearchMode.value == true) {
                    parentViewModel.querySearchData(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }

        setupCreatePlaylistPrompt()
        setupPlaylistPrompt()

        setupPage()

        return binding.root
    }

    private fun initializeSongGroupInfo() {
        Timber.d("initializeSongGroupInfo: ")
        currentSongGroup?.let { songGroup ->
            binding.songGroupInfo.setSongGroupTitleText(songGroup.group.mediaMetadata.albumTitle.toString())

            // Determine what icon to display for song group
            if(songGroup.type == SongGroupType.ALBUM && songGroup.songs.isNotEmpty()) {
                songGroup.songs[0].mediaMetadata.artworkUri?.let { songArt ->
                    val customImage = "album_${songGroup.songs[0].mediaMetadata.albumTitle}"
                    UtilImpl.drawMediaItemArt(
                        binding.songGroupInfo.getSongGroupImage(),
                        songArt,
                        Size(200, 200),
                        customImage
                    )
                }

                //Remove drag ability from songs in an album.
                itemTouchHelper.attachToRecyclerView(null)

            } else { // Playlist icon
                val ableToDraw = UtilImpl.setPlaylistImageFromAppStorage(binding.songGroupInfo.getSongGroupImage(), songGroup.group.mediaMetadata.albumTitle.toString())

                if(!ableToDraw) {
                    binding.songGroupInfo.getSongGroupImage()
                        .setImageResource(R.drawable.white_note)
                }

                //Adds drag ability to songs in a playlist.
                itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)
            }
        }
    }

    private fun clearCurrentSongs() {
        Timber.d("clearCurrentSongs: ")
        binding.displayRecyclerview.adapter?.let { adapter ->
            (adapter as SongListAdapter).setSongs(listOf(), SongGroupType.SEARCH_LIST)
        }
    }

    private fun restoreLastDisplaySongs() {
        Timber.d("restoreLastDisplaySongs: ")

        if(currentSongGroup?.type != SongGroupType.SEARCH_LIST) {
            Timber.d("restoreLastDisplaySongs: currentSongGroup.type != Search_list")
            return
        } else {
            Timber.d("restoreLastDisplaySongs: $currentSongGroup, lastDisplaySongGroup=$lastDisplaySongGroup")
            currentSongGroup = lastDisplaySongGroup
        }

        determineIfShowingInformationScreen(currentSongGroup)

        currentSongGroup?.let { songGroup ->
            if(binding.displayRecyclerview.adapter == null) {
                binding.displayRecyclerview.adapter = SongListAdapter(
                    songGroup.songs,
                    this::handleSongSetting,
                    this::handleSongClicked,
                    this::handleAlbumClicked,
                    this::handlePlaylistClicked,
                    this::handleSongSelected,
                    songGroup.type,
                    this::handleViewHolderHandleDrag
                )
                determineIfShowingInformationScreen(songGroup)
            } else {
                (binding.displayRecyclerview.adapter as SongListAdapter).setSongs(songGroup.songs, songGroup.type)
            }
        }
    }

    private fun activateSearchMode() {
        Timber.d("activateSearchMode: ")
        binding.searchContainer.visibility = View.VISIBLE

        val searchMediaItem = MediaItem.Builder().setMediaId("Search").setMediaMetadata(
                MediaMetadata.Builder().setTitle("Search").build()
            ).build()


        currentSongGroup = SongGroup(
            type = SongGroupType.SEARCH_LIST,
            songs = listOf(),
            group = searchMediaItem,
        )
    }

    private fun deactivateSearchMode() {
        Timber.d("deactivateSearchMode: ")
        binding.searchEditText.setText("")
        binding.searchContainer.visibility = View.GONE
    }

    private fun activateDisplayMode() {
        Timber.d("activateDisplayMode: ")
        restoreLastDisplaySongs()

        currentSongGroup?.let { songGroup ->
            if(currentSongGroup?.songs?.isNotEmpty() == true) {
                initializeSongGroupInfo()

                binding.songGroupInfo.visibility = View.VISIBLE
            }
        }
    }

    private fun deactivateDisplayMode() {
        Timber.d("deactivateDisplayMode: ")
        binding.songGroupInfo.visibility = View.GONE
        clearCurrentSongs()
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
            parentViewModel.removeVirtualKeyboard()
            viewModel.clearMultiSelectSongs()
        }

        //Option 2 Button will be add a new playlist
        binding.createPlaylistPrompt.setOption2ButtonText(Const.ADD)
        binding.createPlaylistPrompt.setOption2ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            parentViewModel.createNamedPlaylist(binding.createPlaylistPrompt.getUserInputtedText())
            binding.createPlaylistPrompt.visibility = View.GONE
        }
    }

    /**
     * Sets up the adding a song to a Playlist Prompt functionality.
     */
    private fun setupPlaylistPrompt() {
        //When add button is clicked, I should add songs into playlists
        binding.playlistPrompt.onAddButtonClick {
            val checkedPlaylists: List<String> = viewModel.checkedPlaylists.value ?: listOf()
            val playlistAddSongs: List<MediaItem> = songsToAddToPlaylistPrompt ?: listOf()

            parentViewModel.addSongsToAPlaylist(
                checkedPlaylists,
                playlistAddSongs
            )

            viewModel.clearMultiSelectSongs()
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

//        viewModel.isShowingMultiSelectPrompt.observe(viewLifecycleOwner) { isShowing ->
//            if(isShowing) {
//                binding.multiSelectPrompt.visibility = View.VISIBLE
//            } else {
//                binding.multiSelectPrompt.visibility = View.GONE
//            }
//        }

        viewModel.isPlaylistPromptAddClickable.observe(viewLifecycleOwner) { isClickable ->
            binding.playlistPrompt.updateAddButtonClickability(isClickable)
        }
    }

    private fun handleSongSetting(menuOption: MenuOptionUtil.MenuOption, mediaItems: List<MediaItem>, fromMultiSelect: Boolean = false) {
        Timber.d("handleSongSetting: menuOption=$menuOption, mediaItems=${mediaItems.map { it.mediaMetadata.title }}")

        when (menuOption) {
            PLAY_SONG_GROUP -> handlePlaySongGroup()
            ADD_TO_PLAYLIST -> {
                viewModel.prepareSongsForPlaylists()
                songsToAddToPlaylistPrompt = mediaItems
                handleAddToPlaylist(mediaItems)

                //TODO there is an edgecase where user can add a song from a playlist to the same playlist and it wont refresh...
            }
            REMOVE_FROM_PLAYLIST -> {
                if(mediaItems.isNotEmpty()) {
                    val deletedSongPositions = (binding.displayRecyclerview.adapter as SongListAdapter).removeSongs(mediaItems.map { it.mediaMetadata.title.toString() })
                    if(deletedSongPositions.size == 1) {
                        (binding.displayRecyclerview.adapter as SongListAdapter).notifyItemRemoved(deletedSongPositions.first())
                    } else {
                        val sortedDeletedSongPositions = deletedSongPositions.sorted()
                        (binding.displayRecyclerview.adapter as SongListAdapter).notifyDataSetChanged()

                        savePlaylistChanges()
                    }

                    ///(adapter as SongListAdapter).clearAllSelected()
                    viewModel.clearMultiSelectSongs()
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
            parentViewModel.removeVirtualKeyboard()
        }
    }

    private fun handleAlbumClicked(album: MediaItem) {
        parentViewModel.querySongsFromAlbum(album)
        parentViewModel.removeVirtualKeyboard()
        parentViewModel.handleCancelSearchButtonClick()
    }

    private fun handlePlaylistClicked(playlist: MediaItem) {
        parentViewModel.querySongsFromPlaylist(playlist)
        parentViewModel.removeVirtualKeyboard()
        parentViewModel.handleCancelSearchButtonClick()
    }

    private fun handleSongSelected(mediaItem: MediaItem, isSelected:Boolean) {
        if(isSelected) {
            viewModel.selectSongs(listOf(mediaItem), showPrompt = true)
        } else {
            viewModel.unselectSong(mediaItem)
        }
    }

    private fun handleAddToPlaylist(mediaItems: List<MediaItem>) {
        binding.playlistPrompt.showPrompt()
    }

    private fun handleAddToQueue(mediaItems: List<MediaItem>) {
        Timber.d("handleAddToQueue: mediaItems=${mediaItems.map { it.mediaMetadata.title }}")
        parentViewModel.addSongsToEndOfQueue(mediaItems)
    }

    private fun handleCheckStats() {
        //TODO...
    }

    /**
     * Shows a prompt for the user to choose a playlist or album.
     * Should show when there is no songs in the current song list, not an empty playlist.
     */
    private fun determineIfShowingInformationScreen(songGroup: SongGroup?) {
        
        Timber.d("determineIfShowingInformationScreen: songGroup.type=${songGroup?.type}, songGroup.songs=${songGroup?.songs}")

        if(songGroup == null) {
            binding.songListInformationScreen.visibility = View.VISIBLE
        }

        songGroup?.let { it ->
            if( it.type == SongGroupType.SEARCH_LIST) {
                binding.songListInformationScreen.visibility = View.GONE
            } else if( it.type != SongGroupType.PLAYLIST && it.songs.isEmpty()) {
                binding.songListInformationScreen.visibility = View.VISIBLE
                binding.songGroupInfo.visibility = View.GONE
            } else {
                binding.songListInformationScreen.visibility = View.GONE
                binding.songGroupInfo.visibility = View.VISIBLE
            }
        }
    }

    private fun removeInformationScreen() {
        binding.songListInformationScreen.visibility = View.GONE
    }

    private fun setupPage() {
        binding.songGroupInfo.setSongGroupTitleText("PARTICULAR ALBUM - ARTIST")

        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        //First Icon will be the playlists
        binding.songListInformationScreen.setFirstInfo(getString(R.string.choose_a_playlist_to_view))
        ResourcesCompat.getDrawable(resources, R.drawable.playlist_icon, null)?.let { drawable ->
            binding.songListInformationScreen.setFirstIcon(drawable)
        }
        binding.songListInformationScreen.setFirstIconCallback { parentViewModel.setPage(PageType.PLAYLIST_PAGE) }

        //Second Icon will be the Albums
        binding.songListInformationScreen.setSecondInfo(getString(R.string.choose_an_album_to_view))
        ResourcesCompat.getDrawable(resources, R.drawable.browse_album_icon, null)?.let { drawable ->
            binding.songListInformationScreen.setSecondIcon(drawable)
        }
        binding.songListInformationScreen.setSecondIconCallback { parentViewModel.setPage(PageType.ALBUM_PAGE) }
    }
}