package com.example.locationtrackerservicetest.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.example.locationtrackerservicetest.service.GeofenceBroadcastReceiver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class LocationRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LocationRepository"
        @Volatile
        private var instance: LocationRepository? = null

        fun getInstance(context: Context): LocationRepository {
            return instance ?: synchronized(this) {
                instance ?: LocationRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val _locationFlow = MutableSharedFlow<Location>()
    val locationFlow = _locationFlow.asSharedFlow()

    // use according to need here all geofence status updated in this flow
    private val _geofenceStatusFlow = MutableStateFlow<GeofenceStatus>(GeofenceStatus.Idle)
    val geofenceStatusFlow = _geofenceStatusFlow.asStateFlow()

    suspend fun addGeofence(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Float = 100f,
        expirationDurationMillis: Long = Geofence.NEVER_EXPIRE
    ) {
        try {
            Log.i("LocationRepository", "Adding geofence at location: ($latitude, $longitude) with radius: $radiusInMeters meters")
            val geofence = Geofence.Builder()
                .setRequestId("GEOFENCE_ID")
                .setCircularRegion(latitude, longitude, radiusInMeters)
                .setExpirationDuration(expirationDurationMillis)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(30000) // 30 seconds delay for better accuracy
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence)
                .build()
            
            Log.d(TAG, "Creating geofence request with initial triggers: ENTER and EXIT")

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, GeofenceBroadcastReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            _geofenceStatusFlow.value = GeofenceStatus.Adding
            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()
            _geofenceStatusFlow.value = GeofenceStatus.Active
            Log.i("LocationRepository", "Geofence successfully added at ($latitude, $longitude)")

        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Security exception: ${e.message}")
            _geofenceStatusFlow.value = GeofenceStatus.Error(e.message ?: "Permission denied")
        } catch (e: Exception) {
            Log.e("LocationRepository", "Failed to add geofence: ${e.message}")
            _geofenceStatusFlow.value = GeofenceStatus.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun updateLocation(location: Location) {
        try {
            _locationFlow.emit(location)
            Log.d("LocationRepository", "Location updated: ${location.latitude}, ${location.longitude}")
        } catch (e: Exception) {
            Log.e("LocationRepository", "Failed to update location: ${e.message}")
        }
    }

    sealed class GeofenceStatus {
        object Idle : GeofenceStatus()
        object Adding : GeofenceStatus()
        object Active : GeofenceStatus()
        object Removed : GeofenceStatus()
        data class Error(val message: String) : GeofenceStatus()
    }

    suspend fun removeGeofences() {
        try {
            _geofenceStatusFlow.value = GeofenceStatus.Idle
            geofencingClient.removeGeofences(
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, GeofenceBroadcastReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            ).await()
            _geofenceStatusFlow.value = GeofenceStatus.Removed
            Log.i("LocationRepository", "Geofence successfully removed")
        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Security exception: ${e.message}")
            _geofenceStatusFlow.value = GeofenceStatus.Error(e.message ?: "Permission denied")
        } catch (e: Exception) {
            Log.e("LocationRepository", "Failed to remove geofence: ${e.message}")
            _geofenceStatusFlow.value = GeofenceStatus.Error(e.message ?: "Unknown error")
        }
    }
}