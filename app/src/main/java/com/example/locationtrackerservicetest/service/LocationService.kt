package com.example.locationtrackerservicetest.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.locationtrackerservicetest.MainActivity
import com.example.locationtrackerservicetest.R
import com.example.locationtrackerservicetest.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: LocationRepository
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val LOCATION_UPDATE_INTERVAL = 30000L // 30 seconds for better battery life
    }

    override fun onCreate() {
        super.onCreate()
        repository = LocationRepository.getInstance(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationCallback()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    serviceScope.launch {
                        repository.updateLocation(location)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                .setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL * 2)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e("LocationService", "Failed to request location updates: ${e.message}")
            }
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission not granted: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Location Tracking Active")
        .setContentText("Tracking your location in background")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}