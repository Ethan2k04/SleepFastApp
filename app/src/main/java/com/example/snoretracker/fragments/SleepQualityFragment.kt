package com.example.snoretracker.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.snoretracker.MainActivity
import com.example.snoretracker.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SleepQualityFragment : BottomSheetDialogFragment() {

    private var topMargin = 100f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_sleep_quality, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cancelButton: ImageButton = requireView().findViewById(R.id.sleepQualityCancelButton)

        cancelButton.setOnClickListener{
            val mainActivity = activity as? MainActivity
            mainActivity?.vibratePhoneClick()
            dismiss()
        }

        // 获取 BottomSheet 的 View
        val bottomSheet = view.parent as? View
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            // 设置为展开状态，并使其高度为 MATCH_PARENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            it.layoutParams.height = (ViewGroup.LayoutParams.MATCH_PARENT - topMargin).toInt()
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        dismiss()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // 可选的滑动逻辑
                }
            })
        }
    }
}
