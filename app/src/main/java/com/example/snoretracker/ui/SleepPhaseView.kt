package com.example.snoretracker.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.snoretracker.R

class SleepPhaseView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val color_awake = resources.getColor(R.color.phase_red)
    private val color_rem = resources.getColor(R.color.phase_cyan)
    private val color_core = resources.getColor(R.color.phase_blue)
    private val color_deep = resources.getColor(R.color.phase_purple)

    // 假设 scaleFactor 是一个已定义的比例因子
    private val scaleFactor = 0.45f
    private val marginStart = 100f * scaleFactor
    private val marginLeft = 200f * scaleFactor
    private val marginTop = 30f * scaleFactor
    private val marginVertical = 60f * scaleFactor
    private val marginTimeText = 10f * scaleFactor
    private val padding = 10f * scaleFactor
    private val rectHeight = 100f * scaleFactor
    private val cornerRadius = 20f * scaleFactor
    val timePoints = listOf(0f * scaleFactor, 400f * scaleFactor, 800f * scaleFactor, 1200f * scaleFactor, 1600f * scaleFactor)
    val timeTextList = listOf("22:00", "01:00", "04:00", "07:00", "10:00")
    val stageTextList = listOf("Awake", "REM", "Core", "Deep")

    // 定义矩形的水平起止信息（只存储 left 和 right 坐标）
    private val stages = mapOf(
        "awakeStages" to listOf(
            Pair(0f * scaleFactor, 50f * scaleFactor),
            Pair(1350f * scaleFactor, 1400f * scaleFactor)
        ),
        "remStages" to listOf(
            Pair(50f * scaleFactor, 100f * scaleFactor),
            Pair(700f * scaleFactor, 800f * scaleFactor),
            Pair(1300f * scaleFactor, 1350f * scaleFactor),
            Pair(1400f * scaleFactor, 1500f * scaleFactor),
            Pair(1550f * scaleFactor, 1600f * scaleFactor)
        ),
        "coreStages" to listOf(
            Pair(100f * scaleFactor, 300f * scaleFactor),
            Pair(400f * scaleFactor, 700f * scaleFactor),
            Pair(800f * scaleFactor, 1100f * scaleFactor),
            Pair(1200f * scaleFactor, 1300f * scaleFactor),
            Pair(1500f * scaleFactor, 1550f * scaleFactor)
        ),
        "deepStages" to listOf(
            Pair(300f * scaleFactor, 400f * scaleFactor),
            Pair(1100f * scaleFactor, 1200f * scaleFactor)
        )
    )

    private val awakePaint = Paint().apply {
        color = color_awake
        style = Paint.Style.FILL
    }

    private val remPaint = Paint().apply {
        color = color_rem
        style = Paint.Style.FILL
    }

    private val corePaint = Paint().apply {
        color = color_core
        style = Paint.Style.FILL
    }

    private val deepPaint = Paint().apply {
        color = color_deep
        style = Paint.Style.FILL
    }

    private val timePaint = Paint().apply {
        color = resources.getColor(R.color.light_grey)
        textSize = 50f * scaleFactor
        textAlign = Paint.Align.CENTER
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f * scaleFactor
        textAlign = Paint.Align.LEFT
    }

    private val solidLinePaint = Paint().apply {
        color = resources.getColor(R.color.light_grey)
        strokeWidth = 2f * scaleFactor
        style = Paint.Style.STROKE
    }

    private val gradientPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 150
    }

    private fun drawSmoothConnections(canvas: Canvas, stages: List<RectF>) {
        val path = Path()

        gradientPaint.xfermode = PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC)

        for (i in 0 until stages.size - 1) {
            val current = stages[i]
            val next = stages[i + 1]

            // 创建渐变效果
            val gradient = LinearGradient(
                0f, current.centerY(),
                0f, next.centerY(),
                intArrayOf(
                    getColorFromStage(current),
                    getColorFromStage(current),
                    getColorFromStage(next),
                    getColorFromStage(next)
                ),
                floatArrayOf(
                    0f,
                    0.2f,
                    0.8f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )
            gradientPaint.shader = gradient

            // 为当前的阶段矩形绘制渐变背景
            val backgroundRect = RectF(
                current.left - padding,
                current.top - padding,
                current.right + padding,
                current.bottom + padding
            )
            canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, gradientPaint)

            // 连接线的绘制
            path.moveTo(current.centerX(), current.centerY())
            path.lineTo(next.centerX(), next.centerY())

            // 绘制连接线的包裹矩形
            val connectionRect = RectF(current.right - padding, current.centerY(), next.left + padding, next.centerY())
            path.addRect(connectionRect, Path.Direction.CW)

            path.close()

            canvas.drawPath(path, gradientPaint)
            path.reset()
        }

        // 为最后一个阶段矩形绘制渐变背景
        val lastRect = stages.last()
        val lastBackgroundRect = RectF(
            lastRect.left - padding,
            lastRect.top - padding,
            lastRect.right + padding,
            lastRect.bottom + padding
        )

        canvas.drawRoundRect(lastBackgroundRect, cornerRadius, cornerRadius, gradientPaint)
    }

    // 根据矩形的颜色决定渐变的起始和结束颜色
    private fun getColorFromStage(rect: RectF): Int {
        // 这里可以根据矩形的类型来返回不同的颜色
        return when {
            rect.centerY() < rectHeight -> color_awake
            rect.centerY() < 2 * rectHeight + marginVertical -> color_rem
            rect.centerY() < 3 * rectHeight + 2 * marginVertical -> color_core
            else -> color_deep
        }
    }

    // 计算矩形的顶边和底边，依次从上到下
    private fun createRectangles(stagesMap: Map<String, List<Pair<Float, Float>>>): List<RectF> {
        val allStages = mutableListOf<RectF>()
        var currentTop = marginTop

        stagesMap.forEach { (stageName, pairs) ->
            val currentBottom = currentTop + rectHeight
            pairs.forEach { (left, right) ->
                val rect = RectF(left + marginStart + marginLeft, currentTop, right + marginStart + marginLeft, currentBottom)
                allStages.add(rect)
            }
            currentTop = currentBottom + marginVertical // 更新下一个阶段的顶边坐标
        }

        return allStages.sortedBy { it.left } // 按照 left 排序
    }

    // 绘制矩形
    private fun drawStages(canvas: Canvas) {
        val allStages = createRectangles(stages)

        // 绘制背景坐标轴
        drawBackgroundAxes(canvas)

        drawSmoothConnections(canvas, allStages)

        // 绘制阶段矩形
        for (rect in allStages) {
            when {
                rect.centerY() < rectHeight -> canvas.drawRoundRect(rect, cornerRadius, cornerRadius, awakePaint)
                rect.centerY() < 2 * rectHeight + marginVertical -> canvas.drawRoundRect(rect, cornerRadius, cornerRadius, remPaint)
                rect.centerY() < 3 * rectHeight + 2 * marginVertical -> canvas.drawRoundRect(rect, cornerRadius, cornerRadius, corePaint)
                else -> canvas.drawRoundRect(rect, cornerRadius, cornerRadius, deepPaint)
            }
        }

        // 绘制时间刻度
        for (i in timeTextList.indices) {
            canvas.drawText(
                timeTextList[i],
                timePoints[i] + marginStart + marginLeft,
                4 * (rectHeight + marginVertical) + marginTop + marginTimeText,
                timePaint
            )
        }

        // 绘制状态文字
        for (i in stageTextList.indices) {
            canvas.drawText(
                stageTextList[i],
                marginStart,
                i * marginVertical + (2 * i + 1) * rectHeight / 2 + marginTop,
                textPaint
            )
        }
    }

    // 绘制背景坐标轴
    private fun drawBackgroundAxes(canvas: Canvas) {
        // 横向实线分割
        val numberOfLines = 4 // 实线的数量
        for (i in 0 until numberOfLines) {
            val yPosition = i * (rectHeight + marginVertical)
            canvas.drawLine(
                marginStart,
                yPosition,
                timePoints.last() + marginStart + marginLeft,
                yPosition,
                solidLinePaint
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawStages(canvas)
    }
}
