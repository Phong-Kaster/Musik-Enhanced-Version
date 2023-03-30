package com.example.musik.Song

import android.app.Notification
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.musik.Homepage.HomeActivity
import com.example.musik.Mutilpurpose.Multipurpose
import com.example.musik.R
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerNotificationManager

/**
 * @since 09-03-2023
 * 1. Song Service which is a foreground service. It provides a compact music player
 * in notification system.
 * We can play/pause, skip next, skip previous from lock screen
 *
 * 2. SongBinder which is IBinder, is an object defines
 * the programming interface that clients can use to interact with the service
 */
class SongService : Service() {

    private val songBinder = SongBinder()
    lateinit var exoPlayer : ExoPlayer
    private lateinit var notificationManager: PlayerNotificationManager

    /*name & albumCover are used to store name & album Cover that are sent back to HomeActivity*/
    /*private lateinit var name: String
    private lateinit var albumCover: Uri
    private lateinit var artist: String*/

    override fun onBind(p0: Intent?): IBinder {
        return songBinder
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        super.onCreate()

        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
        exoPlayer = ExoPlayer.Builder(applicationContext).build()
        exoPlayer.setAudioAttributes(attributes, true)

        val channelId = resources.getString(R.string.app_name)
        val notificationId = 1896

        notificationManager = PlayerNotificationManager.Builder(this, notificationId, channelId)
            .setNotificationListener(notificationListener)
            .setMediaDescriptionAdapter(descriptionAdapter)
            .setChannelImportance(IMPORTANCE_HIGH)
            .setSmallIconResourceId(R.drawable.ic_swastika)
            .setChannelNameResourceId(R.string.app_name)
            .setChannelDescriptionResourceId(R.drawable.ic_key_music)
            .setNextActionIconResourceId(R.drawable.ic_skip_next_v2)
            .setPreviousActionIconResourceId(R.drawable.ic_skip_previous_v2)
            .setPlayActionIconResourceId(R.drawable.ic_play_v2)
            .setPauseActionIconResourceId(R.drawable.ic_pause_v2)
            .setStopActionIconResourceId(R.drawable.ic_close)
            .build()

        notificationManager.setPlayer(exoPlayer)
        notificationManager.setUseStopAction(false)
        notificationManager.setPriority(NotificationCompat.PRIORITY_MAX)
        notificationManager.setUseRewindAction(false)
        notificationManager.setUseFastForwardAction(false)
    }

    /**
     * @since 09-03-2023
     * onDestroy
     * stop & release exoplayer
     * stop foreground service
     */
    override fun onDestroy() {
        super.onDestroy()
        if( exoPlayer.isPlaying ) { exoPlayer.stop() }
        notificationManager.setPlayer(null)
        exoPlayer.release()
        stopForeground(true)
        stopSelf()
    }

    /**
     * @since 09-03-2023
     * Notification description adapter
     */
    private val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter{
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return exoPlayer.currentMediaItem!!.mediaMetadata.title!!
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun createCurrentContentIntent(player: Player): PendingIntent? {
           val intent = Intent( applicationContext, HomeActivity::class.java)/*open Main activity intent*/
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            val openIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return  openIntent
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return null
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            /*Step 0: define value to send back HomeActivity if users click on notification media control*/
            val albumCover = exoPlayer.currentMediaItem?.mediaMetadata?.artworkUri as? Uri


            /*Step 1: get album cover as Uri, if song doesn't have Album cover, it will be replaced by uriDefault*/
            val uriDefault = Multipurpose.getUriToDrawable(applicationContext, R.drawable.img_song)
            val uri = albumCover ?: uriDefault


            /*Step 2: setup uri to become Album Cover
            * if the drawable of imageView is null then set uriDefault instead
            * */
            val imageView = ImageView(applicationContext)
            imageView.setImageURI(uri)
            if( imageView.drawable == null)
            {
                imageView.setImageURI(uriDefault)
            }

            return (imageView.drawable as BitmapDrawable).bitmap
        }
    }/*end Description Adapter*/


    /**
     * @since 09-03-2023
     * Notification Listener
     * onNotificationCancelled - when we close notification
     * onNotificationPosted - when notification appears
     */
    private val notificationListener = object : PlayerNotificationManager.NotificationListener{
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            super.onNotificationCancelled(notificationId, dismissedByUser)
            stopForeground(true)
            if( exoPlayer.isPlaying ) exoPlayer.pause()
        }

        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
            super.onNotificationPosted(notificationId, notification, ongoing)
            startForeground(notificationId, notification)
        }
    }/*end notification listener*/



    /**
     * @since 09-03-2023
     * IBinder is an object defines the programming interface that clients can use to interact with the service
     */
    inner class SongBinder : Binder(){
        fun getInstance() : SongService
        {
            return this@SongService
        }
    }
}