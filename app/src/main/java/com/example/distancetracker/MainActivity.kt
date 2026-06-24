package com.example.distancetracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvDistance: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnViewLog: Button

    private val PERMISSION_REQUEST_CODE = 100

    private val distanceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val distanceInMeters = intent?.getFloatExtra(TrackingService.EXTRA_DISTANCE, 0f) ?: 0f
            updateDistanceText(distanceInMeters)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDistance = findViewById(R.id.tvDistance)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnViewLog = findViewById(R.id.btnViewLog)

        btnStart.setOnClickListener {
            if (checkPermissions()) {
                startTracking()
            } else {
                requestPermissions()
            }
        }

        btnStop.setOnClickListener {
            stopTracking()
        }

        btnViewLog.setOnClickListener {
            showLogs()
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            distanceReceiver,
            IntentFilter(TrackingService.ACTION_UPDATE_DISTANCE)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(distanceReceiver)
    }

    private fun checkPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fineLocation && postNotifications
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking()
            }
        }
    }

    private fun startTracking() {
        btnStart.isEnabled = false
        btnStop.isEnabled = true
        tvDistance.text = getString(R.string.distance_label)

        val serviceIntent = Intent(this, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopTracking() {
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        
        val serviceIntent = Intent(this, TrackingService::class.java)
        stopService(serviceIntent)
    }

    private fun updateDistanceText(distanceInMeters: Float) {
        val km = distanceInMeters / 1000f
        tvDistance.text = String.format(Locale.getDefault(), "Distance: %.2f km", km)
    }

    private fun showLogs() {
        val sharedPreferences = getSharedPreferences("DistanceLogs", Context.MODE_PRIVATE)
        val logs = sharedPreferences.getString("logs", "No logs available.")

        AlertDialog.Builder(this)
            .setTitle("Trip Logs")
            .setMessage(logs)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Clear") { dialog, _ ->
                sharedPreferences.edit().remove("logs").apply()
                dialog.dismiss()
            }
            .show()
    }
}
