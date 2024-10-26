package com.example.sleepfast

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SleepFastApp : Application() {
    // 全局访问点
    companion object {
        lateinit var instance: SleepFastApp

        fun get(): SleepFastApp {
            return instance
        }
    }

    // 日期组件
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val dateId = dateFormat.format(calendar.time).toInt()

    // 音频分类器组件
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var audioClassificationListener: AudioClassificationListener
    lateinit var sleepAudioClassifier: SleepAudioClassifier

    // 数据库组件
    lateinit var database: AppDatabase
    lateinit var audioCategoryDao: AudioCategoryDao
    lateinit var sleepTimeDao: SleepTimeDao

    // 默认睡眠时间设置
    val defaultStartTimeHour = 24
    val defaultStartTimeMinute = 0
    val defaultEndTimeHour = 6
    val defaultEndTimeMinute = 0

    // 振动器组件
    lateinit var vibrator: Vibrator

    override fun onCreate() {
        super.onCreate()

        // 绑定单例
        instance = this

        // 设置应用默认值
        setUpDefaultValue()

        // 初始化其他必要的组件
        database = Room.databaseBuilder(this, AppDatabase::class.java, "sleep-data-db").build()
        audioCategoryDao = database.audioCategoryDao()
        sleepTimeDao = database.sleepTimeDao()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun vibratePhoneClick() {
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    }

    private fun setUpDefaultValue(){
        val defaultConfig = this.getSharedPreferences("DefaultConfig", Context.MODE_PRIVATE)
        val editor = defaultConfig.edit()

        editor.putInt("defaultStartTimeHour", defaultStartTimeHour)
        editor.putInt("defaultStartTimeMinute", defaultStartTimeMinute)
        editor.putInt("defaultEndTimeHour", defaultEndTimeHour)
        editor.putInt("defaultEndTimeMinute", defaultEndTimeMinute)
        editor.apply()
    }

//    // 开始睡眠跟踪并更新数据
//    suspend fun startSleepTracking(startTimeCalendar: Calendar, endTimeCalendar: Calendar) {
//        val startHour = startTimeCalendar.get(Calendar.HOUR_OF_DAY)
//        val startMinute = startTimeCalendar.get(Calendar.MINUTE)
//        val endHour = endTimeCalendar.get(Calendar.HOUR_OF_DAY)
//        val endMinute = endTimeCalendar.get(Calendar.MINUTE)
//
//        // 创建并插入新的 SleepTime 记录
//        val newSleepTime = SleepTime(
//            dateId = dateId,
//            startHour = startHour,
//            startMinute = startMinute,
//            endHour = endHour,
//            endMinute = endMinute
//        )
//        withContext(Dispatchers.IO) {
//            sleepTimeDao.upsertSleepTime(newSleepTime)
//        }
//    }
}