package com.example.sleepfast.viewmodels

import android.app.Application
import android.app.AlarmManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.sleepfast.AppDatabase
import com.example.sleepfast.SleepFastApp
import com.example.sleepfast.SleepTime
import com.example.sleepfast.SleepTimeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SleepViewModel(application: Application) : AndroidViewModel(application) {

    // Application的全局访问点
    val sleepFastApp = SleepFastApp.get()

    // 当前日历实例
    val calendar = Calendar.getInstance()

    // 简单的日期格式器
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    // 格式化当前日期为字符串，并转换为整数的日期ID
    val dateId = dateFormat.format(calendar.time).toInt()

    // 数据库和接口
    private val db: AppDatabase
    private val sleepTimeDao: SleepTimeDao

    // AlarmManager的LiveData
    val alarmManager = MutableLiveData<AlarmManager>()

    // 闹钟时间的LiveData
    val startTimeCalendar = MutableLiveData(Calendar.getInstance())
    val endTimeCalendar = MutableLiveData(Calendar.getInstance())

    // 按钮状态的LiveData
    val isStartPressed = MutableLiveData<Boolean>()
    val isSwitchChecked = MutableLiveData<Boolean>()

    init {
        // 初始化AlarmManager
        val alarmManagerInstance = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.value = alarmManagerInstance

        // 初始化数据库
        db = (application as SleepFastApp).database
        sleepTimeDao = db.sleepTimeDao()
    }

    // 更新时间用户界面的函数
    fun updateTimeUI(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int){
        startTimeCalendar.value?.set(Calendar.HOUR_OF_DAY, startHour)
        startTimeCalendar.value?.set(Calendar.MINUTE, startMinute)

        endTimeCalendar.value?.set(Calendar.HOUR_OF_DAY, endHour)
        endTimeCalendar.value?.set(Calendar.MINUTE, endMinute)

        startTimeCalendar.postValue(startTimeCalendar.value)
        endTimeCalendar.postValue(endTimeCalendar.value)
    }

    // 挂起函数，用于插入或更新睡眠数据
    suspend fun upsertSleepData(){
        val startHour = startTimeCalendar.value?.get(Calendar.HOUR_OF_DAY) ?: 0
        val startMinute = startTimeCalendar.value?.get(Calendar.MINUTE) ?: 0
        val endHour = endTimeCalendar.value?.get(Calendar.HOUR_OF_DAY) ?: 0
        val endMinute = endTimeCalendar.value?.get(Calendar.MINUTE) ?: 0

        // 创建并插入或更新SleepTime记录
        val newSleepTime = SleepTime(
            dateId = dateId,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute
        )

        // 在IO线程上执行数据库操作
        withContext(Dispatchers.IO) {
            sleepTimeDao.upsertSleepTime(newSleepTime)
        }
    }

    fun setStartPressed(isPressed: Boolean) {
        isStartPressed.value = isPressed
    }

    fun setSwitchChecked(isPressed: Boolean) {
        isSwitchChecked.value = isPressed
    }
}