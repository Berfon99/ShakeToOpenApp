//ShakeDetectionViewModel.kt
package com.example.shaketoopen

import androidx.lifecycle.ViewModel

class ShakeDetectionViewModel : ViewModel() {
    var shakeThreshold: Float = 0f
    var shakeTimeWindow: Long = 0
    var shakeTimeMax: Long = 0
    var shakeCount: Int = 0
    var firstShakeTime: Long = 0
    var lastTime: Long = 0
    var goCircleGreen: Boolean = false
    var shakeToXCTrackEnabled: Boolean = false
    var detectionEnabled: Boolean = true
}
