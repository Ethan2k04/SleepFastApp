package com.example.sleepfast

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.sleepfast.animation.ScaleAnimationManager
import com.example.sleepfast.fragments.secondary.SleepPhaseBottomFragment
import com.example.sleepfast.fragments.OldUserFragment
import com.example.sleepfast.fragments.secondary.SleepQualityBottomFragment
import com.example.sleepfast.fragments.secondary.SleepTrendBottomFragment
import com.example.sleepfast.fragments.secondary.SoundBottomFragment
import com.example.sleepfast.services.AlarmService
import com.example.sleepfast.services.ForegroundService
import org.tensorflow.lite.support.label.Category
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.room.Room
import com.example.sleepfast.receivers.AlarmReceiver
import com.example.sleepfast.ui.CustomFrameLayout
import com.example.sleepfast.ui.RingView
import com.example.sleepfast.ui.SleepTrendView
import com.example.sleepfast.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface AudioClassificationListener {
    fun onError(error: String)
    fun onResult(results: List<Category>, inferenceTime: Long)
}

class OldMainActivity : AppCompatActivity(), RingView.TimeUpdateListener, RingView.ArcTouchListener {

    // ViewModel实例
    private lateinit var viewModel: MainViewModel

    // 控件变量
    private lateinit var ringView: RingView
    private lateinit var btnBottomMenu: ImageButton
    private lateinit var btnStartTimeLeft: ImageButton
    private lateinit var btnStartTimeRight: ImageButton
    private lateinit var btnEndTimeLeft: ImageButton
    private lateinit var btnEndTimeRight: ImageButton
    private lateinit var sleepTimeText: TextView
    private lateinit var wakeUpText: TextView
    private lateinit var startDayText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var weekDaysContainer: LinearLayout
    private lateinit var alarmPanelView: LinearLayout
    private lateinit var wakeUpLayout: LinearLayout
    private lateinit var bedTimeLayout: LinearLayout
    private lateinit var alarmSwitch: Switch
    private lateinit var snoozeSwitch: Switch
    private lateinit var seekBarVolume: SeekBar

    // 动画管理器
    private lateinit var btnStart: FrameLayout
    private lateinit var btnStartFrame: CustomFrameLayout
    private lateinit var btnStartLayout: RelativeLayout
    private lateinit var scaleAnimationManagerStart: ScaleAnimationManager
    private lateinit var btnStop: FrameLayout
    private lateinit var btnStopFrame: CustomFrameLayout
    private lateinit var btnStopLayout: RelativeLayout
    private lateinit var scaleAnimationManagerStop: ScaleAnimationManager
    private lateinit var trendDashBoard: RelativeLayout
    private lateinit var trendFrame: CustomFrameLayout
    private lateinit var scaleAnimationManagerTrend: ScaleAnimationManager
    private lateinit var phaseDashBoard: RelativeLayout
    private lateinit var phaseFrame: CustomFrameLayout
    private lateinit var scaleAnimationManagerPhase: ScaleAnimationManager
    private lateinit var qualityDashBoard: RelativeLayout
    private lateinit var qualityFrame: CustomFrameLayout
    private lateinit var scaleAnimationManagerQuality: ScaleAnimationManager
    private lateinit var soundDashBoard: RelativeLayout
    private lateinit var soundFrame: CustomFrameLayout
    private lateinit var scaleAnimationManagerSound: ScaleAnimationManager

    // 数据面板
    private lateinit var sleepTrendView: SleepTrendView
    private lateinit var snoreTimeNum: TextView
    private lateinit var speechTimeNum: TextView
    private lateinit var wakeupTimeNum: TextView

    // 数据库
    lateinit var db: AppDatabase
    private lateinit var audioCategoryDao: AudioCategoryDao
    private lateinit var sleepTimeDao: SleepTimeDao

    // 音频分类器
    private lateinit var sleepAudioClassifier: SleepAudioClassifier
    private val audioClassificationListener = object : AudioClassificationListener {
        override fun onResult(results: List<Category>, inferenceTime: Long) {
            runOnUiThread {
                // 在非主线程执行数据库操作
                lifecycleScope.launch {
                    results.forEach { category ->
                        val audioCategory = AudioCategory(
                            dateId = dateId,
                            label = category.label,
                            score = category.score,
                            timestamp = System.currentTimeMillis()
                        )
                        audioCategoryDao.insertCategory(audioCategory)
                    }
                    updateSoundViewsWithData()
                }
            }
        }

        override fun onError(error: String) {
            runOnUiThread {
                showToast(error)
            }
        }
    }

