package com.example.shaketoopen

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(private val context: Context, private val viewModel: ShakeDetectionViewModel) {

    fun saveSettings() {
        val sharedPreferences = context.getSharedPreferences("ShakeToOpenSettings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("shakeThreshold", viewModel.shakeThreshold)
        editor.putLong("shakeTimeWindow", viewModel.shakeTimeWindow)
        editor.putLong("shakeTimeMax", viewModel.shakeTimeMax)
        editor.putBoolean("shakeToXCTrackEnabled", viewModel.shakeToXCTrackEnabled)
        editor.apply()
    }


    fun loadSettings() {
        val sharedPreferences = context.getSharedPreferences("ShakeToOpenSettings", Context.MODE_PRIVATE)
        viewModel.shakeThreshold = sharedPreferences.getFloat("shakeThreshold", 7.5f)
        viewModel.shakeTimeWindow = sharedPreferences.getLong("shakeTimeWindow", 500L)
        viewModel.shakeTimeMax = sharedPreferences.getLong("shakeTimeMax", 2000L)
        viewModel.shakeToXCTrackEnabled =
            sharedPreferences.getBoolean("shakeToXCTrackEnabled", true)
    }
}