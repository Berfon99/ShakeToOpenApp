package com.example.shaketoopen

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.TextView
import kotlin.math.sqrt

class ShakeDetector(
    private val context: Context,
    private val viewModel: ShakeDetectionViewModel,
    private val statusText: TextView,
    private val wakeLockManager: WakeLockManager,
    private val xcTrackLauncher: XCTrackLauncher
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private val tag = "ShakeDetector"
    private val mainHandler = Handler(Looper.getMainLooper())

    // Add a periodic heartbeat log to verify the service is running
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            val screenOn = isScreenOn()
            Log.d(tag, "Heartbeat - Service active, screen on: $screenOn, detection enabled: ${viewModel.detectionEnabled}")
            mainHandler.postDelayed(this, 10000) // Log every 10 seconds
        }
    }

    fun initializeSensors() {
        Log.d(tag, "initializeSensors() called")
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Log.e(tag, "Accelerometer sensor not available")
            return
        }

        // Register with SENSOR_DELAY_GAME for more responsive detection
        val registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        Log.d(tag, "Sensor registration result: $registered")

        // Start heartbeat
        mainHandler.post(heartbeatRunnable)
    }

    private fun isScreenOn(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!viewModel.detectionEnabled) return

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Log shake magnitude periodically to see if sensors are responding
            if (currentTime % 5000 < 100) { // Log roughly every 5 seconds
                val screenOn = isScreenOn()
                Log.d(tag, "Current magnitude: $magnitude, threshold: ${viewModel.shakeThreshold}, screen on: $screenOn")
            }

            if (magnitude > viewModel.shakeThreshold) {
                Log.d(tag, "Shake detected! Magnitude: $magnitude, threshold: ${viewModel.shakeThreshold}, screen on: ${isScreenOn()}")
                handleShake(currentTime)
            } else if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeMax) {
                if (viewModel.shakeCount > 0) {
                    Log.d(tag, "Resetting shake detection - timeout exceeded")
                    resetShakeDetection()
                }
            }
            viewModel.lastTime = currentTime
        }
    }

    private fun handleShake(currentTime: Long) {
        Log.d(tag, "handleShake() called, count: ${viewModel.shakeCount}, first shake time: ${viewModel.firstShakeTime}, current time: $currentTime")
        when (viewModel.shakeCount) {
            0 -> {
                viewModel.shakeCount = 1
                Log.d(tag, "First shake detected!")
                mainHandler.post {
                    statusText.setBackgroundColor(Color.parseColor("#FFA500"))
                    statusText.text = statusText.context.getString(R.string.one_shake_detected)
                }
                viewModel.firstShakeTime = currentTime
            }

            1 -> {
                val timeDiff = currentTime - viewModel.firstShakeTime
                Log.d(tag, "Second shake detected! Time difference: $timeDiff ms, window: ${viewModel.shakeTimeWindow}, max: ${viewModel.shakeTimeMax}")
                if (timeDiff > viewModel.shakeTimeWindow && timeDiff < viewModel.shakeTimeMax) {
                    viewModel.shakeCount = 2
                    Log.d(tag, "Valid second shake - launching XCTrack, shake to XCTrack enabled: ${viewModel.shakeToXCTrackEnabled}")
                    mainHandler.post {
                        statusText.setBackgroundColor(Color.GREEN)
                        viewModel.goCircleGreen = true
                        statusText.text = statusText.context.getString(R.string.two_shakes_detected)
                    }
                    if (viewModel.shakeToXCTrackEnabled) {
                        // Wake up screen first, then launch with a slight delay
                        wakeLockManager.wakeUpScreen()
                        // Add a small delay to ensure screen is fully on
                        mainHandler.postDelayed({
                            Log.d(tag, "Launching XCTrack after wake")
                            xcTrackLauncher.launchXCTrack()
                        }, 1000)
                    }
                }
            }
        }
    }

    private fun resetShakeDetection() {
        Log.d(tag, "resetShakeDetection() called")
        viewModel.shakeCount = 0
        mainHandler.post {
            statusText.setBackgroundColor(Color.RED)
            viewModel.goCircleGreen = false
            statusText.text = statusText.context.getString(R.string.status_waiting)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(tag, "onAccuracyChanged() called with accuracy: $accuracy")
    }

    fun unregisterListener() {
        Log.d(tag, "unregisterListener() called")
        mainHandler.removeCallbacks(heartbeatRunnable)
        sensorManager.unregisterListener(this)
    }
}