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
import androidx.appcompat.content.res.AppCompatResources
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
import java.io.File

/**
 * Dual-purpose song list page hosted at index 4 in `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Shows either the full library or the tracks within a selected album/playlist, driven by
 * [MainViewModel.currentSongGroup]. Also supports:
 * - **Inline search** — toggled via a toolbar button; replaces the song list with
 *   [SongGroupType.SEARCH_LIST] results and restores the previous group on cancel.
 * - **Multi-select + batch add-to-playlist** — managed by [SongListViewModel].
 * - **Playlist drag-to-reorder** — [itemTouchHelper] is attached only when the active group
 *   is a [SongGroupType.PLAYLIST]; drag is disabled for albums.
 *
 * Uses [MainViewModel] (activity-scoped) for library and playback operations, and
 * [SongListViewModel] (fragment-scoped) for transient multi-select state.
 */
@AndroidEntryPoint
class SongListFragment : Fragment() {
    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: SongListViewModel by viewModels()

    /**
     * The song group currently displayed in the list. Replaced by a transient
     * [SongGroupType.SEARCH_LIST] group when search mode is active; restored from
     * [lastDisplaySongGroup] when search mode is cancelled.
     */
    private var currentSongGroup: SongGroup? = null

    /**
     * The last non-search song group shown before search mode was activated.
     * Used by [restoreLastDisplaySongs] to recover the album/playlist view when the user
     * cancels a search.
     */
    private var lastDisplaySongGroup: SongGroup? = null

    /**
     * Songs staged for the "add to playlist" prompt. Set when the user selects tracks and
     * taps "Add to playlist"; read when the user confirms the prompt selection.
     */
    private var songsToAddToPlaylistPrompt: List<MediaItem>? = null

