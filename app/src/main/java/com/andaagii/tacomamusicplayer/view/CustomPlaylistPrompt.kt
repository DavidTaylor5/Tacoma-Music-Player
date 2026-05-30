package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.andaagii.tacomamusicplayer.adapter.PlaylistPromptAdapter
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.databinding.ViewCustomPlaylistPromptBinding

//TODO move this so that there only exists one, and it is part of the PlayerDisplayFragment
/**
 * Overlay dialog that lets the user add selected songs to one or more existing playlists.
 *
 * Displays a [androidx.recyclerview.widget.RecyclerView] of playlists via [PlaylistPromptAdapter],
 * each with a checkbox. The "Add" button is enabled only when at least one playlist is checked,
 * controlled by [updateAddButtonClickability]. Tapping the background or the close button hides
 * the view and resets all checkboxes via [closePrompt].
 *
 * Callbacks for all user actions are registered via the `set*` and `on*` functions rather than
 * passed to the constructor, so the host fragment can wire them after layout inflation.
 */
class CustomPlaylistPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomPlaylistPromptBinding

    private var onAddButtonClicked : () -> Unit = {}
    private var onCreateNewPlaylistClicked : () -> Unit = {}
    private var onCloseButtonClicked : () -> Unit = {}
    private var playlistCheckedHandler: (String, Boolean) -> Unit = { playlistTitle, isChecked ->

    }


    init {
        attrs?.let {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomPlaylistPromptBinding.inflate(inflater, this, true)

            //Note don't forget the layoutManager
            binding.displayRecyclerview.layoutManager = LinearLayoutManager(this.context)

            binding.closeButton.setOnClickListener {
                closePrompt()
            }

            binding.promptBackground.setOnClickListener {
                closePrompt()
            }

            binding.addButton.setOnClickListener {
                onAddButtonClicked()
            }

            binding.createNewPlaylistBtn.setOnClickListener {
                onCreateNewPlaylistClicked()
            }

            binding.closeButton.setOnClickListener {
                onCloseButtonClicked()
            }
        }
    }

    /**
     * Replaces the RecyclerView content with [playlists] and triggers a full rebind.
     *
     * @param playlists The current list of available playlists to display with checkboxes.
     */
    fun setPlaylistData(playlists: List<MediaItem>) {
        binding.displayRecyclerview.adapter = PlaylistPromptAdapter(
            playlists,
            this::onPlaylistChecked
        )

        //Now that data has changed -> notify the rv to update!
        val rv = binding.displayRecyclerview
        val rvAdapter = rv.adapter
        rvAdapter!!.notifyDataSetChanged()
    }

    /** Forwards checkbox change events from the adapter to the registered [playlistCheckedHandler]. */
    private fun onPlaylistChecked(playlist: String, boolean: Boolean) {
        playlistCheckedHandler(playlist, boolean)
    }

    /**
     * Hides the prompt, resets all playlist checkboxes, and disables the Add button.
     *
     * Called when the user taps the background, the close button, or after a successful add action.
     */
    fun closePrompt() {
        this.visibility = View.GONE
        (binding.displayRecyclerview.adapter as PlaylistPromptAdapter).removeAllChecks()
        updateAddButtonClickability(false)
    }

    /** Makes the prompt visible so the user can select playlists. */
    fun showPrompt() {
        this.visibility = View.VISIBLE
    }

    /** Registers [onAdd] as the callback invoked when the "Add" button is tapped. */
    fun onAddButtonClick(onAdd: () -> Unit) {
        onAddButtonClicked = onAdd
    }

    /** Registers [onClose] as the callback invoked when the close button is tapped. */
    fun onCloseButtonClicked(onClose: () -> Unit) {
        onCloseButtonClicked = onClose
    }

    /** Registers [onClick] as the callback invoked when the "Create new playlist" button is tapped. */
    fun onCreateNewPlaylistClicked(onClick: () -> Unit) {
        onCreateNewPlaylistClicked = onClick
    }

    /**
     * Registers [handler] to receive checkbox toggle events from [PlaylistPromptAdapter].
     *
     * The handler receives the playlist title and the new checked state, and is typically used
     * by the host fragment to update the ViewModel's selected-playlists state.
     */
    fun setPlaylistCheckedHandler(handler: (String, Boolean) -> Unit) {
        playlistCheckedHandler = handler
    }

    /**
     * Enables or disables the "Add" button based on whether at least one playlist is checked.
     *
     * @param canClick `true` to enable the Add button; `false` to disable it.
     */
    fun updateAddButtonClickability(canClick: Boolean) {
        binding.addButton.isEnabled = canClick
    }

}