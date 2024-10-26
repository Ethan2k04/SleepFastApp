package com.example.sleepfast.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.sleepfast.services.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("ACTION") ?: "START"
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ACTION", action)
        }

        when (action) {
            "STOP" -> context.stopService(serviceIntent)
            else -> context.startService(serviceIntent)
        }

        // 将是否已经触发的信息回传给MainActivity
        if (action != "STOP") {
            sendAlarmStartedBroadcast(context)
        }
    }

    private fun sendAlarmStartedBroadcast(context: Context) {
        val broadcastIntent = Intent("ALARM_STARTED").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        context.sendBroadcast(broadcastIntent)
    }
}
