package com.example.shaketoopen

import androidx.lifecycle.ViewModel

class ShakeDetectionViewModel : ViewModel() {
    var shakeThreshold = 15.0f
    var shakeTimeWindow = 500L // Temps minimum entre secousses (par défaut 0.5 seconde)
    var shakeTimeMax = 2000L // Temps maximum pour que les deux secousses aient lieu (par défaut 2 secondes)
    var lastTime: Long = 0
    var shakeCount = 0
    var goCircleGreen = false  // Flag pour vérifier si GO est déjà vert
    var detectionEnabled = true  // Activer la détection de secousses dès le lancement
    var shakeToXCTrackEnabled = false  // Flag pour vérifier si le mode "Secousses to XCTrack" est activé
    var firstShakeTime: Long = 0 // Temps de la première secousse
}