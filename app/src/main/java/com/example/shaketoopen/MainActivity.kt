package com.example.shaketoopen

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var viewModel: ShakeDetectionViewModel

    private lateinit var statusText: TextView
    private lateinit var sensitivityValue: TextView
    private lateinit var sensitivitySlider: SeekBar
    private lateinit var timeValue: TextView
    private lateinit var timeSlider: SeekBar
    private lateinit var timeMaxValue: TextView
    private lateinit var timeMaxSlider: SeekBar
    private lateinit var launchXCTrackButton: Button
    private lateinit var toggleShakeToXCTrackButton: Button
    private lateinit var closeButton: Button

    private lateinit var wakeLock: PowerManager.WakeLock
    private val handler = Handler(Looper.getMainLooper())
    private val inactivityTimeout: Long = 10000 // 10 seconds of inactivity

    private val releaseWakeLockRunnable = Runnable {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViewModel()
        initializeSensors()
        initializeUI()
        acquireWakeLock()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this).get(ShakeDetectionViewModel::class.java)
        // Set default values
        viewModel.shakeThreshold = 22.5f // Default sensitivity value corresponding to "Sensitivity 3"
        viewModel.shakeTimeWindow = 500L // Default minimum time value
        viewModel.shakeTimeMax = 2000L // Default maximum time value
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Toast.makeText(this, "Accelerometer sensor not available", Toast.LENGTH_SHORT).show()
            return
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun initializeUI() {
        statusText = findViewById(R.id.status_text)
        sensitivityValue = findViewById(R.id.sensitivity_value)
        sensitivitySlider = findViewById(R.id.sensitivity_slider)
        timeValue = findViewById(R.id.time_value)
        timeSlider = findViewById(R.id.time_slider)
        timeMaxValue = findViewById(R.id.time_max_value)
        timeMaxSlider = findViewById(R.id.time_max_slider)
        launchXCTrackButton = findViewById(R.id.launch_xctrack_button)
        toggleShakeToXCTrackButton = findViewById(R.id.toggle_shake_to_xctrack_button)
        closeButton = findViewById(R.id.close_button)

        // Set default values in UI
        sensitivitySlider.progress = 2 // Default sensitivity slider position corresponding to "Sensitivity 3"
        sensitivityValue.text = getString(R.string.sensitivity_level, 3) // Default sensitivity value
        timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0) // Default minimum time value
        timeMaxValue.text = getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0) // Default maximum time value
        timeMaxSlider.progress = 2 // Default maximum time slider position corresponding to 2.0 s

        setupSliders()
        updateStatusText()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "ShakeToOpen::WakeLock"
        )
        wakeLock.acquire()
        handler.postDelayed(releaseWakeLockRunnable, inactivityTimeout)
    }

    private fun setupSliders() {
        sensitivitySlider.max = 4
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeThreshold = 30f - (progress * 3.75f)
                sensitivityValue.text = getString(R.string.sensitivity_level, progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        timeSlider.max = 8
        timeSlider.progress = 3 // Default minimum time slider position corresponding to 0.5 s
        timeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeWindow = (200 + progress * 100).toLong()
                timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                if (viewModel.shakeTimeWindow > viewModel.shakeTimeMax) {
                    viewModel.shakeTimeWindow = viewModel.shakeTimeMax
                    timeSlider.progress = ((viewModel.shakeTimeMax - 200) / 100).toInt()
                    timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        timeMaxSlider.max = 5
        timeMaxSlider.progress = 2 // Default maximum time slider position corresponding to 2.0 s
        timeMaxSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeMax = (500 + progress * 500).toLong()
                timeMaxValue.text = getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)
                if (viewModel.shakeTimeMax < viewModel.shakeTimeWindow) {
                    viewModel.shakeTimeMax = viewModel.shakeTimeWindow
                    timeMaxSlider.progress = ((viewModel.shakeTimeMax - 500) / 500).toInt()
                    timeMaxValue.text = getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateStatusText() {
        statusText.text = getString(R.string.detection_in_progress)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!viewModel.detectionEnabled) return

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            if (magnitude > viewModel.shakeThreshold) {
                handleShake(currentTime)
            } else if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeMax) {
                resetShakeDetection()
            }
            viewModel.lastTime = currentTime
        }
    }

    private fun handleShake(currentTime: Long) {
        when (viewModel.shakeCount) {
            0 -> {
                viewModel.shakeCount = 1
                statusText.setBackgroundColor(Color.parseColor("#FFA500"))
                statusText.text = getString(R.string.one_shake_detected)
                viewModel.firstShakeTime = currentTime
            }
            1 -> {
                if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeWindow && currentTime - viewModel.firstShakeTime < viewModel.shakeTimeMax) {
                    viewModel.shakeCount = 2
                    statusText.setBackgroundColor(Color.GREEN)
                    viewModel.goCircleGreen = true
                    statusText.text = getString(R.string.two_shakes_detected)
                    if (viewModel.shakeToXCTrackEnabled) {
                        // Acquire the wake lock to turn on the screen
                        acquireWakeLock()
                        // Bring the app to the foreground if it is not already
                        bringAppToForeground()
                        // Launch XCTrack after the screen is turned on
                        launchXCTrack(null)
                    }
                }
            }
        }
    }

    private fun resetShakeDetection() {
        viewModel.shakeCount = 0
        statusText.setBackgroundColor(Color.RED)
        viewModel.goCircleGreen = false
        statusText.text = getString(R.string.status_waiting)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(releaseWakeLockRunnable)
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    fun toggleShakeToXCTrack(view: View) {
        viewModel.shakeToXCTrackEnabled = !viewModel.shakeToXCTrackEnabled
        toggleShakeToXCTrackButton.text = if (viewModel.shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }
    }

    fun closeApp(view: View) {
        finish()
    }

    private fun updateStatusColor() {
        statusText.setBackgroundColor(
            when {
                viewModel.goCircleGreen -> Color.GREEN
                viewModel.shakeCount == 1 -> Color.parseColor("#FFA500")
                else -> Color.RED
            }
        )
    }

    // Method to bring the app to the foreground
    private fun bringAppToForeground() {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        if (runningTasks.isNotEmpty()) {
            val topActivity = runningTasks[0].topActivity
            if (topActivity?.packageName != packageName) {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(launchIntent)
            }
        }
    }

    // Method to launch XCTrack
    fun launchXCTrack(view: View?) {
        val packageManager = packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")

        if (launchIntent != null) {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcesses = activityManager.runningAppProcesses
            val isXCTrackRunning = runningAppProcesses.any { it.processName == "org.xcontest.XCTrack" }

            if (isXCTrackRunning) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            } else {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to launch XCTrack", e)
                Toast.makeText(this, "Failed to launch XCTrack", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "XCTrack not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
