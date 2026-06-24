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
    private var distanceTextView: TextView? = null
    private var startButton: Button? = null
    private var stopButton: Button? = null

    private var isTracking = false
    private var totalDistanceInMeters = 0.0
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Using safe finding handles in case IDs in activity_main.xml are capitalized or named slightly differently
        distanceTextView = findViewById(resources.getIdentifier("distanceTextView", "id", packageName))
            ?: findViewById(resources.getIdentifier("distance_text", "id", packageName))
        
        startButton = findViewById(resources.getIdentifier("startButton", "id", packageName))
            ?: findViewById(resources.getIdentifier("start_button", "id", packageName))
            
        stopButton = findViewById(resources.getIdentifier("stopButton", "id", packageName))
            ?: findViewById(resources.getIdentifier("stop_button", "id", packageName))
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        startButton?.setOnClickListener { startTracking() }
        stopButton?.setOnClickListener { stopTracking() }
        
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

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000L, 
            5f, 
            this
        )
        
        Toast.makeText(this, "Tracking Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        locationManager.removeUpdates(this)
        Toast.makeText(this, "Tracking Stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onLocationChanged(location: Location) {
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
        val textString = String.format(Locale.getDefault(), "%.2f km", distanceInKm)
        distanceTextView?.text = textString
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            locationManager.removeUpdates(this)
        }
    }
}
