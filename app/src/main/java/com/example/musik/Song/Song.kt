package com.example.musik.Song

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable


class Song
constructor(var albumCover: Uri,var name: String, var artist: String, var album: String, var uri: Uri, var duration: Long) {

}