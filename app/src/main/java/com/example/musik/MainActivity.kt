package com.example.musik

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.musik.Homepage.HomeActivity
import com.example.musik.Mutilpurpose.Multipurpose


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLooper = Looper.getMainLooper()
        val runnable = Runnable {
            val mainIntent = Intent(this, HomeActivity::class.java)
            startActivity(mainIntent)
            finish()
        }

        Handler(mainLooper).postDelayed(runnable, 1500)
    }

    override fun onResume() {
        super.onResume()
        Multipurpose.setStatusBarColor(this, window)
    }
}