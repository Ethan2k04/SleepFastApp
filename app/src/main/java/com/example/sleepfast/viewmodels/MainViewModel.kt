// MainViewModel.kt
package com.example.sleepfast.viewmodels

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sleepfast.SleepFastApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// 数据类，用于存储音频统计信息
data class AudioStats(val snoreTime: Float, val speechTime: Float, val wakeupTime: Float)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val sleepFastApp = SleepFastApp.get()

    // LiveData 用于观察起止时间
    private val _startTimeCalendar = MutableLiveData(Calendar.getInstance())
    val startTimeCalendar: LiveData<Calendar> get() = _startTimeCalendar

    private val _endTimeCalendar = MutableLiveData(Calendar.getInstance())
    val endTimeCalendar: LiveData<Calendar> get() = _endTimeCalendar

    // LiveData 用于获得闹钟服务
    private val alarmManagerInstance = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val _alarmManager = MutableLiveData(alarmManagerInstance)
    val alarmManager: LiveData<AlarmManager> get() = _alarmManager

    // LiveData 用于观察音频统计数据
    private val _audioStats = MutableLiveData<AudioStats>()
    val audioStats: LiveData<AudioStats> get() = _audioStats

    // LiveData 用于观察平均睡眠时间
    private val _avgSleepTime = MutableLiveData<String>()
    val avgSleepTime: LiveData<String> get() = _avgSleepTime

    // 获取当前日期的 ID
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val dateId = dateFormat.format(calendar.time).toInt()

    // 起止时间对
    private val _sleepWakeTimes = MutableLiveData<List<Pair<Float, Float>>>()
    val sleepWakeTimes: LiveData<List<Pair<Float, Float>>> = _sleepWakeTimes

    // 默认值接口
    val defaultConfig = sleepFastApp.getSharedPreferences("DefaultConfig", Context.MODE_PRIVATE)
    val defaultStartTimeHour = defaultConfig.getInt("defaultStartTimeHour", 0)
    val defaultStartTimeMinute = defaultConfig.getInt("defaultStartTimeMinute", 0)
    val defaultEndTimeHour = defaultConfig.getInt("defaultEndTimeHour", 0)
    val defaultEndTimeMinute = defaultConfig.getInt("defaultEndTimeMinute", 0)

    init {
        // 获取默认值
        fetchDefaultValue()

        // 初始化LiveData值
        viewModelScope.launch {
            updateAudioStats()
            updateAvgSleepTime()
            updateSleepWakeData()
        }
    }

    private fun fetchDefaultValue(){
        // 初始化默认值
        startTimeCalendar.value?.set(Calendar.HOUR_OF_DAY, defaultStartTimeHour)
        startTimeCalendar.value?.set(Calendar.MINUTE, defaultStartTimeMinute)
        endTimeCalendar.value?.set(Calendar.HOUR_OF_DAY, defaultEndTimeHour)
        endTimeCalendar.value?.set(Calendar.MINUTE, defaultEndTimeMinute)
    }

    fun updateSleepWakeData() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val sleepTimes = (0..6).map { day ->
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + day)
                val dateId = dateFormat.format(calendar.time).toInt()
                sleepFastApp.sleepTimeDao.getSleepTimeByDate(dateId).let { sleepTime ->
                    val startHour = sleepTime?.startHour?.toFloat() ?: defaultConfig.getInt("defaultStartTimeHour", 0).toFloat()
                    val startMinute = sleepTime?.startMinute?.toFloat() ?: defaultConfig.getInt("defaultStartTimeMinute", 0).toFloat()
                    val endHour = sleepTime?.endHour?.toFloat() ?: defaultConfig.getInt("defaultEndTimeHour", 0).toFloat()
                    val endMinute = sleepTime?.endMinute?.toFloat() ?: defaultConfig.getInt("defaultEndTimeMinute", 0).toFloat()
                    Pair(startHour + startMinute / 60f, endHour + endMinute / 60f)
                }
            }

            _sleepWakeTimes.postValue(sleepTimes)
        }
    }

    // 更新音频统计数据
    private suspend fun updateAudioStats() {
        withContext(Dispatchers.IO) {
            val snoreTime = sleepFastApp.audioCategoryDao.getTotalTimeForLabel(dateId, "Snoring") ?: 0.0f
            val speechTime = sleepFastApp.audioCategoryDao.getTotalTimeForLabel(dateId, "Speech") ?: 0.0f
            val wakeupTime = sleepFastApp.audioCategoryDao.getTotalTimeForLabel(dateId, "Inside, small room") ?: 0.0f
            _audioStats.postValue(AudioStats(snoreTime, speechTime, wakeupTime))
        }
    }

    // 更新平均睡眠时间
    suspend fun updateAvgSleepTime() {
        withContext(Dispatchers.IO) {
            var totalSleepTimeLastWeek = 0f

            var defaultSleepDuration = (defaultEndTimeHour - defaultStartTimeHour) + (defaultEndTimeMinute - defaultStartTimeMinute) / 60f
            if (defaultStartTimeHour > defaultEndTimeHour) {
                defaultSleepDuration += 24f
            }

            // 计算过去一周的平均睡眠时间
            for (i in (dateId - 6)..dateId) {
                val sleepDuration = sleepFastApp.sleepTimeDao.getSleepDurationByDate(i) ?: defaultSleepDuration
                totalSleepTimeLastWeek += sleepDuration / 7f
            }

            val avgTime = totalSleepTimeLastWeek
            val totalMinutes = (avgTime * 60).roundToInt()
            val hour = totalMinutes / 60
            val minute = totalMinutes % 60
            val avgSleepTimeString = "${hour}h${minute}m"

            _avgSleepTime.postValue(avgSleepTimeString)
        }
    }
}
