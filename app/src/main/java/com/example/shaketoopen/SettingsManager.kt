package com.example.shaketoopen

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(private val context: Context, private val viewModel: ShakeDetectionViewModel) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ShakeToOpenSettings", Context.MODE_PRIVATE)

    fun loadSettings() {
        viewModel.shakeThreshold = sharedPreferences.getFloat("shakeThreshold", 15f)
        viewModel.shakeTimeWindow = sharedPreferences.getLong("shakeTimeWindow", 500)
        viewModel.shakeTimeMax = sharedPreferences.getLong("shakeTimeMax", 1000)
        viewModel.shakeToXCTrackEnabled = sharedPreferences.getBoolean("shakeToXCTrackEnabled", true)
        viewModel.detectionEnabled = sharedPreferences.getBoolean("detectionEnabled", true)
    }

    fun saveSettings() {
        with(sharedPreferences.edit()) {
            putFloat("shakeThreshold", viewModel.shakeThreshold)
            putLong("shakeTimeWindow", viewModel.shakeTimeWindow)
            putLong("shakeTimeMax", viewModel.shakeTimeMax)
            putBoolean("shakeToXCTrackEnabled", viewModel.shakeToXCTrackEnabled)
            putBoolean("detectionEnabled", viewModel.detectionEnabled)
            apply()
        }
    }
}