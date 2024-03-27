package com.example.tacomamusicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.tacomamusicplayer.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicService : MediaLibraryService() {

    val TAG = MusicService::class.java.simpleName

    lateinit var player: ExoPlayer
    private var session: MediaLibrarySession? = null

    val musicMap: HashMap<String, List<MediaItem>> = HashMap()

    //val availableAlbums: List<String>

//    private var sessionToken: MediaSession. = null

    val rootItem = MediaItem.Builder()
        .setMediaId("root")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("musicapprootwhichisnotvisibletocontrollers")
                .build()
        )
        .build()

    val child1 = MediaItem.Builder()
        .setMediaId("childNodeOne")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("musicapprootwhichisnotvisibletocontrollers")
                .build()
        )
        .build()

    val child2 = MediaItem.Builder()
        .setMediaId("childNodeTwo")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("musicapprootwhichisnotvisibletocontrollers")
                .build()
        )
        .build()

    //TODO I'm going to need to get Room database for linking known albums to music mp3 uris.... song items


    private val librarySessionCallback: MediaLibrarySession.Callback = object : MediaLibrarySession.Callback {
        //TODO

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

            return Futures.immediateFuture(
                LibraryResult.ofItemList(
                    when(parentId) {
                        "root" -> listOf(child1, child2)
                        else -> listOf()
                    },
                    params
                )
            )

            return super.onGetChildren(session, browser, parentId, page, pageSize, params)
        }

        //TODO each album is a media item
        //TODO there is a root node media item
        //TODO each song is also a media item
        //I will use a hasmap which will have <albumNodeId>, list<MediaItem>

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {

//            val sessionCommands =
//                ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
//                    .add(onCustomCommand())

//            val playerCommands =
//                ConnectionResult.DEFAULT_PLAYER_COMMANDS



            return super.onConnect(session, controller)
        }
    }

    private val serviceIOScope = CoroutineScope(Dispatchers.IO)
    private val serviceMainScope = CoroutineScope(Dispatchers.Main)

    val notificationChannelId = "CHANNEL_BRUH"
    private lateinit var _notificationBuilder: Notification.Builder
    lateinit var channel: NotificationChannel

    private fun queryMusic() {
        serviceIOScope.launch {
            //scan music
            //add to tracklist...
        }
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        //TODO MOVE THIS LATER...
//        val ACTION_BACK = "action_back"
//        val ACTION_PLAY_PAUSE = "action_play_pause"
//        val ACTION_SKIP = "action_skip"
//        val backIntent = Intent(this, NotificationControlReceiver::class.java).apply {
//            action = ACTION_BACK
//            putExtra("extraID", 11)
//        }
//        val backPendingIntent : PendingIntent = PendingIntent.getBroadcast(this, 0, backIntent, PendingIntent.FLAG_IMMUTABLE)
//        val playPauseIntent = Intent(this, NotificationControlReceiver::class.java).apply {
//            action = ACTION_PLAY_PAUSE
//            putExtra("extraID", 22)
//        }
//        val playPausePendingIntent : PendingIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
//        val skipIntent = Intent(this, NotificationControlReceiver::class.java).apply {
//            action = ACTION_SKIP
//            putExtra("extraID", 33)
//        }
//        val skipPendingIntent : PendingIntent = PendingIntent.getBroadcast(this, 0, skipIntent, PendingIntent.FLAG_IMMUTABLE)

        try {

//            //starting in foreground with notification...
//            channel = NotificationChannel(notificationChannelId, "David_Channel", NotificationManager.IMPORTANCE_DEFAULT)
//            channel.description = "david's channel for foreground service notification"
//
//            val notificationManager = this.getSystemService(NotificationManager::class.java)
//            notificationManager.createNotificationChannel(channel)
//
//            _notificationBuilder = Notification.Builder(this, notificationChannelId)
//            _notificationBuilder.setContentTitle("MUSIC FOREGROUND SERVICE")
//            _notificationBuilder.setContentText("Artist - Album")
//            _notificationBuilder.setSmallIcon(R.drawable.baseline_play_arrow_24)
//            _notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.baseline_play_arrow_24))
//                .addAction(R.drawable.baseline_play_arrow_24, "back", backPendingIntent)
//            _notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.baseline_play_arrow_24))
//                .addAction(R.drawable.baseline_play_arrow_24, "back", playPausePendingIntent)
//            _notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.baseline_play_arrow_24))
//                .addAction(R.drawable.baseline_play_arrow_24, "back", skipPendingIntent)
//            _notificationBuilder.setStyle(Notification.MediaStyle())
//
//            ServiceCompat.startForeground(
//                this,
//                100,
//                _notificationBuilder.build(),
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
//            )

        } catch (e: Exception) {
            Log.d(TAG, "onStartCommand: error=$e")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification() {
        val notificationManager = this.getSystemService(NotificationManager::class.java)
        _notificationBuilder.setContentTitle("new Title")
        _notificationBuilder.setContentText("new text")

        notificationManager.notify(100, _notificationBuilder.build())

    }


    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeMediaSession()

        //setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build()) //THIS IS ONLY FOR CUSTOM NOTIFICATIONS requires unstable api
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if(!player.playWhenReady || player.mediaItemCount == 0)
            stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            session = null
        }
        super.onDestroy()
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Log.d("SERVICE", "onGetSession: ")
        return session
    }

    //NOW I JUST NEED TO MOVE THIS TO A SERVICE...
    fun initializePlayer(): Boolean {

        var playerBuilder: ExoPlayer.Builder = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .setAudioAttributes(AudioAttributes.DEFAULT, true) //coordinate audio //fade out other audio from other apps
            .setHandleAudioBecomingNoisy(true) // disconnect headphones , switch to speakers mode
            //.setWakeMode(C.WAKE_MODE_LOCAL) //device doesn't sleep when playing audio with screen off. //TODO wake lock is running error...

        player = playerBuilder.build()

        player.addListener(PlayerEventListener())
        player.playWhenReady = true //this can be a variable

        val pkgName = applicationContext.packageName
        //path for local file...
        val path = Uri.parse("android.resource://" + pkgName + "/" + R.raw.earth)


        //player.setMediaItem(MediaItem.fromUri(path))
        player.setMediaItems(listOf(MediaItem.fromUri(path), MediaItem.fromUri(path), MediaItem.fromUri(path)))
        player.prepare()
        player.play()
        return true
    }

    fun initializeMediaSession(): Boolean {

        session = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .build()

        //bruh moment the auto session notification didn't show up because I didn't actually add the session...
        addSession(session!!)

        return true
    }


    private class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            if (playbackState == Player.STATE_ENDED) {
                //TODO SOMETHING
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                //TODO type of error
            } else {
                //TODO other type of error
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            //TODO a track has changed....
        }
    }
}