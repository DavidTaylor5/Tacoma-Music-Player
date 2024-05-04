package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.os.ExecutorCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.ViewholderAlbumBinding
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import timber.log.Timber
import java.util.concurrent.Executors

//I'm going to use this recyclerview to show all available albums...
class AlbumListAdapter(private val dataSet: List<MediaItem>): RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class AlbumViewHolder(val binding : ViewholderAlbumBinding): RecyclerView.ViewHolder(binding.root) {

    }

    //Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderAlbumBinding.inflate(inflater, parent, false)

        return AlbumViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    @OptIn(UnstableApi::class) override fun onBindViewHolder(viewHolder: AlbumViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var albumTitle = "Default ALBUM"
        var albumArtist = ""
        var albumDuration = ""
        var albumUri = "" //TODO I'll have to get a solution  for this... else default picture

        //First check that dataSet has a value for position
        if(position < dataSet.size) {

//            val a = dataSet[position]
//            val b = DefaultMediaSourceFactory(viewHolder.itemView.context)
//
//            val trackGroupsFuture = MetadataRetriever.retrieveMetadata(b, dataSet[position])
//
//            val executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1))
//
//
//            dataSet[position].requestMetadata.toBundle().apply {
//                this.getString()
//            }

            //val player = ExoPlayer.Builder(viewHolder.itemView.context).build()

            //player.setMediaItems(listOf(dataSet[0]))



            //player.mediaMetadata

//            val a = dataSet[0].requestMetadata.extras.toString()
//            val b = dataSet[0].requestMetadata.mediaUri

            Timber.d("onBindViewHolder: CHECKING VALUES player.mediaMetadata=${dataSet[0].mediaId}, ${dataSet[0].mediaMetadata} ${dataSet[0].localConfiguration} ${dataSet[0].requestMetadata.mediaUri} ${dataSet[0].mediaMetadata.title}, ${dataSet[0].mediaMetadata.albumTitle}, ${dataSet[0].mediaMetadata.albumArtist}, ${dataSet[0].mediaMetadata.artworkUri}")



//            Futures.addCallback(
//                trackGroupsFuture,
//                object : FutureCallback<TrackGroupArray?> {
//                    override fun onSuccess(trackGroups: TrackGroupArray?) {
//                        Timber.d("onSuccess: ")
////                        if (trackGroups != null) {
////                            Timber.d("onSuccess: trackGroups not null!")
////
////                            handleMetadata(trackGroups)
////                        }
//                    }
//
//                    override fun onFailure(t: Throwable) {
//                        Timber.d("onFailure: ")
////                        handleFailure(t)
//                    }
//
//                    fun handleMetadata(trackGroupArray: TrackGroupArray) {
//                        Timber.d("handleMetadata: trackGroupArray=${trackGroupArray}")
//
//                    }
//
//                    fun handleFailure(t: Throwable) {
//                        Timber.d("handleFailure: t=$t")
//                    }
//
//                },
//                executor
//            )

        }

        fun handleMetaData(trackGroupArray: TrackGroupArray) {

        }

        fun handleFailure(t: Throwable) {

        }


        // Get element from  your dataset at this position and replace the contents of the view with that element
        //viewHolder.albumInfo.text = "Default ALBUM \n Default Artist \n Default Duration"//dataSet[position]
        viewHolder.binding.albumInfo.text = "Default ALBUM \n Default Artist \n Default Duration"





    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}