package com.example.shaketoopen

import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 15.0f
    private var shakeTimeWindow = 500L // Temps minimum entre secousses (par défaut 0.5 seconde)
    private var shakeTimeMax = 2000L // Temps maximum pour que les deux secousses aient lieu (par défaut 2 secondes)
    private var lastTime: Long = 0
    private var shakeCount = 0
    private var goCircleGreen = false  // Flag pour vérifier si GO est déjà vert
    private var detectionEnabled = true  // Activer la détection de secousses dès le lancement
    private var shakeToXCTrackEnabled = false  // Flag pour vérifier si le mode "Secousses to XCTrack" est activé
    private var firstShakeTime: Long = 0 // Temps de la première secousse

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
        timeValue.text = getString(R.string.time_delay, shakeTimeWindow / 1000.0)
        timeMaxValue.text = getString(R.string.time_max_delay, shakeTimeMax / 1000.0)

        // Sensibilité du slider
        sensitivitySlider.max = 4 // Set the maximum value to 4 for 5 levels
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Ajustement de la sensibilité de manière inversement proportionnelle
                // New mapping: progress 0 -> threshold 30, progress 4 -> threshold 15
                shakeThreshold = 30f - (progress * 3.75f)  // Adjust the multiplier to spread out the threshold values
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

        // Mettre à jour le texte de statut pour indiquer que la détection est en cours
        statusText.text = getString(R.string.detection_in_progress)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("shakeToXCTrackEnabled", shakeToXCTrackEnabled)
        outState.putInt("shakeCount", shakeCount)
        outState.putBoolean("goCircleGreen", goCircleGreen)
        outState.putLong("lastTime", lastTime)
        outState.putLong("firstShakeTime", firstShakeTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        shakeToXCTrackEnabled = savedInstanceState.getBoolean("shakeToXCTrackEnabled")
        shakeCount = savedInstanceState.getInt("shakeCount")
        goCircleGreen = savedInstanceState.getBoolean("goCircleGreen")
        lastTime = savedInstanceState.getLong("lastTime")
        firstShakeTime = savedInstanceState.getLong("firstShakeTime")

        // Mettre à jour l'état du bouton et du cercle GO
        toggleShakeToXCTrackButton.text = if (shakeToXCTrackEnabled) {
            getString(R.string.disable_shake_to_xctrack)
        } else {
            getString(R.string.enable_shake_to_xctrack)
        }

        if (goCircleGreen) {
            goCircle.setBackgroundColor(Color.GREEN)
        } else if (shakeCount == 1) {
            goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
        } else {
            goCircle.setBackgroundColor(Color.RED)  // Rouge
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!detectionEnabled) return

        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val currentTime = System.currentTimeMillis()
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Vérification de l'intensité de la secousse
            if (magnitude > shakeThreshold) {
                if (shakeCount == 0) {
                    // Première secousse détectée, GO devient orange
                    shakeCount = 1
                    goCircle.setBackgroundColor(Color.parseColor("#FFA500")) // Orange
                    statusText.text = getString(R.string.one_shake_detected)
                    firstShakeTime = currentTime // Enregistrer le temps de la première secousse
                } else if (shakeCount == 1 && currentTime - firstShakeTime > shakeTimeWindow && currentTime - firstShakeTime < shakeTimeMax) {
                    // Deux secousses détectées, GO devient vert et reste vert
                    shakeCount = 2
                    goCircle.setBackgroundColor(Color.GREEN)
                    goCircleGreen = true  // On empêche de remettre GO en rouge
                    statusText.text = getString(R.string.two_shakes_detected)
                    if (shakeToXCTrackEnabled) {
                        launchXCTrack(null)  // Lancer XCTrack si le mode "Secousses to XCTrack" est activé
                    }
                }
            } else if (currentTime - firstShakeTime > shakeTimeMax) {
                // Réinitialiser le compteur de secousses si la seconde secousse n'arrive pas dans le délai maximum
                shakeCount = 0
                goCircle.setBackgroundColor(Color.RED)  // Remettre GO en rouge
                goCircleGreen = false  // Réinitialiser l'état du GO
                statusText.text = getString(R.string.status_waiting)
            }
            lastTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    // Méthode pour lancer XCTrack
    @SuppressWarnings("unused")
    fun launchXCTrack(view: View?) {
        val packageManager = packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage("org.xcontest.XCTrack")

        if (launchIntent != null) {
            // XCTrack est installé
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningTasks = activityManager.getRunningTasks(1)
            val topActivity = runningTasks[0].topActivity
            if (topActivity?.packageName == "org.xcontest.XCTrack") {
                // XCTrack est en cours d'exécution, le mettre en avant-plan
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(launchIntent)
        } else {
            // XCTrack n'est pas installé
            Toast.makeText(this, "XCTrack non installé", Toast.LENGTH_SHORT).show()
        }
    }

    // Méthode pour activer/désactiver le mode "Secousses to XCTrack"
    @SuppressWarnings("unused")
    fun toggleShakeToXCTrack(view: View) {
        shakeToXCTrackEnabled = !shakeToXCTrackEnabled
        if (shakeToXCTrackEnabled) {
            toggleShakeToXCTrackButton.text = getString(R.string.disable_shake_to_xctrack)
        } else {
            toggleShakeToXCTrackButton.text = getString(R.string.enable_shake_to_xctrack)
        }
    }

    // Méthode pour fermer l'application
    @SuppressWarnings("unused")
    fun closeApp(view: View) {
        finish()  // Ferme l'application
    }
}
