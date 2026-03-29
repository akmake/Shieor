package com.example.goodstart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.goodstart.alarm.ZmanimRescheduler
import com.example.goodstart.geofence.GeofenceCheckWorker
import com.example.goodstart.geofence.GeofenceHelper
import com.example.goodstart.notification.MidnightReminderReceiver
import com.example.goodstart.notification.ZmanBriefWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            ZmanimRescheduler.rescheduleAll(context)

            // Re-register geofences (cleared by Google Play Services on reboot)
            GeofenceHelper.registerActiveGeofences(context)
            // Re-start the periodic location-check worker
            val hasActiveZones = GeofenceHelper.loadZones(context).any { it.active }
            if (hasActiveZones) {
                GeofenceCheckWorker.enqueue(context)
            }

            // Re-schedule notifications
            if (MidnightReminderReceiver.isEnabled(context)) {
                MidnightReminderReceiver.schedule(context)
            }
            if (ZmanBriefWorker.isEnabled(context)) {
                ZmanBriefWorker.enqueue(context)
            }
        }
    }
}
