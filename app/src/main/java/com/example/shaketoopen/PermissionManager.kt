package com.example.shaketoopen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    private val locationPermissionRequestCode = 100
    private val backgroundLocationRequestCode = 101
    private val tag = "PermissionManager"
    private var onPermissionsGrantedCallback: (() -> Unit)? = null

    fun checkAndRequestPermissions(onPermissionsGranted: () -> Unit) {
        Log.d(tag, "checkAndRequestPermissions() called")
        onPermissionsGrantedCallback = onPermissionsGranted

        val permissionsToRequest = mutableListOf<String>()

        // Check location permissions
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Check for sensor permissions
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
        }

        // Add wake lock permission if needed
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WAKE_LOCK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.WAKE_LOCK)
        }

        if (permissionsToRequest.isEmpty()) {
            // All permissions granted, check for background location
            checkBackgroundLocationPermission(onPermissionsGranted)
        } else {
            // Request permissions
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                locationPermissionRequestCode
            )
        }
    }

    private fun checkBackgroundLocationPermission(onPermissionsGranted: () -> Unit) {
        // Check for background location permission on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request background location permission
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    backgroundLocationRequestCode
                )
            } else {
                // Background location already granted
                onPermissionsGranted()
            }
        } else {
            // Background location not needed on older Android versions
            onPermissionsGranted()
        }
    }

    fun requestAppSettings() {
        Log.d(tag, "Opening app settings")
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        Log.d(tag, "onRequestPermissionsResult() called for code: $requestCode")
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(tag, "Basic permissions granted")
                // Now check for background location
                checkBackgroundLocationPermission {
                    onPermissionsGrantedCallback?.invoke()
                }
            } else {
                Log.d(tag, "Basic permissions denied")
                // Consider showing a dialog explaining why permissions are needed
            }
        } else if (requestCode == backgroundLocationRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "Background location permission granted")
                onPermissionsGrantedCallback?.invoke()
            } else {
                Log.d(tag, "Background location permission denied")
                // App can still work without background location, but with limitations
                onPermissionsGrantedCallback?.invoke()
            }
        }
    }
}