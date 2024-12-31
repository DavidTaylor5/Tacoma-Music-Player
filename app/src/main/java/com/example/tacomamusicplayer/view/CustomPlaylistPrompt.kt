package com.example.tacomamusicplayer.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.tacomamusicplayer.databinding.ViewCustomInformationScreenBinding
import com.example.tacomamusicplayer.databinding.ViewCustomPlaylistPromptBinding

class CustomPlaylistPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomPlaylistPromptBinding

    init {
        attrs?.let {

            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            //I gotta remember to actually attach to the view
            binding = ViewCustomPlaylistPromptBinding.inflate(inflater, this, true)
        }
    }

    fun setPlaylistData() {

    }

    fun setFirstIcon(imageDrawable: Drawable) {
        binding.firstIcon.setImageDrawable(imageDrawable)
    }

    fun setFirstIconCallback(callback: () -> Unit) {
        binding.firstClickable.setOnClickListener {
            callback()
        }
    }

    fun setSecondIcon(imageDrawable: Drawable) {
        binding.secondIcon.setImageDrawable(imageDrawable)
    }

    fun setSecondIconCallback(callback: () -> Unit) {
        binding.secondClickable.setOnClickListener {
            callback()
        }
    }

    fun setFirstInfo(info: String) {
        binding.firstInfo.text = info
    }

    fun setSecondInfo(info: String) {
        binding.secondInfo.text = info
    }
}