    // 音频录制权限相关
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    // 闹钟相关变量
    private lateinit var alarmPlayingReceiver: BroadcastReceiver
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var alarmUri: Uri
    private lateinit var vibrator: Vibrator

    // 闹钟状态变量
    private var isAlarmSet = false
    private var isAlarmPlaying = false
    private var isStartPressed = false
    private var alarmVolume = 0.5f

    // 时间显示相关变量
    private var startTimeCalendar: Calendar = Calendar.getInstance()
    private var endTimeCalendar: Calendar = Calendar.getInstance()

    // 日期和时间格式化
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val dateId = dateFormat.format(calendar.time).toInt()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.old_activity_main)

        // 设置底部导航栏的颜色
        window.navigationBarColor = resources.getColor(R.color.bg_grey, theme)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // 初始化数据
        initializeData()

        // 初始化控件
        initializeViews()

        // 更新日期标题
        updateTitle()

        // 设置顶部日期
        setupWeekDays()

        // 初始化服务
        initServices()

        // 更新UI显示
        updateTimeUI()

        // 更新RingView
        updateRingView()

        // 设置按钮事件
        setupButtonListeners()

        // 设置观察者
        setUpObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isAlarmSet){
            unregisterReceiver(alarmPlayingReceiver)
        }
    }

    private fun initializeViews() {
        btnStart = findViewById(R.id.btnStart)
        btnStartFrame = findViewById(R.id.btnStartFrame)
        btnStartLayout = findViewById(R.id.btnStartLayout)
        scaleAnimationManagerStart = ScaleAnimationManager(btnStartLayout, btnStartFrame)
        btnStop = findViewById(R.id.btnStop)
        btnStopFrame = findViewById(R.id.btnStopFrame)
        btnStopLayout = findViewById(R.id.btnStopLayout)
        scaleAnimationManagerStop = ScaleAnimationManager(btnStopLayout, btnStopFrame)
        btnBottomMenu = findViewById(R.id.btnMonthMenu)
        sleepTimeText = findViewById(R.id.sleepTimeText)
        wakeUpText = findViewById(R.id.wakeTimeText)
        startDayText = findViewById(R.id.startDayText)
        scrollView = findViewById(R.id.scrollView)
        ringView = findViewById(R.id.ringView)
        weekDaysContainer = findViewById(R.id.weekDaysContainer)
        alarmSwitch = findViewById(R.id.alarmSwitch)
        alarmPanelView = findViewById(R.id.alarmPanelView)
        bedTimeLayout = findViewById(R.id.bedTimeLayout)
        wakeUpLayout = findViewById((R.id.wakeUpLayout))
        seekBarVolume = findViewById(R.id.volumeSeekBar)
        snoozeSwitch = findViewById(R.id.snoozeSwitch)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // 数据面板
//        trendDashBoard = findViewById(R.id.trendDashBoard)
//        trendFrame = findViewById(R.id.trendFrame)
//        scaleAnimationManagerTrend = ScaleAnimationManager(trendDashBoard, trendFrame)
//        phaseDashBoard = findViewById(R.id.phaseDashBoard)
//        phaseFrame = findViewById(R.id.phaseFrame)
//        scaleAnimationManagerPhase = ScaleAnimationManager(phaseDashBoard, phaseFrame)
//        qualityDashBoard = findViewById(R.id.qualityDashBoard)
//        qualityFrame = findViewById(R.id.qualityFrame)
//        scaleAnimationManagerQuality = ScaleAnimationManager(qualityDashBoard, qualityFrame)
//        soundDashBoard = findViewById(R.id.soundDashBoard)
//        soundFrame = findViewById(R.id.soundFrame)
//        scaleAnimationManagerSound = ScaleAnimationManager(soundDashBoard, soundFrame)
//        sleepTrendView = findViewById(R.id.sleepTrendView)
//        sleepTrendView.setViewModelStoreOwner(this)
//        snoreTimeNum = findViewById(R.id.snoreTimeNum)
//        speechTimeNum = findViewById(R.id.speechTimeNum)
//        wakeupTimeNum = findViewById(R.id.wakeupTimeNum)
    }

    private fun initializeData() {

        // 数据库实例初始化
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "sleep-data-db").build()
        audioCategoryDao = db.audioCategoryDao()
        sleepTimeDao = db.sleepTimeDao()

        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }

        // 设置振动器
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // 初始化分类器模型
        sleepAudioClassifier = SleepAudioClassifier(
            this,
            audioClassificationListener
        )
        sleepAudioClassifier.stopAudioClassification()
        sleepAudioClassifier.currentModel = "YAMNET.tflite"

        // 设置闹钟铃声
        alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initServices(){
        // 闹钟前台进程
        val intent = Intent(this, ForegroundService::class.java)
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
        ContextCompat.startForegroundService(this, intent)
        createNotificationChannel()
        registerReceiver(alarmStartedReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    private fun updateTimeUI() {
        // 设置RingView的监听器
        ringView.timeUpdateListener = this
        ringView.arcTouchListener = this

        // 睡觉和起床时间文字
        sleepTimeText.text = formatTime(startTimeCalendar)
        wakeUpText.text = formatTime(endTimeCalendar)
    }

    private fun updateRingView() {
        ringView.setStartTimePosition(startTimeCalendar)
        ringView.setEndTimePosition(endTimeCalendar)
        ringView.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupButtonListeners(){
        // 微调时间按钮点击事件
        btnStartTimeLeft.setOnClickListener {
            vibratePhoneClick()
            adjustStartTime(-1)
        }

        btnStartTimeRight.setOnClickListener {
            vibratePhoneClick()
            adjustStartTime(1)
        }

        btnEndTimeLeft.setOnClickListener {
            vibratePhoneClick()
            adjustEndTime(-1)
        }

        btnEndTimeRight.setOnClickListener {
            vibratePhoneClick()
            adjustEndTime(1)
        }

        // 闹钟拨码点击事件
        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                vibratePhoneClick()
                // 展开 alarmBoardView
                alarmPanelView.visibility = View.VISIBLE

                // 让 scrollView 滚动到 alarmBoardView 的位置
                scrollView.post {
                    scrollView.smoothScrollTo(0, alarmPanelView.bottom)
                }

                // 设置闹钟为开
                if(isStartPressed && !isAlarmPlaying && !isAlarmSet){
                    startAlarm()
                }
            }
            else {
                vibratePhoneClick()
                // 收起 alarmBoardView
                alarmPanelView.visibility = View.GONE

                // 设置闹钟为关
                if(isStartPressed && !isAlarmPlaying && isAlarmSet){
                    stopAlarm()
                }
            }
        }

        // 贪睡拨码点击事件
        snoozeSwitch.setOnCheckedChangeListener { _, isChecked ->
            vibratePhoneClick()
        }

        // 初始化 SeekBar
        seekBarVolume.max = 100
        seekBarVolume.progress = 50
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                alarmVolume = progress / 100f
                updateAlarmVolume(alarmVolume)
                // 保存音量设置
                val sharedPreferences = getSharedPreferences("AlarmConfig", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putFloat("alarmVolume", alarmVolume)
                editor.putBoolean("snoozeSwitchChecked", snoozeSwitch.isChecked)
                editor.apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if(!isAlarmPlaying){
                    playAlarmSeekBar()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if(!isAlarmPlaying){
                    stopAlarmSeekBar()
                }
            }
        })
    }

    private fun setUpObservers(){
        // 观察LiveData以实时更新UI
        viewModel.audioStats.observe(this) { audioStats ->
            snoreTimeNum.text = "${audioStats.snoreTime.toInt()}t"
            speechTimeNum.text = "${audioStats.speechTime.toInt()}t"
            wakeupTimeNum.text = "${audioStats.wakeupTime.toInt()}t"
        }
    }

    suspend fun updateSoundViewsWithData() {
        val snoreTime: Float
        val speechTime: Float
        val insideSmallRoomTime: Float

        // 在后台线程中进行数据库操作
        withContext(Dispatchers.IO) {
            snoreTime = audioCategoryDao.getTotalTimeForLabel(dateId, "Snoring") ?: 0.0f
            speechTime = audioCategoryDao.getTotalTimeForLabel(dateId, "Speech") ?: 0.0f
            insideSmallRoomTime = audioCategoryDao.getTotalTimeForLabel(dateId, "Inside, small room") ?: 0.0f
        }

        // 在主线程中更新UI
        withContext(Dispatchers.Main) {
            snoreTimeNum.text = snoreTime.toInt().toString() + "t"
            speechTimeNum.text = speechTime.toInt().toString() + "t"
            wakeupTimeNum.text = insideSmallRoomTime.toInt().toString() + "t"
        }
    }

    private fun updateAlarmVolume(volume: Float) {
        val intent = Intent(this, AlarmService::class.java)
        intent.putExtra("ACTION", "SET_VOLUME")
        intent.putExtra("VOLUME", volume)
        startService(intent)
    }

    private fun updateTitle() {
        val dayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)

        val currentDay = dayFormat.format(calendar.time)
        val currentDate = dateFormat.format(calendar.time)

        findViewById<TextView>(R.id.titleDay).text = currentDay
        findViewById<TextView>(R.id.titleDate).text = currentDate
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
                textView.setTextColor(ContextCompat.getColor(this, R.color.highlight_grey))
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.bg_day_circle_highlight))
            } else {
                textView.setTextColor(ContextCompat.getColor(this, R.color.normal_grey))
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.bg_day_circle_normal))
            }
            weekDaysContainer.addView(dayView)

            // 移到下一天
            weekDaysCalendar.add(Calendar.DAY_OF_WEEK, 1)
        }
    }

    private fun adjustStartTime(amount: Int) {
        startTimeCalendar.add(Calendar.MINUTE, amount * 1)
        updateTimeUI()
        updateRingView()
    }

    private fun adjustEndTime(amount: Int) {
        endTimeCalendar.add(Calendar.MINUTE, amount * 1)
        updateTimeUI()
        updateRingView()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startAlarm() {
        var time: Long
        try {
            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra("ACTION", "START")
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            time = (endTimeCalendar.timeInMillis - (endTimeCalendar.timeInMillis % 60000))

            if (System.currentTimeMillis() > time) {
                showToast("Invalid time: End time is before current time.")
                time += (1000 * 60 * 60 * 24)
            }

            // 设置闹钟
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)

            val hour = endTimeCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = endTimeCalendar.get(Calendar.MINUTE)
            val timeString = String.format("%02d:%02d", hour, minute)
            showToast("Alarm set for: $timeString")
        } catch (e: SecurityException) {
            showToast("Unable to set alarm: exact alarm permission required.")
            requestExactAlarmPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopAlarm() {
        try {
            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra("ACTION", "STOP")
            sendBroadcast(intent)
            alarmManager.cancel(pendingIntent)
            showToast("Alarm canceled and stopped.")
        } catch (e: SecurityException) {
            showToast("Unable to cancel alarm: exact alarm permission required.")
            requestExactAlarmPermission()
        }
    }

    private fun playAlarmSeekBar() {
        val intent = Intent(this, AlarmService::class.java)
        intent.putExtra("ACTION", "START")
        startService(intent)
    }

    private fun stopAlarmSeekBar() {
        val intent = Intent(this, AlarmService::class.java)
        intent.putExtra("ACTION", "STOP")
        startService(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun vibratePhoneClick() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun formatTime(calendar: Calendar): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Sleep Master Channel",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "通道描述"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkPermissions(): Boolean {
        val recordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return recordAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Recording Permission Granted")
            } else {
                showToast("Recording Permission Denied")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun canScheduleExactAlarms(): Boolean {
        return alarmManager.canScheduleExactAlarms()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestExactAlarmPermission() {
        if (!canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    override fun onRingTimeUpdate(startTime: String, endTime: String) {
        // Update UI with the new times
        sleepTimeText.text = startTime
        wakeUpText.text = endTime
        // Parse the start time and end time
        val (startHour, startMinute) = startTime.split(":").map { it.toInt() }
        val (endHour, endMinute) = endTime.split(":").map { it.toInt() }

        // 使用Calendar的set方法更新小时和分钟
        startTimeCalendar.set(Calendar.HOUR_OF_DAY, startHour)
        startTimeCalendar.set(Calendar.MINUTE, startMinute)

        endTimeCalendar.set(Calendar.HOUR_OF_DAY, endHour)
        endTimeCalendar.set(Calendar.MINUTE, endMinute)

        // Determine the text for dayTextStart
        startDayText.text = when {
            // Case: 18:00 <= startTime <= 23:59 and endTime is after midnight
            startHour in 18..23 -> "Tonight"
            // Case: 00:00 <= startTime < 18:00 and endTime is after midnight
            startHour in 0..17 && endHour >= 0 && startHour > endHour -> "Today"
            // Other cases
            else -> "Tomorrow"
        }
    }

    override fun onRingTouched(isInsideArc: Boolean) {
        scrollView.requestDisallowInterceptTouchEvent(isInsideArc)
    }

//    @RequiresApi(Build.VERSION_CODES.S)
//    fun onBtnStartClickHandler(view: View){
//        isStartPressed = true
//
//        if(alarmSwitch.isChecked && !isAlarmSet){
//            isAlarmSet = true
//            startAlarm()
//        }
//
//        lifecycleScope.launch {
//            viewModel.startSleepTracking(startTimeCalendar, endTimeCalendar)
//        }
//
//        if (checkPermissions()) {
//            sleepAudioClassifier.initClassifier()
//            vibratePhoneClick()
//            btnStart.visibility = Button.GONE
//            btnStop.visibility = Button.VISIBLE
//            ringView.setDraggable(false)
//            ringView.setColors(R.color.dark_orange)
//            ringView.setIcons(
//                ResourcesCompat.getDrawable(resources, R.drawable.ic_bed_dark_16dp, null),
//                ResourcesCompat.getDrawable(resources, R.drawable.ic_alarm_dark_16dp, null)
//            )
//        } else {
//            requestPermissions()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    fun onBtnStopClickHandler(view: View){isStartPressed = false
//        vibratePhoneClick()
//        btnStart.visibility = Button.VISIBLE
//        btnStop.visibility = Button.GONE
//        isAlarmPlaying = false
//        if(alarmSwitch.isChecked && isAlarmSet){
//            isAlarmSet = false
//            stopAlarm()
//        }
//
//        sleepAudioClassifier.stopAudioClassification()
//        ringView.setDraggable(true)
//        ringView.setColors(R.color.light_orange)
//        ringView.setIcons(
//            ResourcesCompat.getDrawable(resources, R.drawable.ic_bed_light_16dp, null),
//            ResourcesCompat.getDrawable(resources, R.drawable.ic_alarm_light_16dp, null)
//        )
//    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onBtnUserPanelClickHandler(view: View) {
        val oldUserFragment = OldUserFragment()
        oldUserFragment.setDao(sleepTimeDao, audioCategoryDao)
        vibratePhoneClick()
        oldUserFragment.show(supportFragmentManager, oldUserFragment.tag)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onBtnSleepTrendClickHandler(view: View) {
        val sleepTrendBottomFragment = SleepTrendBottomFragment()
        vibratePhoneClick()
        sleepTrendBottomFragment.show(supportFragmentManager, sleepTrendBottomFragment.tag)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onBtnSleepPhaseClickHandler(view: View) {
        val sleepPhaseBottomFragment = SleepPhaseBottomFragment()
        vibratePhoneClick()
        sleepPhaseBottomFragment.show(supportFragmentManager, sleepPhaseBottomFragment.tag)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onBtnSleepQualityClickHandler(view: View) {
        val sleepQualityBottomFragment = SleepQualityBottomFragment()
        vibratePhoneClick()
        sleepQualityBottomFragment.show(supportFragmentManager, sleepQualityBottomFragment.tag)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onBtnSleepSoundClickHandler(view: View) {
        val sleepSoundFragment = SoundBottomFragment()
        vibratePhoneClick()
        sleepSoundFragment.show(supportFragmentManager, sleepSoundFragment.tag)
    }
}