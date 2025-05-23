package com.example.shaketoopen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import android.os.PowerManager
import android.content.Context.POWER_SERVICE

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ShakeDetectionViewModel
    private lateinit var settingsManager: SettingsManager
    private lateinit var wakeLockManager: WakeLockManager
    private lateinit var xcTrackLauncher: XCTrackLauncher
    private lateinit var permissionManager: PermissionManager
    private lateinit var shakeDetector: ShakeDetector

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
            xcTrackLauncher.launchXCTrack()
        }
    }
    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate() called")
        setContentView(R.layout.activity_main)

        // Initialize managers (do this first)
        viewModel = ViewModelProvider(this)[ShakeDetectionViewModel::class.java]
        settingsManager = SettingsManager(this, viewModel)
        wakeLockManager = WakeLockManager(this)
        xcTrackLauncher = XCTrackLauncher(this)
        permissionManager = PermissionManager(this)
        Log.d(tag, "PermissionManager initialized")

        // Request ignore battery optimizations
        requestIgnoreBatteryOptimization()

        // Check and request location permissions (do this early)
        permissionManager.checkAndRequestPermissions {
            Log.d(tag, "Permissions already granted or just granted")
            initializeApp()
        }
        Log.d(tag, "checkAndRequestPermissions() called")
    }

    // Add this new method
    private fun requestIgnoreBatteryOptimization() {
        try {
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error requesting battery optimization exception", e)
        }
    }

    // Make sure service is started in onResume
    override fun onResume() {
        super.onResume()
        // Ensure service is running
        val serviceIntent = Intent(this, ShakeDetectionService::class.java)
        startForegroundService(serviceIntent)
        Log.d(tag, "onResume() - started/refreshed foreground service")
    }

    // Make sure service stays running when activity is stopped
    override fun onStop() {
        super.onStop()
        // Update settings
        settingsManager.saveSettings()
        Log.d(tag, "onStop() - saved settings, service continues running")
    }

    private fun initializeApp() {
        Log.d(tag, "initializeApp() called")
        // Initialize UI elements
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

        // Initialize shake detector
        shakeDetector = ShakeDetector(this, viewModel, statusText, wakeLockManager, xcTrackLauncher)

        // Register the receiver to listen for the broadcast
        val filter = IntentFilter("com.example.shaketoopen.LAUNCH_XCTRACK")
        registerReceiver(xcTrackReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Start the foreground service
        val intent = Intent(this, ShakeDetectionService::class.java)
        startForegroundService(intent)

        // Load settings, initialize sensors, UI, and acquire wake lock
        settingsManager.loadSettings()
        shakeDetector.initializeSensors()
        initializeUI()
        wakeLockManager.acquireWakeLock()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun initializeUI() {
        Log.d(tag, "initializeUI() called")
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
        launchXCTrackButton.setOnClickListener {
            xcTrackLauncher.launchXCTrack()
        }
        sensitivitySlider.progress = ((30f - viewModel.shakeThreshold) / 3.75f).toInt()
        sensitivityValue.text =
            getString(R.string.sensitivity_level, sensitivitySlider.progress + 1)
        timeSlider.progress = ((viewModel.shakeTimeWindow - 200) / 100).toInt()
        timeMaxSlider.progress = ((viewModel.shakeTimeMax - 500) / 500).toInt()
    }

    private fun setupSliders() {
        Log.d(tag, "setupSliders() called")
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

        timeMaxSlider.max = 4
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

    fun toggleShakeToXCTrack(unusedView: View) {
        viewModel.shakeToXCTrackEnabled = !viewModel.shakeToXCTrackEnabled
        toggleShakeToXCTrackButton.text = if (viewModel.shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }
    }

    fun closeApp(unusedView: View) {
        finish()
    }

    override fun onPause() {
        super.onPause()
        wakeLockManager.releaseWakeLock()
        settingsManager.saveSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector.unregisterListener()
        unregisterReceiver(xcTrackReceiver)
    }
}