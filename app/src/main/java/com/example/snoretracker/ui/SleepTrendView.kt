package com.example.snoretracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.snoretracker.R
import com.example.snoretracker.SleepTimeDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class SleepTrendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var sleepTimeDao: SleepTimeDao

    // 位置定义
    private val marginBetween = 40f
    private val marginLeft = 50f
    private val marginTop = 200f
    private val cornerRadius = 30f
    private val rectLength = 3f
    private var scaleFactor = 0.5f

    // 默认睡眠时间和醒来时间
    private var defaultStartTimeHour = 0
    private var defaultStartTimeMinute = 0
    private var defaultEndTimeHour = 0
    private var defaultEndTimeMinute = 0

    // 用于绘制矩形的 Paint
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    // 颜色定义
    private val backgroundColor = ContextCompat.getColor(context, R.color.background_green)
    private val progressColor = ContextCompat.getColor(context, R.color.progress_green)
    private val grayColor = ContextCompat.getColor(context, R.color.dark_grey)

    // 睡眠和醒来时间（以小时为单位）
    private var sleepWakeTimes = mutableListOf<Pair<Float, Float>>()

    // 设置 SleepTimeDao 的方法
    fun setSleepTimeDao(dao: SleepTimeDao) {
        sleepTimeDao = dao
        fetchSleepData()
    }

    init {
        // 初始化时获取默认配置
        loadDefaultConfig()
    }

    private fun loadDefaultConfig() {
        val sharedPreferences = context.getSharedPreferences("DefaultConfig", Context.MODE_PRIVATE)
        defaultStartTimeHour = sharedPreferences.getInt("defaultStartTimeHour", 0)
        defaultStartTimeMinute = sharedPreferences.getInt("defaultStartTimeMinute", 0)
        defaultEndTimeHour = sharedPreferences.getInt("defaultEndTimeHour", 6)
        defaultEndTimeMinute = sharedPreferences.getInt("defaultEndTimeMinute", 0)
    }

    private fun fetchSleepData() {
        if (!::sleepTimeDao.isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val sleepTimes = (0..6).map { day ->
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + day)
                val dateId = dateFormat.format(calendar.time).toInt()
                sleepTimeDao.getSleepTimeByDate(dateId).let { sleepTime ->
                    val startHour = sleepTime?.startHour?.toFloat() ?: defaultStartTimeHour.toFloat()
                    val startMinute = sleepTime?.startMinute?.toFloat() ?: defaultStartTimeMinute.toFloat()
                    val endHour = sleepTime?.endHour?.toFloat() ?: defaultEndTimeHour.toFloat()
                    val endMinute = sleepTime?.endMinute?.toFloat() ?: defaultEndTimeMinute.toFloat()
                    Pair(startHour + startMinute / 60f, endHour + endMinute / 60f)
                }
            }

            // 更新 UI
            withContext(Dispatchers.Main) {
                sleepWakeTimes = sleepTimes.toMutableList()
                postInvalidate()
            }
        }
    }

    fun notifySleepDateChanged(dayIndex: Int) {
        if (::sleepTimeDao.isInitialized && dayIndex in 0 until sleepWakeTimes.size) {
            fetchSleepData()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)

        val rectWidth = 4 * (width - marginBetween * (sleepWakeTimes.size + 1)) / sleepWakeTimes.size
        val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

        sleepWakeTimes.forEachIndexed { index, (startTime, endTime) ->
            val startTimeAdjusted = if(startTime < endTime && startTime < 12) startTime + 24 else startTime
            val endTimeAdjusted = endTime + 24

        paint.color = when {
                index == currentDayOfWeek -> progressColor
                index < currentDayOfWeek -> backgroundColor
                else -> grayColor
            }

            val rectLeft = marginBetween + index * (rectWidth + marginBetween) + marginLeft
            val rectTop = marginTop + rectLength * (height * (1 - startTimeAdjusted / 24f))
            val rectBottom = marginTop + rectLength * (height * (1 - endTimeAdjusted / 24f))

            canvas.drawRoundRect(
                RectF(rectLeft, rectTop, rectLeft + rectWidth, rectBottom),
                cornerRadius, cornerRadius, paint
            )
        }

        canvas.restore()
    }
}
