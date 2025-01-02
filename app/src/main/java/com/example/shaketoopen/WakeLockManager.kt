package com.example.shaketoopen

import android.content.Context
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager

class WakeLockManager(private val context: Context) {

    private var wakeLock: PowerManager.WakeLock? = null
    private val tag = "WakeLockManager"

    fun acquireWakeLock() {
        Log.d(tag, "acquireWakeLock() called")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ShakeToOpen::WakeLock"
        )
        wakeLock?.acquire(10000) // Timeout of 10 seconds
    }

    fun wakeUpScreen() {
        Log.d(tag, "wakeUpScreen() called")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ShakeToOpen::WakeLock"
        )
        wakeLock.acquire(5000) // Hold for 5 seconds to ensure screen stays on
    }

    fun releaseWakeLock() {
        Log.d(tag, "releaseWakeLock() called")
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}