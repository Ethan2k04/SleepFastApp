package com.example.sleepfast.fragments

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.RECEIVER_NOT_EXPORTED
import androidx.core.app.ActivityCompat
import androidx.core.app.AlarmManagerCompat.canScheduleExactAlarms
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sleepfast.viewmodels.SleepViewModel
import com.example.sleepfast.R
import com.example.sleepfast.SleepFastApp
import com.example.sleepfast.animation.ScaleAnimationManager
import com.example.sleepfast.receivers.AlarmReceiver
import com.example.sleepfast.services.AlarmService
import com.example.sleepfast.services.ForegroundService
import com.example.sleepfast.ui.CustomFrameLayout
import com.example.sleepfast.ui.RingView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SleepFragment : Fragment(), RingView.TimeUpdateListener, RingView.ArcTouchListener {
    // Application的全局访问点
    val sleepFastApp = SleepFastApp.get()

    // ViewModel实例
    private lateinit var sleepViewModel: SleepViewModel

    // 视图组件
    private lateinit var scrollView: ScrollView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var startDayText: TextView
    private lateinit var weekDaysContainer: LinearLayout
    private lateinit var ringView: RingView
    private lateinit var sleepTimeText: TextView
    private lateinit var wakeTimeText: TextView
    private lateinit var alarmPanelView: LinearLayout
    private lateinit var wakeTimeLayout: LinearLayout
    private lateinit var sleepTimeLayout: LinearLayout

    // 按钮组件
    private lateinit var btnBottomMenu: ImageButton
    private lateinit var btnStart: FrameLayout
    private lateinit var btnStartFrame: CustomFrameLayout
    private lateinit var btnStartLayout: RelativeLayout
    private lateinit var scaleAnimationManagerStart: ScaleAnimationManager
    private lateinit var btnStop: FrameLayout
    private lateinit var btnStopFrame: CustomFrameLayout
    private lateinit var btnStopLayout: RelativeLayout
    private lateinit var scaleAnimationManagerStop: ScaleAnimationManager

    // 闹钟设置组件
    private lateinit var alarmSwitch: Switch
    private lateinit var snoozeSwitch: Switch
    private lateinit var seekBarVolume: SeekBar
    private lateinit var pendingIntent: PendingIntent

    // 控制变量
    private var isAlarmSet = false
    private var isAlarmPlaying = false
    private var isStartPressed = false

    // 音频录制权限相关
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sleep, container, false)

        sleepViewModel = ViewModelProvider(requireActivity())[SleepViewModel::class.java]

        initializeViews(view)

        return view
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTitle()

        setupWeekDays()

        setUpListeners()

        setupSeekBar()

        setUpLiveData()

        initServices()
    }

    private fun initializeViews(view: View) {
        btnStart = view.findViewById(R.id.btnStart)
        btnStartFrame = view.findViewById(R.id.btnStartFrame)
        btnStartLayout = view.findViewById(R.id.btnStartLayout)
        scaleAnimationManagerStart = ScaleAnimationManager(btnStartLayout, btnStartFrame)
        btnStop = view.findViewById(R.id.btnStop)
        btnStopFrame = view.findViewById(R.id.btnStopFrame)
        btnStopLayout = view.findViewById(R.id.btnStopLayout)
        scaleAnimationManagerStop = ScaleAnimationManager(btnStopLayout, btnStopFrame)
        btnBottomMenu = view.findViewById(R.id.btnMonthMenu)
        sleepTimeText = view.findViewById(R.id.sleepTimeText)
        wakeTimeText = view.findViewById(R.id.wakeTimeText)
        startDayText = view.findViewById(R.id.startDayText)
        scrollView = view.findViewById(R.id.scrollView)
        ringView = view.findViewById(R.id.ringView)
        weekDaysContainer = view.findViewById(R.id.weekDaysContainer)
        alarmSwitch = view.findViewById(R.id.alarmSwitch)
        alarmPanelView = view.findViewById(R.id.alarmPanelView)
        sleepTimeLayout = view.findViewById(R.id.bedTimeLayout)
        wakeTimeLayout = view.findViewById(R.id.wakeUpLayout)
        seekBarVolume = view.findViewById(R.id.volumeSeekBar)
        snoozeSwitch = view.findViewById(R.id.snoozeSwitch)
        scrollView = view.findViewById(R.id.scrollView)
        seekBarVolume = view.findViewById(R.id.volumeSeekBar)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        // 设置刷新环的颜色
        swipeRefreshLayout.setColorSchemeColors(
            resources.getColor(R.color.light_orange, null)
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            resources.getColor(R.color.fg_grey, null)
        )
    }

    override fun onRingTimeUpdate(startTime: String, endTime: String) {
        val (startHour, startMinute) = startTime.split(":").map { it.toInt() }
        val (endHour, endMinute) = endTime.split(":").map { it.toInt() }

        // 更新 viewmodel 中的 calendar 数据
        sleepViewModel.updateTimeUI(startHour, startMinute, endHour, endMinute)
    }

    override fun onRingTouched(isInsideArc: Boolean) {
        scrollView.requestDisallowInterceptTouchEvent(isInsideArc)
    }

    private fun setupTitle() {
        val dayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)

        val currentDay = dayFormat.format(sleepViewModel.calendar.time)
        val currentDate = dateFormat.format(sleepViewModel.calendar.time)

        view?.findViewById<TextView>(R.id.titleDay)?.text = currentDay
        view?.findViewById<TextView>(R.id.titleDate)?.text = currentDate
    }

    private fun setupWeekDays() {
        val weekDaysCalendar = Calendar.getInstance()
        val currentDayOfWeek = weekDaysCalendar.get(Calendar.DAY_OF_WEEK)
        val dateFormat = SimpleDateFormat("E", Locale.ENGLISH)

        // 将日历移到周日为一周的第一天
        weekDaysCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        for (i in 1..7) {
            val dayView = layoutInflater.inflate(R.layout.item_week_day, weekDaysContainer, false)
            val imageView = dayView.findViewById<ImageView>(R.id.week_day_image)
            val textView = dayView.findViewById<TextView>(R.id.week_day_text)

            // 设置文本为首字母
            textView.text = dateFormat.format(weekDaysCalendar.time)[0].toString()

            // 处理选中日期的样式
            if (i == currentDayOfWeek) {
                textView.setTextColor(resources.getColor(R.color.highlight_grey))
                imageView.setImageDrawable(resources.getDrawable(R.drawable.bg_day_circle_highlight))
            } else {
                textView.setTextColor(resources.getColor(R.color.normal_grey))
                imageView.setImageDrawable(resources.getDrawable(R.drawable.bg_day_circle_normal))
            }
            weekDaysContainer.addView(dayView)

            // 移到下一天
            weekDaysCalendar.add(Calendar.DAY_OF_WEEK, 1)
        }
    }

    private fun setupSeekBar() {

        fun updateAlarmVolume(volume: Float) {
            val intent = Intent(context, AlarmService::class.java)
            intent.putExtra("ACTION", "SET_VOLUME")
            intent.putExtra("VOLUME", volume)
            context?.startService(intent)
        }

        fun playAlarmSeekBar() {
            val intent = Intent(context, AlarmService::class.java)
            intent.putExtra("ACTION", "START")
            context?.startService(intent)
        }

        fun stopAlarmSeekBar() {
            val intent = Intent(context, AlarmService::class.java)
            intent.putExtra("ACTION", "STOP")
            context?.startService(intent)
        }

        fun onSeekBarProgressChanged(progress: Int) {
            val alarmVolume = progress / 100f
            updateAlarmVolume(alarmVolume)

            val sharedPreferences = context?.getSharedPreferences("AlarmConfig", Context.MODE_PRIVATE)
            val editor = sharedPreferences?.edit()
            editor?.putFloat("alarmVolume", alarmVolume)
            editor?.putBoolean("snoozeSwitchChecked", snoozeSwitch.isChecked)
            editor?.apply()
        }

        fun onSeekBarStartTracking() {
            if (!isAlarmPlaying) {
                playAlarmSeekBar()
            }
        }

        fun onSeekBarStopTracking() {
            if (!isAlarmPlaying) {
                stopAlarmSeekBar()
            }
        }

        seekBarVolume.max = 100
        seekBarVolume.progress = 50
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onSeekBarProgressChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                onSeekBarStartTracking()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onSeekBarStopTracking()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setUpListeners(){
        // 设置RingView的监听器
        ringView.timeUpdateListener = this
        ringView.arcTouchListener = this

        btnStartFrame.setOnClickListener {
            onBtnStartClickHandler(it)
        }

        btnStopFrame.setOnClickListener {
            onBtnStopClickHandler(it)
        }

        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchAlarmChanged(isChecked)
        }

        swipeRefreshLayout.setOnRefreshListener {
            handleRefresh()
        }
    }

    private fun setUpLiveData(){
        // 睡觉和起床时间文字
        sleepViewModel.startTimeCalendar.observe(viewLifecycleOwner) { startTime ->
            sleepTimeText.text = formatTime(startTime)
        }

        sleepViewModel.endTimeCalendar.observe(viewLifecycleOwner) { endTime ->
            wakeTimeText.text = formatTime(endTime)
        }

        sleepViewModel.isStartPressed.observe(viewLifecycleOwner) { isPressed ->
            if (isPressed) {
                btnStart.visibility = View.GONE
                btnStop.visibility = View.VISIBLE
                ringView.setDraggable(false)
                ringView.setColors(R.color.dark_orange)
                ringView.setIcons(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_bed_dark_16dp, null),
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_alarm_dark_16dp, null)
                )
            } else {
                btnStart.visibility = View.VISIBLE
                btnStop.visibility = View.GONE
                ringView.setDraggable(true)
                ringView.setColors(R.color.light_orange)
                ringView.setIcons(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_bed_light_16dp, null),
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_alarm_light_16dp, null)
                )
            }
        }

        sleepViewModel.isSwitchChecked.observe(viewLifecycleOwner){ isChecked ->
            if (isChecked) {
                alarmSwitch.isChecked = true
                alarmPanelView.visibility = View.VISIBLE
//                scrollView.post {
//                    scrollView.smoothScrollTo(0, alarmPanelView.bottom)
//                }
//
//                if (sleepViewModel.isStartPressed.value == true && !isAlarmPlaying && !isAlarmSet) {
//                    startAlarm()
//                }
            } else {
                alarmSwitch.isChecked = false
                alarmPanelView.visibility = View.GONE
//                if (sleepViewModel.isStartPressed.value == true && !isAlarmPlaying && isAlarmSet) {
//                    stopAlarm()
//                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initServices(){
        // 闹钟前台进程
        val intent = Intent(context, ForegroundService::class.java)
        val filter = IntentFilter("ALARM_STARTED").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        // 定义闹钟接收者
        val alarmStartedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Handle the broadcast
                if (intent.action == "ALARM_STARTED") {
                    // Update UI or notify the user
                    isAlarmPlaying = true
                }
            }
        }
        context?.let { ContextCompat.startForegroundService(it, intent) }
        createNotificationChannel()
        context?.registerReceiver(alarmStartedReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    private fun handleRefresh() {
        // 模拟网络请求或数据加载
        lifecycleScope.launch {
            // 延迟 3 秒
            delay(3000)
            // 停止刷新动画
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun formatTime(calendar: Calendar): String {
        return SimpleDateFormat("HH:mm", Locale.ENGLISH).format(calendar.time)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startAlarm() {
        var time: Long
        try {
            val intent = Intent(context, AlarmReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            intent.putExtra("ACTION", "START")
            val startTime = sleepViewModel.startTimeCalendar.value?.timeInMillis ?: 0
            val endTime = sleepViewModel.endTimeCalendar.value?.timeInMillis ?: 0
            time = (endTime - (startTime % 60000))

            if (System.currentTimeMillis() > time) {
                Toast.makeText(context, "Invalid time: End time is before current time.", Toast.LENGTH_SHORT).show()
                time += (1000 * 60 * 60 * 24)
            }

            // 设置闹钟
            sleepViewModel.alarmManager.value?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)

            val hour = sleepViewModel.endTimeCalendar.value?.get(Calendar.HOUR_OF_DAY) ?: 0
            val minute = sleepViewModel.endTimeCalendar.value?.get(Calendar.MINUTE) ?: 0
            val timeString = String.format("%02d:%02d", hour, minute)
            Toast.makeText(context, "Alarm set for: $timeString", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Unable to set alarm: exact alarm permission required.", Toast.LENGTH_SHORT).show()
            requestExactAlarmPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopAlarm() {
        try {
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("ACTION", "STOP")
            context?.sendBroadcast(intent)
            sleepViewModel.alarmManager.value?.cancel(pendingIntent)
            Toast.makeText(context, "Alarm canceled and stopped.", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Unable to cancel alarm: exact alarm permission required.", Toast.LENGTH_SHORT).show()
            requestExactAlarmPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onBtnStartClickHandler(view: View){
        sleepFastApp.vibratePhoneClick()
        isStartPressed = true
        sleepViewModel.setStartPressed(isStartPressed)

        lifecycleScope.launch {
            sleepViewModel.upsertSleepData()
        }

        if(sleepViewModel.isSwitchChecked.value == true && !isAlarmSet){
            isAlarmSet = true
            startAlarm()
        }

        if (checkRecordingPermissions()) {
            sleepFastApp.sleepAudioClassifier.startAudioClassification()
        } else {
            requestRecordingPermissions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onBtnStopClickHandler(view: View){
        sleepFastApp.vibratePhoneClick()
        isStartPressed = false
        sleepViewModel.setStartPressed(isStartPressed)

        if(sleepViewModel.isSwitchChecked.value == true && isAlarmSet){
            isAlarmSet = false
            stopAlarm()
        }

        if (checkRecordingPermissions()) {
            sleepFastApp.sleepAudioClassifier.stopAudioClassification()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onSwitchAlarmChanged(isChecked: Boolean) {
        if (isChecked) {
            alarmPanelView.visibility = View.VISIBLE
            sleepViewModel.setSwitchChecked(isChecked)

            if (sleepViewModel.isStartPressed.value == true && !isAlarmPlaying && !isAlarmSet) {
                startAlarm()
            }
        } else {
            alarmPanelView.visibility = View.GONE

            sleepViewModel.setSwitchChecked(isChecked)

            if (sleepViewModel.isStartPressed.value == true && !isAlarmPlaying && isAlarmSet) {
                stopAlarm()
            }
        }
    }

    private fun checkRecordingPermissions(): Boolean {
        val recordAudioPermission = context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO) }
        return recordAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordingPermissions() {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestExactAlarmPermission() {
        if (!sleepViewModel.alarmManager.value?.let { canScheduleExactAlarms(it) }!!) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Sleep Master Channel",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel Description"
            }
            val manager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
