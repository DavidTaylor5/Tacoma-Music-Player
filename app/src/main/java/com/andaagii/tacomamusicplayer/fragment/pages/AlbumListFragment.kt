package com.andaagii.tacomamusicplayer.fragment.pages

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.AlbumGridAdapter
import com.andaagii.tacomamusicplayer.adapter.AlbumListAdapter
import com.andaagii.tacomamusicplayer.databinding.FragmentAlbumlistBinding
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.AlbumTabViewModel
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AlbumListFragment: Fragment() {
    private lateinit var binding: FragmentAlbumlistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    private val viewModel: AlbumTabViewModel by activityViewModels()

    //The name of the most recent playlist that I want to update the image for
    private var albumCustomImageName = "empty"
    private var selectedAlbumName = "unknown"

    private var currLayout: LayoutType = LayoutType.LINEAR_LAYOUT

    private val getCroppedPicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == RESULT_OK) {
            Timber.d("getCroppedPicture: RESULT_OK")
            result.data?.let { cropData ->
                val croppedUri = UCrop.getOutput(cropData)
                croppedUri?.let { uri ->
                    parentViewModel.updateSongGroupImage(
                        title = selectedAlbumName,
                        artFileName = uri.path.toString()
                    )
                }
            }
        } else if(result.resultCode == UCrop.RESULT_ERROR) {
            val error = result.data
            Timber.d("getCroppedPicture: RESULT_ERROR cropError=${error?.let { e ->  UCrop.getError(e)} }")
        }
    }

    //Callback for when user chooses a playlist Image
    private val getPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Handle the returned Uri
        val pictureUri = uri

        if(pictureUri == null) {
            Timber.d("getPicture: The picture is null!")
        }

        pictureUri?.let { uri ->
            val saveFileUri = UtilImpl.getSaveFileUri(
                context = requireContext(),
                fileName = selectedAlbumName,
                isCustom = true
            )
            UCrop.of(uri, saveFileUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(700, 700)
                .start(requireActivity(), getCroppedPicture)
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
                viewModel.albumTabState.collect { state ->

                    // Sort the albums by user preference
                    val sortedAlbums = SortingUtil.sortAlbums(
                        state.albums,
                        state.sorting
                    )

                    // Adjust Layout button
                    currLayout = state.layout
                    if(currLayout == LayoutType.LINEAR_LAYOUT) {
                        binding.layoutOption.setBackgroundResource(R.drawable.baseline_table_rows_24)
                    } else {
                        binding.layoutOption.setBackgroundResource(R.drawable.baseline_grid_view_24)
                    }

                    // check if adapter is initialized
                    if(binding.displayRecyclerview.adapter != null) {

                        // check if layout matches expected layout
                        if(binding.displayRecyclerview.adapter is AlbumListAdapter
                            && state.layout != LayoutType.LINEAR_LAYOUT) {
                            initializeGridLayout(albums = sortedAlbums)
                        } else if(binding.displayRecyclerview.adapter is AlbumGridAdapter
                            && state.layout != LayoutType.TWO_GRID_LAYOUT) {
                            initializeLinearLayout(albums = sortedAlbums)
                        } else {
                            val adapter = binding.displayRecyclerview.adapter
                            when(adapter) {
                                is AlbumListAdapter -> { adapter.submitList(sortedAlbums) }
                                is AlbumGridAdapter -> { adapter.submitList(sortedAlbums) }
                                else -> { Timber.e("onCreateView: Error Unable to submit list of unknown adapter type.")}
                            }
                        }
                    } else {
                        //album is not initialized
                        when(state.layout) {
                            LayoutType.LINEAR_LAYOUT -> { initializeLinearLayout(sortedAlbums) }
                            LayoutType.TWO_GRID_LAYOUT -> { initializeGridLayout(sortedAlbums) }
                        }
                    }
                }
            }
        }

        binding.layoutOption.setOnClickListener {
            if(currLayout == LayoutType.LINEAR_LAYOUT) {
                viewModel.saveAlbumLayout(requireContext(), LayoutType.TWO_GRID_LAYOUT)
            } else {
                viewModel.saveAlbumLayout(requireContext(), LayoutType.LINEAR_LAYOUT)
            }
        }

        binding.settingsOption.setOnClickListener {
            val menu = PopupMenu(
                this.context,
                binding.settingsOption,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )
            menu.menuInflater.inflate(R.menu.sorting_options_album, menu.menu)

            menu.setOnMenuItemClickListener {
                Toast.makeText(this.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()

                //Update the Sorting for the tab.
                val chosenSortingOption = SortingUtil.determineSortingOptionFromTitle(it.title.toString())
                viewModel.saveAlbumSorting(requireContext(), chosenSortingOption)

                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        return binding.root
    }

    private fun initializeLinearLayout(
        albums: List<MediaItem>
    ) {
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.displayRecyclerview.adapter = AlbumListAdapter(
            this@AlbumListFragment::onAlbumClick,
            parentViewModel::playAlbum,
            this@AlbumListFragment::handleAlbumSetting
        )
        (binding.displayRecyclerview.adapter as AlbumListAdapter)
            .submitList(albums)
    }

    private fun initializeGridLayout(
        albums: List<MediaItem>
    ) {
        binding.displayRecyclerview.layoutManager = GridLayoutManager(context, UtilImpl.determineGridSize())
        binding.displayRecyclerview.adapter = AlbumGridAdapter(
            this@AlbumListFragment::onAlbumClick,
            parentViewModel::playAlbum,
            this@AlbumListFragment::handleAlbumSetting
        )
        (binding.displayRecyclerview.adapter as AlbumGridAdapter)
            .submitList(albums)
    }

    private fun addCustomAlbumImage(album:MediaItem, customAlbumImageName: String) {
        //Album that I should update the image for
        albumCustomImageName = customAlbumImageName
        selectedAlbumName = album.mediaMetadata.albumTitle.toString()

        // ActivityResultLauncher is able to launch the activity to kick off the request for a result.
        getPicture.launch("image/*")
    }

    private fun handleAlbumSetting(option: MenuOptionUtil.MenuOption, album: MediaItem, customAlbumImageName: String? = null) {
        when (option) {
            MenuOptionUtil.MenuOption.PLAY_ALBUM ->  {
                parentViewModel.playAlbum(album)
            }
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> {
                parentViewModel.addAlbumToBackOfQueue(album)
            }
            MenuOptionUtil.MenuOption.ADD_ALBUM_IMAGE -> {
                customAlbumImageName?.let {
                    addCustomAlbumImage(album, customAlbumImageName)
                }
            }
            else -> {
                Timber.d("handleAlbumSetting: unhandled album menu option")
            }
        }
    }

    private fun onAlbumClick(album: MediaItem) {
        parentViewModel.querySongsFromAlbum(album)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }
}