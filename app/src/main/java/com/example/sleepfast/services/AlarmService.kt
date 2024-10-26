package com.example.sleepfast.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi

class AlarmService : Service() {
    private var alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    private lateinit var vibrator: Vibrator
    private var mediaPlayer: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.getStringExtra("ACTION") ?: "START"
        val volume = intent.getFloatExtra("VOLUME", 0.5f)

        when (action) {
            "STOP" -> stopAlarm()
            "START" -> startAlarm()
            "SET_VOLUME" -> setVolume(volume)
        }

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startAlarm() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 2000, 1000)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))

        mediaPlayer = MediaPlayer.create(this, alarmUri).apply {
            val sharedPreferences = getSharedPreferences("AlarmConfig", Context.MODE_PRIVATE)
            val savedVolume = sharedPreferences.getFloat("alarmVolume", .5f)
            setVolume(savedVolume, savedVolume)
            start()
            isLooping = true
            start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopAlarm() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.cancel()
        }

        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    private fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
