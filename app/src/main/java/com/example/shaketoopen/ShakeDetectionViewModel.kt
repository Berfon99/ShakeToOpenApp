package com.example.shaketoopen

import androidx.lifecycle.ViewModel

class ShakeDetectionViewModel : ViewModel() {
    var shakeThreshold = 15.0f
    var shakeTimeWindow = 500L // Minimum time between shakes (default 0.5 seconds)
    var shakeTimeMax = 2000L // Maximum time for two shakes to occur (default 2 seconds)
    var lastTime: Long = 0
    var shakeCount = 0
    var goCircleGreen = false  // Flag to check if GO is already green
    var detectionEnabled = true  // Enable shake detection on launch
    var shakeToXCTrackEnabled = false  // Flag to check if "Shake to XCTrack" mode is enabled
    var firstShakeTime: Long = 0 // Time of the first shake
}
