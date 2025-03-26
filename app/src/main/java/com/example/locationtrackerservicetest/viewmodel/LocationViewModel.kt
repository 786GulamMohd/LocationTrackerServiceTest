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

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationRepository = LocationRepository.getInstance(application)
    private val _location = MutableLiveData<android.location.Location?>()
    val location: LiveData<android.location.Location?> = _location

    private fun getLocationOnChange() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                    _location.postValue(location)
                }
        }
    }

    fun startLocationTracking() {
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().startForegroundService(intent)
        getLocationOnChange()
    }

    fun stopLocationTracking() {
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().stopService(intent)
    }

    // Add Geofencing Location according to need
    fun addGeofence(latitude: Double, longitude: Double, radiusInMeters: Float) {
        viewModelScope.launch {
            locationRepository.addGeofence(latitude, longitude, radiusInMeters)
        }
    }

    // remove Geofencing when don't use
    fun removeGeofence() {
        viewModelScope.launch {
            locationRepository.removeGeofences()
        }
    }
}