package com.example.goodstart.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.goodstart.geofence.GeofenceReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.location.Location
import androidx.core.content.ContextCompat
import com.example.goodstart.geofence.distanceBetween

data class SilentZone(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float = 100f,
    val active: Boolean = true
)

data class LocationZoneState(
    val zones: List<SilentZone> = emptyList(),
    val hasDndPermission: Boolean = false,
    val hasLocationPermission: Boolean = false
)

class LocationZoneViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val prefs = ctx.getSharedPreferences("LocationZones", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val geofencingClient = LocationServices.getGeofencingClient(ctx)

    private val _state = MutableStateFlow(LocationZoneState())
    val state = _state.asStateFlow()

    init { loadZones() }

    fun updatePermissions(hasDnd: Boolean, hasLocation: Boolean) {
        _state.value = _state.value.copy(hasDndPermission = hasDnd, hasLocationPermission = hasLocation)
    }

    fun addZone(name: String, latLng: LatLng, radiusMeters: Float) {
        val zone = SilentZone(
            id = System.currentTimeMillis().toString(),
            name = name,
            lat = latLng.latitude,
            lng = latLng.longitude,
            radiusMeters = radiusMeters
        )
        val updated = _state.value.zones + zone
        saveZones(updated)
        _state.value = _state.value.copy(zones = updated)
        
        val active = updated.filter { it.active }
        registerGeofences(active)
        checkCurrentLocationAgainstZones(active)
    }

    fun removeZone(id: String) {
        val updated = _state.value.zones.filter { it.id != id }
        saveZones(updated)
        _state.value = _state.value.copy(zones = updated)
        geofencingClient.removeGeofences(listOf(id))
    }

    fun toggleZone(id: String) {
        val updated = _state.value.zones.map {
            if (it.id == id) it.copy(active = !it.active) else it
        }
        saveZones(updated)
        _state.value = _state.value.copy(zones = updated)
        
        val active = updated.filter { it.active }
        val allIds = updated.map { it.id }
        
        geofencingClient.removeGeofences(allIds).addOnCompleteListener {
            if (active.isNotEmpty()) registerGeofences(active)
        }
    }

    fun reregisterAll() {
        val active = _state.value.zones.filter { it.active }
        if (active.isNotEmpty()) {
            registerGeofences(active)
            checkCurrentLocationAgainstZones(active)
        }
    }

    private fun checkCurrentLocationAgainstZones(zones: List<SilentZone>) {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val inZone = zones.any { z -> 
                        distanceBetween(loc.latitude, loc.longitude, z.lat, z.lng) <= z.radiusMeters
                    }
                    if (inZone) {
                        Log.d("GeofenceVM", "Manually forcing silent mode because user is in zone!")
                        GeofenceReceiver.runMuteLogic(ctx, Geofence.GEOFENCE_TRANSITION_ENTER)
                    }
                }
            }
        }
    }

    private fun registerGeofences(zones: List<SilentZone>) {
        if (zones.isEmpty()) return
        val fences = zones.map { z ->
            Geofence.Builder()
                .setRequestId(z.id)
                .setCircularRegion(z.lat, z.lng, z.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(10000) // 10 seconds
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT or
                    Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .build()
        }
        val req = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(fences)
            .build()

        try {
            geofencingClient.addGeofences(req, geofencePendingIntent()).run {
                addOnSuccessListener { Log.d("GeofenceVM", "Successfully added ${fences.size} geofences") }
                addOnFailureListener { Log.e("GeofenceVM", "Failed to add geofences: ${it.message}") }
            }
        } catch (e: SecurityException) {
            Log.e("GeofenceVM", "SecurityException: ${e.message}")
        }
    }

    private fun geofencePendingIntent(): PendingIntent {
        val intent = Intent(ctx, GeofenceReceiver::class.java).apply {
            action = "com.example.goodstart.ACTION_GEOFENCE_EVENT"
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            ctx, 0, intent, flags
        )
    }

    private fun saveZones(zones: List<SilentZone>) {
        prefs.edit().putString("zones", gson.toJson(zones)).apply()
    }

    private fun loadZones() {
        val json = prefs.getString("zones", null) ?: return
        val type = object : TypeToken<List<SilentZone>>() {}.type
        val zones: List<SilentZone> = try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
        _state.value = _state.value.copy(zones = zones)
    }
}
