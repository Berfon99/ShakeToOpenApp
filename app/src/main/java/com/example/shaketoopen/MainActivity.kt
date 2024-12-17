package com.example.shaketoopen

import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
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
    private lateinit var goCircle: FrameLayout
    private lateinit var goText: TextView
    private lateinit var launchXCTrackButton: Button
    private lateinit var toggleShakeToXCTrackButton: Button
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(ShakeDetectionViewModel::class.java)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        statusText = findViewById(R.id.status_text)
        sensitivityValue = findViewById(R.id.sensitivity_value)
        sensitivitySlider = findViewById(R.id.sensitivity_slider)
        timeValue = findViewById(R.id.time_value)
        timeSlider = findViewById(R.id.time_slider)
        timeMaxValue = findViewById(R.id.time_max_value)
        timeMaxSlider = findViewById(R.id.time_max_slider)
        goCircle = findViewById(R.id.go_circle)
        goText = findViewById(R.id.go_text)
        launchXCTrackButton = findViewById(R.id.launch_xctrack_button)
        toggleShakeToXCTrackButton = findViewById(R.id.toggle_shake_to_xctrack_button)
        closeButton = findViewById(R.id.close_button)

        // Initialize default values
        sensitivitySlider.progress = 3 // Set default sensitivity to 3
        timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
        timeMaxValue.text = getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)

        // Sensitivity slider
        sensitivitySlider.max = 4 // Set the maximum value to 4 for 5 levels
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Adjust sensitivity inversely proportional
                // New mapping: progress 0 -> threshold 30, progress 4 -> threshold 15
                viewModel.shakeThreshold = 30f - (progress * 3.75f)  // Adjust the multiplier to spread out the threshold values
                sensitivityValue.text = getString(R.string.sensitivity_level, progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Minimum time between shakes (milliseconds)
        timeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeWindow = (200 + progress * 50).toLong() // Convert progress to milliseconds
                timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                if (viewModel.shakeTimeWindow > viewModel.shakeTimeMax) {
                    viewModel.shakeTimeWindow = viewModel.shakeTimeMax
                    timeSlider.progress = ((viewModel.shakeTimeMax - 200) / 50).toInt()
                    timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Maximum time for two shakes to occur (milliseconds)
        timeMaxSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeMax = (500 + progress * 500).toLong() // Convert progress to milliseconds
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

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Update status text to indicate detection is in progress
        statusText.text = getString(R.string.detection_in_progress)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("shakeToXCTrackEnabled", viewModel.shakeToXCTrackEnabled)
        outState.putInt("shakeCount", viewModel.shakeCount)
        outState.putBoolean("goCircleGreen", viewModel.goCircleGreen)
        outState.putLong("lastTime", viewModel.lastTime)
        outState.putLong("firstShakeTime", viewModel.firstShakeTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.shakeToXCTrackEnabled = savedInstanceState.getBoolean("shakeToXCTrackEnabled")
        viewModel.shakeCount = savedInstanceState.getInt("shakeCount")
        viewModel.goCircleGreen = savedInstanceState.getBoolean("goCircleGreen")
        viewModel.lastTime = savedInstanceState.getLong("lastTime")
        viewModel.firstShakeTime = savedInstanceState.getLong("firstShakeTime")

        // Update the state of the button and the GO circle
        toggleShakeToXCTrackButton.text = if (viewModel.shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }

        updateGoCircleColor()
    }

    // Method to launch XCTrack
    fun launchXCTrack(view: View?) {
        val packageManager = packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")

        if (launchIntent != null) {
            // XCTrack is installed
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningAppProcesses = activityManager.runningAppProcesses
            val isXCTrackRunning = runningAppProcesses.any { it.processName == "org.xcontest.XCTrack" }

            if (isXCTrackRunning) {
                // XCTrack is running, bring it to the foreground
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            } else {
                // Add flags to launch a new task
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to launch XCTrack", e)
                Toast.makeText(this, "Failed to launch XCTrack", Toast.LENGTH_SHORT).show()
            }
        } else {
            // XCTrack is not installed
            Toast.makeText(this, "XCTrack not installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!viewModel.detectionEnabled) return

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            // Check shake intensity
            if (magnitude > viewModel.shakeThreshold) {
                if (viewModel.shakeCount == 0) {
                    // First shake detected, GO turns orange
                    viewModel.shakeCount = 1
                    goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
                    statusText.text = getString(R.string.one_shake_detected)
                    viewModel.firstShakeTime = currentTime // Record the time of the first shake
                } else if (viewModel.shakeCount == 1 && currentTime - viewModel.firstShakeTime > viewModel.shakeTimeWindow && currentTime - viewModel.firstShakeTime < viewModel.shakeTimeMax) {
                    // Two shakes detected, GO turns green and stays green
                    viewModel.shakeCount = 2
                    goCircle.setBackgroundColor(Color.GREEN)
                    viewModel.goCircleGreen = true  // Prevent GO from turning red again
                    statusText.text = getString(R.string.two_shakes_detected)
                    if (viewModel.shakeToXCTrackEnabled) {
                        launchXCTrack(null)  // Launch XCTrack if "Shake to XCTrack" mode is enabled
                    }
                }
            } else if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeMax) {
                // Reset shake counter if the second shake does not occur within the maximum time
                viewModel.shakeCount = 0
                goCircle.setBackgroundColor(Color.RED)  // Turn GO red again
                viewModel.goCircleGreen = false  // Reset GO state
                statusText.text = getString(R.string.status_waiting)
            }
            viewModel.lastTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    // Method to enable/disable "Shake to XCTrack" mode
    @SuppressWarnings("unused")
    fun toggleShakeToXCTrack(view: View) {
        viewModel.shakeToXCTrackEnabled = !viewModel.shakeToXCTrackEnabled
        toggleShakeToXCTrackButton.text = if (viewModel.shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }
    }

    // Method to close the application
    @SuppressWarnings("unused")
    fun closeApp(view: View) {
        finish()  // Close the application
    }

    private fun updateGoCircleColor() {
        if (viewModel.goCircleGreen) {
            goCircle.setBackgroundColor(Color.GREEN)
        } else if (viewModel.shakeCount == 1) {
            goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
        } else {
            goCircle.setBackgroundColor(Color.RED)  // Red
        }
    }
}
