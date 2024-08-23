package com.example.snoretracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.snoretracker.AppDatabase
import com.example.snoretracker.AudioCategoryDao
import com.example.snoretracker.MainActivity
import com.example.snoretracker.R
import com.example.snoretracker.SleepTimeDao
import com.example.snoretracker.animation.ScaleAnimationManager
import com.example.snoretracker.ui.CustomFrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class UserFragment : BottomSheetDialogFragment() {

    private lateinit var btnClear: FrameLayout
    private lateinit var btnClearFrame: CustomFrameLayout
    private lateinit var btnClearLayout: RelativeLayout
    private lateinit var scaleAnimationManagerClear: ScaleAnimationManager
    private lateinit var audioCategoryDao: AudioCategoryDao
    private lateinit var sleepTimeDao: SleepTimeDao

    private var topMargin = 100f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化组件
        btnClear = view.findViewById(R.id.btnClear)
        btnClearFrame = view.findViewById(R.id.btnClearFrame)
        btnClearLayout = view.findViewById(R.id.btnClearLayout)
        scaleAnimationManagerClear = ScaleAnimationManager(btnClearLayout, btnClearFrame)

        // 设置 btnClear 点击事件
        btnClearFrame.setOnClickListener {
            showClearDataConfirmationDialog()
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

    // 设置 SleepTimeDao 的方法
    fun setDao(stDao: SleepTimeDao, acDao: AudioCategoryDao) {
        sleepTimeDao = stDao
        audioCategoryDao = acDao
    }

    private fun showClearDataConfirmationDialog() {
        // 创建并显示确认删除数据的对话框
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to clear all data?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            // 执行删除数据的操作
            clearData()

            // 关闭 BottomSheet
            dismiss()

            // 收起 BottomSheet
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            // 取消操作，关闭对话框，保持原样
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun clearData() {
        // 执行清除数据的逻辑
        if (!::sleepTimeDao.isInitialized || !::audioCategoryDao.isInitialized) return

        lifecycleScope.launch {
            // 清空表中的所有数据
            sleepTimeDao.clearAllSleepTime()
            audioCategoryDao.clearAllCategories()
        }
    }
}
