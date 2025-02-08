package com.example.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.tacomamusicplayer.databinding.ViewCustomCreatePlaylistPromptBinding

class CustomCreatePlaylistPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomCreatePlaylistPromptBinding

    private var onAddButtonClicked : () -> Unit = {}
    private var onCancelButtonClicked : () -> Unit = {}

    init {
        attrs?.let {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomCreatePlaylistPromptBinding.inflate(inflater, this, true)

            binding.addButton.setOnClickListener { onAddButtonClicked() }

            binding.cancelButton.setOnClickListener { onCancelButtonClicked() }
        }
    }

    fun closePrompt() {
        this.visibility = View.GONE
    }

    fun showPrompt() {
        this.visibility = View.VISIBLE
    }

    fun setAddButtonFunctionality(onClick: () -> Unit) {
        onAddButtonClicked = onClick
    }

    fun setCancelButtonFunctionality(onClick: () -> Unit) {
        onCancelButtonClicked = onClick
    }

    fun getCurrentPlaylistTitle(): String {
        return binding.createPlaylistTitleInputText.text.toString()
    }

    fun resetCurrentPlaylistTitle() {
        binding.createPlaylistTitleInputText.setText("")
    }
}