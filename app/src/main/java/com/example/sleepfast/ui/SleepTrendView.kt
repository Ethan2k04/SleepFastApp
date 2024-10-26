package com.example.sleepfast.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.sleepfast.viewmodels.MainViewModel
import com.example.sleepfast.R
import java.util.Calendar

class SleepTrendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 来自 MainActivity 的 ViewModel
    private lateinit var viewModel: MainViewModel

    // 数值数据
    private var avgSleepTimeHour: Int = 0
    private var avgSleepTimeMinute: Int = 0

    // 数据容器
    private var sleepWakeTimes = mutableListOf<Pair<Float, Float>>()

    // 位置定义
    private var scaleFactor = 1f

    // 颜色定义
    private val backgroundColor = ContextCompat.getColor(context, R.color.background_green)
    private val progressColor = ContextCompat.getColor(context, R.color.progress_green)
    private val grayColor = ContextCompat.getColor(context, R.color.dark_grey)

    // 画笔定义
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    fun setViewModelStoreOwner(owner: ViewModelStoreOwner) {
        viewModel = ViewModelProvider(owner)[MainViewModel::class.java]
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.sleepWakeTimes.observe(context as LifecycleOwner) { sleepTimes ->
            sleepWakeTimes = sleepTimes.toMutableList()
            postInvalidate()
        }
        viewModel.avgSleepTime.observe(context as LifecycleOwner) { avgSleepTime ->
            val (hour, minute) = extractHourAndMinute(avgSleepTime) ?: Pair(0, 0)
            avgSleepTimeHour = hour
            avgSleepTimeMinute = minute
            postInvalidate()
        }
    }

    fun extractHourAndMinute(timeString: String): Pair<Int, Int>? {
        val regex = """(\d+)h(\d+)m""".toRegex()
        val matchResult = regex.find(timeString)

        return if (matchResult != null) {
            val (hour, minute) = matchResult.destructured
            Pair(hour.toInt(), minute.toInt())
        } else {
            null
        }
    }

    // 将 px 转换为 dp 的方法
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun drawTimeText(canvas: Canvas, x: Float, y: Float, scale: Float){
        // 固定位置参数
        val marginVertical = dpToPx(24f)
        val marginHorizontal = dpToPx(24f)
        val marginText = dpToPx(24f)

        // 绘制时间数值
        paint.textAlign = Paint.Align.LEFT
        paint.color = ContextCompat.getColor(context, R.color.white)
        paint.textSize = dpToPx(36f) * scale
        canvas.drawText(avgSleepTimeHour.toString(), x * scale, y * scale, paint)

        paint.color = ContextCompat.getColor(context, R.color.light_grey)
        paint.textSize = dpToPx(20f) * scale
        canvas.drawText("hr", (x + marginText) * scale, y * scale, paint)

        paint.color = ContextCompat.getColor(context, R.color.white)
        paint.textSize = dpToPx(36f) * scale
        canvas.drawText(avgSleepTimeMinute.toString(), (x + marginText + marginHorizontal) * scale, y * scale, paint)

        if(avgSleepTimeMinute > 9){

            paint.color = ContextCompat.getColor(context, R.color.light_grey)
            paint.textSize = dpToPx(20f) * scale
            canvas.drawText("min", (x + marginText * 2.8f + marginHorizontal) * scale, y * scale, paint)
        }
        else{
            paint.color = ContextCompat.getColor(context, R.color.light_grey)
            paint.textSize = dpToPx(20f) * scale
            canvas.drawText("min", (x + marginText * 2 + marginHorizontal) * scale, y * scale, paint)
        }

        // 标题文字
        paint.textAlign = Paint.Align.LEFT
        paint.color = ContextCompat.getColor(context, R.color.light_grey)
        paint.textSize = dpToPx(16f) * scale
        canvas.drawText("Last 7 Days", x * scale, (y + marginVertical) * scale, paint)
    }

    private fun drawTimeRect(canvas: Canvas, x: Float, y: Float, scale: Float) {
        val marginBetween = dpToPx(8f)
        val marginHorizontal = dpToPx(16f)
        val marginVertical = dpToPx(16f)
        val cornerRadius = dpToPx(4f)
        val rectWidth = (width - marginHorizontal - marginBetween * (sleepWakeTimes.size + 1)) / sleepWakeTimes.size
        val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

        sleepWakeTimes.forEachIndexed { index, (startTime, endTime) ->
            val startTimeAdjusted = if (startTime < endTime && startTime < 12) startTime + 24 else startTime
            val endTimeAdjusted = endTime + 24

            paint.color = when {
                index == currentDayOfWeek -> progressColor
                index < currentDayOfWeek -> backgroundColor
                else -> grayColor
            }

            val rectLeft = x + index * (rectWidth + marginBetween)
            val rectTop = y + (height - marginVertical) * (1 - startTimeAdjusted / 24f)
            val rectBottom = y + (height - marginVertical) * (1 - endTimeAdjusted / 24f)

            canvas.drawRoundRect(
                RectF(rectLeft, rectTop, rectLeft + rectWidth, rectBottom),
                cornerRadius, cornerRadius, paint
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        drawTimeText(canvas, dpToPx(16f), dpToPx(36f), scaleFactor)

        drawTimeRect(canvas, dpToPx(16f), dpToPx(100f), scaleFactor)

        canvas.restore()
    }
}
