package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.databinding.ViewCustomNavigationControlBinding
import com.andaagii.tacomamusicplayer.enumtype.PageType
import timber.log.Timber

/**
 * CustomNavigationControl is used to move between music chooser fragments, playlists, albums, songs.
 */
class CustomNavigationControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomNavigationControlBinding
    private val defaultPage: PageType = PageType.PLAYLIST_PAGE

    init {

        attrs?.let {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomNavigationControlBinding.inflate(inflater, this, true)

            val typedArray = context.obtainStyledAttributes(it, R.styleable.CustomNavigationControl)

            //possible xml custom attributes

            val queueImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_queueImg, 0)
            val playerImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_playerImg, 0)
            val playlistImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_playlistImg, 0)
            val albumBrowseImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_albumBrowseImg, 0)
            val albumImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_albumImg, 0)

            typedArray.close()

            binding.queueButton?.setImageDrawable(ContextCompat.getDrawable(context, queueImg))
            binding.playerButton?.setImageDrawable(ContextCompat.getDrawable(context, playerImg))
            binding.playlistButton.setImageDrawable(ContextCompat.getDrawable(context, playlistImg))
            binding.albumlistButton.setImageDrawable(ContextCompat.getDrawable(context, albumBrowseImg))
            binding.songlistButton.setImageDrawable(ContextCompat.getDrawable(context, albumImg))

            setFocusOnNavigationButton(defaultPage)
        }

    }

    fun setQueueButtonOnClick(callback: () -> Unit) {
        binding.queueButton?.setOnClickListener { callback() }
    }

    fun setPlayerButtonOnClick(callback: () -> Unit) {
        binding.playerButton?.setOnClickListener { callback() }
    }

    fun setPlaylistButtonOnClick(callback: () -> Unit) {
        binding.playlistButton.setOnClickListener { callback() }
    }

    fun setBrowseAlbumButtonOnClick(callback: () -> Unit) {
        binding.albumlistButton.setOnClickListener { callback() }
    }

    fun setAlbumButtonOnClick(callback: () -> Unit) {
        binding.songlistButton.setOnClickListener { callback() }
    }

    fun setAllIconBackgroundsGrey() {
        binding.queueButton?.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
        binding.playerButton?.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
        binding.playlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
        binding.albumlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
        binding.songlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
    }


    fun setFocusOnNavigationButton(page: PageType) {
        Timber.d("setFocusOnNavigationButton: page=$page")
        setAllIconBackgroundsGrey()
        when(page) {
            PageType.QUEUE_PAGE -> {
                binding.queueButton?.setBackgroundColor(ContextCompat.getColor(context, R.color.accent))
            }
            PageType.PLAYER_PAGE -> {
                binding.playerButton?.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green))
            }
            PageType.PLAYLIST_PAGE -> {
                binding.playlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.playlist_button_focused))
            }
            PageType.ALBUM_PAGE -> {
                binding.albumlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.albumlist_button_focused))
            }
            PageType.SONG_PAGE -> {
                binding.songlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.songlist_button_focused))
            }
        }
    }
}