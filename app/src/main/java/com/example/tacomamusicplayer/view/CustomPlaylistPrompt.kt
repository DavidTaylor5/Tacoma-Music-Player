package com.example.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.PlaylistPromptAdapter
import com.example.tacomamusicplayer.data.Playlist
import com.example.tacomamusicplayer.databinding.ViewCustomPlaylistPromptBinding

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

            //I gotta remember to actually attach to the view
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
     * Show all available playlists in the prompt.
     */
    fun setPlaylistData(playlists: List<Playlist>) {
        binding.displayRecyclerview.adapter = PlaylistPromptAdapter(
            playlists,
            this::onPlaylistChecked
        )

        //Now that data has changed -> notify the rv to update!
        val rv = binding.displayRecyclerview
        val rvAdapter = rv.adapter
        rvAdapter!!.notifyDataSetChanged()
    }

    private fun onPlaylistChecked(playlist: String, boolean: Boolean) {
        playlistCheckedHandler(playlist, boolean)
    }

    fun closePrompt() {
        this.visibility = View.GONE
    }

    fun showPrompt() {
        this.visibility = View.VISIBLE
    }

    fun onAddButtonClick(onAdd: () -> Unit) {
        onAddButtonClicked = onAdd
    }

    fun onCloseButtonClicked(onClose: () -> Unit) {
        onCloseButtonClicked = onClose
    }

    fun onCreateNewPlaylistClicked(onClick: () -> Unit) {
        onCreateNewPlaylistClicked = onClick
    }

    fun setPlaylistCheckedHandler(handler: (String, Boolean) -> Unit) {
        playlistCheckedHandler = handler
    }

    fun updateAddButtonClickability(canClick: Boolean) {
        binding.addButton.isEnabled = canClick
    }

}