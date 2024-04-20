package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tacomamusicplayer.databinding.FragmentMusicPlayingBinding

class MusicPlayingFragment: Fragment() {

    private lateinit var binding: FragmentMusicPlayingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicPlayingBinding.inflate(inflater)
        return binding.root
    }
}