package com.andaagii.tacomamusicplayer.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.andaagii.tacomamusicplayer.composables.PermissionDeniedScreen

/**
 * Shown when the `READ_MEDIA_AUDIO` runtime permission has been denied.
 *
 * This fragment has no ViewModel — it delegates all UI to [PermissionDeniedScreen]. The
 * "Open Settings" callback launches the system app-settings page so the user can grant
 * the permission and return to the app.
 */
class PermissionDeniedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PermissionDeniedScreen(
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
