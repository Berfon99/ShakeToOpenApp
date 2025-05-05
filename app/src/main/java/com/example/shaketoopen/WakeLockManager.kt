package com.example.shaketoopen

import android.content.Context
import android.os.PowerManager
import android.util.Log

class WakeLockManager(private val context: Context) {

    private var wakeLock: PowerManager.WakeLock? = null
    private val tag = "WakeLockManager"

    fun acquireWakeLock() {
        Log.d(tag, "acquireWakeLock() called")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // Release any existing wake lock first
        releaseWakeLock()

        // Use a partial wake lock that keeps CPU on but allows screen to turn off
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ShakeToOpen::BackgroundWakeLock"
        )

        // Acquire indefinitely - we'll manage it ourselves
        wakeLock?.setReferenceCounted(false)
        wakeLock?.acquire()

        Log.d(tag, "Wake lock acquired successfully: ${wakeLock?.isHeld}")
    }

    fun wakeUpScreen() {
        Log.d(tag, "wakeUpScreen() called")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        if (!powerManager.isInteractive) {
            Log.d(tag, "Screen is off, turning it on")

            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "ShakeToOpen::ScreenWakeLock"
            )

            try {
                screenWakeLock.acquire(10000) // Hold for 10 seconds to ensure screen stays on
                Log.d(tag, "Screen wake lock acquired successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error acquiring screen wake lock", e)
            }
        } else {
            Log.d(tag, "Screen is already on")
        }
    }

    fun releaseWakeLock() {
        Log.d(tag, "releaseWakeLock() called, isHeld: ${wakeLock?.isHeld}")
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(tag, "Wake lock released successfully")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing wake lock", e)
        }
    }
}