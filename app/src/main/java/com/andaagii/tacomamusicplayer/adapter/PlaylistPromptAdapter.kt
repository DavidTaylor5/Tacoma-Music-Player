package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.databinding.ViewholderPlaylistPromptBinding
import timber.log.Timber

/**
 * A recyclerview adapter that is able to take a list of Playlist Items and displays them.
 */
class PlaylistPromptAdapter(
    private val playlists: List<Playlist>,
    private val onPlaylistChecked: (String, Boolean) -> Unit,
): RecyclerView.Adapter<PlaylistPromptAdapter.PlaylistPromptViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class PlaylistPromptViewHolder(val binding : ViewholderPlaylistPromptBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistPromptViewHolder {
        Timber.d("onCreateViewHolder: ")


        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderPlaylistPromptBinding.inflate(inflater, parent, false)

        return PlaylistPromptViewHolder(binding)
    }

    @OptIn(UnstableApi::class) override fun onBindViewHolder(viewHolder: PlaylistPromptViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        viewHolder.binding.playlistName.text = playlists[position].title

        viewHolder.binding.root.setOnClickListener {
            viewHolder.binding.addCheckbox.isChecked = !viewHolder.binding.addCheckbox.isChecked
        }

        viewHolder.binding.addCheckbox.setOnCheckedChangeListener { compoundButton, b ->
            onPlaylistChecked(playlists[position].title ?: "UNKNOWN", b)
        }
    }

    override fun onBindViewHolder(
        holder: PlaylistPromptViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        
        Timber.d("onBindViewHolder: ")
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

}