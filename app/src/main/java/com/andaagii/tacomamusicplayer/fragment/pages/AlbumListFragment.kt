package com.andaagii.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.andaagii.tacomamusicplayer.adapter.AlbumGridAdapter
import com.andaagii.tacomamusicplayer.adapter.AlbumListAdapter
import com.andaagii.tacomamusicplayer.databinding.FragmentAlbumlistBinding
import com.andaagii.tacomamusicplayer.enum.LayoutType
import com.andaagii.tacomamusicplayer.enum.PageType
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class AlbumListFragment(

): Fragment() {

    private lateinit var binding: FragmentAlbumlistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    private var currentAlbumList: List<MediaItem> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAlbumlistBinding.inflate(inflater)

        val observer: Observer<List<MediaItem>> =
            Observer { mediaList ->

                currentAlbumList = mediaList

                Timber.d("onCreateView: found albumList.size=${mediaList.size}")
                binding.displayRecyclerview.adapter = AlbumListAdapter(
                    mediaList,
                    this::onAlbumClick,
                    parentViewModel::playAlbum
                )
            }

        //Now I just need to create different fragments for each type?
        parentViewModel.albumMediaItemList.observe(viewLifecycleOwner, observer)

        binding.layoutButton.setOnClickListener {
            //update the current layout...
            //If I'm on gridlayout, switch to linear layout and vice versa.
            if(parentViewModel.layoutForAlbumTab.value == LayoutType.LINEAR_LAYOUT) {
                //Update Layout State / Save to datastore
                parentViewModel.saveAlbumLayout(requireContext(), LayoutType.TWO_GRID_LAYOUT)
            } else {
                //Update Layout State / Save to datastore
                parentViewModel.saveAlbumLayout(requireContext(), LayoutType.LINEAR_LAYOUT)
            }
        }

        parentViewModel.layoutForAlbumTab.observe(viewLifecycleOwner) { layout ->
            updateAlbumLayout(layout)
        }

        parentViewModel.sortingForAlbumTab.observe(viewLifecycleOwner) { sortingOption ->
            //updateAlbumSorting?
        }

        setupPage()

        return binding.root
    }

    private fun updateAlbumSorting(sorting: SortingUtil.SortingOption) {
        Timber.d("updateAlbumSorting: ")

        //TODO SORT currentAlbumList, then set to currentAlbumList

        binding.displayRecyclerview.adapter.let { adapter ->
            when(adapter) {
                is AlbumListAdapter -> {
                    //TODO call adapter.updateData(currentAlbumList)
                    (adapter as AlbumListAdapter).updateData(currentAlbumList)
                }
                is AlbumGridAdapter -> {
                    //TODO call adapter. updateData(currentAlbumList)
                    (adapter as AlbumGridAdapter).updateData(currentAlbumList)
                }
            }
        }
    }

    private fun updateAlbumLayout(layout: LayoutType) {
        Timber.d("updateAlbumLayout: ")

        //TODO Dangerous, what if I only update one adapter... this is not efficient?
        if(layout == LayoutType.LINEAR_LAYOUT) {
            binding.layoutButton.text = LayoutType.LINEAR_LAYOUT.type()
            binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.displayRecyclerview.adapter = AlbumListAdapter(
                currentAlbumList,
                this::onAlbumClick,
                parentViewModel::playAlbum
            )
        } else if(layout == LayoutType.TWO_GRID_LAYOUT) {
            binding.layoutButton.text = LayoutType.TWO_GRID_LAYOUT.type()
            binding.displayRecyclerview.layoutManager = GridLayoutManager(context, 2)
            binding.displayRecyclerview.adapter = AlbumGridAdapter(
                currentAlbumList,
                this::onAlbumClick,
                parentViewModel::playAlbum
            )
        }
    }

    private fun onAlbumClick(albumTitle: String) {
        parentViewModel.querySongsFromAlbum(albumTitle)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }

    private fun setupPage() {
        binding.sectionTitle.text = "ALBUMS"

        //TODO allow the user to choose between linear playlists and grid playlists
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//        binding.displayRecyclerview.layoutManager = GridLayoutManager(context, 2)
    }
}