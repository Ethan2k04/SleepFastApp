package com.example.snoretracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.snoretracker.R
import kotlin.math.cos
import kotlin.math.sin

class GaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.dark_grey)
        style = Paint.Style.FILL
        strokeWidth = 6f
    }

    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val tickCount = 40 // 增加刻度的数量
    private val tickLength = 30f
    private val pointerWidth = 12f // 圆角矩形的宽度
    private val pointerHeight = 50f // 圆角矩形的高度
    private val pointerRadius = 12f // 圆角半径

    private val startAngle = 135f
    private val sweepAngle = 270f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = 500f
        val height = 500f
        val marginTop = -80f
        val marginLeft = -80f
        val radius = width / 2 - 100

        // Draw the ticks
        for (i in 0 until tickCount) {
            val angle = startAngle + i * (sweepAngle / (tickCount - 1))
            val tickStartX = marginLeft + width / 2 + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val tickStartY = marginTop + height / 2 + radius * sin(Math.toRadians(angle.toDouble())).toFloat()

            val tickEndX = marginLeft + width / 2 + (radius - tickLength) * cos(Math.toRadians(angle.toDouble())).toFloat()
            val tickEndY = marginTop + height / 2 + (radius - tickLength) * sin(Math.toRadians(angle.toDouble())).toFloat()

            canvas.drawLine(tickStartX, tickStartY, tickEndX, tickEndY, tickPaint)
        }

        // Draw the pointer as a rounded rectangle
        val pointerIndex = 20 // 指针指向某个刻度，这里设置为第20个刻度
        val pointerAngle = startAngle + pointerIndex * (sweepAngle / (tickCount - 1))
        val pointerX = marginLeft + width / 2 + (radius - pointerHeight / 2 + 10f) * cos(Math.toRadians(pointerAngle.toDouble())).toFloat()
        val pointerY = marginTop + height / 2 + (radius - pointerHeight / 2 + 10f) * sin(Math.toRadians(pointerAngle.toDouble())).toFloat()

        val rectF = RectF(
            pointerX - pointerWidth / 2,
            pointerY - pointerHeight / 2,
            pointerX + pointerWidth / 2,
            pointerY + pointerHeight / 2
        )
        canvas.drawRoundRect(rectF, pointerRadius, pointerRadius, pointerPaint)

        // Draw the text
        canvas.drawText("Low", marginLeft + width / 2 - 100f, height / 2 + 80f, textPaint)
        canvas.drawText("High", marginLeft + width / 2 + 40f, height / 2 + 80f, textPaint)
    }
}
