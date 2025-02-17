package com.example.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.tacomamusicplayer.databinding.ViewCustomInputTextPromptBinding

class CustomInputTextPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomInputTextPromptBinding

    private var option1ButtonOnClick : () -> Unit = {}
    private var option2ButtonOnClick : () -> Unit = {}

    init {
        attrs?.let {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomInputTextPromptBinding.inflate(inflater, this, true)

            binding.option1Button.setOnClickListener { option1ButtonOnClick() }

            binding.option2Button.setOnClickListener { option2ButtonOnClick() }
        }
    }

    fun setTextInputHint(hint: String) {
        binding.textPromptLayout.hint = hint
    }

    fun setOption1ButtonText(b1Text: String) {
        binding.option1Button.text = b1Text
    }

    fun setOption2ButtonText(b2Text: String) {
        binding.option2Button.text = b2Text
    }

    fun closePrompt() {
        this.visibility = View.GONE
    }

    fun showPrompt() {
        this.visibility = View.VISIBLE
    }

    fun setOption1ButtonOnClick(onClick: () -> Unit) {
        option1ButtonOnClick = onClick
    }

    fun setOption2ButtonOnClick(onClick: () -> Unit) {
        option2ButtonOnClick = onClick
    }

    fun getUserInputtedText(): String {
        return binding.promptInput.text.toString()
    }

    fun resetUserInput() {
        binding.promptInput.setText("")
    }
}