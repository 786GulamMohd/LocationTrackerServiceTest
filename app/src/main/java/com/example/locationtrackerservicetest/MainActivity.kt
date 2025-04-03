package com.example.locationtrackerservicetest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.locationtrackerservicetest.service.LocationService
import com.example.locationtrackerservicetest.viewmodel.LocationViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: LocationViewModel by viewModels()
    private lateinit var locationText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                requestBackgroundLocationPermission()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted
                requestBackgroundLocationPermission()
            }

            else -> {
                // No location access granted
                Toast.makeText(
                    this,
                    "Precise location permission is required for background location tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val backgroundLocationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkNotificationPermission()
        } else {
            Toast.makeText(
                this,
                "Background location permission is required for continuous tracking",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startLocationService()
            viewModel.startLocationTracking()
        } else {
            Toast.makeText(
                this,
                "Notification permission is required for background service",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private lateinit var geofenceStatusText: TextView
    private lateinit var addGeofenceButton: Button
    private lateinit var clearGeofenceButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        locationText = findViewById(R.id.locationText)
        geofenceStatusText = findViewById(R.id.geofenceStatusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        addGeofenceButton = findViewById(R.id.addGeofenceButton)
        clearGeofenceButton = findViewById(R.id.clearGeofenceButton)

        setupButtons()
        checkLocationPermission()
        observeLocation()
        observeGeofenceStatus()
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            if (checkPermissions()) {
                startLocationService()
                viewModel.startLocationTracking()
            } else {
                checkLocationPermission()
            }
        }

        stopButton.setOnClickListener {
            stopLocationService()
            viewModel.stopLocationTracking()
            viewModel.removeGeofence()
        }

        addGeofenceButton.setOnClickListener {
            viewModel.addCurrentLocationGeofence()
            Toast.makeText(this, "Geofence added at current location", Toast.LENGTH_SHORT).show()
        }

        clearGeofenceButton.setOnClickListener {
            viewModel.removeGeofence()
            Toast.makeText(this, "Geofence cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeGeofenceStatus() {
        viewModel.isInsideGeofence.observe(this) { isInside ->
            geofenceStatusText.text = if (isInside == null) "Geofence Status: Not active" else "Geofence Status: ${if (isInside) "Inside" else "Outside"}"
        }
    }

    private fun observeLocation() {
        viewModel.location.observe(this, Observer { location ->
            location?.let {
                val timestamp =
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(it.time))
                locationText.text =
                    "Location at $timestamp\nLatitude: ${it.latitude}\nLongitude: ${it.longitude}\nAltitude: ${if (it.hasAltitude()) it.altitude else "N/A"}\nAccuracy: ${if (it.hasAccuracy()) it.accuracy else "N/A"}"
            }
        })
    }

    private fun checkPermissions(): Boolean {
        val hasLocationPermission =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasBackgroundPermission =
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        return hasLocationPermission && hasBackgroundPermission && hasNotificationPermission
    }

    private fun checkLocationPermission() {
        when {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                requestBackgroundLocationPermission()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Location permission is required for tracking",
                    Toast.LENGTH_LONG
                ).show()
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        when {
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                checkNotificationPermission()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Background location permission is required for continuous tracking",
                    Toast.LENGTH_LONG
                ).show()
                backgroundLocationPermissionRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            else -> {
                backgroundLocationPermissionRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    startLocationService()
                    viewModel.startLocationTracking()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        "Notification permission is required for background service",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startLocationService()
            viewModel.startLocationTracking()
        }
    }

    private fun startLocationService() {
        startForegroundService(Intent(this, LocationService::class.java))
    }

    private fun stopLocationService() {
        stopService(Intent(this, LocationService::class.java))
    }
}