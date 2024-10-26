package com.example.sleepfast

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sleepfast.fragments.MeditationFragment
import com.example.sleepfast.fragments.InsightFragment
import com.example.sleepfast.fragments.SleepFragment
import com.example.sleepfast.fragments.UserFragment
import com.example.sleepfast.viewmodels.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category

class MainActivity : AppCompatActivity() {

    val sleepFastApp = SleepFastApp.get()

    private lateinit var mainViewModel: MainViewModel

    private lateinit var bottomNavigationView: BottomNavigationView

    private val REQUEST_RECORD_AUDIO_PERMISSION = 1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        window.navigationBarColor = resources.getColor(R.color.bg_grey, theme)

        setupBlur()

        setUpAudioClassifier()

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        if (savedInstanceState == null) {
            loadFragment(SleepFragment())
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_sleep -> {
                    loadFragment(SleepFragment())
                    true
                }
                R.id.action_insight -> {
                    loadFragment(InsightFragment())
                    true
                }
                R.id.action_gene -> {
                    loadFragment(MeditationFragment())
                    true
                }
                R.id.action_user -> {
                    loadFragment(UserFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBlur(){
        val blurView: BlurView = findViewById(R.id.blurView)
        val decorView = window.decorView
        val rootView: View = window.decorView.rootView
        val windowBackground = decorView.background

        blurView.setupWith(rootView as ViewGroup, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(10f)

        blurView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 48f)
            }
        }
        blurView.clipToOutline = true
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container_main, fragment)
        fragmentTransaction.commit()
    }

    private fun setUpAudioClassifier(){
        sleepFastApp.audioClassificationListener = object : AudioClassificationListener {
            override fun onResult(results: List<Category>, inferenceTime: Long) {
                sleepFastApp.applicationScope.launch {
                    results.forEach { category ->
                        val audioCategory = AudioCategory(
                            dateId = sleepFastApp.dateId,
                            label = category.label,
                            score = category.score,
                            timestamp = System.currentTimeMillis()
                        )
                        sleepFastApp.audioCategoryDao.insertCategory(audioCategory)
                    }
                }
            }
            override fun onError(error: String) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
            }
        }

        if (!checkRecordingPermissions()) {
            requestRecordingPermissions()
        }

        sleepFastApp.sleepAudioClassifier = SleepAudioClassifier(this, sleepFastApp.audioClassificationListener)
        sleepFastApp.sleepAudioClassifier.currentModel = "YAMNET.tflite"
        sleepFastApp.sleepAudioClassifier.initClassifier()
    }

    private fun checkRecordingPermissions(): Boolean {
        val recordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        Toast.makeText(this, "Recording Permissions Checked", Toast.LENGTH_SHORT).show()
        return recordAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordingPermissions() {
        Toast.makeText(this, "Recording Permissions Denied, Try To Require again", Toast.LENGTH_SHORT).show()
        ActivityCompat.requestPermissions(
            this as Activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }
}