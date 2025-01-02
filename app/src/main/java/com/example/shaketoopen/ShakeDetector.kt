package com.example.shaketoopen

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.TextView
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val viewModel: ShakeDetectionViewModel,
    private val statusText: TextView,
    private val wakeLockManager: WakeLockManager,
    private val xcTrackLauncher: XCTrackLauncher
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private val TAG = "ShakeDetector"

    fun initializeSensors() {
        Log.d(TAG, "initializeSensors() called")
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Log.e(TAG, "Accelerometer sensor not available")
            return
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.d(TAG, "onSensorChanged() called")
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
        Log.d(TAG, "handleShake() called")
        when (viewModel.shakeCount) {
            0 -> {
                viewModel.shakeCount = 1
                statusText.setBackgroundColor(Color.parseColor("#FFA500"))
                statusText.text = statusText.context.getString(R.string.one_shake_detected)
                viewModel.firstShakeTime = currentTime
            }

            1 -> {
                if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeWindow && currentTime - viewModel.firstShakeTime < viewModel.shakeTimeMax) {
                    viewModel.shakeCount = 2
                    statusText.setBackgroundColor(Color.GREEN)
                    viewModel.goCircleGreen = true
                    statusText.text = statusText.context.getString(R.string.two_shakes_detected)
                    if (viewModel.shakeToXCTrackEnabled) {
                        wakeLockManager.wakeUpScreen()
                        xcTrackLauncher.launchXCTrack()
                    }
                }
            }
        }
    }

    private fun resetShakeDetection() {
        Log.d(TAG, "resetShakeDetection() called")
        viewModel.shakeCount = 0
        statusText.setBackgroundColor(Color.RED)
        viewModel.goCircleGreen = false
        statusText.text = statusText.context.getString(R.string.status_waiting)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged() called")
    }

    fun unregisterListener() {
        Log.d(TAG, "unregisterListener() called")
        sensorManager.unregisterListener(this)
    }
}