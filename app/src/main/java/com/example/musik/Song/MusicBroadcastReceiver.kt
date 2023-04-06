package com.example.musik.Song

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.musik.Homepage.HomeActivity
import com.example.musik.Mutilpurpose.Constant
import com.example.musik.R
import kotlin.system.exitProcess

class MusicBroadcastReceiver : BroadcastReceiver() {

    private val tag = "MusicBroadcastReceiver"
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            //only play next or prev song, when music list contains more than one song
            Constant.ACTION_PREVIOUS -> previous()
            Constant.ACTION_PLAY_PAUSE -> play(context!!)
            Constant.ACTION_NEXT -> next()
            Constant.ACTION_EXIT -> exit()
        }
    }

    private fun previous(){
        Log.d(tag, "previous")
        if (HomeActivity.musicService!!.exoPlayer!!.hasPreviousMediaItem()) {
            HomeActivity.musicService!!.exoPlayer!!.seekToPrevious()
            HomeActivity.musicService!!.exoPlayer!!.play()
            HomeActivity.musicService!!.setupMusicNotification(R.drawable.ic_pause)
        }
    }

    private fun play(context: Context){
        Log.d(tag, "play")
        /*val intent = Intent(context, MusicService::class.java)
        intent.putExtra("action", Constant.ACTION_PLAY_PAUSE)
        context.startService(intent)
        Log.d(tag, "action play sent back music service")*/

        if (HomeActivity.musicService!!.exoPlayer!!.isPlaying) {
            HomeActivity.musicService!!.exoPlayer!!.pause()
            HomeActivity.musicService!!.setupMusicNotification(R.drawable.ic_play)
            HomeActivity.homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_play_v2)
            HomeActivity.homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_play)
        }
        /*Step 2 - Case 2: exoplayer is not playing music */
        else {
            HomeActivity.musicService!!.exoPlayer!!.play()
            HomeActivity.musicService!!.setupMusicNotification(R.drawable.ic_pause)
            HomeActivity.homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_pause_v2)
            HomeActivity.homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun next(){
        Log.d(tag, "next")
        if (HomeActivity.musicService!!.exoPlayer!!.hasNextMediaItem()) {
            HomeActivity.musicService!!.exoPlayer!!.seekToNext()
            HomeActivity.musicService!!.exoPlayer!!.play()
            HomeActivity.musicService!!.setupMusicNotification(R.drawable.ic_pause)
        }
    }

    private fun exit(){
        Log.d(tag, "exit")
        HomeActivity.musicService!!.exoPlayer = null
        HomeActivity.musicService!!.stopForeground(true)
        HomeActivity.musicService!!.stopSelf()
        HomeActivity.musicService = null
        exitProcess(1)
    }

}