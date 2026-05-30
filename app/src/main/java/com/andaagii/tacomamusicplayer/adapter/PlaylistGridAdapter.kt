package com.andaagii.tacomamusicplayer.adapter

import android.annotation.SuppressLint
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
import com.andaagii.tacomamusicplayer.databinding.ViewholderPlaylistGridLayoutBinding
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import timber.log.Timber
import java.io.File

/**
 * [ListAdapter] for displaying playlists in a two-column grid layout.
 *
 * Inflates `viewholder_playlist_grid_layout.xml` for each cell. The popup menu is triggered by a
 * long-press on the cell, consistent with [AlbumGridAdapter].
 *
 * Uses [MediaItemDiffCallback] for efficient DiffUtil-based list updates.
 *
 * @param onPlaylistClick Invoked when the user taps a grid cell to drill into its track list.
 * @param onPlayIconClick Reserved for future use — not wired in the current grid layout.
 * @param handlePlaylistSetting Invoked when the user selects an item from the long-press popup
 *   menu. Receives the resolved [MenuOptionUtil.MenuOption] and a list containing the playlist title.
 */
class PlaylistGridAdapter(
    private val onPlaylistClick: (MediaItem) -> Unit,
    private val onPlayIconClick: (String) -> Unit,
    val handlePlaylistSetting: (MenuOptionUtil.MenuOption, List<String>) -> Unit
): ListAdapter<MediaItem, PlaylistGridAdapter.PlaylistGridViewHolder>(MediaItemDiffCallback) {

    /** ViewHolder that holds the inflated [ViewholderPlaylistGridLayoutBinding] for a single grid cell. */
    class PlaylistGridViewHolder(val binding: ViewholderPlaylistGridLayoutBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistGridViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderPlaylistGridLayoutBinding.inflate(inflater, parent, false)

        return PlaylistGridViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: PlaylistGridViewHolder, position: Int) {
        val playlist = getItem(position)
        viewHolder.binding.playlistName.text = playlist.mediaMetadata.albumTitle

        val playlistArtUri = playlist.mediaMetadata.artworkUri

        viewHolder.binding.itemContainer.setOnClickListener {
            onPlaylistClick(playlist)
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

        // Show album art based on mediaItem (can either be original or custom)
        val artFile = File(playlistArtUri.toString())
        if(artFile.exists()) {
            viewHolder.binding.playlistArt.setImageURI(playlistArtUri)
        } else {
            viewHolder.binding.playlistArt.setImageDrawable(AppCompatResources.getDrawable(viewHolder.binding.root.context, R.drawable.white_note))
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

    /**
     * Translates the selected popup [item] at [position] into a [MenuOptionUtil.MenuOption] and
     * forwards it to [handlePlaylistSetting] alongside the playlist's title.
     */
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