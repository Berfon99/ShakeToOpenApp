package com.example.shaketoopen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

class XCTrackLauncher(private val context: Context) {

    private val tag = "XCTrackLauncher"
    private val handler = Handler(Looper.getMainLooper())

    fun launchXCTrack() {
        Log.d(tag, "launchXCTrack() called")

        try {
            // First try to bring XCTrack to front if running
            val launchIntent = context.packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")
            if (launchIntent != null) {
                // Set flags to bring to front
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

                Log.d(tag, "Starting XCTrack with intent: $launchIntent")
                context.startActivity(launchIntent)

                // Try a second time after a brief delay (in case first attempt doesn't work with screen just turning on)
                handler.postDelayed({
                    try {
                        val retryIntent = context.packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")
                        retryIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        retryIntent?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

                        Log.d(tag, "Retry - starting XCTrack with intent: $retryIntent")
                        context.startActivity(retryIntent)
                    } catch (e: Exception) {
                        Log.e(tag, "Error in retry launch", e)
                    }
                }, 2000)
            } else {
                // XCTrack is not installed, open the Play Store
                Log.d(tag, "XCTrack not found, opening Play Store")
                val playStoreIntent = Intent(Intent.ACTION_VIEW)
                playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                playStoreIntent.data = Uri.parse("market://details?id=org.xcontest.XCTrack")
                context.startActivity(playStoreIntent)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error launching XCTrack", e)
        }
    }
}