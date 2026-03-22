package com.example.goodstart.geofence

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nm    = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // DND access required on Android 6+ to set silent
        if (!nm.isNotificationPolicyAccessGranted) return

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER ->
                audio.ringerMode = AudioManager.RINGER_MODE_SILENT
            Geofence.GEOFENCE_TRANSITION_EXIT ->
                audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }
}
