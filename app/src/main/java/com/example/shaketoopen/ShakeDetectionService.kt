package com.example.shaketoopen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.math.sqrt

class ShakeDetectionService : Service(), ViewModelStoreOwner, SensorEventListener {

    private lateinit var viewModel: ShakeDetectionViewModel
    override val viewModelStore: ViewModelStore = ViewModelStore()
    private val tag = "ShakeDetectionService"
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var wakeLockManager: WakeLockManager
    private lateinit var xcTrackLauncher: XCTrackLauncher
    private val handler = Handler(Looper.getMainLooper())
    private var serviceRunning = false

    companion object {
        private const val CHANNEL_ID = "ShakeDetectionChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate() called")
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(ShakeDetectionViewModel::class.java)
        wakeLockManager = WakeLockManager(this)
        xcTrackLauncher = XCTrackLauncher(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        createNotificationChannel()
        startForegroundService()
        initializeSensors()
        schedulePeriodicalCheck()
        serviceRunning = true

        // Acquire wake lock to keep service running
        wakeLockManager.acquireWakeLock()
    }

    private fun schedulePeriodicalCheck() {
        val runnable = object : Runnable {
            override fun run() {
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                val isScreenOn = pm.isInteractive
                Log.d(tag, "Service check - still running, screen on: $isScreenOn")

                // Re-register sensors if needed
                if (serviceRunning) {
                    ensureSensorRegistered()
                    handler.postDelayed(this, 30000) // Check every 30 seconds
                }
            }
        }
        handler.postDelayed(runnable, 30000)
    }

    private fun ensureSensorRegistered() {
        // Re-register the sensor listener to ensure it's active
        try {
            sensorManager.unregisterListener(this)
            val registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            Log.d(tag, "Sensor re-registered: $registered")
        } catch (e: Exception) {
            Log.e(tag, "Error re-registering sensor", e)
        }
    }

    private fun initializeSensors() {
        Log.d(tag, "initializeSensors() called in service")
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Log.e(tag, "Accelerometer sensor not available")
            return
        }
        val registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        Log.d(tag, "Sensor registration result: $registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand() called")
        // If service gets restarted, reacquire wake lock
        if (!::wakeLockManager.isInitialized) {
            wakeLockManager = WakeLockManager(this)
        }
        wakeLockManager.acquireWakeLock()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(tag, "onBind() called")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy() called")
        serviceRunning = false
        sensorManager.unregisterListener(this)
        wakeLockManager.releaseWakeLock()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!viewModel.detectionEnabled) return

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Log sensor data periodically to verify service is receiving updates
            if (currentTime % 10000 < 100) {
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                val isScreenOn = pm.isInteractive
                Log.d(tag, "Service sensor reading - magnitude: $magnitude, screen on: $isScreenOn")
            }

            if (magnitude > viewModel.shakeThreshold) {
                Log.d(tag, "Service detected shake! Magnitude: $magnitude")
                handleShake(currentTime)
            } else if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeMax) {
                if (viewModel.shakeCount > 0) {
                    resetShakeDetection()
                }
            }
            viewModel.lastTime = currentTime
        }
    }

    private fun handleShake(currentTime: Long) {
        Log.d(tag, "Service handleShake() called, count: ${viewModel.shakeCount}")
        when (viewModel.shakeCount) {
            0 -> {
                viewModel.shakeCount = 1
                viewModel.firstShakeTime = currentTime
                Log.d(tag, "Service detected first shake")
            }

            1 -> {
                val timeDiff = currentTime - viewModel.firstShakeTime
                Log.d(tag, "Service detected second shake! Time diff: $timeDiff ms")
                if (timeDiff > viewModel.shakeTimeWindow && timeDiff < viewModel.shakeTimeMax) {
                    viewModel.shakeCount = 2
                    Log.d(tag, "Service validated second shake - broadcasting")

                    if (viewModel.shakeToXCTrackEnabled) {
                        // Broadcast intent to launch XCTrack
                        val broadcastIntent = Intent("com.example.shaketoopen.LAUNCH_XCTRACK")
                        sendBroadcast(broadcastIntent)

                        // Also directly handle in case broadcast fails
                        wakeLockManager.wakeUpScreen()
                        // Add delay to ensure screen is on
                        handler.postDelayed({
                            xcTrackLauncher.launchXCTrack()
                        }, 1000)
                    }
                }
            }
        }
    }

    private fun resetShakeDetection() {
        viewModel.shakeCount = 0
    }

    private fun createNotificationChannel() {
        Log.d(tag, "createNotificationChannel() called")
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Shake Detection Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun startForegroundService() {
        Log.d(tag, "startForegroundService() called")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shake Detection Service")
            .setContentText("Detecting shakes in background...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(tag, "onAccuracyChanged() called with accuracy: $accuracy")
    }
}