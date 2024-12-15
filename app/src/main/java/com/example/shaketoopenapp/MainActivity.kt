package com.example.shaketoopen

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 15.0f
    private var shakeTimeWindow = 500L // Temps minimum entre secousses (par défaut 0.5 seconde)
    private var shakeTimeMax = 2000L // Temps maximum pour que les deux secousses aient lieu (par défaut 2 secondes)
    private var lastTime: Long = 0
    private var shakeCount = 0
    private var goCircleGreen = false  // Flag pour vérifier si GO est déjà vert
    private var detectionEnabled = false

    private lateinit var statusText: TextView
    private lateinit var sensitivityValue: TextView
    private lateinit var sensitivitySlider: SeekBar
    private lateinit var timeValue: TextView
    private lateinit var timeSlider: SeekBar
    private lateinit var timeMaxValue: TextView
    private lateinit var timeMaxSlider: SeekBar
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
        timeMaxValue = findViewById(R.id.time_max_value)
        timeMaxSlider = findViewById(R.id.time_max_slider)
        goCircle = findViewById(R.id.go_circle)
        goText = findViewById(R.id.go_text)
        toggleDetectionButton = findViewById(R.id.toggle_detection_button)
        resetButton = findViewById(R.id.reset_button)
        closeButton = findViewById(R.id.close_button)

        // Initialiser les valeurs par défaut
        timeValue.text = getString(R.string.time_delay, shakeTimeWindow / 1000.0)
        timeMaxValue.text = getString(R.string.time_max_delay, shakeTimeMax / 1000.0)

        // Sensibilité du slider
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shakeThreshold = 15f + progress * 2f  // Ajustement de la sensibilité
                sensitivityValue.text = getString(R.string.sensitivity_level, progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Temps minimum entre secousses (millisecondes)
        timeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shakeTimeWindow = (200 + progress * 50).toLong() // Convertir la progression en millisecondes
                timeValue.text = getString(R.string.time_delay, shakeTimeWindow / 1000.0)
                if (shakeTimeWindow > shakeTimeMax) {
                    shakeTimeWindow = shakeTimeMax
                    timeSlider.progress = ((shakeTimeMax - 200) / 50).toInt()
                    timeValue.text = getString(R.string.time_delay, shakeTimeWindow / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Temps maximum pour que les deux secousses aient lieu (millisecondes)
        timeMaxSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shakeTimeMax = (500 + progress * 500).toLong() // Convertir la progression en millisecondes
                timeMaxValue.text = getString(R.string.time_max_delay, shakeTimeMax / 1000.0)
                if (shakeTimeMax < shakeTimeWindow) {
                    shakeTimeMax = shakeTimeWindow
                    timeMaxSlider.progress = ((shakeTimeMax - 500) / 500).toInt()
                    timeMaxValue.text = getString(R.string.time_max_delay, shakeTimeMax / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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
                    if (shakeCount == 1) {
                        // Première secousse détectée, GO devient orange
                        goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
                        statusText.text = getString(R.string.one_shake_detected)
                    } else if (shakeCount == 2 && !goCircleGreen && currentTime - lastTime < shakeTimeMax) {
                        // Deux secousses détectées, GO devient vert et reste vert
                        goCircle.setBackgroundColor(Color.GREEN)
                        goCircleGreen = true  // On empêche de remettre GO en rouge
                        statusText.text = getString(R.string.two_shakes_detected)
                    }
                } else {
                    shakeCount = 1
                    goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
                    statusText.text = getString(R.string.one_shake_detected)
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

    // Méthode pour démarrer/arrêter la détection
    @SuppressWarnings("unused")
    fun toggleDetection(view: View) {
        detectionEnabled = !detectionEnabled
        if (detectionEnabled) {
            toggleDetectionButton.text = getString(R.string.stop_detection)
            statusText.text = getString(R.string.detection_in_progress)
        } else {
            toggleDetectionButton.text = getString(R.string.start_detection)
            statusText.text = getString(R.string.detection_stopped)
        }
    }

    // Méthode pour réinitialiser la détection
    @SuppressWarnings("unused")
    fun resetDetection(view: View) {
        shakeCount = 0
        goCircle.setBackgroundColor(Color.RED)  // Remettre GO en rouge
        goCircleGreen = false  // Réinitialiser l'état du GO
        statusText.text = getString(R.string.status_waiting)
    }

    // Méthode pour fermer l'application
    @SuppressWarnings("unused")
    fun closeApp(view: View) {
        finish()  // Ferme l'application
    }
}