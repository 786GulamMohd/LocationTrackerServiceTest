package com.example.locationtrackerservicetest.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GeofencePreferences private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "geofence_prefs"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_RADIUS = "radius"
        private const val KEY_ACTIVE = "active"
        private const val KEY_INSIDE_GEOFENCE = "inside_geofence"

        @Volatile
        private var instance: GeofencePreferences? = null

        fun getInstance(context: Context): GeofencePreferences {
            return instance ?: synchronized(this) {
                instance ?: GeofencePreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    @Synchronized
    fun saveGeofence(latitude: Double, longitude: Double, radius: Float) {
        prefs.edit().apply {
            putString(KEY_LATITUDE, latitude.toString())
            putString(KEY_LONGITUDE, longitude.toString())
            putFloat(KEY_RADIUS, radius)
            putBoolean(KEY_ACTIVE, true)
            apply()
        }
    }

    @Synchronized
    fun clearGeofence() {
        prefs.edit().clear().apply()
    }

    private val _geofenceStateFlow = MutableStateFlow<Boolean>(prefs.getBoolean(KEY_INSIDE_GEOFENCE, false))

    init {
        // Initialize the StateFlow with the current geofence state from SharedPreferences
        _geofenceStateFlow.value = prefs.getBoolean(KEY_INSIDE_GEOFENCE, false)
    }

    @Synchronized
    fun saveGeofenceState(isInside: Boolean) {
        android.util.Log.d("GeofencePreferences", "Updating geofence state to: $isInside")
        prefs.edit(commit = true) { putBoolean(KEY_INSIDE_GEOFENCE, isInside) }
        _geofenceStateFlow.value = isInside
        android.util.Log.d("GeofencePreferences", "StateFlow updated to: ${_geofenceStateFlow.value}")
    }

    fun observeGeofenceState(): StateFlow<Boolean> = _geofenceStateFlow.asStateFlow()

    @Synchronized
    fun isInsideGeofence(): Boolean {
        return prefs.getBoolean(KEY_INSIDE_GEOFENCE, false)
    }

    @Synchronized
    fun getGeofenceData(): GeofenceData? {
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return null

        val latitude = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull()
        val longitude = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull()
        val radius = prefs.getFloat(KEY_RADIUS, 1000f)

        return if (latitude != null && longitude != null && radius > 0) {
            GeofenceData(latitude, longitude, radius)
        } else null
    }

    data class GeofenceData(
        val latitude: Double,
        val longitude: Double,
        val radius: Float
    )
}