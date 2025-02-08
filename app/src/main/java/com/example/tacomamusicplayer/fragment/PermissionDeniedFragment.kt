package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tacomamusicplayer.databinding.FragmentPermissionDeniedBinding

class PermissionDeniedFragment: Fragment() {

    private lateinit var binding: FragmentPermissionDeniedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPermissionDeniedBinding.inflate(inflater)
        return binding.root
    }
}