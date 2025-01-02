package com.example.shaketoopen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class XCTrackLauncher(private val context: Context) {

    private val tag = "XCTrackLauncher"

    fun launchXCTrack() {
        Log.d(tag, "launchXCTrack() called")
        val launchIntent = context.packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            // XCTrack is not installed, open the Play Store
            val playStoreIntent = Intent(Intent.ACTION_VIEW)
            playStoreIntent.data = Uri.parse("market://details?id=org.xcontest.XCTrack")
            context.startActivity(playStoreIntent)
        }
    }
}