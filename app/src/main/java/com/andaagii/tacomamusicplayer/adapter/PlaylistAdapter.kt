package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.databinding.ViewholderPlaylistBinding
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber
import java.io.File

class PlaylistAdapter(
    private var playlists:  List<Playlist>,
    private val onPlaylistClick: (String) -> Unit,
    private val onPlayIconClick: (String) -> Unit,
    val handlePlaylistSetting: (MenuOptionUtil.MenuOption, List<String>) -> Unit,
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

    fun updateData(playlists: List<Playlist>) {
        this.playlists = playlists
        this.notifyDataSetChanged()
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: PlaylistViewHolder, position: Int) {
        viewHolder.binding.playlistName.text = playlists[position].title

        viewHolder.binding.itemContainer.setOnClickListener {
            onPlaylistClick(playlists[position].title)
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

        //Logic for showing custom playist image
        val artFile = playlists[position].artFile
        if(!artFile.isNullOrEmpty()) {
            val appDir = viewHolder.itemView.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val playlistImageFile = File(appDir, artFile)
            if(playlistImageFile.exists()) {
                try {
                    val artUri = Uri.fromFile(playlistImageFile)
                    viewHolder.binding.playlistArt.setImageURI(artUri)
                } catch(e: Exception) {
                    Timber.d("onBindViewHolder: exception when setting playlist art e=$e")
                }
            }
        }

        viewHolder.binding.playButton.setOnClickListener {
            onPlayIconClick(playlists[viewHolder.absoluteAdapterPosition].title)
        }

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
        val playlistTitle = playlists[position].title
        val menuOption = MenuOptionUtil.determineMenuOptionFromTitle(item.title.toString())
        Timber.d("handleMenuItem: menuOption=$menuOption playlistTitle=$playlistTitle")
        handlePlaylistSetting(
            menuOption,
            listOf(playlistTitle)
        )
    }

    override fun getItemCount(): Int {
        return playlists.size
    }
}