package com.andaagii.tacomamusicplayer.fragment.pages

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.andaagii.tacomamusicplayer.adapter.AlbumGridAdapter
import com.andaagii.tacomamusicplayer.adapter.AlbumListAdapter
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.databinding.FragmentAlbumlistBinding
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AlbumListFragment: Fragment() {
    private lateinit var binding: FragmentAlbumlistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    private var currentAlbumList: List<MediaItem> = listOf()

    private var currentSortingType: SortingUtil.SortingOption? = null
    private var currentLayoutType: LayoutType? = null

    //The name of the most recent playlist that I want to update the image for
    private var albumCustomImageName = "empty"

    //Callback for when user chooses a playlist Image
    private val getPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Handle the returned Uri
        val pictureUri = uri

        if(pictureUri == null) {
            Timber.d("getPicture: The picture is null!")
        }

        pictureUri?.let { uri ->
            this.context?.let { fragmentContext ->
                //Save picture to local data
                UtilImpl.saveImageToFile(fragmentContext, uri, albumCustomImageName)
                parentViewModel.updatePlaylistImage(albumCustomImageName, "$albumCustomImageName.jpg")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlbumlistBinding.inflate(inflater)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.availableAlbums.collect { availableAlbums ->
                    currentAlbumList = SortingUtil.sortAlbums(
                        availableAlbums,
                        parentViewModel.sortingForAlbumTab.value
                            ?: SortingUtil.SortingOption.SORTING_NEWEST_RELEASE
                    )

                    if(binding.displayRecyclerview.adapter == null) {
                        //on initial
                        binding.displayRecyclerview.adapter = AlbumListAdapter(
                            currentAlbumList,
                            this@AlbumListFragment::onAlbumClick,
                            parentViewModel::playAlbum,
                            this@AlbumListFragment::handleAlbumSetting
                        )
                    } else {
                        currentLayoutType?.let {
                            updateAlbumLayout(it)
                        }
                        currentSortingType?.let {
                            updateAlbumSorting(it)
                        }
                    }

                }
            }
        }

        parentViewModel.layoutForAlbumTab.observe(viewLifecycleOwner) { layout ->
            updateAlbumLayout(layout)
            currentLayoutType = layout
        }

        parentViewModel.sortingForAlbumTab.observe(viewLifecycleOwner) { sortingOption ->
            updateAlbumSorting(sortingOption)
            currentSortingType = sortingOption
        }

        setupPage()

        return binding.root
    }

    private fun addCustomAlbumImage(customAlbumImageName: String) {
        //Album that I should update the image for
        albumCustomImageName = customAlbumImageName

        // ActivityResultLauncher is able to launch the activity to kick off the request for a result.
        getPicture.launch("image/*")
    }

    private fun updateAlbumSorting(sorting: SortingUtil.SortingOption) {
        Timber.d("updateAlbumSorting: sorting=$sorting")

        //Update the currentAlbumList
        currentAlbumList = SortingUtil.sortAlbums(currentAlbumList, sorting)

        //Set the current album list to be shown
        binding.displayRecyclerview.adapter.let { adapter ->
            when(adapter) {
                is AlbumListAdapter -> {
                    adapter.updateData(currentAlbumList)
                }
                is AlbumGridAdapter -> {
                    adapter.updateData(currentAlbumList)
                }
            }
        }
    }

    private fun updateAlbumLayout(layout: LayoutType) {
        Timber.d("updateAlbumLayout: ")

        //TODO Dangerous, what if I only update one adapter... this is not efficient?
        if(layout == LayoutType.LINEAR_LAYOUT) {
            binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.displayRecyclerview.adapter = AlbumListAdapter(
                currentAlbumList,
                this::onAlbumClick,
                parentViewModel::playAlbum,
                this::handleAlbumSetting
            )
        } else if(layout == LayoutType.TWO_GRID_LAYOUT) {
            binding.displayRecyclerview.layoutManager = GridLayoutManager(context, UtilImpl.determineGridSize())
            binding.displayRecyclerview.adapter = AlbumGridAdapter(
                currentAlbumList,
                this::onAlbumClick,
                parentViewModel::playAlbum,
                this::handleAlbumSetting
            )
        }
    }

    private fun handleAlbumSetting(option: MenuOptionUtil.MenuOption, album: String, customAlbumImageName: String? = null) {
        when (option) {
            MenuOptionUtil.MenuOption.PLAY_ALBUM ->  {
                parentViewModel.playAlbum(album)
            }
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> {
                parentViewModel.addAlbumToBackOfQueue(album)
            }
            MenuOptionUtil.MenuOption.ADD_ALBUM_IMAGE -> {
                customAlbumImageName?.let {
                    addCustomAlbumImage(customAlbumImageName)
                }
            }
            else -> {
                Timber.d("handleAlbumSetting: unhandled album menu option")
            }
        }
    }

    private fun onAlbumClick(albumTitle: String) {
        parentViewModel.querySongsFromAlbum(albumTitle)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }

    private fun setupPage() {
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}