package com.example.tacomamusicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.data.Playlist

class PlaylistAdapter(
    private val playlists:  List<Playlist>
): RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class PlaylistViewHolder(view: View): RecyclerView.ViewHolder(view) { //TODO I need to make a viewholder for this class
        val albumInfo: TextView

        init {
            albumInfo = view.findViewById(R.id.album_info)
        }
    }

    //Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        //Create a new view, which defines the UI of teh list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_playlist, parent, false)

        return PlaylistViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: PlaylistViewHolder, position: Int) {

        // Get element from  your dataset at this position and replace the contents of the view with that element
        viewHolder.albumInfo.text = playlists[position].title
    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return playlists.size
    }

}