//ShakeDetectionService.kt
package com.example.shaketoopen

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ShakeDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var viewModel: ShakeDetectionViewModel
    private lateinit var wakeLock: PowerManager.WakeLock
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        viewModel = ShakeDetectionViewModel()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "ShakeToOpen::WakeLock"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "shake_detection_channel",
                "Shake Detection",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, "shake_detection_channel")
            .setContentTitle("Shake Detection")
            .setContentText("Shake detection is running")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
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
                viewModel.firstShakeTime = currentTime
            }
            1 -> {
                if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeWindow && currentTime - viewModel.firstShakeTime < viewModel.shakeTimeMax) {
                    viewModel.shakeCount = 2
                    viewModel.goCircleGreen = true
                    if (viewModel.shakeToXCTrackEnabled) {
                        // Acquire the wake lock to turn on the screen
                        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
                        // Bring the app to the foreground if it is not already
                        bringAppToForeground()
                        // Launch XCTrack after the screen is turned on
                        launchXCTrack()
                        // Release the wake lock after launching XCTrack
                        releaseWakeLock()
                    }
                }
            }
        }
    }

    private fun resetShakeDetection() {
        viewModel.shakeCount = 0
        viewModel.goCircleGreen = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

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

    private fun launchXCTrack() {
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
                Log.e("ShakeDetectionService", "Failed to launch XCTrack", e)
            }
        }
    }

    // Method to release the wake lock
    private fun releaseWakeLock() {
        handler.postDelayed({
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }, 5000) // Release the wake lock after 5 seconds
    }
}
