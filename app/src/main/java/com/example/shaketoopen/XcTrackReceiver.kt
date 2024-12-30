package com.example.shaketoopen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class XcTrackReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Log the broadcast reception
        Log.d("XcTrackReceiver", "Received launch intent for XCTrack")

        // Handle the broadcast (e.g., launch the XCTrack app or perform other actions)
        val launchIntent = Intent(Intent.ACTION_MAIN)
        launchIntent.setPackage("org.xcontest.XCTrack")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("XcTrackReceiver", "Failed to launch XCTrack", e)
        }
    }
}
