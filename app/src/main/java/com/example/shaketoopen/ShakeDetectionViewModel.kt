//ShakeDetectionViewModel.kt
package com.example.shaketoopen

import androidx.lifecycle.ViewModel

class ShakeDetectionViewModel : ViewModel() {
    var shakeThreshold: Float = 15f
    var shakeTimeWindow: Long = 500
    var shakeTimeMax: Long = 1000
    var shakeCount: Int = 0
    var firstShakeTime: Long = 0
    var lastTime: Long = 0
    var shakeToXCTrackEnabled: Boolean = true
    var detectionEnabled: Boolean = true
    var goCircleGreen: Boolean = false
}