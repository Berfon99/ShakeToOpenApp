package com.example.shaketoopen

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager



class WakeLockManager(private val context: Context) {

    private lateinit var wakeLock: PowerManager.WakeLock
    private val handler = Handler(Looper.getMainLooper())

    private val releaseWakeLockRunnable = Runnable {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    fun acquireWakeLock() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ShakeToOpen::WakeLock"
        )

    }


    fun wakeUpScreen() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ShakeToOpen::WakeLock"
        )
        wakeLock.acquire(5000) // Hold for 5 seconds to ensure screen stays on
    }

    // Function to turn on the screen if it's off
    fun turnOnScreen() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ShakeToOpen::WakeLock"
        )
        wakeLock.acquire(5000) // Hold the screen on for 5 seconds
    }



    fun releaseWakeLock() {
        handler.postDelayed(releaseWakeLockRunnable, 5000)
    }
}