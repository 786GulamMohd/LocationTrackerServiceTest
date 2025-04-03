package com.example.locationtrackerservicetest.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.locationtrackerservicetest.repository.LocationRepository
import com.example.locationtrackerservicetest.service.LocationService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import com.example.locationtrackerservicetest.util.GeofencePreferences

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationRepository = LocationRepository.getInstance(application)
    private val geofencePrefs = GeofencePreferences.getInstance(application)
    private val _location = MutableLiveData<android.location.Location?>()
    val location: LiveData<android.location.Location?> = _location
    private val _isInsideGeofence = MutableLiveData<Boolean>()
    val isInsideGeofence: LiveData<Boolean> = _isInsideGeofence

    init {
        _isInsideGeofence.value = geofencePrefs.isInsideGeofence()
    }

    private fun getLocationOnChange() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                _location.postValue(location)
            }
        }
    }

    private fun observeGeofenceState() {
        viewModelScope.launch {
            _isInsideGeofence.postValue(geofencePrefs.isInsideGeofence())
            // Observe geofence state changes from GeofenceBroadcastReceiver
            geofencePrefs.observeGeofenceState().collect { isInside ->
                _isInsideGeofence.postValue(isInside)
            }
        }
    }

    fun startLocationTracking() {
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().startForegroundService(intent)
        getLocationOnChange()
        observeGeofenceState()
    }

    fun stopLocationTracking() {
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().stopService(intent)
    }

    // Add Geofencing Location according to need
    fun addGeofence(latitude: Double, longitude: Double, radiusInMeters: Float) {
        viewModelScope.launch {
            locationRepository.addGeofence(latitude, longitude, radiusInMeters)
            geofencePrefs.saveGeofence(latitude, longitude, radiusInMeters)
            _isInsideGeofence.value = geofencePrefs.isInsideGeofence()
        }
    }

    // remove Geofencing when don't use
    fun removeGeofence() {
        viewModelScope.launch {
            locationRepository.removeGeofences()
            geofencePrefs.clearGeofence()
            _isInsideGeofence.value = false
        }
    }

    fun restoreGeofence() {
        viewModelScope.launch {
            geofencePrefs.getGeofenceData()?.let { data ->
                locationRepository.addGeofence(data.latitude, data.longitude, data.radius)
                _isInsideGeofence.value = geofencePrefs.isInsideGeofence()
            }
        }
    }

    fun addCurrentLocationGeofence(radiusInMeters: Float = 1000f) {
        viewModelScope.launch {
            location.value?.let { currentLocation ->
                // Log current location details
                android.util.Log.i("LocationViewModel", "Adding geofence at current location: (${currentLocation.latitude}, ${currentLocation.longitude}), accuracy: ${currentLocation.accuracy}m")
                
                // Only add geofence if location accuracy is good enough
                if (currentLocation.accuracy <= radiusInMeters / 2) {
                    locationRepository.addGeofence(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        radiusInMeters
                    )
                    geofencePrefs.saveGeofence(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        radiusInMeters
                    )
                    // Set initial state to inside since we're adding at current location
                    android.util.Log.i("LocationViewModel", "Geofence added successfully with initial state: inside")
                } else {
                    android.util.Log.w("LocationViewModel", "Location accuracy (${currentLocation.accuracy}m) is not good enough for geofence radius ${radiusInMeters}m")
                }
            } ?: run {
                android.util.Log.e("LocationViewModel", "Cannot add geofence: current location is null")
            }
        }
    }
}