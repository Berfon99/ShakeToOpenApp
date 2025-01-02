package com.example.shaketoopen

import android.content.Context
import android.os.PowerManager
import android.util.Log

class WakeLockManager(private val context: Context) {

    private var wakeLock: PowerManager.WakeLock? = null
    private val TAG = "WakeLockManager"

    fun acquireWakeLock() {
        Log.d(TAG, "acquireWakeLock() called")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ShakeToOpen::WakeLock"
        )
        wakeLock?.acquire()
    }

    fun wakeUpScreen() {
        Log.d(TAG, "wakeUpScreen() called")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ShakeToOpen::WakeLock"
        )
        wakeLock.acquire(5000) // Hold for 5 seconds to ensure screen stays on
    }

    fun releaseWakeLock() {
        Log.d(TAG, "releaseWakeLock() called")
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}