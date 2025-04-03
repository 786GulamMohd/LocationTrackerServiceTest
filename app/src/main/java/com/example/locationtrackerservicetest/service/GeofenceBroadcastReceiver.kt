package com.example.locationtrackerservicetest.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.locationtrackerservicetest.R
import com.example.locationtrackerservicetest.util.GeofencePreferences
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "geofence_channel"
        private const val CHANNEL_NAME = "Geofence Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for geofence transitions"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        Log.i(TAG, "GeofenceBroadcastReceiver registered and received event at $formattedTimestamp")
        Log.d(TAG, "Received geofence event with action: ${intent.action}")
        
        // Create notification channel for Android O and above
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(true)
            enableVibration(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null. Intent extras: ${intent.extras}")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage (code: ${geofencingEvent.errorCode})")
            showNotification(context, "Geofence Error", "Error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.e(TAG, "No triggering geofences found. Transition type: ${geofencingEvent.geofenceTransition}")
            return
        }

        // Log triggering geofences details
        triggeringGeofences.forEach { geofence ->
            Log.d(TAG, "Triggered geofence ID: ${geofence.requestId}")
        }

        // Get the transition location
        val location = geofencingEvent.triggeringLocation
        if (location == null) {
            Log.e(TAG, "Location is null")
            return
        }
        val timestampMillis = System.currentTimeMillis()
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))

        // Get current geofence data for comparison
        val currentGeofence = GeofencePreferences.getInstance(context).getGeofenceData()
        val geofenceInfo = if (currentGeofence != null) {
            "Current geofence: (${currentGeofence.latitude}, ${currentGeofence.longitude}, radius: ${currentGeofence.radius}m)"
        } else {
            "No active geofence data found"
        }

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.i(TAG, "Geofence ENTER at $dateTime\n" +
                    "Location: (${location.latitude}, ${location.longitude})\n" +
                    "Accuracy: ${location.accuracy}m\n" +
                    geofenceInfo)
                showNotification(context, "Entered monitored area", "You have entered the geofence area")
                // Update preferences to mark as inside geofence
                GeofencePreferences.getInstance(context).saveGeofenceState(true)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.i(TAG, "Geofence EXIT at $dateTime\n" +
                    "Location: (${location.latitude}, ${location.longitude})\n" +
                    "Accuracy: ${location.accuracy}m\n" +
                    geofenceInfo)
                showNotification(context, "Left monitored area", "You have left the geofence area")
                // Update preferences to mark as outside geofence
                GeofencePreferences.getInstance(context).saveGeofenceState(false)
            }
            else -> {
                Log.e(TAG, "Invalid geofence transition type: $geofenceTransition at $dateTime")
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}