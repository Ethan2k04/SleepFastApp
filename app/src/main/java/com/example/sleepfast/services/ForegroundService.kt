package com.example.sleepfast.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sleep Master")
            .setContentText("App is running in the background")
            .setPriority(NotificationCompat.PRIORITY_LOW)
        return notificationBuilder.build()
    }

    companion object {
        private const val CHANNEL_ID = "Sleep Master Channel"
        private const val NOTIFICATION_ID = 1
    }
}
