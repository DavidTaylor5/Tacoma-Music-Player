package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.andaagii.tacomamusicplayer.databinding.ViewCustomMultiSelectPromptBinding

class CustomMultiSelectPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomMultiSelectPromptBinding

    private var menuIconOnClick : () -> Unit = {}
    private var closeIconOnClick: () -> Unit = {}

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewCustomMultiSelectPromptBinding.inflate(inflater, this, true)

        binding.menuIcon.setOnClickListener {
            menuIconOnClick()
        }

        binding.closeIcon.setOnClickListener {
            closeIconOnClick()
        }
    }

    fun setPromptText(text: String) {
        binding.descriptionText.text = text
    }

    fun setOnMenuIconClick(onClick: () -> Unit) {
        menuIconOnClick = onClick
    }

    fun setOnCloseIconClick(onClick: () -> Unit) {
        closeIconOnClick = onClick
    }
}