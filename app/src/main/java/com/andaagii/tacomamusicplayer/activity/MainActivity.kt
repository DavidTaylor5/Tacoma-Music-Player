package com.andaagii.tacomamusicplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.andaagii.tacomamusicplayer.composables.TacomaMusicPlayerApp
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.worker.CatalogMusicWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.UUID

// Preferences DataStore, for storing settings in the app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The sole [ComponentActivity] in the app.
 *
 * Responsibilities:
 * - Calls [enableEdgeToEdge] and [setContent] with [TacomaMusicPlayerApp], which owns the
 *   Compose [androidx.navigation.compose.NavHost] and all screen navigation logic.
 * - Enqueues [CatalogMusicWorker] via [WorkManager] to scan and catalog the device library
 *   after audio permission is granted (called from [TacomaMusicPlayerApp] via [queryMusic]).
 * - Re-hides system UI chrome in [onResume] via [UtilImpl.hideNavigationUI].
 * - Persists the playback queue to the database in [onPause].
 * - Forwards runtime permission results to [MainViewModel.handlePermissionResult].
 *
 * The [dataStore] extension property is a top-level DataStore instance that backs
 * [com.andaagii.tacomamusicplayer.util.DataStoreUtil] preferences.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by viewModels()

    private lateinit var workManager: WorkManager
    private lateinit var currentWorkerId: UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TacomaMusicPlayerApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume: ")
        // Re-hide system bars on every resume in case the OS restored them (e.g. after a dialog).
        UtilImpl.hideNavigationUI(window)
        viewModel.checkPermissionsIfOnPermissionDeniedScreen()
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveQueue()
    }

    /**
     * Receives the result of a runtime permission request and forwards it to [MainViewModel].
     *
     * [MainViewModel.handlePermissionResult] updates [MainViewModel.isAudioPermissionGranted],
     * which [TacomaMusicPlayerApp] observes to initiate playback and library scanning if granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.d("onRequestPermissionsResult: requestCode=$requestCode")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.handlePermissionResult(requestCode, permissions, grantResults)
    }

    /**
     * Enqueues [CatalogMusicWorker] to scan and catalog the device music library.
     *
     * Cancels any previously running catalog work first to prevent parallel executions that
     * could produce duplicate database entries. Called once after audio permission is granted.
     */
    fun queryMusic() {
        val catalogWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<CatalogMusicWorker>()
            .build()

        workManager = WorkManager.getInstance(this)
        currentWorkerId = catalogWorkRequest.id

        workManager.cancelAllWork()
        workManager.enqueue(catalogWorkRequest)
    }
}
