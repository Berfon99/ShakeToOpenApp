package com.example.shaketoopen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    private val locationPermissionRequestCode = 100
    private val tag = "PermissionManager"
    private var onPermissionsGrantedCallback: (() -> Unit)? = null

    fun checkAndRequestPermissions(onPermissionsGranted: () -> Unit) {
        Log.d(tag, "checkAndRequestPermissions() called")
        onPermissionsGrantedCallback = onPermissionsGranted
        if (checkPermissions()) {
            onPermissionsGranted()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d(tag, "checkPermissions() called")
        val locationPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return locationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        Log.d(tag, "requestPermissions() called")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionRequestCode
        )
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        Log.d(tag, "onRequestPermissionsResult() called")
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "Permissions granted")
                onPermissionsGrantedCallback?.invoke()
            } else {
                Log.d(tag, "Permissions denied")
            }
        }
    }
}