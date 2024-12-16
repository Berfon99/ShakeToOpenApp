package com.example.shaketoopen

import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var viewModel: ShakeDetectionViewModel

    private lateinit var statusText: TextView
    private lateinit var sensitivityValue: TextView
    private lateinit var sensitivitySlider: SeekBar
    private lateinit var timeValue: TextView
    private lateinit var timeSlider: SeekBar
    private lateinit var timeMaxValue: TextView
    private lateinit var timeMaxSlider: SeekBar
    private lateinit var goCircle: FrameLayout
    private lateinit var goText: TextView
    private lateinit var launchXCTrackButton: Button
    private lateinit var toggleShakeToXCTrackButton: Button
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(ShakeDetectionViewModel::class.java)

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
        launchXCTrackButton = findViewById(R.id.launch_xctrack_button)
        toggleShakeToXCTrackButton = findViewById(R.id.toggle_shake_to_xctrack_button)
        closeButton = findViewById(R.id.close_button)

        // Initialiser les valeurs par défaut
        sensitivitySlider.progress = 3 // Définir la sensibilité par défaut à 3
        timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
        timeMaxValue.text = getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)

        // Sensibilité du slider
        sensitivitySlider.max = 4 // Set the maximum value to 4 for 5 levels
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Ajustement de la sensibilité de manière inversement proportionnelle
                // New mapping: progress 0 -> threshold 30, progress 4 -> threshold 15
                viewModel.shakeThreshold = 30f - (progress * 3.75f)  // Adjust the multiplier to spread out the threshold values
                sensitivityValue.text = getString(R.string.sensitivity_level, progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Temps minimum entre secousses (millisecondes)
        timeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeWindow = (200 + progress * 50).toLong() // Convertir la progression en millisecondes
                timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                if (viewModel.shakeTimeWindow > viewModel.shakeTimeMax) {
                    viewModel.shakeTimeWindow = viewModel.shakeTimeMax
                    timeSlider.progress = ((viewModel.shakeTimeMax - 200) / 50).toInt()
                    timeValue.text = getString(R.string.time_delay, viewModel.shakeTimeWindow / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Temps maximum pour que les deux secousses aient lieu (millisecondes)
        timeMaxSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.shakeTimeMax = (500 + progress * 500).toLong() // Convertir la progression en millisecondes
                timeMaxValue.text = getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)
                if (viewModel.shakeTimeMax < viewModel.shakeTimeWindow) {
                    viewModel.shakeTimeMax = viewModel.shakeTimeWindow
                    timeMaxSlider.progress = ((viewModel.shakeTimeMax - 500) / 500).toInt()
                    timeMaxValue.text = getString(R.string.time_max_delay, viewModel.shakeTimeMax / 1000.0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Mettre à jour le texte de statut pour indiquer que la détection est en cours
        statusText.text = getString(R.string.detection_in_progress)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("shakeToXCTrackEnabled", viewModel.shakeToXCTrackEnabled)
        outState.putInt("shakeCount", viewModel.shakeCount)
        outState.putBoolean("goCircleGreen", viewModel.goCircleGreen)
        outState.putLong("lastTime", viewModel.lastTime)
        outState.putLong("firstShakeTime", viewModel.firstShakeTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.shakeToXCTrackEnabled = savedInstanceState.getBoolean("shakeToXCTrackEnabled")
        viewModel.shakeCount = savedInstanceState.getInt("shakeCount")
        viewModel.goCircleGreen = savedInstanceState.getBoolean("goCircleGreen")
        viewModel.lastTime = savedInstanceState.getLong("lastTime")
        viewModel.firstShakeTime = savedInstanceState.getLong("firstShakeTime")

        // Mettre à jour l'état du bouton et du cercle GO
        toggleShakeToXCTrackButton.text = if (viewModel.shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }

        updateGoCircleColor()
    }

    // Méthode pour lancer XCTrack
    fun launchXCTrack(view: View?) {
        val packageManager = packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")

        if (launchIntent != null) {
            // XCTrack est installé
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningAppProcesses = activityManager.runningAppProcesses
            val isXCTrackRunning = runningAppProcesses.any { it.processName == "org.xcontest.XCTrack" }

            if (isXCTrackRunning) {
                // XCTrack est en cours d'exécution, le mettre en avant-plan
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            } else {
                // Ajouter des flags pour lancer une nouvelle tâche
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to launch XCTrack", e)
                Toast.makeText(this, "Échec du lancement de XCTrack", Toast.LENGTH_SHORT).show()
            }
        } else {
            // XCTrack n'est pas installé
            Toast.makeText(this, "XCTrack non installé", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!viewModel.detectionEnabled) return

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            // Vérification de l'intensité de la secousse
            if (magnitude > viewModel.shakeThreshold) {
                if (viewModel.shakeCount == 0) {
                    // Première secousse détectée, GO devient orange
                    viewModel.shakeCount = 1
                    goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
                    statusText.text = getString(R.string.one_shake_detected)
                    viewModel.firstShakeTime = currentTime // Enregistrer le temps de la première secousse
                } else if (viewModel.shakeCount == 1 && currentTime - viewModel.firstShakeTime > viewModel.shakeTimeWindow && currentTime - viewModel.firstShakeTime < viewModel.shakeTimeMax) {
                    // Deux secousses détectées, GO devient vert et reste vert
                    viewModel.shakeCount = 2
                    goCircle.setBackgroundColor(Color.GREEN)
                    viewModel.goCircleGreen = true  // On empêche de remettre GO en rouge
                    statusText.text = getString(R.string.two_shakes_detected)
                    if (viewModel.shakeToXCTrackEnabled) {
                        launchXCTrack(null)  // Lancer XCTrack si le mode "Secousses to XCTrack" est activé
                    }
                }
            } else if (currentTime - viewModel.firstShakeTime > viewModel.shakeTimeMax) {
                // Réinitialiser le compteur de secousses si la seconde secousse n'arrive pas dans le délai maximum
                viewModel.shakeCount = 0
                goCircle.setBackgroundColor(Color.RED)  // Remettre GO en rouge
                viewModel.goCircleGreen = false  // Réinitialiser l'état du GO
                statusText.text = getString(R.string.status_waiting)
            }
            viewModel.lastTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    // Méthode pour activer/désactiver le mode "Secousses to XCTrack"
    @SuppressWarnings("unused")
    fun toggleShakeToXCTrack(view: View) {
        viewModel.shakeToXCTrackEnabled = !viewModel.shakeToXCTrackEnabled
        toggleShakeToXCTrackButton.text = if (viewModel.shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }
    }

    // Méthode pour fermer l'application
    @SuppressWarnings("unused")
    fun closeApp(view: View) {
        finish()  // Ferme l'application
    }

    private fun updateGoCircleColor() {
        if (viewModel.goCircleGreen) {
            goCircle.setBackgroundColor(Color.GREEN)
        } else if (viewModel.shakeCount == 1) {
            goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
        } else {
            goCircle.setBackgroundColor(Color.RED)  // Rouge
        }
    }
}