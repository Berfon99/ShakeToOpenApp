package com.example.shaketoopen

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 15.0f
    private var shakeTimeWindow = 1000L // Temps entre secousses ajusté par le slider
    private var lastTime: Long = 0
    private var shakeCount = 0
    private var goCircleGreen = false  // Nouveau flag pour vérifier si GO est déjà vert
    private var detectionEnabled = false

    private lateinit var statusText: TextView
    private lateinit var sensitivityValue: TextView
    private lateinit var sensitivitySlider: SeekBar
    private lateinit var timeValue: TextView
    private lateinit var timeSlider: SeekBar
    private lateinit var goCircle: FrameLayout
    private lateinit var goText: TextView
    private lateinit var toggleDetectionButton: Button
    private lateinit var resetButton: Button
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        statusText = findViewById(R.id.status_text)
        sensitivityValue = findViewById(R.id.sensitivity_value)
        sensitivitySlider = findViewById(R.id.sensitivity_slider)
        timeValue = findViewById(R.id.time_value)
        timeSlider = findViewById(R.id.time_slider)
        goCircle = findViewById(R.id.go_circle)
        goText = findViewById(R.id.go_text)
        toggleDetectionButton = findViewById(R.id.toggle_detection_button)
        resetButton = findViewById(R.id.reset_button)
        closeButton = findViewById(R.id.close_button)

        // Sensibilité du slider
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shakeThreshold = 15f + progress * 2f  // Ajustement de la sensibilité
                sensitivityValue.text = "Sensibilité : Niveau ${progress + 1}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Temps entre secousses (millisecondes)
        timeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shakeTimeWindow = (progress + 1).toLong()
                timeValue.text = "Délai : ${shakeTimeWindow} ms"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bouton pour démarrer/arrêter la détection
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

        // Bouton pour réinitialiser la détection
        resetButton.setOnClickListener {
            shakeCount = 0
            goCircle.setBackgroundColor(Color.RED)  // Remettre GO en rouge
            goCircleGreen = false  // Réinitialiser l'état du GO
            statusText.text = "Statut : En attente de secousses..."
        }

        // Bouton pour fermer l'application
        closeButton.setOnClickListener {
            finish()  // Ferme l'application
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!detectionEnabled) return

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Affichage de l'intensité des secousses
            if (magnitude > shakeThreshold) {
                if (currentTime - lastTime < shakeTimeWindow) {
                    shakeCount++
                    if (shakeCount == 2 && !goCircleGreen) {
                        // Deux secousses détectées, GO devient vert et reste vert
                        goCircle.setBackgroundColor(Color.GREEN)
                        goCircleGreen = true  // On empêche de remettre GO en rouge
                        statusText.text = "Deux secousses détectées!"
                    }
                } else {
                    shakeCount = 0
                }
                lastTime = currentTime
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
