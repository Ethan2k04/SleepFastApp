package com.example.sleepfast.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class CustomFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    override fun performClick(): Boolean {
        // 可以在这里添加自定义的点击行为
        super.performClick()
        return true
    }
}
