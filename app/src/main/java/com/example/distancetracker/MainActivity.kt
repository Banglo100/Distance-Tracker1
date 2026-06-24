package com.example.distancetracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var distanceTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var isTracking = false
    private var totalDistanceInMeters = 0.0
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        distanceTextView = findViewById(R.id.distanceTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        startButton.setOnClickListener { startTracking() }
        stopButton.setOnClickListener { stopTracking() }
        
        updateDisplay()
    }

    private fun startTracking() {
        if (isTracking) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        isTracking = true
        totalDistanceInMeters = 0.0
        lastLocation = null
        updateDisplay()

        // CRITICAL FIX: Force hardware GPS provider, request updates every 2 seconds, 
        // and ignore movements less than 5 meters to filter out indoor drift/glitches.
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000L, 
            5f, 
            this
        )
        
        Toast.makeText(this, "Tracking Started", Short.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        locationManager.removeUpdates(this)
        Toast.makeText(this, "Tracking Stopped", Short.LENGTH_SHORT).show()
    }

    override fun onLocationChanged(location: Location) {
        // Only calculate distance if the GPS signal is highly accurate (under 15 meters margin of error)
        if (location.accuracy > 15) return

        if (lastLocation != null) {
            val distanceSegment = lastLocation!!.distanceTo(location)
            totalDistanceInMeters += distanceSegment
            updateDisplay()
        }
        lastLocation = location
    }

    private fun updateDisplay() {
        val distanceInKm = totalDistanceInMeters / 1000.0
        distanceTextView.text = String.format(Locale.getDefault(), "%.2f km", distanceInKm)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            locationManager.removeUpdates(this)
        }
    }
}
