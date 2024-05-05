package com.example.tacomamusicplayer.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.tacomamusicplayer.databinding.ViewCustomInformationScreenBinding

class CustomInformationScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomInformationScreenBinding

    init {
        attrs?.let {

            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            //I gotta remember to actually attach to the view
            binding = ViewCustomInformationScreenBinding.inflate(inflater, this, true)
        }
    }

    fun setImageIcon(imageDrawable: Drawable) {
        binding.informationIcon.setImageDrawable(imageDrawable)
    }

    fun setInformationText(info: String) {
        binding.informationText.text = info
    }

    fun setImageClickCallback(callback: () -> Unit) {
        binding.informationIcon.setOnClickListener {
            callback()
        }
    }
}