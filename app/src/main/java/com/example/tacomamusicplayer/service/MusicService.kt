package com.example.tacomamusicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
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
import com.example.tacomamusicplayer.SongModel
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

    //TODO I want to map Album MediaItems to Song MediaItems [albums contain songs...]
    val albumToSongMap: HashMap<MediaItem, List<MediaItem>> = HashMap()

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

    val playlistItem = MediaItem.Builder()
        .setMediaId("playlistItem")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("musicapprootwhichisnotvisibletocontrollers")
                .build()
        )
        .build()

    val libraryItem = MediaItem.Builder()
        .setMediaId("libraryItem")
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

            //Because I'm getting the library root, I should actually start querying the songs in the background
            queryMusicOnDevice()

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
                        "root" -> listOf(playlistItem, libraryItem)
                        else -> listOf()
                    },
                    params
                )
            )
        }

        //TODO each album is a media item
        //TODO there is a root node media item
        //TODO each song is also a media item
        //I will use a hasmap which will have <albumNodeId>, list<MediaItem>
    }

    private val serviceIOScope = CoroutineScope(Dispatchers.IO)
    private val serviceMainScope = CoroutineScope(Dispatchers.Main)

    val notificationChannelId = "CHANNEL_BRUH"
    private lateinit var _notificationBuilder: Notification.Builder
    lateinit var channel: NotificationChannel

    /**
     * Query Music in background coroutine, I don't want this causing stuttering on UI.
     */
    private fun queryMusicOnDevice() {
        serviceIOScope.launch {
            readAudioFromStorage()
        }
    }

    /**
     * Use MedaiStore to query music in android/music file. Requires permission \[permission...]
     */
    private fun readAudioFromStorage(): List<SongModel> {

        Log.d(TAG, "readAudioFromStorage: ")
        val tempAudioList: MutableList<SongModel> = ArrayList()

        val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.AudioColumns.DATA, // 0 -> url
            MediaStore.Audio.AudioColumns.TITLE, //song title
            MediaStore.Audio.AudioColumns.ALBUM, //album title
            MediaStore.Audio.ArtistColumns.ARTIST, //artist
            MediaStore.Audio.AudioColumns.DURATION, //duration in  milliseconds
            MediaStore.Audio.AudioColumns.TRACK, // track # in album
            MediaStore.Audio.Media.ALBUM_ID, //what the hell is this?
            MediaStore.Audio.Albums.ALBUM, //album name again
            MediaStore.Audio.Albums.ARTIST, //artist again...
        )

        //TODO I should also be able to registerContentObserver for contentResolver...

        this.contentResolver.query(
            uriExternal,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while(cursor.moveToNext()) {
                Log.d(TAG, "readAudioFromStorage: ${cursor.getString(0)}, ${cursor.getString(1)}, ${cursor.getString(2)}, ${cursor.getString(3)}, ${cursor.getString(4)}, ${cursor.getString(5)}, ${cursor.getString(6)}, ${cursor.getString(7)}, ${cursor.getString(8)}") //setMedia items here?

                //TODO createSongMediaItem
                //TODO createAlbumMediaItem

                //TODO Start making the albumToSongMap
            }
        }

        Log.d(TAG, "readAudioFromStorage: done searching!")

        return tempAudioList
    }

    private fun createSongMediaItem(
        songTitle: String = "UNKONWN SONG TITLE",
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
        songDuration: Long,
        trackNumber: Int
        ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("libraryItem")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle("musicapprootwhichisnotvisibletocontrollers")
                    .setAlbumTitle("ALBUM TITLE")
                    .setArtist("ARTIST")
                    .setDescription("Description I'll just pass song length here... TODO calculate song minutes and seconds")
                    .setTrackNumber(0)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    private fun createAlbumMediaItem(
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
        ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("libraryItem")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setAlbumArtist("ARTIST")
                    .setAlbumTitle("ALBUM TITLE")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .build()
            )
            .build()
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