package com.example.shaketoopen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val TAG = "PermissionManager"

    fun checkAndRequestPermissions(onPermissionsGranted: () -> Unit, onPermissionsDenied: () -> Unit) {
        Log.d(TAG, "checkAndRequestPermissions() called")
        if (checkPermissions()) {
            onPermissionsGranted()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d(TAG, "checkPermissions() called")
        val locationPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return locationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        Log.d(TAG, "requestPermissions() called")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult() called")
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions granted")
            } else {
                Log.d(TAG, "Permissions denied")
            }
        }
    }
}