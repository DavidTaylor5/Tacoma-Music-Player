package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.data.Playlist
import com.example.tacomamusicplayer.databinding.ViewholderPlaylistBinding
import com.example.tacomamusicplayer.util.UtilImpl
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

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: PlaylistViewHolder, position: Int) {
        viewHolder.binding.playlistName.text = playlists[position].title

        //TODO On Click navigate...
        viewHolder.binding.itemContainer.setOnClickListener {
            //TODO I need to update onAlbumClick to have a songGroup passed through? Also name change...
            onAlbumClick(playlists[position].title ?: "Unknown title?")
        }

        //Determine Playlist Duration Information
        val numberOfSongs = playlists[position].songs.songs.size
        viewHolder.binding.durationTracks.text = if(numberOfSongs == 1) "1 track" else {"$numberOfSongs tracks"}

        val playlistDuration = playlists[position].songs.songs.fold(0L) { acc, songData ->
            val songDuration = songData.duration.toLongOrNull() ?: 0
            acc + songDuration
        }

        val playlistDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(playlistDuration)
        viewHolder.binding.durationTime.text = playlistDurationReadable
    }

    override fun getItemCount(): Int {
        return playlists.size
    }
}