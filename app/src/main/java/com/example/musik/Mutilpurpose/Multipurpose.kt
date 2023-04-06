package com.example.musik.Mutilpurpose

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.annotation.AnyRes
import androidx.core.content.ContextCompat
import com.example.musik.R
import com.example.musik.Song.Song
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata


class Multipurpose {
    companion object{
        fun setStatusBarColor(context: Context, window: Window)
        {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.statusBarColor = ContextCompat.getColor(context, R.color.black);
        }


        /**
         * @since 07-03-2023
         * slide the view from below itself to the current position
         */

        fun slideUp(view: View) {
            view.visibility = View.VISIBLE
            val animate = TranslateAnimation(
                0F,  // fromXDelta
                0F,  // toXDelta
                view.height.toFloat(),  // fromYDelta
                0F
            ) // toYDelta
            animate.duration = 400
            animate.fillAfter = true
            view.startAnimation(animate)
        }

        /**
         * @since 07-03-2023
         * slide the view from its current position to below itself
         */
        fun slideDown(view: View) {
            val animate = TranslateAnimation(
                0F,  // fromXDelta
                0F,  // toXDelta
                0F,  // fromYDelta
                view.height.toFloat()
            ) // toYDelta
            animate.duration = 400
            animate.fillAfter = true
            view.startAnimation(animate)
        }

        /**
         * @since 07-03-2023
         * get readable time
         */
        fun getReadableTimestamp(duration: Int): String
        {
            var output = "";

            val hour = duration / (1000*60*60)
            val minute = (duration%(1000*60*60)) / (1000 * 60)
            /*val second = ( ( ( duration % (1000*60*60) ) % (1000 * 60 * 60) ) % (1000*60) ) / 1000*/

            val second = (((duration%(1000*60*60))%(1000*60*60))%(1000*60))/1000
            var hourValue = "00"
            val minuteValue = if( minute < 10 ) "0$minute" else minute.toString()
            val secondValue = if( second < 10)  "0$second" else second.toString()

            if( hour > 1)
            {
                hourValue = "0$minute"
                output += "$hourValue:"
            }


            output += "$minuteValue:$secondValue"
            return output
        }

        /**
         * get uri to drawable or any other resource type if u wish
         * @param context - context
         * @param drawableId - drawable res id
         * @return - uri
         */
        fun getUriToDrawable(
            context: Context,
            @AnyRes drawableId: Int
        ): Uri {
            return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://" + context.resources.getResourcePackageName(drawableId)
                        + '/' + context.resources.getResourceTypeName(drawableId)
                        + '/' + context.resources.getResourceEntryName(drawableId)
            )
        }

        /**
         * get uri to any resource type via given Resource Instance
         * @param res - resources instance
         * @param resId - resource id
         * @throws Resources.NotFoundException if the given ID does not exist.
         * @return - Uri to resource by given id
         */
        @Throws(NotFoundException::class)
        fun getUriToResource(res: Resources, @AnyRes resId: Int): Uri {
            return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE +
                        "://" + res.getResourcePackageName(resId)
                        + '/' + res.getResourceTypeName(resId)
                        + '/' + res.getResourceEntryName(resId)
            )
        }


        /**
         * @since 06-03-2023
         * get all songs that their format is Media Item
         * @return ArrayList<MediaItem>
         */
        fun getMediaItems(list: ArrayList<Song>): ArrayList<MediaItem> {
            val dataSet = arrayListOf<MediaItem>()
            for (song in list) {
                val elementMetaData = MediaMetadata.Builder()
                    .setTitle(song.name)
                    .setArtist(song.artist)
                    .setArtworkUri(song.albumCover)
                    .build()

                val element = MediaItem.Builder()
                    .setUri(song.uri)
                    .setMediaMetadata(elementMetaData)
                    .build()
                dataSet.add(element)
            }
            return dataSet
        }/*end getMediaItems*/
    }
}