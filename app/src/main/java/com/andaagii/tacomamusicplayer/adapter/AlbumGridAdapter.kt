package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.diff.MediaItemDiffCallback
import com.andaagii.tacomamusicplayer.databinding.ViewholderAlbumGridLayoutBinding
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber
import java.io.File

/**
 * A recyclerview adapter that is able to take a list of Album Media Items and display them.
 */
class AlbumGridAdapter(
    private val onAlbumClick: (MediaItem) -> Unit,
    private val onPlayIconClick: (MediaItem) -> Unit,
    private val handleAlbumOption: (MenuOptionUtil.MenuOption, MediaItem, String?) -> Unit,
): ListAdapter<MediaItem, AlbumGridAdapter.AlbumGridViewHolder>(MediaItemDiffCallback) {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class AlbumGridViewHolder(val binding : ViewholderAlbumGridLayoutBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumGridViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        //TODO I will also need an if statement if I want to switch between bindings
        val binding = ViewholderAlbumGridLayoutBinding.inflate(inflater, parent, false)

        return AlbumGridViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    @OptIn(UnstableApi::class) override fun onBindViewHolder(viewHolder: AlbumGridViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        val album = getItem(position)
        //val albumMetadata = album.mediaMetadata
        val albumArtUri = album.mediaMetadata.artworkUri
        val customImage = UtilImpl.getImageBaseNameFromExternalStorage(
            groupTitle = album.mediaMetadata.albumTitle.toString(),
            artist = album.mediaMetadata.albumArtist.toString(),
            songGroupType = SongGroupType.ALBUM
        )
        Timber.d("onBindViewHolder: CHECKING VALUES albumTitle=${album.mediaMetadata.albumTitle}, albumArtist=${album.mediaMetadata.albumArtist}, albumArtUri=${album.mediaMetadata.artworkUri}")

        val albumTitle = album.mediaMetadata.albumTitle
        val albumArtist = album.mediaMetadata.albumArtist
        val albumUri = album.mediaMetadata.artworkUri ?: Uri.EMPTY

        viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(album) }

        // Show album art based on mediaItem (can either be original or custom)
        val artFile = File(albumArtUri.toString())
        if(artFile.exists()) {
            viewHolder.binding.albumArt.setImageURI(albumArtUri)
        } else {
            viewHolder.binding.albumArt.setImageDrawable(AppCompatResources.getDrawable(viewHolder.binding.root.context, R.drawable.white_note))
        }

        viewHolder.binding.albumName.text = albumTitle

        album.mediaMetadata.releaseYear?.let { year ->
            if(year > 0) {
                viewHolder.binding.descriptionText.text = "$year | $albumArtist"
            }
        }

        viewHolder.binding.itemContainer.setOnLongClickListener {
            val menu = PopupMenu(
                viewHolder.itemView.context,
                viewHolder.binding.itemContainer,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )

            menu.menuInflater.inflate(R.menu.album_options, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(viewHolder.itemView.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()

                //Handle Album Option
                val customImageName = UtilImpl.getImageBaseNameFromExternalStorage(
                    groupTitle = album.mediaMetadata.albumTitle.toString(),
                    artist = album.mediaMetadata.albumArtist.toString(),
                    songGroupType = SongGroupType.ALBUM
                )
                handleAlbumOption(
                    MenuOptionUtil.determineMenuOptionFromTitle(it.title.toString()),
                    album,
                    customImageName
                )

                return@setOnMenuItemClickListener true
            }
            menu.show()

            return@setOnLongClickListener true
        }
    }
}