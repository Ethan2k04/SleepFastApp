package com.example.snoretracker.animation

import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.RelativeLayout
import com.example.snoretracker.ui.CustomFrameLayout

class ScaleAnimationManager(private val entity: RelativeLayout, private val frame: CustomFrameLayout) {

    private val scaleFactor = 0.95f
    private val animationDuration = 200L
    private var isScalingDown = false

    init {
        setupTouchListener()
    }

    private fun createScaleAnimation(fromScale: Float, toScale: Float, duration: Long): ScaleAnimation {
        val pivotX = entity.width / 2f
        val pivotY = entity.height / 2f
        return ScaleAnimation(
            fromScale, toScale,
            fromScale, toScale,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            this.duration = duration
            fillAfter = true
        }
    }

    private fun startScaleDownAnimation() {
        if (!isScalingDown) {
            isScalingDown = true
            val scaleDownAnimation = createScaleAnimation(1.0f, scaleFactor, animationDuration)
            entity.startAnimation(scaleDownAnimation)
        }
    }

    private fun startScaleUpAnimation() {
        if (isScalingDown) {
            isScalingDown = false
            val scaleUpAnimation = createScaleAnimation(scaleFactor, 1.0f, animationDuration)
            entity.startAnimation(scaleUpAnimation)
        }
    }

    private fun setupTouchListener() {
        frame.post {
            var initialTouchX = 0f
            var initialTouchY = 0f
            val touchSlop = ViewConfiguration.get(frame.context).scaledTouchSlop

            frame.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.x
                        initialTouchY = event.y
                        startScaleDownAnimation()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = Math.abs(event.x - initialTouchX)
                        val deltaY = Math.abs(event.y - initialTouchY)
                        if (deltaX > touchSlop || deltaY > touchSlop) {
                            // 手指移动时，不做任何处理，只是保持缩小状态
                            return@setOnTouchListener true
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (Math.abs(event.x - initialTouchX) <= touchSlop &&
                            Math.abs(event.y - initialTouchY) <= touchSlop) {
                            // 手指没有移动，保持缩小状态
                            frame.performClick()
                        }
                        startScaleUpAnimation()
                        true
                    }
                    else -> false
                }
            }
        }
    }
}
