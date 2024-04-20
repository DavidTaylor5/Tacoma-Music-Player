package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.AlbumAdapter
import com.example.tacomamusicplayer.databinding.FragmentScreenSlidePageBinding

class ScreenSlidePageFragment(val position: Int): Fragment() {

    private lateinit var binding: FragmentScreenSlidePageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        binding = FragmentScreenSlidePageBinding.inflate(inflater)

        binding.sectionTitle.text = "sectionTitle: " + position.toString()

        binding.displayRecyclerview.adapter = AlbumAdapter(listOf("What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip"))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        return binding.root
    }
}