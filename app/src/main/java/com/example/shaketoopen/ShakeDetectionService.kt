package com.example.shaketoopen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class ShakeDetectionService : Service(), ViewModelStoreOwner {

    private lateinit var viewModel: ShakeDetectionViewModel
    override val viewModelStore: ViewModelStore = ViewModelStore()
    private val tag = "ShakeDetectionService"

    companion object {
        private const val CHANNEL_ID = "ShakeDetectionChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate() called")
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(ShakeDetectionViewModel::class.java) // This line is correct!

        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand() called")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(tag, "onBind() called")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy() called")
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
            .setContentText("Detecting shakes...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}