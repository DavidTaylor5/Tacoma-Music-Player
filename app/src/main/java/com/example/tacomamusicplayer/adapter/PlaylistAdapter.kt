package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.data.Playlist
import com.example.tacomamusicplayer.databinding.ViewholderPlaylistBinding
import com.example.tacomamusicplayer.util.MenuOptionUtil
import com.example.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

class PlaylistAdapter(
    private val playlists:  List<Playlist>,
    private val onAlbumClick: (String) -> Unit,
    val handlePlaylistSetting: (MenuOptionUtil.MenuOption, List<String>) -> Unit
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

        viewHolder.binding.itemContainer.setOnClickListener {
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

        viewHolder.binding.menuIcon.setOnClickListener {

            val menu = PopupMenu(viewHolder.itemView.context, viewHolder.binding.menuIcon)

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
        when(MenuOptionUtil.determineMenuOptionFromTitle(item.title.toString())) {
            MenuOptionUtil.MenuOption.PLAY_PLAYLIST_ONLY -> {
                handlePlaylistSetting(
                    MenuOptionUtil.MenuOption.PLAY_PLAYLIST_ONLY,
                    listOf(playlists[position].title)
                )
            }
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> addPlaylistToQueue()
            MenuOptionUtil.MenuOption.RENAME_PLAYLIST -> renamePlaylists(listOf(), listOf())
            MenuOptionUtil.MenuOption.ADD_PLAYLIST_IMAGE -> addPlaylistImage()
            MenuOptionUtil.MenuOption.REMOVE_PLAYLIST -> {
                handlePlaylistSetting(
                    MenuOptionUtil.MenuOption.REMOVE_PLAYLIST,
                    listOf(playlists[position].title)
                )
            }
            else -> Timber.d("handleMenuItem: UNKNOWN menuitem...")
        }
    }

    private fun addPlaylistToQueue() {
        //todo parentViewModel.addPlaylistToQueue(playlistTitle?)
    }

    private fun renamePlaylists(oldNames: List<String>, newNames: List<String>) {
        //TODO should bring up the renaming prompt
    }

    private fun addPlaylistImage() {
        //TODO logic for grabbing an image
        //TODO logic for saving that image in the database
        //TODO Can I save an app to local app data?
    }

    override fun getItemCount(): Int {
        return playlists.size
    }
}