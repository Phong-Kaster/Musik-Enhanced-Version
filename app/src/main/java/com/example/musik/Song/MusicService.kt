package com.example.musik.Song

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.musik.Homepage.HomeActivity
import com.example.musik.Mutilpurpose.Constant
import com.example.musik.R
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes

class MusicService : Service() {

    private val tag = "MusicService"
    private var myBinder = MyBinder()
    private lateinit var mediaSessionCompat : MediaSessionCompat
    private lateinit var notification: Notification
    var exoPlayer: ExoPlayer? = null


    override fun onBind(intent: Intent?): IBinder {
        mediaSessionCompat = MediaSessionCompat(baseContext, "tag")
        return myBinder
    }

    inner class MyBinder : Binder()
    {
        fun getInstance(): MusicService {
            return this@MusicService
        }
    }

    override fun onCreate() {
        super.onCreate()
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
        exoPlayer = ExoPlayer.Builder(applicationContext).build()
        exoPlayer!!.setAudioAttributes(attributes, true)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*return super.onStartCommand(intent, flags, startId)*/
        Log.d(tag, "onStartCommand")
        return START_STICKY
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun setupMusicNotification(icon: Int) {
        /*1. setup basically*/
        /*val notificationManagerCompat = NotificationManagerCompat.from(applicationContext)*/
        val style = androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken)
        val album = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.img_album)
        val flag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT


        /*2. setup intent for PLAY, NEXT & PREVIOUS*/
        val intentPrevious =
            Intent(applicationContext, MusicBroadcastReceiver::class.java).setAction(Constant.ACTION_PREVIOUS)
        val intentPlayPause =
            Intent(applicationContext, MusicBroadcastReceiver::class.java).setAction(Constant.ACTION_PLAY_PAUSE)
        val intentNext =
            Intent(applicationContext, MusicBroadcastReceiver::class.java).setAction(Constant.ACTION_NEXT)
        val intentExit =
            Intent(applicationContext, MusicBroadcastReceiver::class.java).setAction(Constant.ACTION_EXIT)


        val pendingIntentPrevious = PendingIntent.getBroadcast(applicationContext, 0, intentPrevious, flag)
        val pendingIntentPlayPause =
            PendingIntent.getBroadcast(applicationContext, 0, intentPlayPause, flag)
        val pendingIntentNext = PendingIntent.getBroadcast(applicationContext, 0, intentNext, flag)
        val pendingIntentExit = PendingIntent.getBroadcast(applicationContext, 0, intentExit, flag)


        val intentHome = Intent(this, HomeActivity::class.java)
        val pendingIntentHome: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intentHome)
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        /*3. setup notification for playing music & attach pending intents*/
        val position = HomeActivity.position
        val song = exoPlayer!!.currentMediaItem!!.mediaMetadata.title!!
        val artist = exoPlayer!!.currentMediaItem!!.mediaMetadata.artist!!

        Log.d(tag, "setupMusicNotification: $song")
        Log.d(tag, "setupMusicNotification: $artist")

        notification = NotificationCompat.Builder(applicationContext, Constant.CHANNEL_ID)
            .setContentIntent(pendingIntentHome)
            .setContentTitle(song)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_swastika)
            .setLargeIcon(album)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_skip_previous_v2, "Previous", pendingIntentPrevious)
            .addAction(icon, "Play", pendingIntentPlayPause)
            .addAction(R.drawable.ic_skip_next_v2, "Next", pendingIntentNext)
            .addAction(R.drawable.ic_close, "Exit", pendingIntentExit)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(style)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        /*notificationManagerCompat.notify(1, notification)*/
        startForeground(13, notification)
    }
}