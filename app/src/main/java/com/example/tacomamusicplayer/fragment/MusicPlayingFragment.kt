package com.example.tacomamusicplayer.fragment

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.navigation.fragment.findNavController
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.FragmentMusicPlayingBinding
import com.example.tacomamusicplayer.enum.ScreenType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber


class MusicPlayingFragment: Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentMusicPlayingBinding

    private var controller: MediaController? = null

    val detector = object : GestureDetector.SimpleOnGestureListener() {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView: ")
        binding = FragmentMusicPlayingBinding.inflate(inflater)

        setupLibraryButtonAnimation(binding)

        val gesture = GestureDetector(container!!.context, detector)

        //TODO clean up this code

        //TODO rename library button -> library section /  its actually an image view
        //TODO set the final behavior for this part of the app, section swipe up or double tap will go to library...

        //TODO I'll have a clear area that will be the swipe zone / double press zone [single press can cause lots of accidental user input]

        //TODO Animation changes work but I will need to fix the horizontal layouts for the APP [WILL CRASH on orientation change!]

        //TODO IMPLEMENT THE GESTURE DETECTION / ADD ANIMATION BETWEEN MUSICPLAYINGFRAGMENT AND MUSICCHOOSINGFRAGMENT

        binding.libraryAnimation!!.setOnTouchListener{ v, event ->
            gesture.onTouchEvent(event)
        }

        return binding.root
    }

    private fun setupLibraryButtonAnimation(binding:FragmentMusicPlayingBinding) {
        binding.libraryAnimation!!.setBackgroundResource(R.drawable.library_animation)
        val frameAnimation = binding.libraryAnimation.background as AnimationDrawable
        frameAnimation.start()
    }

    @OptIn(UnstableApi::class) override fun onStart() {
        super.onStart()

        parentViewModel.mediaController.observe(this) { controller ->
            binding.playerView.player = controller
            this.controller = controller
            binding.playerView.showController()
        }

        parentViewModel.songQueue.observe(this) { songs ->
            controller?.addMediaItems(songs)
        }

        binding.psychoButton.setOnClickListener {
            findNavController().navigate(ScreenType.PERMISSION_DENIED_SCREEN.route())
        }

        binding.navigateChooseMusic.setOnClickListener {
            findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())
        }

//        binding.libraryButton?.setOnClickListener {
//            Timber.d("Button CLicked: ")
//            findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())
//        }
    }

}