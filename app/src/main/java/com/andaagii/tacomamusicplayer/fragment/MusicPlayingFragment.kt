package com.andaagii.tacomamusicplayer.fragment

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Size
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.navigation.fragment.findNavController
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.databinding.FragmentMusicPlayingBinding
import com.andaagii.tacomamusicplayer.enum.ScreenType
import com.andaagii.tacomamusicplayer.enum.ShuffleType
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class MusicPlayingFragment: Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentMusicPlayingBinding

    private var controller: MediaController? = null
    private var currentSongInfo: SongData? = null

    private val detector = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Timber.d("onDoubleTap: navigate to the music chooser screen!")

            //navigate to the music chooser fragment...
            findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())

            return super.onDoubleTap(e)
        }

        override fun onDown(e: MotionEvent): Boolean {
            Timber.d("onDown: ")
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            Timber.d("onFling: e1=$e1, e2=$e2, velocityX=$velocityX, velocityY=$velocityY")

            if(velocityY < -500) {
                Timber.d("onFling: navigate to the music chooser screen!")

                //navigate to the music chooser fragment...
                findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: ")

        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_down)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView: ")
        binding = FragmentMusicPlayingBinding.inflate(inflater)

//        setupLibraryButtonAnimation(binding)

        val gesture = GestureDetector(container!!.context, detector)

        //TODO clean up this code

        //TODO rename library button -> library section /  its actually an image view

        //TODO Animation changes work but I will need to fix the horizontal layouts for the APP [WILL CRASH on orientation change!]

        //TODO save the currently playing queue for when I exit and start the app again...

        binding.librarySection?.setOnTouchListener { v, event ->
            gesture.onTouchEvent(event)
        }

        binding.queueButton?.setOnClickListener {
            findNavController().navigate(ScreenType.MUSIC_QUEUE_SCREEN.route())
        }

        return binding.root
    }

//    private fun setupLibraryButtonAnimation(binding:FragmentMusicPlayingBinding) {
//        binding.libraryAnimation!!.setBackgroundResource(R.drawable.library_animation)
//        val frameAnimation = binding.libraryAnimation.background as AnimationDrawable
//        frameAnimation.start()
//    }

    private fun updateUIForCurrentSong() {
        updateCurrentSongArt()
        updateCurrentSongTitle()
        updateCurrentSongArtist()
        updateCurrentAlbumTitle()
    }

    private fun updateCurrentSongArt() {
        this.context?.resources?.let { res ->
            this.context?.contentResolver?.let { resolver ->
                controller?.mediaMetadata?.artworkUri?.let { artwork ->
                    try {
                        val songArt = resolver.loadThumbnail(artwork, Size(500, 500), null)
                        val songDrawable = BitmapDrawable(res, songArt)
                        binding.songArt?.setImageDrawable(songDrawable)
                    } catch(e: Exception) {
                        Timber.d("onStart: Couldn't load song art, e=$e")
                    }
                }
            }
        }
    }

    private fun updateCurrentSongTitle() {
        controller?.mediaMetadata?.title?.let { title ->
            binding.songTitleTextview?.text  = title
        }
    }

    private fun updateCurrentSongArtist() {
        controller?.mediaMetadata?.artist?.let { artist ->
            binding.artistNameTextview?.text  = artist
        }
    }

    private fun updateCurrentAlbumTitle() {
        controller?.mediaMetadata?.albumTitle?.let { albumTitle ->
            binding.albumTitleTextview?.text  = albumTitle
        }
    }

    @OptIn(UnstableApi::class) override fun onStart() {
        super.onStart()

        parentViewModel.mediaController.observe(this) { controller ->
            binding.playerView.player = controller
            this.controller = controller
            binding.playerView.showController()

            //Update UI with current song in the controller (this will be called when I first come to this fragment!)
            updateUIForCurrentSong()

            if(controller.isPlaying) {
                binding.playButton?.setBackgroundResource(R.drawable.baseline_pause_24)
            } else {
                binding.playButton?.setBackgroundResource(R.drawable.baseline_play_arrow_24)
            }
        }

        parentViewModel.currentPlayingSongInfo.observe(this) { currentSong ->
            if(currentSong != currentSongInfo) {
                currentSongInfo = currentSong
                updateUIForCurrentSong()
            }
        }

        parentViewModel.repeatMode.observe(this) { repeatMode ->
            Timber.d("onStart: repeatMode=$repeatMode")
            when(repeatMode) {
                Player.REPEAT_MODE_OFF -> {  binding.repeatToggle?.setBackgroundResource(R.drawable.one_x) }
                Player.REPEAT_MODE_ONE -> {  binding.repeatToggle?.setBackgroundResource(R.drawable.repeat_one) }
                Player.REPEAT_MODE_ALL -> {  binding.repeatToggle?.setBackgroundResource(R.drawable.repeat) }
            }
        }

        parentViewModel.isShuffled.observe(this) { isShuffled ->
            Timber.d("onStart: isShuffled=$isShuffled")
            if(isShuffled == ShuffleType.SHUFFLED) {
                binding.shuffleToggle?.setBackgroundResource(R.drawable.shuffle)
            } else {
                binding.shuffleToggle?.setBackgroundResource(R.drawable.right_arrow)
            }
        }

        parentViewModel.isPlaying.observe(this) { isPlaying ->
            Timber.d("onStart: isPlaying=$isPlaying")
            if(isPlaying) {
                binding.playButton?.setBackgroundResource(R.drawable.baseline_pause_24)
            } else {
                binding.playButton?.setBackgroundResource(R.drawable.white_play_arrow)
            }
        }

        binding.prevButton?.setOnClickListener {
            Timber.d("prevButton_onClick: ")
            controller?.seekToPrevious()
        }

        binding.playButton?.setOnClickListener { button ->
            Timber.d("playButton_onClick: ")

            controller?.let {
                if(!it.isPlaying) {
                    button.setBackgroundResource(R.drawable.baseline_pause_24)
                    it.play()
                } else {
                    button.setBackgroundResource(R.drawable.baseline_play_arrow_24)
                    it.pause()
                }
            }
        }

        binding.songArt?.setOnClickListener {
//            AnimationUtils.loadAnimation(this.context, R.anim.fly_up_out).also { animation ->
//                binding.songArt?.startAnimation(animation)
//            }
//            AnimationUtils.loadAnimation(this.context, R.anim.fly_up_in).also { animation ->
//                binding.alternateSongArt?.startAnimation(animation)
//            }

            //TODO If I want to add advanced Animations I will further investigate this code...
//            Handler(Looper.getMainLooper()).postDelayed({
//                //TODO at the end I want the images switched...
////                binding.songArt.setImageDrawable()
////                binding.alternateSongArt.setImageDrawable()
////                binding.songArt?.visibility = View.GONE
////                binding.alternateSongArt?.visibility = View.VISIBLE
//            }, 2000)
        }

        binding.repeatToggle?.setOnClickListener {
            parentViewModel.flipRepeatMode()
        }

        binding.shuffleToggle?.setOnClickListener {
            parentViewModel.flipShuffleState()
        }

        binding.seekBack?.setOnClickListener {
            controller?.let {
                it.seekBack()
            }
        }

        binding.seekForward?.setOnClickListener {
            controller?.let {
                it.seekForward()
            }
        }

        binding.nextButton?.setOnClickListener {
            Timber.d("nextButton_onClick: ")
            controller?.seekToNextMediaItem()
        }
    }

}