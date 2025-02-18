package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.andaagii.tacomamusicplayer.databinding.CustomSongGroupInfoViewBinding

class CustomSongGroupInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: CustomSongGroupInfoViewBinding

    private var onMenuIconPressed : () -> Unit = {}


    init {
        attrs?.let {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = CustomSongGroupInfoViewBinding.inflate(inflater, this, true)

            binding.menuIcon.setOnClickListener {
                onMenuIconPressed()
            }
        }
    }

    fun setOnMenuIconPressed(onPress: () -> Unit) {
        onMenuIconPressed = onPress
    }

    fun setSongGroupTitleText(title: String) {
        binding.songGroupTitle.text = title
        binding.songGroupImage
    }

    fun getMenuIconView(): View { return binding.menuIcon }

    fun getSongGroupImage(): ImageView { return binding.songGroupImage }
}