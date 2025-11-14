package com.andaagii.tacomamusicplayer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.databinding.ViewholderPlaylistGridLayoutBinding
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber
import java.io.File

class PlaylistGridAdapter(
    private var playlists:  List<MediaItem>,
    private val onPlaylistClick: (MediaItem) -> Unit,
    private val onPlayIconClick: (String) -> Unit,
    val handlePlaylistSetting: (MenuOptionUtil.MenuOption, List<String>) -> Unit
): RecyclerView.Adapter<PlaylistGridAdapter.PlaylistGridViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class PlaylistGridViewHolder(val binding: ViewholderPlaylistGridLayoutBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistGridViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderPlaylistGridLayoutBinding.inflate(inflater, parent, false)

        return PlaylistGridViewHolder(binding)
    }

    fun updateData(playlists: List<MediaItem>) {
        this.playlists = playlists
        this.notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: PlaylistGridViewHolder, position: Int) {
        viewHolder.binding.playlistName.text = playlists[position].mediaMetadata.albumTitle

        viewHolder.binding.itemContainer.setOnClickListener {
            onPlaylistClick(playlists[position])
        }

//        //Determine Playlist Duration Information //TODO how can I update duration and track #
//        val numberOfSongs = playlists[position].songs.songs.size
//
//        val playlistDuration = playlists[position].songs.songs.fold(0L) { acc, songData ->
//            val songDuration = songData.duration.toLongOrNull() ?: 0
//            acc + songDuration
//        }
//
//        val playlistDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(playlistDuration)
//
//        val durationTracks = if(numberOfSongs == 1) "1 track" else {"$numberOfSongs tracks"}
//
//        viewHolder.binding.descriptionText.text = "$durationTracks | $playlistDurationReadable"

        //Logic for showing custom playist image
        val artFile = playlists[position].mediaMetadata.artworkUri.toString() //TODO this might need to be updated.
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

        viewHolder.binding.itemContainer.setOnLongClickListener {

            Toast.makeText(viewHolder.itemView.context, "Long Click on Playlist!", Toast.LENGTH_SHORT).show()

            val menu = PopupMenu(
                viewHolder.itemView.context,
                viewHolder.binding.playlistName,
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

            return@setOnLongClickListener true
        }
    }

    private fun handleMenuItem(item: MenuItem, position: Int) {
        val playlistTitle = playlists[position].mediaMetadata.albumTitle.toString()
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