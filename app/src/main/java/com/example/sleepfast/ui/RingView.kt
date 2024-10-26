package com.example.sleepfast.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.example.sleepfast.R
import com.example.sleepfast.SleepFastApp
import com.example.sleepfast.viewmodels.SleepViewModel
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class RingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    data class ArcData(
        val startAngle: Float,
        val sweepAngle: Float,
        val gradientStartColor: Int,
        val gradientEndColor: Int,
        val startColor: Int,
        val endColor: Int
    )

    // 位置定义
    private var centerX = 0f
    private var centerY = 0f
    private var innerCircleXStart = 0f
    private var innerCircleYStart = 0f
    private var innerCircleXEnd = 0f
    private var innerCircleYEnd = 0f
    private var innerRingRadius = 0f
    private var innerCircleRadius = 40f
    private var arcDragStartAngle = 0f
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
    private var outerCircleXStart = 0f
    private var outerCircleYStart = 0f
    private var outerCircleXEnd = 0f
    private var outerCircleYEnd = 0f
    private var outerCircleRadius = 40f
    private var outerRingRadius = 0f

    // 圆弧段数据
    private val arcData = mutableListOf<ArcData>()

    // 控制变量
    private var isDraggable = true
    private var isDraggingCircle = false
    private var isDraggingArc = false
    private var isTouchingRing = false
    private var touchingStartCircle = false
    private var touchingEndCircle = false

    // view持有的日历变量
    private lateinit var startTimeCalendar: Calendar
    private lateinit var endTimeCalendar: Calendar

    // 默认睡眠时间和醒来时间
    private var defaultStartTimeHour = 0
    private var defaultStartTimeMinute = 0
    private var defaultEndTimeHour = 0
    private var defaultEndTimeMinute = 0

    // 时间更新回调函数
    interface TimeUpdateListener {
        fun onRingTimeUpdate(startTime: String, endTime: String)
    }

    val arcs = listOf(
        ArcData(
            startAngle = -120f,
            sweepAngle = 100f,
            gradientStartColor = resources.getColor(R.color.dark_orange),
            gradientEndColor = resources.getColor(R.color.light_orange),
            startColor = resources.getColor(R.color.dark_orange),
            endColor = resources.getColor(R.color.dark_orange)
        ),
    )

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

    private val innerCirclePaint = Paint().apply {
        color = resources.getColor(R.color.light_orange)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val innerArcPaint = Paint().apply {
        color = resources.getColor(R.color.light_orange)
        style = Paint.Style.STROKE
        strokeWidth = arcStrokeWidth
        isAntiAlias = true
    }

    private val outerArcPaint = Paint().apply {
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

    init {
        // 初始化时获取默认配置
        loadDefaultConfig()

        // setArcData(arcs)

        setUpCalendar()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val outerRadius = Math.min(width, height) / outerRadiusFactor
        val innerRadius = outerRadius - innerOuterRingMargin
        val startTime = getStartTime()
        val endTime = getEndTime()

        innerRingRadius = innerRadius
        outerRingRadius = outerRadius
        centerX = width / 2f
        centerY = height / 2f
        timeUpdateListener?.onRingTimeUpdate(startTime, endTime)

        // 如果没有初始位置，设置圆的位置在圆环上
        if (innerCircleXStart == 0f && innerCircleYStart == 0f) {
            val (hours, minutes) = calendar2Time(startTimeCalendar)
            updateInnerCirclePosition(convertTimeToAngle(String.format("%02d:%02d", hours, minutes)).toFloat(), 1)
        }

        if (innerCircleXEnd == 0f && innerCircleYEnd == 0f) {
            val (hours, minutes) = calendar2Time(endTimeCalendar)
            updateInnerCirclePosition(convertTimeToAngle(String.format("%02d:%02d", hours, minutes)).toFloat(), 2)
        }

//        if (outerCircleXStart == 0f && outerCircleYStart == 0f) {
//            updateOuterCirclePosition(convertTimeToAngle(String.format("%02d:%02d", defaultStartTimeHour, defaultStartTimeMinute)).toFloat(), 1)
//        }
//
//        if (outerCircleXEnd == 0f && outerCircleYEnd == 0f) {
//            updateOuterCirclePosition(convertTimeToAngle(String.format("%02d:%02d", defaultEndTimeHour, defaultEndTimeMinute)).toFloat(), 2)
//        }

        // 计算起始角度和终止角度
        val startAngle = calculateAngle(innerCircleXStart, innerCircleYStart)
        val endAngle = calculateAngle(innerCircleXEnd, innerCircleYEnd)

        // 绘制外环
        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint)

        // 绘制每段圆弧
        for (arc in arcData) {
            drawArcWithGradient(canvas, arc)
        }

        // 绘制内环
        canvas.drawCircle(centerX, centerY, innerRadius, innerPaint)

        // 绘制连接圆的弧形条
        drawInnerArcBetweenCircles(canvas)

        // 绘制可以拖动的内环起始圆
        canvas.drawCircle(innerCircleXStart, innerCircleYStart, innerCircleRadius, innerCirclePaint)

        // 绘制可以拖动的内环终点圆
        canvas.drawCircle(innerCircleXEnd, innerCircleYEnd, innerCircleRadius, innerCirclePaint)

        // 绘制图标
        drawIconAtCircle(canvas, startIcon, innerCircleXStart, innerCircleYStart)
        drawIconAtCircle(canvas, endIcon, innerCircleXEnd, innerCircleYEnd)
        drawIconAtCircle(canvas, nightIcon, centerX, centerY - dayNightIconMargin)
        drawIconAtCircle(canvas, dayIcon, centerX, centerY + dayNightIconMargin)

        // 绘制径向线条
        drawRadialLines(canvas, centerX, centerY, innerRingRadius, startAngle, endAngle)

        // 绘制时间刻度
        drawTimeMarks(canvas, innerRadius - arcStrokeWidth / 2)

        // 绘制内环和刻度线之间的等间距刻度线
        drawIntermediateTicks(canvas, innerRadius)
    }

    private fun loadDefaultConfig() {
        val sharedPreferences = context.getSharedPreferences("DefaultConfig", Context.MODE_PRIVATE)
        defaultStartTimeHour = sharedPreferences.getInt("defaultStartTimeHour", 0)
        defaultStartTimeMinute = sharedPreferences.getInt("defaultStartTimeMinute", 0)
        defaultEndTimeHour = sharedPreferences.getInt("defaultEndTimeHour", 0)
        defaultEndTimeMinute = sharedPreferences.getInt("defaultEndTimeMinute", 0)
    }

    private fun setUpCalendar(){
        // 初始化日历
        val currentCalendar = Calendar.getInstance()

        // 将当前分钟向上舍入到最近的5分钟
        val currentMinute = currentCalendar.get(Calendar.MINUTE)
        val roundedMinute = (currentMinute + 4) / 5 * 5
        currentCalendar.set(Calendar.MINUTE, roundedMinute)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)

        // 设置 startTimeCalendar 为当前时间最近的整5分钟
        startTimeCalendar= currentCalendar

        // 设置 endTimeCalendar 为 startTimeCalendar 往后6小时的时间
        val endCalendar = currentCalendar.clone() as Calendar
        endCalendar.add(Calendar.HOUR_OF_DAY, 6)
        endTimeCalendar = endCalendar
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
        innerCirclePaint.color = color
        innerArcPaint.color = color
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
        updateInnerCirclePosition(convertTimeToAngle(String.format("%02d:%02d", hours, minutes)).toFloat(), 1)
    }

    fun setEndTimePosition(endTime: Calendar) {
        val (hours, minutes) = calendar2Time(endTime)
        endTimeCalendar.set(Calendar.HOUR_OF_DAY, hours)
        endTimeCalendar.set(Calendar.MINUTE, minutes)
        updateInnerCirclePosition(convertTimeToAngle(String.format("%02d:%02d", hours, minutes)).toFloat(), 2)
    }

    private fun drawInnerArcBetweenCircles(canvas: Canvas) {
        val startAngle = Math.toDegrees(atan2(innerCircleYStart - centerY, innerCircleXStart - centerX).toDouble()).toFloat()
        val endAngle = Math.toDegrees(atan2(innerCircleYEnd - centerY, innerCircleXEnd - centerX).toDouble()).toFloat()

        val sweepAngle = if (startAngle < endAngle) {
            endAngle - startAngle
        } else {
            360f - (startAngle - endAngle)
        }

        val rectF = RectF(
            centerX - innerRingRadius,
            centerY - innerRingRadius,
            centerX + innerRingRadius,
            centerY + innerRingRadius
        )

        canvas.drawArc(rectF, startAngle, sweepAngle, false, innerArcPaint)
    }

    private fun drawArcWithGradient(canvas: Canvas, arc: ArcData) {
        // Draw start and end circles
        drawArcCircle(canvas, arc.startAngle, arc.startColor)
        drawArcCircle(canvas, arc.startAngle + arc.sweepAngle, arc.endColor)

        val rectF = RectF(
            centerX - outerRingRadius,
            centerY - outerRingRadius,
            centerX + outerRingRadius,
            centerY + outerRingRadius
        )

        val startAngle = arc.startAngle
        val sweepAngle = arc.sweepAngle

        // Create a SweepGradient for the arc
        val sweepGradient = SweepGradient(
            centerX, centerY,
            intArrayOf(arc.gradientStartColor, arc.gradientEndColor, arc.gradientStartColor, arc.gradientEndColor),
            floatArrayOf(0f, sweepAngle / 720f, sweepAngle / 360f, 1f)
        )

        outerArcPaint.shader = sweepGradient

        // Save the canvas state, rotate it, draw the arc, and restore the canvas
        canvas.save()
        canvas.rotate(startAngle, centerX, centerY)
        canvas.drawArc(rectF, 0f, sweepAngle, false, outerArcPaint)
        canvas.restore()
    }

    private fun drawArcCircle(canvas: Canvas, angle: Float, color: Int) {
        val circleRadius = outerCircleRadius
        val circleX = centerX + outerRingRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val circleY = centerY + outerRingRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val circlePaint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(circleX, circleY, circleRadius, circlePaint)
    }

    fun setOuterArcData(data: List<ArcData>) {
        arcData.clear()
        arcData.addAll(data)
        invalidate()
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
                    var angle = atan2(event.y - centerY, event.x - centerX)
                    angle = adjustAngleToNearestFiveMinutes(angle)
                    if (dragCircleIndex == 1) {
                        updateInnerCirclePosition(angle, 1)
                        touchingStartCircle = true
                    } else if (dragCircleIndex == 2) {
                        updateInnerCirclePosition(angle, 2)
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

    private fun adjustAngleToNearestFiveMinutes(angle: Float): Float {
        val angleInDegrees = (angle * (180 / Math.PI)).toFloat() // 转换为角度
        val totalMinutesInADay = 24 * 60
        val minutesPerDegree = totalMinutesInADay / 360f // 每度代表的分钟数

        // 计算当前角度对应的时间（分钟数）
        val minutes = angleInDegrees * minutesPerDegree

        // 将分钟数调整为最接近的整5分钟
        val nearestFiveMinute = Math.round(minutes / 5) * 5

        // 将调整后的分钟数转换回角度
        val adjustedAngle = (nearestFiveMinute.toFloat() / totalMinutesInADay) * 360

        return (adjustedAngle * (Math.PI / 180)).toFloat() // 转换回弧度
    }

    private fun isTouchInsideCircle(touchX: Float, touchY: Float): Int {
        val distanceStart = hypot(touchX - innerCircleXStart, touchY - innerCircleYStart)
        val distanceEnd = hypot(touchX - innerCircleXEnd, touchY - innerCircleYEnd)
        return when {
            distanceStart <= innerCircleRadius -> 1
            distanceEnd <= innerCircleRadius-> 2
            else -> 0
        }
    }

    private fun isTouchInsideArc(touchX: Float, touchY: Float): Boolean {
        val startAngle = atan2(innerCircleYStart - centerY, innerCircleXStart - centerX)
        val endAngle = atan2(innerCircleYEnd - centerY, innerCircleXEnd - centerX)
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

        return isInAngleRange && distanceToCenter in (innerRingRadius - arcStrokeWidth / 2)..(innerRingRadius + arcStrokeWidth / 2)
    }

    private fun isTouchInsideInnerRing(touchX: Float, touchY: Float): Boolean {
        val distanceToCenter = hypot(touchX - centerX, touchY - centerY)
        return distanceToCenter in innerRingRadius - touchSafeMargin ..innerRingRadius + touchSafeMargin
    }

    private fun updateInnerCirclePosition(angle: Float, circleIndex: Int) {
        if (circleIndex == 1) {
            innerCircleXStart = centerX + innerRingRadius * cos(angle)
            innerCircleYStart = centerY + innerRingRadius * sin(angle)
            if(touchingStartCircle){
                val (hours, minutes) = convertAngleToTime(angle)
                startTimeCalendar.set(Calendar.HOUR_OF_DAY, hours)
                startTimeCalendar.set(Calendar.MINUTE, minutes)
            }
        } else if (circleIndex == 2) {
            innerCircleXEnd = centerX + innerRingRadius * cos(angle)
            innerCircleYEnd = centerY + innerRingRadius * sin(angle)
            if(touchingEndCircle){
                val (hours, minutes) = convertAngleToTime(angle)
                endTimeCalendar.set(Calendar.HOUR_OF_DAY, hours)
                endTimeCalendar.set(Calendar.MINUTE, minutes)
            }
        }
    }

    private fun updateOuterCirclePosition(angle: Float, circleIndex: Int) {
        if (circleIndex == 1) {
            outerCircleXStart = centerX + outerRingRadius * cos(angle)
            outerCircleYStart = centerY + outerRingRadius * sin(angle)
        } else if (circleIndex == 2) {
            outerCircleXEnd = centerX + outerRingRadius * cos(angle)
            outerCircleYEnd = centerY + outerRingRadius * sin(angle)
        }
    }

    private fun updateArcPositions(currentAngle: Float) {
        val angleDifference = currentAngle - arcDragStartAngle
        val startAngle = atan2(innerCircleYStart - centerY, innerCircleXStart - centerX)
        val endAngle = atan2(innerCircleYEnd - centerY, innerCircleXEnd - centerX)
        val newStartAngle = startAngle + angleDifference
        val newEndAngle = endAngle + angleDifference

        innerCircleXStart = (centerX + innerRingRadius * cos(newStartAngle))
        innerCircleYStart = (centerY + innerRingRadius * sin(newStartAngle))

        val (hoursStart, minutesStart) = convertAngleToTime(newStartAngle)
        startTimeCalendar.set(Calendar.HOUR_OF_DAY, hoursStart)
        startTimeCalendar.set(Calendar.MINUTE, minutesStart)

        innerCircleXEnd = (centerX + innerRingRadius * cos(newEndAngle))
        innerCircleYEnd = (centerY + innerRingRadius * sin(newEndAngle))

        val (hoursEnd, minutesEnd) = convertAngleToTime(newEndAngle)
        endTimeCalendar.set(Calendar.HOUR_OF_DAY, hoursEnd)
        endTimeCalendar.set(Calendar.MINUTE, minutesEnd)

        arcDragStartAngle = currentAngle
    }

    private fun convertAngleToTime(angle: Float): Pair<Int, Int> {
        val totalMinutes = (angle / (2 * Math.PI) * 24 * 60).toInt()
        val roundedMinutes = Math.round(totalMinutes / 5.0) * 5
        val hours = (6 + roundedMinutes / 60) % 24
        val minutes = roundedMinutes % 60

        return Pair(hours.toInt(), minutes.toInt())
    }


    private fun convertTimeToAngle(time: String): Double {
        val parts = time.split(":")
        val hours = parts[0].toInt()
        var minutes = parts[1].toInt()
        minutes = (Math.round(minutes / 5.0) * 5).toInt()
        val totalMinutes = (hours - 6) * 60 + minutes

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