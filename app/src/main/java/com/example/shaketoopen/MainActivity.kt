//MainActivity.kt
package com.example.shaketoopen

import com.example.shaketoopen.ShakeDetectionService
import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import kotlin.math.sqrt
import android.content.BroadcastReceiver
import android.content.IntentFilter

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var viewModel: ShakeDetectionViewModel
    private lateinit var settingsManager: SettingsManager
    private lateinit var wakeLockManager: WakeLockManager
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


    private val xcTrackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Wake up the screen
            wakeLockManager.wakeUpScreen()
            // Launch XCTrack
            launchXCTrack()
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize managers
        viewModel = ViewModelProvider(this)[ShakeDetectionViewModel::class.java]
        settingsManager = SettingsManager(this, viewModel)
        wakeLockManager = WakeLockManager(this)

        // Register the receiver to listen for the broadcast
        val filter = IntentFilter("com.example.shaketoopen.LAUNCH_XCTRACK")
        registerReceiver(xcTrackReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Check and request location permissions
        checkAndRequestPermissions()

        // Start the foreground service
        val intent = Intent(this, ShakeDetectionService::class.java)
        startForegroundService(intent)


        // Set OnClickListener for the "Launch XCTrack" button
        val launchXCTrackButton: Button = findViewById(R.id.launch_xctrack_button)
        launchXCTrackButton.setOnClickListener {
        launchXCTrack()  // Call your existing method
            }
        }

private fun checkAndRequestPermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    } else {
        initializeApp()
    }
}

private fun initializeApp() {
    initializeViewModel()
    settingsManager.loadSettings()
    initializeSensors()
    initializeUI()
    wakeLockManager.acquireWakeLock()

    // Start the foreground service
    val intent = Intent(this, ShakeDetectionService::class.java)
    startForegroundService(intent)

    // Set OnClickListener for the "Launch XCTrack" button
    val launchXCTrackButton: Button = findViewById(R.id.launch_xctrack_button)
    launchXCTrackButton.setOnClickListener {
        launchXCTrack()  // Call your existing method
    }
}

override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeApp()
        } else {
            // Handle the case where permissions are denied
            Toast.makeText(this, "Location permissions are required to use this app", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}



private fun initializeViewModel() {
        viewModel = ViewModelProvider(this).get(ShakeDetectionViewModel::class.java)
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
        timeValue.text = getString(
            R.string.time_delay,
            viewModel.shakeTimeWindow / 1000.0
        ) // Default minimum time value
        timeMaxValue.text = getString(
            R.string.time_max_delay,
            viewModel.shakeTimeMax / 1000.0
        ) // Default maximum time value

        setupSliders()
        statusText.text = getString(R.string.status_waiting)
        statusText.setBackgroundColor(Color.RED)
        toggleShakeToXCTrackButton.text = if (viewModel.shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }
        closeButton.setOnClickListener {
            closeApp(it)
        }
        sensitivitySlider.progress = ((30f - viewModel.shakeThreshold) / 3.75f).toInt()
        sensitivityValue.text =
            getString(R.string.sensitivity_level, sensitivitySlider.progress + 1)
        timeSlider.progress = ((viewModel.shakeTimeWindow - 200) / 100).toInt()
        timeMaxSlider.progress = ((viewModel.shakeTimeMax - 500) / 500).toInt()
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
        timeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeWindow = (200 + progress * 100).toLong()
                timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                if (viewModel.shakeTimeWindow > viewModel.shakeTimeMax) {
                    viewModel.shakeTimeWindow = viewModel.shakeTimeMax
                    timeSlider.progress = ((viewModel.shakeTimeMax - 200) / 100).toInt()
                    timeValue.text =
                        getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        timeMaxSlider.max = 5
        timeMaxSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeMax = (500 + progress * 500).toLong()
                timeMaxValue.text =
                    getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)
                if (viewModel.shakeTimeMax < viewModel.shakeTimeWindow) {
                    viewModel.shakeTimeMax = viewModel.shakeTimeWindow
                    timeMaxSlider.progress = ((viewModel.shakeTimeMax - 500) / 500).toInt()
                    timeMaxValue.text =
                        getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)
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
                        wakeLockManager.wakeUpScreen()
                        bringAppToForeground()
                        launchXCTrack()
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

    override fun onPause() {
        super.onPause()
        wakeLockManager.releaseWakeLock()
        settingsManager.saveSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        // Unregister the receiver when the activity is destroyed
        unregisterReceiver(xcTrackReceiver)
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



    private fun bringAppToForeground() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun launchXCTrack() {
        val launchIntent = packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            // XCTrack is not installed, open the Play Store
            val playStoreIntent = Intent(Intent.ACTION_VIEW)
            playStoreIntent.data = Uri.parse("market://details?id=org.xcontest.XCTrack")
            startActivity(playStoreIntent)
        }
    }
}
