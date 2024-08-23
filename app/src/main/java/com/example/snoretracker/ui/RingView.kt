package com.example.snoretracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.snoretracker.R
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.properties.Delegates

class RingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var centerX = 0f
    private var centerY = 0f
    private var circleXStart = 0f
    private var circleYStart = 0f
    private var circleXEnd = 0f
    private var circleYEnd = 0f
    private var ringRadius = 0f
    private var arcDragStartAngle = 0f
    private var circleRadius = 40f
    private var arcStrokeWidth = 80f
    private var slotStrokeWidth = 120f
    private var dragCircleIndex = 0
    private var tickMargin = Math.PI * 0.05
    private var tickLength = 20f
    private var dayNightIconMargin = 100f
    private var innerOuterRingMargin = 120f
    private var outerRadiusFactor = 2.5f
    private var radialLineStep = 4f
    private val touchSafeMargin = 50f
    private var isDraggable = true
    private var isDraggingCircle = false
    private var isDraggingArc = false
    private var isTouchingRing = false
    private var touchingStartCircle = false
    private var touchingEndCircle = false
    private var startTimeCalendar: Calendar = Calendar.getInstance()
    private var endTimeCalendar: Calendar = Calendar.getInstance()

    // 默认睡眠时间和醒来时间
    private var defaultStartTimeHour = 0
    private var defaultStartTimeMinute = 0
    private var defaultEndTimeHour = 0
    private var defaultEndTimeMinute = 0

    // 时间更新回调函数
    interface TimeUpdateListener {
        fun onRingTimeUpdate(startTime: String, endTime: String)
    }

    var timeUpdateListener: TimeUpdateListener? = null

    // 添加图标的Bitmap对象
    var startIcon = resources.getDrawable(R.drawable.ic_bed_light_16dp, context.theme)
    var endIcon = resources.getDrawable(R.drawable.ic_alarm_light_16dp, context.theme)
    var nightIcon = resources.getDrawable(R.drawable.ic_night_12dp, context.theme)
    var dayIcon = resources.getDrawable(R.drawable.ic_day_12dp, context.theme)

    private val outerPaint = Paint().apply {
        color = resources.getColor(R.color.fg_grey)
        style = Paint.Style.STROKE
        strokeWidth = arcStrokeWidth
        isAntiAlias = true
    }

    private val innerPaint = Paint().apply {
        color = resources.getColor(R.color.black)
        style = Paint.Style.STROKE
        strokeWidth = slotStrokeWidth
        isAntiAlias = true
    }

    private val circlePaint = Paint().apply {
        color = resources.getColor(R.color.light_orange)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val arcPaint = Paint().apply {
        color = resources.getColor(R.color.light_orange)
        style = Paint.Style.STROKE
        strokeWidth = arcStrokeWidth
        isAntiAlias = true
    }

    private val arcLinePaint = Paint().apply {
        color = resources.getColor(R.color.tick_orange)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    val tickPaint = Paint().apply {
        color = resources.getColor(R.color.light_grey)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val outerRadius = Math.min(width, height) / outerRadiusFactor
        val innerRadius = outerRadius - innerOuterRingMargin
        val startTime = getStartTime()
        val endTime = getEndTime()

        ringRadius = innerRadius
        centerX = width / 2f
        centerY = height / 2f
        timeUpdateListener?.onRingTimeUpdate(startTime, endTime)

        // 如果没有初始位置，设置圆的位置在圆环上
        if (circleXStart == 0f && circleYStart == 0f) {
            updateCirclePosition(convertTimeToAngle(String.format("%02d:%02d", defaultStartTimeHour, defaultStartTimeMinute)).toFloat(), 1)
        }

        if (circleXEnd == 0f && circleYEnd == 0f) {
            updateCirclePosition(convertTimeToAngle(String.format("%02d:%02d", defaultEndTimeHour, defaultEndTimeMinute)).toFloat(), 2)
        }

        // 计算起始角度和终止角度
        val startAngle = calculateAngle(circleXStart, circleYStart)
        val endAngle = calculateAngle(circleXEnd, circleYEnd)

        // 绘制外环
        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint)

        // 绘制内环
        canvas.drawCircle(centerX, centerY, innerRadius, innerPaint)

        // 绘制连接圆的弧形条
        drawArcBetweenCircles(canvas)

        // 绘制可以拖动的起始圆
        canvas.drawCircle(circleXStart, circleYStart, circleRadius, circlePaint)

        // 绘制可以拖动的终点圆
        canvas.drawCircle(circleXEnd, circleYEnd, circleRadius, circlePaint)

        // 绘制图标
        drawIconAtCircle(canvas, startIcon, circleXStart, circleYStart)
        drawIconAtCircle(canvas, endIcon, circleXEnd, circleYEnd)
        drawIconAtCircle(canvas, nightIcon, centerX, centerY - dayNightIconMargin)
        drawIconAtCircle(canvas, dayIcon, centerX, centerY + dayNightIconMargin)

        // 绘制径向线条
        drawRadialLines(canvas, centerX, centerY, ringRadius, startAngle, endAngle)

        // 绘制时间刻度
        drawTimeMarks(canvas, innerRadius - arcStrokeWidth / 2)

        // 绘制内环和刻度线之间的等间距刻度线
        drawIntermediateTicks(canvas, innerRadius)
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

    private fun drawIconAtCircle(canvas: Canvas, icon: Drawable, x: Float, y: Float) {
        val iconWidth = icon.intrinsicWidth
        val iconHeight = icon.intrinsicHeight
        icon.setBounds(
            (x - iconWidth / 2).toInt(),
            (y - iconHeight / 2).toInt(),
            (x + iconWidth / 2).toInt(),
            (y + iconHeight / 2).toInt()
        )
        icon.draw(canvas)
    }

    // 添加方法来设置是否允许拖动
    fun setDraggable(draggable: Boolean) {
        isDraggable = draggable
        invalidate()
    }

    // 添加方法来设置颜色
    fun setColors(colorResId: Int) {
        val color = resources.getColor(colorResId, context.theme)
        circlePaint.color = color
        arcPaint.color = color
        invalidate()
    }

    // 添加方法来设置图标
    fun setIcons(startIcon: Drawable?, endIcon: Drawable?) {
        startIcon?.let { this.startIcon = it }
        endIcon?.let { this.endIcon = it }
        invalidate()
    }

    fun calendar2Time(calendar: Calendar): Pair<Int, Int>{
        // 获取小时（24小时制）
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        // 获取分钟
        val minutes = calendar.get(Calendar.MINUTE)

        return Pair(hours, minutes)
    }

    fun setStartTimePosition(startTime: Calendar) {
        val (hours, minutes) = calendar2Time(startTime)
        startTimeCalendar.set(Calendar.HOUR_OF_DAY, hours)
        startTimeCalendar.set(Calendar.MINUTE, minutes)
        updateCirclePosition(convertTimeToAngle(String.format("%02d:%02d", hours, minutes)).toFloat(), 1)
    }

    fun setEndTimePosition(endTime: Calendar) {
        val (hours, minutes) = calendar2Time(endTime)
        endTimeCalendar.set(Calendar.HOUR_OF_DAY, hours)
        endTimeCalendar.set(Calendar.MINUTE, minutes)
        updateCirclePosition(convertTimeToAngle(String.format("%02d:%02d", hours, minutes)).toFloat(), 2)
    }

    private fun drawArcBetweenCircles(canvas: Canvas) {
        val startAngle = Math.toDegrees(atan2(circleYStart - centerY, circleXStart - centerX).toDouble()).toFloat()
        val endAngle = Math.toDegrees(atan2(circleYEnd - centerY, circleXEnd - centerX).toDouble()).toFloat()

        val sweepAngle = if (startAngle < endAngle) {
            endAngle - startAngle
        } else {
            360f - (startAngle - endAngle)
        }

        val rectF = RectF(
            centerX - ringRadius,
            centerY - ringRadius,
            centerX + ringRadius,
            centerY + ringRadius
        )

        canvas.drawArc(rectF, startAngle, sweepAngle, false, arcPaint)
    }

    private fun calculateAngle(x: Float, y: Float): Float {
        return atan2(y - centerY, x - centerX).toDouble().toFloat()
    }

    private fun drawRadialLines(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, startAngle: Float, endAngle: Float) {
        val angleStep = radialLineStep
        val numberOfLines = (360f / angleStep).toInt()
        for (i in 0 until numberOfLines) {
            val angle = Math.toRadians((i * angleStep - 180f).toDouble())
            var canDraw = false
            if(startAngle > endAngle){
                if(angle in startAngle + tickMargin..Math.PI || angle < endAngle - tickMargin){
                    canDraw = true
                }
            }
            else{
                if(angle in startAngle + tickMargin..endAngle - tickMargin){
                    canDraw = true
                }
            }
            if(canDraw){
                val startX = centerX + (radius - arcStrokeWidth / 2 + tickLength) * cos(angle).toFloat()
                val startY = centerY + (radius - arcStrokeWidth / 2 + tickLength) * sin(angle).toFloat()
                val endX = centerX + (radius + arcStrokeWidth / 2 - tickLength) * cos(angle).toFloat()
                val endY = centerY + (radius + arcStrokeWidth / 2 - tickLength) * sin(angle).toFloat()
                canvas.drawLine(startX, startY, endX, endY, arcLinePaint)
            }
        }
    }

    private fun drawTimeMarks(canvas: Canvas, radius: Float) {
        var timePaint = Paint().apply {
            textSize = 30f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        for (i in 0..23) {
            if(i % 2 == 0){
                timePaint.color = resources.getColor(R.color.light_grey)
                val angle = Math.toRadians((i * 15).toDouble() - 90)
                val x = (centerX + (radius - 80) * cos(angle)).toFloat()
                val y = (centerY + (radius - 80) * sin(angle)).toFloat() + 15
                if(i % 6 == 0){
                    timePaint.color = resources.getColor(R.color.white)
                }
                canvas.drawText(i.toString(), x, y, timePaint)
            }
        }
    }

    private fun drawIntermediateTicks(canvas: Canvas, innerRadius: Float) {
        val tickNum = 48
        val tickInterval = 360f / tickNum
        var tickLength = 0f
        for (i in 0 until tickNum) {
            tickLength = if(i % 4 == 0){
                15f
            } else{
                5f
            }
            val angle = Math.toRadians((i * tickInterval).toDouble())
            val startX = centerX + (innerRadius - arcStrokeWidth) * cos(angle).toFloat()
            val startY = centerY + (innerRadius - arcStrokeWidth) * sin(angle).toFloat()
            val endX = centerX + (innerRadius - arcStrokeWidth - tickLength) * cos(angle).toFloat()
            val endY = centerY + (innerRadius - arcStrokeWidth - tickLength) * sin(angle).toFloat()
            canvas.drawLine(startX, startY, endX, endY, tickPaint)
        }
    }

    interface ArcTouchListener {
        fun onRingTouched(isInsideArc: Boolean)
    }

    var arcTouchListener: ArcTouchListener? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDraggable) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragCircleIndex = isTouchInsideCircle(event.x, event.y)
                isDraggingArc = isTouchInsideArc(event.x, event.y)
                isTouchingRing = isTouchInsideInnerRing(event.x, event.y)
                arcTouchListener?.onRingTouched(isTouchingRing)
                if (dragCircleIndex != 0 || isDraggingArc) {
                    isDraggingCircle = true
                    if (isDraggingArc) {
                        arcDragStartAngle = atan2(event.y - centerY, event.x - centerX)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingCircle) {
                    val angle = atan2(event.y - centerY, event.x - centerX)
                    if (dragCircleIndex == 1) {
                        updateCirclePosition(angle, 1)
                        touchingStartCircle = true
                    } else if (dragCircleIndex == 2) {
                        updateCirclePosition(angle, 2)
                        touchingEndCircle = true
                    } else if (isDraggingArc) {
                        updateArcPositions(angle)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isDraggingCircle = false
                touchingStartCircle = false
                touchingEndCircle = false
            }
        }
        return true
    }

    private fun isTouchInsideCircle(touchX: Float, touchY: Float): Int {
        val distanceStart = hypot(touchX - circleXStart, touchY - circleYStart)
        val distanceEnd = hypot(touchX - circleXEnd, touchY - circleYEnd)
        return when {
            distanceStart <= circleRadius -> 1
            distanceEnd <= circleRadius-> 2
            else -> 0
        }
    }

    private fun isTouchInsideArc(touchX: Float, touchY: Float): Boolean {
        val startAngle = atan2(circleYStart - centerY, circleXStart - centerX)
        val endAngle = atan2(circleYEnd - centerY, circleXEnd - centerX)
        val angle = atan2(touchY - centerY, touchX - centerX)
        val distanceToCenter = hypot(touchX - centerX, touchY - centerY)

        val normalizedStartAngle = (startAngle + 2 * Math.PI).rem(2 * Math.PI).toFloat()
        val normalizedEndAngle = (endAngle + 2 * Math.PI).rem(2 * Math.PI).toFloat()
        val normalizedAngle = (angle + 2 * Math.PI).rem(2 * Math.PI).toFloat()

        val isInAngleRange = if (normalizedStartAngle <= normalizedEndAngle) {
            normalizedAngle in normalizedStartAngle..normalizedEndAngle
        } else {
            normalizedAngle >= normalizedStartAngle || normalizedAngle <= normalizedEndAngle
        }

        return isInAngleRange && distanceToCenter in (ringRadius - arcStrokeWidth / 2)..(ringRadius + arcStrokeWidth / 2)
    }

    private fun isTouchInsideInnerRing(touchX: Float, touchY: Float): Boolean {
        val distanceToCenter = hypot(touchX - centerX, touchY - centerY)
        return distanceToCenter in ringRadius - touchSafeMargin ..ringRadius + touchSafeMargin
    }


    private fun updateCirclePosition(angle: Float, circleIndex: Int) {
        if (circleIndex == 1) {
            circleXStart = centerX + ringRadius * cos(angle)
            circleYStart = centerY + ringRadius * sin(angle)
            if(touchingStartCircle){
                val (hours, minutes) = convertAngleToTime(angle)
                startTimeCalendar.set(Calendar.HOUR_OF_DAY, hours)
                startTimeCalendar.set(Calendar.MINUTE, minutes)
            }
        } else if (circleIndex == 2) {
            circleXEnd = centerX + ringRadius * cos(angle)
            circleYEnd = centerY + ringRadius * sin(angle)
            if(touchingEndCircle){
                val (hours, minutes) = convertAngleToTime(angle)
                endTimeCalendar.set(Calendar.HOUR_OF_DAY, hours)
                endTimeCalendar.set(Calendar.MINUTE, minutes)
            }
        }
    }

    private fun updateArcPositions(currentAngle: Float) {
        val angleDifference = currentAngle - arcDragStartAngle
        val startAngle = atan2(circleYStart - centerY, circleXStart - centerX)
        val endAngle = atan2(circleYEnd - centerY, circleXEnd - centerX)
        val newStartAngle = startAngle + angleDifference
        val newEndAngle = endAngle + angleDifference

        circleXStart = (centerX + ringRadius * cos(newStartAngle))
        circleYStart = (centerY + ringRadius * sin(newStartAngle))

        val (hoursStart, minutesStart) = convertAngleToTime(newStartAngle)
        startTimeCalendar.set(Calendar.HOUR_OF_DAY, hoursStart)
        startTimeCalendar.set(Calendar.MINUTE, minutesStart)

        circleXEnd = (centerX + ringRadius * cos(newEndAngle))
        circleYEnd = (centerY + ringRadius * sin(newEndAngle))

        val (hoursEnd, minutesEnd) = convertAngleToTime(newEndAngle)
        endTimeCalendar.set(Calendar.HOUR_OF_DAY, hoursEnd)
        endTimeCalendar.set(Calendar.MINUTE, minutesEnd)

        arcDragStartAngle = currentAngle
    }

    private fun convertAngleToTime(angle: Float): Pair<Int, Int> {
        val totalMinutes = (angle / (2 * Math.PI) * 24 * 60).toInt()
        val hours = (6 + totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return Pair(hours, minutes)
    }

    private fun convertTimeToAngle(time: String): Double {
        val parts = time.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val totalMinutes = (hours - 6) * 60 + minutes // 以6点为起点
        return totalMinutes.toDouble() / (24 * 60) * 2 * Math.PI
    }

    fun getStartTime(): String{
        val (hours, minutes) = calendar2Time(startTimeCalendar)
        return String.format("%02d:%02d", hours, minutes)
    }

    fun getEndTime(): String{
        val (hours, minutes) = calendar2Time(endTimeCalendar)
        return String.format("%02d:%02d", hours, minutes)
    }
}