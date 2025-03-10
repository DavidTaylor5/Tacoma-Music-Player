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

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewCustomMultiSelectPromptBinding.inflate(inflater, this, true)
    }
}