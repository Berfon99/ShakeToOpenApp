package com.example.shaketoopen

import android.content.Context
import android.content.Intent
import android.net.Uri

class XCTrackLauncher(private val context: Context) {


    fun launchXCTrack() {
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