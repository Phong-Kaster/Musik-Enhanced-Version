package com.example.musik.Homepage

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.Intent.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.Menu
import android.view.View.GONE
import android.widget.ImageView
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.SearchView.VISIBLE
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musik.Mutilpurpose.Constant
import com.example.musik.Mutilpurpose.Establishment
import com.example.musik.Mutilpurpose.Multipurpose
import com.example.musik.R
import com.example.musik.Song.MusicService
import com.example.musik.Song.Song
import com.example.musik.Song.SongAdapter
import com.example.musik.databinding.ActivityHomeBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.util.*
import kotlin.math.log


class HomeActivity : AppCompatActivity(), ServiceConnection {

    companion object{
        //lateinit var exoPlayer: ExoPlayer/*exoPlayer is utilized in other activities so that it must be static*/
        var musicService: MusicService? = null
        var songList: ArrayList<Song> = ArrayList()

        lateinit var homeBinding: ActivityHomeBinding
    }

    private val tag = "HomeActivity"
    private lateinit var songAdapter: SongAdapter




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*setContentView(R.layout.activity_home)*/
        homeBinding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        setupComponent()
        dmcSetUpEvent()


        Establishment.setupNotificationChannel(this)
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
    }


    /**
     * @since 06-03-2023
     * on resume
     */
    override fun onResume() {
        Log.d(tag, "onResume")
        super.onResume()
        Multipurpose.setStatusBarColor(this, window)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    /**
     * @since 09-03-2023
     * if users open status bar & click on Music Player notification
     */
    override fun onNewIntent(intent: Intent?) {
        Log.d(tag, "onNewIntent")
        super.onNewIntent(intent)
        dmcUpdateProgress()
        /*If exoPlayer is playing or pausing*/
        if( musicService!!.exoPlayer!!.isPlaying || !musicService!!.exoPlayer!!.isPlaying && musicService!!.exoPlayer!!.hasPreviousMediaItem())
        {
            val name  = musicService!!.exoPlayer!!.currentMediaItem!!.mediaMetadata.title.toString()
            val albumCover  = musicService!!.exoPlayer!!.currentMediaItem!!.mediaMetadata.artworkUri
            val artist = musicService!!.exoPlayer!!.currentMediaItem!!.mediaMetadata.artist



            /*for COMPACT MEDIA CONTROL, update name, artist & album cover*/
            val iconCompact = if( musicService!!.exoPlayer!!.isPlaying ) R.drawable.ic_pause_v2 else R.drawable.ic_play_v2
            homeBinding.compactMediaControlName.text = name
            homeBinding.compactMediaControlArtist.text = artist
            homeBinding.compactMediaControlAlbumCover.setImageURI(albumCover)
            homeBinding.compactMediaControlPlayPause.setImageResource(iconCompact)

            /*for DEFAULT MEDIA CONTROL, update name, artiest & album cover*/
            val iconDefault = if( musicService!!.exoPlayer!!.isPlaying ) R.drawable.ic_pause else R.drawable.ic_play
            homeBinding.defaultMediaControl.name.text = name
            homeBinding.defaultMediaControl.artist.text = artist
            homeBinding.defaultMediaControl.albumCover.setImageURI(albumCover)
            homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(iconDefault)

            /* DEFAULT MEDIA CONTROL, update seek bar & progress view*/
            val current = Multipurpose.getReadableTimestamp(musicService!!.exoPlayer!!.currentPosition.toInt())
            val duration = Multipurpose.getReadableTimestamp(musicService!!.exoPlayer!!.duration.toInt())

            homeBinding.defaultMediaControl.progressStart.text = current
            homeBinding.defaultMediaControl.progressEnd.text = duration
            homeBinding.defaultMediaControl.seekBar.progress = musicService!!.exoPlayer!!.currentPosition.toInt()
            homeBinding.defaultMediaControl.seekBar.max = musicService!!.exoPlayer!!.duration.toInt()
        }
    }

    /**
     * @since 06-03-2023
     * inflate specific menu in this activity
     * */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top, menu)

        val menuItem = menu.findItem(R.id.buttonSearch)
        val searchView = menuItem.actionView as SearchView


        searchView.queryHint = getString(R.string.enter_keyword)
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(keyword: String): Boolean {
                filterWithKeyword(keyword.lowercase())
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }/*end onCreateOptionsMenu()*/

    /**
     * @since 03-03-2023
     * set up component
     */
    private fun setupComponent()
    {
        setSupportActionBar(homeBinding.toolbar)
        supportActionBar!!.setTitle(R.string.app_name)

        /*musicService!!.exoPlayer = ExoPlayer.Builder(this).build()*/
        //musicService!!.exoPlayer.shuffleModeEnabled = true/*by default, shuffle mode is enabled*/

        val flag = checkPermission()
        if( flag )
        {
            val list = fetch()
            setupRecyclerView(list)
            dmcSetupEvent()
        }
    }




    /*The request code used in ActivityCompat.requestPermissions()
     and returned in the Activity's onRequestPermissionsResult()*/
    private var permissionCode = 1
    private val permissionsRequired = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE

    )
    private fun checkPermission(): Boolean
    {
        val permissionsNeeded = mutableListOf<String>()

        /*add permission that is not granted into permissionNeeded Array*/
        for(element in permissionsRequired)
        {
            val flag: Int = ContextCompat.checkSelfPermission(this, element)
            if( flag != PackageManager.PERMISSION_GRANTED ){
                permissionsNeeded.add(element)
            }
        }

        /*if permissions needed Array is not empty*/
        if(permissionsNeeded.isNotEmpty()){
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), permissionCode )
            return false
        }

        return true
    }/*end checkPermission*/


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val resultStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if(resultStorage == PackageManager.PERMISSION_GRANTED)
        {
            fetch()
        }
        if( resultStorage == PackageManager.PERMISSION_DENIED ) {
            val message = getString(R.string.guide_storage)
            requestPermission(message, Manifest.permission.READ_EXTERNAL_STORAGE )
        }

    }/*end onRequestPermissionsResult*/

    /**
     * open dialog and tell users why Musik need Storage permission
     * - Case 1: if storage permissions is granted, fetch all songs from local storage
     * - Case 2: if storage permissions is not granted, open the application's settings to users give permission
     */
    private val requestPermissionLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
            if(isGranted){
                fetch()
            }
            else
            {
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                with(intent) {
                    data = Uri.fromParts("package", packageName, null)
                    addCategory(CATEGORY_DEFAULT)
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                    addFlags(FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }

                startActivity(intent)
            }
        }

    /**
     * @since 03-03-2023
     * request permission
     */
    private fun requestPermission(message: String, permission: String)
    {
        val dialogInterfacePositive = DialogInterface.OnClickListener{
                _, _ ->
                requestPermissionLauncher.launch(permission)
        }

        val dialogInterfaceNegative = DialogInterface.OnClickListener{
                dialog, _ ->
            Toast.makeText(this, getString(R.string.musik_needs_storage_permission), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            finish()
        }


        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", dialogInterfacePositive)
            .setNegativeButton("No, thank",dialogInterfaceNegative )
            .create()
            .show()
    }

    /**
     * @author Phong-Kaster
     * @since 06-03-2023
     * find and load all songs in local storage to the application
     */
    private fun fetch(): ArrayList<Song> {
        /*Step 1: define list of songs*/
        val songs = arrayListOf<Song>()
        val mediaStorageUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        else
        {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        /*Step 2: define projection*/
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION
        )

        /*Step 3: define order*/
        val sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER

        val cursor = contentResolver.query(mediaStorageUri, projection, null, null, sortOrder)
        val idColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val albumCoverColumn =  cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext())
        {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val albumCover = cursor.getLong(albumCoverColumn)
            val album = cursor.getString(albumColumn)
            val artist = cursor.getString(artistColumn)
            val size = cursor.getLong(sizeColumn) / 1000000// convert from byte to megabyte
            val duration = cursor.getLong(durationColumn)

            val uriSong = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)// = song uri
            val uriAlbumCover = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumCover)// = album cover


            val element = Song(uriAlbumCover, name, artist, album, uriSong, duration)// = create a song instance
            if( size <= 0)// if size <= 0 MB then ignore this song
            {
                continue
            }
            songs.add(element)
        }
        cursor.close()
        return songs
    }/*end fun fetch()*/

    /**
     * @since 06-03-2023
     * setup Recycler View songs
     */
    private fun setupRecyclerView(list: ArrayList<Song>)
    {
        if( list.size == 0)
        {
            Toast.makeText(this, "Empty !", Toast.LENGTH_SHORT).show()
            return
        }
        // update songList
        songList.clear()
        songList.addAll(list)


        // update recycler view
        val layoutManger = LinearLayoutManager(this)
        homeBinding.recyclerView.layoutManager = layoutManger


        songAdapter = SongAdapter(this, list)
        homeBinding.recyclerView.adapter = songAdapter
    }/*end setupRecyclerView*/


    /**
     * @since 07-03-2023
     * Default Media Control stands for D.M.C
     * Compact Media Control stands for C.M.C
     * set up event onClick
     */
    private fun cmcSetupEvent()
    {
        /*===Step 1: This object will update name, artist & album cover from song that users click on*/
        val listener = object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                /*we have to wait ExoPlayer read entire file to get accuracy duration*/
                if(playbackState == ExoPlayer.STATE_READY)
                {
                    val current = Multipurpose.getReadableTimestamp(musicService!!.exoPlayer!!.currentPosition.toInt())
                    val duration = Multipurpose.getReadableTimestamp(musicService!!.exoPlayer!!.duration.toInt())

                    homeBinding.defaultMediaControl.progressStart.text = current
                    homeBinding.defaultMediaControl.progressEnd.text = duration
                    homeBinding.defaultMediaControl.seekBar.progress = musicService!!.exoPlayer!!.currentPosition.toInt()
                    homeBinding.defaultMediaControl.seekBar.max = musicService!!.exoPlayer!!.duration.toInt()

                }

                if( musicService!!.exoPlayer!!.isPlaying )
                {
                    homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_pause_v2)
                    homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)
                }
                else
                {
                    homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_play_v2)
                    homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_play)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                //get name, artist & album cover
                val name = mediaItem!!.mediaMetadata.title
                val artist = mediaItem.mediaMetadata.artist
                var albumCover = mediaItem.mediaMetadata.artworkUri

                /*check if album Uri create a NULL drawable?*/
                val imageView = ImageView(applicationContext)
                imageView.setImageURI(albumCover)
                if( imageView.drawable == null ){
                    albumCover = Multipurpose.getUriToDrawable(applicationContext, R.drawable.img_song)
                }


                // for COMPACT MEDIA CONTROL,  update name, artist, album cover & play/pause button's icon
                homeBinding.compactMediaControlName.text = name
                homeBinding.compactMediaControlArtist.text = artist
                homeBinding.compactMediaControlAlbumCover.setImageURI(albumCover)
                //homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_pause_v2)


                // for DEFAULT MEDIA CONTROL, update name, artist, album cover & play/pause button's icon
                homeBinding.defaultMediaControl.name.text = name
                homeBinding.defaultMediaControl.artist.text = artist
                homeBinding.defaultMediaControl.albumCover.setImageURI(albumCover)
                //homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)


                //for DEFAULT MEDIA CONTROL(dmc), update progress & seek bar
                musicService!!.setupMusicNotification(R.drawable.ic_pause)
                dmcUpdateProgress()
            }
        }
        musicService!!.exoPlayer!!.shuffleModeEnabled = true
        musicService!!.exoPlayer!!.addListener(listener)/*and finally we add the above listener to exo player*/


        /*===Step 2: button play/ pause on Compact media Control*/
        homeBinding.compactMediaControlPlayPause.setOnClickListener {
            /*Step 2 - Case 1: exoplayer is playing music*/
            if (musicService!!.exoPlayer!!.isPlaying) {
                musicService!!.exoPlayer!!.pause()
                homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_play_v2)
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_play)
                musicService!!.setupMusicNotification(R.drawable.ic_play)
            }
            /*Step 2 - Case 2: exoplayer is not playing music */
            else {
                musicService!!.exoPlayer!!.play()
                homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_pause_v2)
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)
                musicService!!.setupMusicNotification(R.drawable.ic_pause)
            }
        }/*end Step 2*/


        /*===Step 3: button skip next & previous*/
        homeBinding.compactMediaControlSkipPrevious.setOnClickListener {
            if (musicService!!.exoPlayer!!.hasPreviousMediaItem()) {
                musicService!!.exoPlayer!!.seekToPrevious()
                musicService!!.exoPlayer!!.play()
                musicService!!.setupMusicNotification(R.drawable.ic_pause)
            }
        }
        homeBinding.compactMediaControlSkipNext.setOnClickListener {
            if (musicService!!.exoPlayer!!.hasNextMediaItem()) {
                musicService!!.exoPlayer!!.seekToNext()
                musicService!!.exoPlayer!!.play()
                musicService!!.setupMusicNotification(R.drawable.ic_pause)
            }
        }/*end Step 3*/
    }

    /**
     * @since 07-03-2023
     * Default Media Control stands for D.M.C
     * Compact Media Control stands for C.M.C
     *
     * this function establishes event clickOn for the layout included at the bottom of this activity
     * this layout shows default media control instead of compact media control
     *
     * all clickOn events which  are declared in this function, is written in "activity_music_player"
     */
    private fun dmcSetupEvent(){
        /*====================SHOW D.M.C - clickOn C.M.C ====================*/
        homeBinding.compactMediaControl.setOnClickListener{

            /*If the app is opened but none of songs is selected,
            * users click on compact media control to shuffle list of songs and play music*/
            if( !musicService!!.exoPlayer!!.isPlaying && !musicService!!.exoPlayer!!.hasNextMediaItem())
            {
                val items = Multipurpose.getMediaItems(songList)
                musicService!!.exoPlayer!!.setMediaItems(items)
                musicService!!.exoPlayer!!.prepare()
                musicService!!.exoPlayer!!.play()
                Toast.makeText(this, "Enjoy this moment!", Toast.LENGTH_SHORT).show()
            }

            /*Slide default media control up from the bottom of screen*/
            Multipurpose.slideUp(homeBinding.defaultMediaControl.layout)
            homeBinding.appBarLayout.visibility = GONE
            homeBinding.defaultMediaControl.layout.visibility = VISIBLE
            homeBinding.defaultMediaControl.layout.isClickable = true
        }/*end SHOW D.M.C - clickOn C.M.C  */

        /*====================BUTTON CLOSE - HIDE D.M.C TEMPORARILY ====================*/
        homeBinding.defaultMediaControl.buttonClose.setOnClickListener{
            Multipurpose.slideDown(homeBinding.defaultMediaControl.layout)
            homeBinding.appBarLayout.visibility = VISIBLE
            homeBinding.defaultMediaControl.layout.visibility = GONE
            homeBinding.defaultMediaControl.layout.isClickable = false
        }/*end BUTTON CLOSE*/
       /* button more*/
    }


    /**
     * @since 06-03-2023
     * filter songs with keyword
     * any song has name or artist matching with keyword then is showed !
     */
    private fun filterWithKeyword(keyword: String){
        val songListFiltered = arrayListOf<Song>()

        if(songList.size == 0) return


        for( element in songList)
        {
            val name = element.name.lowercase(Locale.ROOT)
            val artist = element.artist.lowercase(Locale.ROOT)
            if( name.contains(keyword) || artist.contains(keyword) ) {
                songListFiltered.add(element)
            }
        }
        /*update song List for exoPlayer to play*/

        /*update song list for recycler view to show*/
        songAdapter.reload(songListFiltered)
    }


    /**
     * @since 08-03-2023
     * on Back Pressed
     */
    override fun onBackPressed() {
        if( homeBinding.defaultMediaControl.layout.visibility == VISIBLE)
        {
            Multipurpose.slideDown(homeBinding.defaultMediaControl.layout)
            homeBinding.appBarLayout.visibility = VISIBLE
            homeBinding.defaultMediaControl.layout.visibility = GONE
            homeBinding.defaultMediaControl.layout.isClickable = false
        }
        else
        {
            super.onBackPressed()
        }
    }

    /**
     * @since 08-03-2023
     * dmc stands for Default Media Control
     * update progress & seekbar
     * update indicator's position of seek bar every 1 second
     */
    private fun dmcUpdateProgress()
    {
        val mainLooper = Looper.getMainLooper()
        val runnable = Runnable{
            if( musicService!!.exoPlayer!!.isPlaying)
            {
                val current = Multipurpose.getReadableTimestamp(musicService!!.exoPlayer!!.currentPosition.toInt())
                homeBinding.defaultMediaControl.progressStart.text = current
                homeBinding.defaultMediaControl.seekBar.progress = musicService!!.exoPlayer!!.currentPosition.toInt()
            }
            dmcUpdateProgress()
        }
        Handler(mainLooper).postDelayed(runnable, 1000)
    }

    /**
     * @since 08-03-2023
     * dmc stands for Default Media Control
     * set up event for default media control
     */
    private fun dmcSetUpEvent()
    {
        /*======================= BUTTON PLAY/PAUSE =======================*/
        homeBinding.defaultMediaControl.buttonPlayPause.setOnClickListener{
            /*Step 2 - Case 1: exoplayer is playing music*/
            if (musicService!!.exoPlayer!!.isPlaying) {
                musicService!!.exoPlayer!!.pause()
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_play)
                homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_play_v2)
                musicService!!.setupMusicNotification(R.drawable.ic_play)
            }
            /*Step 2 - Case 2: exoplayer is not playing music */
            else
            {
                musicService!!.exoPlayer!!.play()
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)
                homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_pause_v2)
                musicService!!.setupMusicNotification(R.drawable.ic_pause)
            }
        }/*end BUTTON PLAY/PAUSE*/

        /*BUTTON SKIP PREVIOUS AND SKIP NEXT*/
        homeBinding.defaultMediaControl.buttonSkipPrevious.setOnClickListener {
            if (musicService!!.exoPlayer!!.hasPreviousMediaItem()) {
                musicService!!.exoPlayer!!.seekToPrevious()
                musicService!!.exoPlayer!!.play()
                musicService!!.setupMusicNotification(R.drawable.ic_pause)
            }
        }
        homeBinding.defaultMediaControl.buttonSkipNext.setOnClickListener {
            if (musicService!!.exoPlayer!!.hasNextMediaItem()) {
                musicService!!.exoPlayer!!.seekToNext()
                musicService!!.exoPlayer!!.play()
                musicService!!.setupMusicNotification(R.drawable.ic_pause)
            }
        }/*end BUTTON SKIP PREVIOUS AND SKIP NEXT*/


        /*======================= BUTTON SHUFFLE =======================*/
        homeBinding.defaultMediaControl.buttonShuffle.setOnClickListener {
            if(musicService!!.exoPlayer!!.shuffleModeEnabled)// shuffle off
            {
                musicService!!.exoPlayer!!.shuffleModeEnabled = false
                homeBinding.defaultMediaControl.buttonShuffle.setImageResource(R.drawable.ic_shuffle_off)
                Toast.makeText(this, "Shuffle off", Toast.LENGTH_SHORT).show()
            }
            else// shuffle on
            {
                musicService!!.exoPlayer!!.shuffleModeEnabled = true
                homeBinding.defaultMediaControl.buttonShuffle.setImageResource(R.drawable.ic_shuffle_on)
                Toast.makeText(this, "Shuffle on", Toast.LENGTH_SHORT).show()
            }
        }/*end BUTTON SHUFFLE*/

        /*======================= BUTTON REPEAT =======================*/
        var repeatMode = Constant.REPEAT_MODE_OFF
        homeBinding.defaultMediaControl.buttonRepeat.setOnClickListener {
            when (repeatMode) {
                Constant.REPEAT_MODE_ALL// repeat on
                -> {
                    repeatMode = Constant.REPEAT_MODE_ONE
                    musicService!!.exoPlayer!!.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                    homeBinding.defaultMediaControl.buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_all)
                    Toast.makeText(this, "Repeat mode all", Toast.LENGTH_SHORT).show()
                }
                Constant.REPEAT_MODE_ONE// repeat only one
                -> {
                    repeatMode = Constant.REPEAT_MODE_OFF
                    musicService!!.exoPlayer!!.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    homeBinding.defaultMediaControl.buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_one)
                    Toast.makeText(this, "Repeat mode one", Toast.LENGTH_SHORT).show()
                }
                Constant.REPEAT_MODE_OFF// repeat off
                -> {
                    repeatMode = Constant.REPEAT_MODE_ALL
                    musicService!!.exoPlayer!!.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                    homeBinding.defaultMediaControl.buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_off)
                    Toast.makeText(this, "Repeat mode off", Toast.LENGTH_SHORT).show()
                }
            }
        }/*end BUTTON REPEAT*/

        /* ======================= SEEK BAR =======================*/
        var progressPosition = 0// this variable stores the current position of seek bar
        val seekBarListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                progressPosition = seekBar.progress// update progress position
            }
            /*update current progress position after users leave their off seek bar*/
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if( musicService!!.exoPlayer!!.playbackState == ExoPlayer.STATE_READY)
                {
                    musicService!!.exoPlayer!!.seekTo(progressPosition.toLong())
                    homeBinding.defaultMediaControl.progressStart.text = Multipurpose.getReadableTimestamp(progressPosition)
                    seekBar.progress = progressPosition
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }
        }/*end seekBarListener*/
        homeBinding.defaultMediaControl.seekBar.setOnSeekBarChangeListener(seekBarListener)
        /*end SEEK BAR*/
    }

    /**
     * @since 30-03-2023
     * when service is created successfully and connection between the application and Android OS is setup
     */
    override fun onServiceConnected(p0: ComponentName?, ibinder: IBinder?) {
        Log.d(tag, "onServiceConnected")
        /*if music service is null, it will be created*/
        if(musicService == null)
        {
            val binder = ibinder as MusicService.MyBinder
            musicService = binder.getInstance()

            /*we only set media items for the first time that music service is created*/
            val items = Multipurpose.getMediaItems(songList)
            musicService!!.exoPlayer!!.setMediaItems(items, 0, 0)
            musicService!!.exoPlayer!!.shuffleModeEnabled = true
            /*Log.d(tag, "Music service is NOT NULL")*/
        }

        /*Music service is ready for running, we set up event & playlist*/
        cmcSetupEvent()
        onNewIntent(intent)

        /*create Music player notification*/
        musicService!!.setupMusicNotification(R.drawable.ic_play)

        /*val iconCompact = if( musicService!!.exoPlayer!!.isPlaying ) R.drawable.ic_pause_v2 else R.drawable.ic_play_v2
        val iconDefault = if( musicService!!.exoPlayer!!.isPlaying ) R.drawable.ic_pause else R.drawable.ic_play
        homeBinding.compactMediaControlPlayPause.setImageResource(iconCompact)
        homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(iconDefault)*/
    }

    /**
     * @since 30-03-2023
     * when service is cancelled successfully and connection between the application and Android OS is disconnected
     */
    override fun onServiceDisconnected(p0: ComponentName?) {
        musicService = null
    }/*end onServiceDisconnected*/
}