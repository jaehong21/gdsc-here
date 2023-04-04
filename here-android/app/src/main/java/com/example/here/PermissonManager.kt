package com.example.here

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

class PermissionManager(private val activity: Activity) {
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 316
        private val PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    fun checkPermissions() {
        if (hasAllPermissions().not()) {
            if (shouldShowRequestPermissionRationale()) {
                Toast.makeText(activity, "앱 실행을 위해서는 권한을 설정해야 합니다.", Toast.LENGTH_SHORT).show()
                requestPermissions()
            } else {
                requestPermissions()
            }
        }
    }

    fun hasAllPermissions(): Boolean {
        return PERMISSIONS.all { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
        return PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }
}
