package com.example.shaketoopen

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 15.0f
    private var lastTime: Long = 0
    private val shakeTimeWindow = 1000L
    private var shakeCount = 0
    private var detectionEnabled = false  // Indicateur pour savoir si la détection est active

    private lateinit var statusText: TextView
    private lateinit var sensitivityValue: TextView
    private lateinit var sensitivitySlider: SeekBar
    private lateinit var resetButton: Button
    private lateinit var toggleDetectionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        statusText = findViewById(R.id.status_text)
        sensitivityValue = findViewById(R.id.sensitivity_value)
        sensitivitySlider = findViewById(R.id.sensitivity_slider)
        resetButton = findViewById(R.id.reset_button)
        toggleDetectionButton = findViewById(R.id.toggle_detection_button)

        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shakeThreshold = 10f + progress * 2f // Plus de niveaux de sensibilité (plus gradué)
                sensitivityValue.text = "Sensibilité : Niveau ${progress + 1}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        resetButton.setOnClickListener {
            shakeCount = 0
            statusText.text = "Statut : En attente de secousses..."
            statusText.setBackgroundColor(resources.getColor(android.R.color.transparent))
        }

        toggleDetectionButton.setOnClickListener {
            detectionEnabled = !detectionEnabled
            if (detectionEnabled) {
                toggleDetectionButton.text = "Arrêter la détection"
                statusText.text = "Détection en cours..."
            } else {
                toggleDetectionButton.text = "Démarrer la détection"
                statusText.text = "Détection arrêtée"
            }
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!detectionEnabled) return  // Si la détection est arrêtée, on ne fait rien

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            if (magnitude > shakeThreshold) {
                if (currentTime - lastTime < shakeTimeWindow) {
                    shakeCount++
                    if (shakeCount == 2) {
                        // Modifier le statut en fonction de la force de la secousse
                        statusText.text = "Secousse détectée!"
                        changeBackgroundColor(magnitude) // Changer la couleur de fond en fonction de la force
                        shakeCount = 0
                    }
                } else {
                    shakeCount = 0
                }
                lastTime = currentTime
            }
        }
    }

    private fun changeBackgroundColor(magnitude: Float) {
        // Définir les couleurs selon la magnitude de la secousse
        when {
            magnitude < 20 -> statusText.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            magnitude in 20f..30f -> statusText.setBackgroundColor(resources.getColor(android.R.color.holo_orange_light))
            else -> statusText.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    fun resetDetection() {
        shakeCount = 0
        statusText.text = "Statut : En attente de secousses..."
        statusText.setBackgroundColor(resources.getColor(android.R.color.transparent))
    }

    fun toggleDetection() {
        detectionEnabled = !detectionEnabled
        if (detectionEnabled) {
            statusText.text = "Détection en cours..."
        } else {
            statusText.text = "Détection arrêtée"
        }
    }
}
