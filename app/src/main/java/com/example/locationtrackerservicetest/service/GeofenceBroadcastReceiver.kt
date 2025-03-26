package com.example.locationtrackerservicetest.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.i(TAG, "Entered geofence")
                // Handle enter transition
                // You can send a local broadcast, update UI, or trigger a notification
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.i(TAG, "Exited geofence")
                // Handle exit transition
            }
            else -> {
                Log.e(TAG, "Invalid geofence transition type: $geofenceTransition")
            }
        }
    }
}