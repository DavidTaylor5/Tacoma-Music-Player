package com.andaagii.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.andaagii.tacomamusicplayer.databinding.FragmentPermissionDeniedBinding

/**
 * Shown when the `READ_MEDIA_AUDIO` runtime permission has been denied.
 *
 * This fragment has no ViewModel — it only inflates its layout. The "Open Settings"
 * button that deep-links to the app's system settings page is wired entirely in the
 * XML binding.
 */
class PermissionDeniedFragment : Fragment() {

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
