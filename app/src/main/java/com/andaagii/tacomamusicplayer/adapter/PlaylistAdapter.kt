package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.diff.MediaItemDiffCallback
import com.andaagii.tacomamusicplayer.databinding.ViewholderPlaylistBinding
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import timber.log.Timber
import java.io.File

class PlaylistAdapter(
    private val onPlaylistClick: (MediaItem) -> Unit,
    private val onPlayIconClick: (String) -> Unit,
    val handlePlaylistSetting: (MenuOptionUtil.MenuOption, List<String>) -> Unit,
): ListAdapter<MediaItem, PlaylistAdapter.PlaylistViewHolder>(MediaItemDiffCallback) {

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
        val playlist = getItem(position)

        viewHolder.binding.playlistName.text = playlist.mediaMetadata.albumTitle

        val playlistArtUri = playlist.mediaMetadata.artworkUri

        viewHolder.binding.itemContainer.setOnClickListener {
            onPlaylistClick(playlist)
        }

        //Determine Playlist Duration Information TODO how can I get the track numbers....
//        val numberOfSongs = playlists[position].songs.songs.size
//        viewHolder.binding.durationTracks.text = if(numberOfSongs == 1) "1 track" else {"$numberOfSongs tracks"}
//
//        val playlistDuration = playlists[position].songs.songs.fold(0L) { acc, songData ->
//            val songDuration = songData.duration.toLongOrNull() ?: 0
//            acc + songDuration
//        }

        //val playlistDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(playlistDuration)
        //viewHolder.binding.durationTime.text = playlistDurationReadable

        // Show album art based on mediaItem (can either be original or custom)
        val artFile = File(playlistArtUri.toString())
        if(artFile.exists()) {
            viewHolder.binding.playlistArt.setImageURI(playlistArtUri)
        } else {
            viewHolder.binding.playlistArt.setImageDrawable(AppCompatResources.getDrawable(viewHolder.binding.root.context, R.drawable.white_note))
        }

        viewHolder.binding.playButton.setOnClickListener {
            onPlayIconClick(getItem(viewHolder.absoluteAdapterPosition).mediaMetadata.albumTitle.toString()) //TODO why absolute position?...
        }

        viewHolder.binding.menuIcon.setOnClickListener {
            val menu = PopupMenu(
                viewHolder.itemView.context,
                viewHolder.binding.menuIcon,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )

            menu.menuInflater.inflate(R.menu.playlist_options, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(viewHolder.itemView.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                handleMenuItem(it, position)
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }
    }

    private fun handleMenuItem(item: MenuItem, position: Int) {
        val playlistTitle = getItem(position).mediaMetadata.albumTitle.toString()
        val menuOption = MenuOptionUtil.determineMenuOptionFromTitle(item.title.toString())
        Timber.d("handleMenuItem: menuOption=$menuOption playlistTitle=$playlistTitle")
        handlePlaylistSetting(
            menuOption,
            listOf(playlistTitle)
        )
    }
}