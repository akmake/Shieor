package com.example.goodstart.geofence

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

/**
 * Periodic backup worker that checks device location against saved silent zones.
 * Complements the Google Play Services geofencing API which can be unreliable
 * on many OEMs. Runs every 15 minutes.
 */
class GeofenceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext

        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No fine location permission")
            return Result.success()
        }
        // On Android 10+ background location is required for workers
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No background location permission — worker cannot get location")
            return Result.success()
        }

        val activeZones = GeofenceHelper.loadZones(ctx).filter { it.active }
        if (activeZones.isEmpty()) return Result.success()

        // Also re-register system geofences every cycle (they may have been purged)
        GeofenceHelper.registerActiveGeofences(ctx)

        // Get a fresh location; fall back to lastLocation if fresh fix unavailable
        val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
        var location = try {
            Tasks.await(
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
                30, TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentLocation failed: ${e.message}")
            null
        }
        if (location == null) {
            // Fallback to cached last-known location
            location = try {
                Tasks.await(fusedClient.lastLocation, 10, TimeUnit.SECONDS)
            } catch (_: Exception) { null }
        }
        if (location == null) {
            Log.w(TAG, "No location available at all — skipping check")
            return Result.success()
        }

        val inZone = activeZones.any { z ->
            distanceBetween(location.latitude, location.longitude, z.lat, z.lng) <= z.radiusMeters
        }

        // Compare with last known state to only trigger on CHANGE
        val statePrefs = ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val wasInZone = statePrefs.getBoolean(KEY_IN_ZONE, false)

        if (inZone != wasInZone) {
            statePrefs.edit().putBoolean(KEY_IN_ZONE, inZone).apply()
            val transition = if (inZone)
                Geofence.GEOFENCE_TRANSITION_ENTER
            else
                Geofence.GEOFENCE_TRANSITION_EXIT
            GeofenceReceiver.runMuteLogic(ctx, transition)
            Log.d(TAG, "State changed: wasInZone=$wasInZone → inZone=$inZone")
        } else {
            Log.d(TAG, "No change: inZone=$inZone")
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "GeofenceCheckWorker"
        private const val WORK_NAME = "GeofenceCheckWork"
        private const val STATE_PREFS = "GeofenceWorkerState"
        private const val KEY_IN_ZONE = "in_zone"

        /** Enqueue a periodic 15-minute location check. */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<GeofenceCheckWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
            Log.d(TAG, "Periodic geofence check enqueued")
        }

        /** Cancel the periodic worker (e.g. when all zones removed). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
