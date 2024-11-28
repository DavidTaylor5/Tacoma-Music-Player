package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.adapter.AlbumListAdapter.AlbumViewHolder
import com.example.tacomamusicplayer.data.Playlist
import com.example.tacomamusicplayer.databinding.ViewholderAlbumBinding
import com.example.tacomamusicplayer.databinding.ViewholderPlaylistBinding
import timber.log.Timber

class PlaylistAdapter(
    private val playlists:  List<Playlist>,
    private val onAlbumClick: (String) -> Unit,
): RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class PlaylistViewHolder(val binding: ViewholderPlaylistBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderPlaylistBinding.inflate(inflater, parent, false)

        return PlaylistViewHolder(binding)
    }

//    //Create new views (invoked by the layout manager)
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
//        //Create a new view, which defines the UI of teh list item
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.viewholder_playlist, parent, false)
//
//        return PlaylistViewHolder(view)
//    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: PlaylistViewHolder, position: Int) {
        viewHolder.binding.albumInfo.text = playlists[position].title

        //TODO On Click navigate...
        viewHolder.binding.playlistItem.setOnClickListener {
            onAlbumClick(playlists[position].title ?: "Unknown title?") //TODO replace with playlist title....
        }
    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return playlists.size
    }

}