    /**
     * Provides drag-to-reorder behaviour for playlist song lists.
     *
     * 1. All four directions (UP, DOWN, START, END) are enabled so dragging feels organic —
     *    START/END allow the item to track diagonal finger movements naturally.
     * 2. While dragging, the item's alpha is reduced to 0.5 to indicate movement.
     * 3. On drop ([onMove]), the adapter's backing model is updated, the adapter is notified,
     *    and [savePlaylistChanges] persists the new order to the database.
     * 4. Horizontal swipe ([onSwiped]) is intentionally a no-op — reorder-only mode.
     *
     * Attached to the RecyclerView only for [SongGroupType.PLAYLIST] groups; detached
     * (set to `null`) for albums via [initializeSongGroupInfo].
     */
    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ACTION_STATE_DRAG) {
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

                    adapter.moveItem(from, to)
                    adapter.notifyItemMoved(from, to)

                    // Persist the new position order to the database after each move.
                    savePlaylistChanges()

                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // Horizontal swipe is not used for song removal; intentionally ignored.
                }
            }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onPause() {
        Timber.d("onPause: ")
        super.onPause()
        // Clear multi-select on pause so stale selections are not visible if the user
        // navigates away and returns to this fragment.
        viewModel.clearMultiSelectSongs()
    }

    /**
     * Persists the current adapter song order to the database if and only if the active
     * group is a [SongGroupType.PLAYLIST] and the order has actually changed.
     *
     * Calling this for albums would be a no-op in terms of semantics, but the guard
     * prevents unnecessary ViewModel calls.
     */
    private fun savePlaylistChanges() {
        currentSongGroup?.let { songGroup ->
            if (songGroup.type == SongGroupType.PLAYLIST) {
                val finalSongOrder = (binding.displayRecyclerview.adapter as SongListAdapter).getSongOrder()

                if (determineIfPlaylistSongsHaveChanged(songGroup.songs, finalSongOrder)) {
                    songGroup.songs = finalSongOrder
                    parentViewModel.updatePlaylistOrder(songGroup)
                }
            } else {
                Timber.d("savePlaylistChanges: songGroup=$songGroup is not of type PLAYLIST, therefore no save.")
            }
        }
    }

    /**
     * Returns `true` if the song order has changed since the last save.
     *
     * Used as a guard in [savePlaylistChanges] to avoid writing to the database on drag
     * operations that return an item to its original position.
     *
     * @param originalSongOrder The song list at the time the group was loaded or last saved.
     * @param finalSongOrder The current song list from the adapter.
     */
    private fun determineIfPlaylistSongsHaveChanged(
        originalSongOrder: List<MediaItem>,
        finalSongOrder: List<MediaItem>
    ): Boolean {
        return originalSongOrder != finalSongOrder
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSonglistBinding.inflate(inflater)

        // Rebuild the adapter and header view every time the displayed group changes
        // (e.g., user taps an album from AlbumListFragment or a playlist from PlaylistFragment).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.currentSongGroup.collect { songGroup ->
                    songGroup ?: return@collect
                    Timber.d("onCreateView: title=${songGroup.group.mediaMetadata.albumTitle}")

                    currentSongGroup = songGroup
                    lastDisplaySongGroup = songGroup

                    parentViewModel.handleCancelSearchButtonClick()

                    binding.displayRecyclerview.adapter = SongListAdapter(
                        dataSet = songGroup.songs,
                        handleSongSetting = this@SongListFragment::handleSongSetting,
                        handleSongClick = this@SongListFragment::handleSongClicked,
                        handleAlbumClick = this@SongListFragment::handleAlbumClicked,
                        handleSearchSongClick = parentViewModel::playAlbumAtSongPosition,
                        handlePlaylistClick = this@SongListFragment::handlePlaylistClicked,
                        handleSongSelected = this@SongListFragment::handleSongSelected,
                        songGroupType = songGroup.type,
                        onHandleDrag = this@SongListFragment::handleViewHolderHandleDrag
                    )
                    determineIfShowingInformationScreen(songGroup)
                    initializeSongGroupInfo()
                }
            }
        }

        // Update the adapter with search results as the user types. Save the pre-search group
        // to lastDisplaySongGroup the first time a non-search group is active, so it can be
        // restored when search mode ends.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.currentSearchList.collect { searchItems ->
                    searchItems ?: return@collect
                    currentSongGroup?.let { songGroup ->
                        if (songGroup.type != SongGroupType.SEARCH_LIST) {
                            Timber.d("onCreateView: saving currentSongGroup to lastDisplaySongGroup")
                            lastDisplaySongGroup = songGroup
                        }
                    }

                    val searchMediaItem = MediaItem.Builder().setMediaId("Search").setMediaMetadata(
                        MediaMetadata.Builder().setTitle("Search").build()
                    ).build()

                    currentSongGroup = SongGroup(
                        type = SongGroupType.SEARCH_LIST,
                        songs = searchItems,
                        group = searchMediaItem,
                    )

                    if (binding.displayRecyclerview.adapter == null) {
                        currentSongGroup?.let { songGroup ->
                            binding.displayRecyclerview.adapter = SongListAdapter(
                                dataSet = songGroup.songs,
                                handleSongSetting = this@SongListFragment::handleSongSetting,
                                handleSongClick = this@SongListFragment::handleSongClicked,
                                handleAlbumClick = this@SongListFragment::handleAlbumClicked,
                                handleSearchSongClick = parentViewModel::playAlbumAtSongPosition,
                                handlePlaylistClick = this@SongListFragment::handlePlaylistClicked,
                                handleSongSelected = this@SongListFragment::handleSongSelected,
                                songGroupType = songGroup.type,
                                onHandleDrag = this@SongListFragment::handleViewHolderHandleDrag
                            )
                            determineIfShowingInformationScreen(songGroup)
                        }
                    } else {
                        (binding.displayRecyclerview.adapter as SongListAdapter).setSongs(searchItems, SongGroupType.SEARCH_LIST)
                    }
                }
            }
        }

        // StateFlow always re-emits to new collectors, so viewLifecycleOwner is sufficient
        // to keep the search icon in sync even after configuration changes.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.isShowingSearchMode.collect { isShowing ->
                    if (isShowing) {
                        binding.searchOption.setBackgroundResource(R.drawable.baseline_search_off_24)
                    } else {
                        binding.searchOption.setBackgroundResource(R.drawable.baseline_search_24)
                    }
                }
            }
        }

        binding.searchOption.setOnClickListener {
            parentViewModel.flipSearchButtonState()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.isShowingSearchMode.collect { isShowing ->
                    if (isShowing) {
                        activateSearchMode()
                        deactivateDisplayMode()
                        removeInformationScreen()
                    } else {
                        deactivateSearchMode()
                        activateDisplayMode()
                    }

                    // Clear any selected rows when toggling search mode to avoid stale highlights.
                    binding.displayRecyclerview.adapter?.let { adapter ->
                        (adapter as SongListAdapter).clearAllSelected()
                    }
                }
            }
        }

        // Filter out the internal queue and original-order playlists so they never appear
        // in the "add to playlist" chooser presented to the user.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.availablePlaylists.collect { playlists ->
                    val playlistsWithoutQueue = playlists.filter { playlist ->
                        playlist.mediaMetadata.albumTitle != Const.PLAYLIST_QUEUE_TITLE &&
                            playlist.mediaMetadata.albumTitle != Const.ORIGINAL_QUEUE_ORDER
                    }
                    binding.playlistPrompt.setPlaylistData(playlistsWithoutQueue)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isShowingPlaylistPrompt.collect { isShowing ->
                    if (isShowing) {
                        binding.playlistPrompt.visibility = View.VISIBLE
                    } else {
                        binding.playlistPrompt.visibility = View.GONE
                    }
                }
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
            menu.menuInflater.inflate(R.menu.songlist_songgroup_options, menu.menu)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentlySelectedSongs.collect { currentlySelectedSongs ->
                    binding.multiSelectPrompt.setPromptText("${currentlySelectedSongs.size} songs selected")

                    if (currentlySelectedSongs.isEmpty()) {
                        Timber.d("onCreateView: set multiselectPrompt to GONE")
                        // INVISIBLE rather than GONE because custom views can exhibit layout glitches
                        // when re-adding a GONE view to the hierarchy mid-animation.
                        binding.multiSelectPrompt.visibility = View.INVISIBLE
                    } else {
                        Timber.d("onCreateView: set multiselectPrompt to VISIBLE")
                        binding.multiSelectPrompt.visibility = View.VISIBLE
                    }

                    binding.displayRecyclerview.adapter?.let { adapter ->
                        if (currentlySelectedSongs.isEmpty()) {
                            (adapter as SongListAdapter).clearAllSelected()
                        }
                    }
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
            // Show playlist-specific options (e.g., remove) when inside a playlist;
            // show album options (e.g., add to queue) otherwise.
            if (currentSongGroup?.type == SongGroupType.PLAYLIST) {
                menu.menuInflater.inflate(R.menu.multi_select_playlist_options, menu.menu)
            } else {
                menu.menuInflater.inflate(R.menu.multi_select_album_options, menu.menu)
            }
            menu.setOnMenuItemClickListener {
                Toast.makeText(this.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                handleSongSetting(
                    MenuOptionUtil.determineMenuOptionFromTitle(it.title.toString()),
                    viewModel.currentlySelectedSongs.value
                )
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
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                parentViewModel.removeVirtualKeyboard()
                binding.searchEditText.clearFocus()
                true
            } else {
                false
            }
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Timber.d("onTextChanged: User is typing: $s")
                if (parentViewModel.isShowingSearchMode.value) {
                    parentViewModel.querySearchData(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }

        setupCreatePlaylistPrompt()
        setupPlaylistPrompt()
        setupPage()

        return binding.root
    }

    /**
     * Populates the `CustomSongGroupInfoView` header with the active group's title and artwork,
     * and enables or disables drag-to-reorder based on group type.
     *
     * Drag is enabled only for [SongGroupType.PLAYLIST] groups — album tracks have a fixed
     * order determined by MediaStore and cannot be reordered by the user.
     */
    private fun initializeSongGroupInfo() {
        Timber.d("initializeSongGroupInfo: ")
        currentSongGroup?.let { songGroup ->
            binding.songGroupInfo.setSongGroupTitleText(songGroup.group.mediaMetadata.albumTitle.toString())

            val artFile = File(songGroup.group.mediaMetadata.artworkUri.toString())
            if (artFile.exists()) {
                binding.songGroupInfo.getSongGroupImage().setImageURI(songGroup.group.mediaMetadata.artworkUri)
            } else {
                binding.songGroupInfo.getSongGroupImage().setImageDrawable(
                    AppCompatResources.getDrawable(binding.root.context, R.drawable.white_note)
                )
            }

            // Detach drag from albums so users cannot accidentally reorder tracks that
            // have a fixed MediaStore order. Attach for playlists.
            if (songGroup.type == SongGroupType.ALBUM && songGroup.songs.isNotEmpty()) {
                itemTouchHelper.attachToRecyclerView(null)
            } else {
                itemTouchHelper.attachToRecyclerView(binding.displayRecyclerview)
            }
        }
    }

    /**
     * Clears the song list adapter, used when entering search mode so the search results
     * start from an empty state rather than the previous album/playlist list.
     */
    private fun clearCurrentSongs() {
        Timber.d("clearCurrentSongs: ")
        binding.displayRecyclerview.adapter?.let { adapter ->
            (adapter as SongListAdapter).setSongs(listOf(), SongGroupType.SEARCH_LIST)
        }
    }

    /**
     * Restores the last non-search song group after search mode is cancelled.
     *
     * Returns early without doing anything if [currentSongGroup] is not a [SongGroupType.SEARCH_LIST],
     * which means the user cancelled search before any results were displayed. Otherwise, swaps
     * [currentSongGroup] back to [lastDisplaySongGroup] and refreshes the adapter.
     */
    private fun restoreLastDisplaySongs() {
        Timber.d("restoreLastDisplaySongs: ")

        if (currentSongGroup?.type != SongGroupType.SEARCH_LIST) {
            Timber.d("restoreLastDisplaySongs: currentSongGroup.type != Search_list")
            return
        } else {
            Timber.d("restoreLastDisplaySongs: $currentSongGroup, lastDisplaySongGroup=$lastDisplaySongGroup")
            currentSongGroup = lastDisplaySongGroup
        }

        determineIfShowingInformationScreen(currentSongGroup)

        currentSongGroup?.let { songGroup ->
            if (binding.displayRecyclerview.adapter == null) {
                binding.displayRecyclerview.adapter = SongListAdapter(
                    dataSet = songGroup.songs,
                    handleSongSetting = this::handleSongSetting,
                    handleSongClick = this::handleSongClicked,
                    handleAlbumClick = this::handleAlbumClicked,
                    handleSearchSongClick = parentViewModel::playAlbumAtSongPosition,
                    handlePlaylistClick = this::handlePlaylistClicked,
                    handleSongSelected = this::handleSongSelected,
                    songGroupType = songGroup.type,
                    onHandleDrag = this::handleViewHolderHandleDrag
                )
                determineIfShowingInformationScreen(songGroup)
            } else {
                (binding.displayRecyclerview.adapter as SongListAdapter).setSongs(songGroup.songs, songGroup.type)
            }
        }
    }

    /** Shows the search input bar and replaces [currentSongGroup] with an empty search group. */
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

    /** Hides and clears the search input bar. */
    private fun deactivateSearchMode() {
        Timber.d("deactivateSearchMode: ")
        binding.searchEditText.setText("")
        binding.searchContainer.visibility = View.GONE
    }

    /** Restores the pre-search song group and shows the group-info header if songs are present. */
    private fun activateDisplayMode() {
        Timber.d("activateDisplayMode: ")
        restoreLastDisplaySongs()

        currentSongGroup?.let { songGroup ->
            if (currentSongGroup?.songs?.isNotEmpty() == true) {
                initializeSongGroupInfo()
                binding.songGroupInfo.visibility = View.VISIBLE
            }
        }
    }

    /** Hides the group-info header and clears the song list, preparing for search results. */
    private fun deactivateDisplayMode() {
        Timber.d("deactivateDisplayMode: ")
        binding.songGroupInfo.visibility = View.GONE
        clearCurrentSongs()
    }

    /** Initiates a drag operation for [viewHolder] via [itemTouchHelper]. */
    private fun handleViewHolderHandleDrag(viewHolder: ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    /**
     * Wires the create-playlist text prompt in the song list context.
     *
     * Option 1 (Cancel) closes the prompt and clears multi-select state.
     * Option 2 (Add) calls [MainViewModel.createNamedPlaylist] with the user-entered name.
     */
    private fun setupCreatePlaylistPrompt() {
        binding.createPlaylistPrompt.setTextInputHint(Const.NEW_PLAYLIST_HINT)

        binding.createPlaylistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.createPlaylistPrompt.setOption1ButtonOnClick {
            binding.createPlaylistPrompt.closePrompt()
            parentViewModel.removeVirtualKeyboard()
            viewModel.clearMultiSelectSongs()
        }

        binding.createPlaylistPrompt.setOption2ButtonText(Const.ADD)
        binding.createPlaylistPrompt.setOption2ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            parentViewModel.createNamedPlaylist(binding.createPlaylistPrompt.getUserInputtedText())
            binding.createPlaylistPrompt.visibility = View.GONE
        }
    }

    /**
     * Wires the "add songs to playlist" prompt overlay.
     *
     * The Add button calls [MainViewModel.addSongsToAPlaylist] with the checked playlists and
     * the songs staged in [songsToAddToPlaylistPrompt]. The "Create new playlist" shortcut
     * opens [binding.createPlaylistPrompt] inline. The Close button dismisses both prompts.
     */
    private fun setupPlaylistPrompt() {
        binding.playlistPrompt.onAddButtonClick {
            val checkedPlaylists: List<String> = viewModel.checkedPlaylists.value
            val playlistAddSongs: List<MediaItem> = songsToAddToPlaylistPrompt ?: listOf()

            parentViewModel.addSongsToAPlaylist(checkedPlaylists, playlistAddSongs)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isPlaylistPromptAddClickable.collect { isClickable ->
                    binding.playlistPrompt.updateAddButtonClickability(isClickable)
                }
            }
        }
    }

    /**
     * Routes a song-level or group-level menu action to the appropriate handler.
     *
     * For [REMOVE_FROM_PLAYLIST]: a single-item removal uses [RecyclerView.Adapter.notifyItemRemoved]
     * for a smooth animation; a multi-item removal falls back to `notifyDataSetChanged` because
     * the shifted positions make individual notifications unreliable.
     *
     * Note: adding a single song to the queue via [ADD_TO_QUEUE] requires the song's media ID
     * to be set correctly; this is a known limitation when invoked from the queue page.
     *
     * @param menuOption The action selected from the popup or multi-select menu.
     * @param mediaItems The tracks the action should be applied to.
     * @param fromMultiSelect `true` when invoked from the multi-select toolbar rather than
     *   an individual row menu.
     */
    private fun handleSongSetting(
        menuOption: MenuOptionUtil.MenuOption,
        mediaItems: List<MediaItem>,
        fromMultiSelect: Boolean = false
    ) {
        Timber.d("handleSongSetting: menuOption=$menuOption, mediaItems=${mediaItems.map { it.mediaMetadata.title }}")

        when (menuOption) {
            PLAY_SONG_GROUP -> handlePlaySongGroup()
            ADD_TO_PLAYLIST -> {
                viewModel.prepareSongsForPlaylists()
                songsToAddToPlaylistPrompt = mediaItems
                handleAddToPlaylist(mediaItems)
            }
            REMOVE_FROM_PLAYLIST -> {
                if (mediaItems.isNotEmpty()) {
                    val deletedSongPositions = (binding.displayRecyclerview.adapter as SongListAdapter)
                        .removeSongs(mediaItems.map { it.mediaMetadata.title.toString() })

                    if (deletedSongPositions.size == 1) {
                        // Single removal — animate with notifyItemRemoved.
                        (binding.displayRecyclerview.adapter as SongListAdapter)
                            .notifyItemRemoved(deletedSongPositions.first())
                    } else {
                        // Multi-removal — positions shift unpredictably; use a full rebind.
                        (binding.displayRecyclerview.adapter as SongListAdapter).notifyDataSetChanged()
                        savePlaylistChanges()
                    }

                    viewModel.clearMultiSelectSongs()
                }
            }
            ADD_TO_QUEUE -> handleAddToQueue(mediaItems)
            CHECK_STATS -> handleCheckStats()
            else -> Timber.d("handleSongSetting: UNKNOWN SETTING")
        }
    }

    /** Plays the active [currentSongGroup] starting from the first track. */
    private fun handlePlaySongGroup() {
        currentSongGroup?.let { songGroup ->
            parentViewModel.playSongGroupAtPosition(songGroup, 0)
        }
    }

    /**
     * Replaces the queue with [currentSongGroup] and starts playback at [position].
     *
     * Also dismisses the virtual keyboard since this is typically triggered from a search
     * result tap where the keyboard is open.
     *
     * @param position The zero-based index of the song within [currentSongGroup] to play first.
     */
    private fun handleSongClicked(position: Int) {
        currentSongGroup?.let { songGroup ->
            parentViewModel.playSongGroupAtPosition(songGroup, position)
            parentViewModel.removeVirtualKeyboard()
        }
    }

    /**
     * Drills into the selected album's track list.
     *
     * @param album The album [MediaItem] to browse.
     */
    private fun handleAlbumClicked(album: MediaItem) {
        parentViewModel.querySongsFromAlbum(album)
        parentViewModel.removeVirtualKeyboard()
        parentViewModel.handleCancelSearchButtonClick()
    }

    /**
     * Drills into the selected playlist's track list.
     *
     * @param playlist The playlist [MediaItem] to browse.
     */
    private fun handlePlaylistClicked(playlist: MediaItem) {
        parentViewModel.querySongsFromPlaylist(playlist)
        parentViewModel.removeVirtualKeyboard()
        parentViewModel.handleCancelSearchButtonClick()
    }

    /**
     * Updates [SongListViewModel] with the selection state change for a single song row.
     *
     * @param mediaItem The track whose selection state changed.
     * @param isSelected `true` when the row was selected; `false` when it was deselected.
     */
    private fun handleSongSelected(mediaItem: MediaItem, isSelected: Boolean) {
        if (isSelected) {
            viewModel.selectSongs(listOf(mediaItem), showPrompt = true)
        } else {
            viewModel.unselectSong(mediaItem)
        }
    }

    /**
     * Shows the "add to playlist" prompt overlay for [mediaItems].
     *
     * @param mediaItems The tracks to be added to the chosen playlist(s).
     */
    private fun handleAddToPlaylist(mediaItems: List<MediaItem>) {
        binding.playlistPrompt.showPrompt()
    }

    /**
     * Appends [mediaItems] to the end of the current playback queue.
     *
     * @param mediaItems The tracks to enqueue.
     */
    private fun handleAddToQueue(mediaItems: List<MediaItem>) {
        Timber.d("handleAddToQueue: mediaItems=${mediaItems.map { it.mediaMetadata.title }}")
        parentViewModel.addSongsToEndOfQueue(mediaItems)
    }

    /**
     * Intended to show statistics for the selected track. Not yet implemented.
     */
    private fun handleCheckStats() {
        // Not yet implemented.
    }

    /**
     * Determines whether to show the empty-state information screen or the song list.
     *
     * Display rules:
     * - `null` group → show the information screen (no group has been selected yet).
     * - [SongGroupType.SEARCH_LIST] → always hide the information screen (search has its
     *   own empty state handled by the search UI).
     * - Any other type with an empty song list → show the information screen and hide the
     *   group-info header.
     * - Non-empty group → hide the information screen and show the group-info header.
     *
     * @param songGroup The currently active [SongGroup], or `null` if none is loaded.
     */
    private fun determineIfShowingInformationScreen(songGroup: SongGroup?) {
        Timber.d("determineIfShowingInformationScreen: songGroup.type=${songGroup?.type}, songGroup.songs=${songGroup?.songs}")

        if (songGroup == null) {
            binding.songListInformationScreen.visibility = View.VISIBLE
            return
        }

        songGroup.let { it ->
            if (it.type == SongGroupType.SEARCH_LIST) {
                binding.songListInformationScreen.visibility = View.GONE
            } else if (it.type != SongGroupType.PLAYLIST && it.songs.isEmpty()) {
                binding.songListInformationScreen.visibility = View.VISIBLE
                binding.songGroupInfo.visibility = View.GONE
            } else {
                binding.songListInformationScreen.visibility = View.GONE
                binding.songGroupInfo.visibility = View.VISIBLE
            }
        }
    }

    /** Forces the empty-state information screen to hidden regardless of group state. */
    private fun removeInformationScreen() {
        binding.songListInformationScreen.visibility = View.GONE
    }

    /**
     * Initialises the RecyclerView layout manager and wires the two shortcut buttons on the
     * empty-state information screen that navigate to the playlist and album browsing pages.
     */
    private fun setupPage() {
        binding.songGroupInfo.setSongGroupTitleText("PARTICULAR ALBUM - ARTIST")
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        binding.songListInformationScreen.setFirstInfo(getString(R.string.choose_a_playlist_to_view))
        ResourcesCompat.getDrawable(resources, R.drawable.playlist_icon, null)?.let { drawable ->
            binding.songListInformationScreen.setFirstIcon(drawable)
        }
        binding.songListInformationScreen.setFirstIconCallback { parentViewModel.setPage(PageType.PLAYLIST_PAGE) }

        binding.songListInformationScreen.setSecondInfo(getString(R.string.choose_an_album_to_view))
        ResourcesCompat.getDrawable(resources, R.drawable.browse_album_icon, null)?.let { drawable ->
            binding.songListInformationScreen.setSecondIcon(drawable)
        }
        binding.songListInformationScreen.setSecondIconCallback { parentViewModel.setPage(PageType.ALBUM_PAGE) }
    }
}
