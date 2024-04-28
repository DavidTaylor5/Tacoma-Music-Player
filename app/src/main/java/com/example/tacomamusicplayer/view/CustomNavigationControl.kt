package com.example.tacomamusicplayer.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.CustomExoControllerUpdateBinding
import com.example.tacomamusicplayer.databinding.ViewCustomNavigationControlBinding
import com.example.tacomamusicplayer.enum.PageType

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

            //I gotta remember to actually attach to the view
            binding = ViewCustomNavigationControlBinding.inflate(inflater, this, true)

            val typedArray = context.obtainStyledAttributes(it, R.styleable.CustomNavigationControl)

            //possible xml custom attributes
            val playlistImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_playlistImg, 0)
            val albumBrowseImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_albumBrowseImg, 0)
            val albumImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_albumImg, 0)

            binding.playlistButton.setImageDrawable(ContextCompat.getDrawable(context, playlistImg))
            binding.albumlistButton.setImageDrawable(ContextCompat.getDrawable(context, albumBrowseImg))
            binding.songlistButton.setImageDrawable(ContextCompat.getDrawable(context, albumImg))

            setFocusOnNavigationButton(defaultPage)
        }

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

    fun setFocusOnNavigationButton(page: PageType) {
        when(page) {
            PageType.PLAYLIST_PAGE -> {
                binding.playlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.playlist_button_focused))
                binding.albumlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
                binding.songlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
            }
            PageType.ALBUM_PAGE -> {
                binding.playlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
                binding.albumlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.albumlist_button_focused))
                binding.songlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
            }
            PageType.SONG_PAGE -> {
                binding.playlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
                binding.albumlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.unfocused_button))
                binding.songlistButton.setBackgroundColor(ContextCompat.getColor(context, R.color.songlist_button_focused))
            }
        }
    }
}