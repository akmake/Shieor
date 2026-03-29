package com.example.goodstart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.goodstart.alarm.ZmanimAlarmService;
import com.example.goodstart.alarm.ZmanimRescheduler;

public class ZmanimAlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_ZMAN_LABEL    = "zman_label";
    public static final String EXTRA_RING_COUNT    = "ring_count";
    public static final String EXTRA_RING_DURATION = "ring_duration";
    public static final String EXTRA_RINGTONE_URI  = "ringtone_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        String zmanLabel      = intent.getStringExtra(EXTRA_ZMAN_LABEL);
        int    ringCount      = intent.getIntExtra(EXTRA_RING_COUNT, 3);
        int    ringDuration   = intent.getIntExtra(EXTRA_RING_DURATION, 20);
        String ringtoneUri    = intent.getStringExtra(EXTRA_RINGTONE_URI);
        if (ringtoneUri == null) ringtoneUri = "";

        // הפעל את שירות הצלצול
        Intent serviceIntent = new Intent(context, ZmanimAlarmService.class);
        serviceIntent.putExtra(ZmanimAlarmService.EXTRA_ZMAN_LABEL,    zmanLabel);
        serviceIntent.putExtra(ZmanimAlarmService.EXTRA_RING_COUNT,     ringCount);
        serviceIntent.putExtra(ZmanimAlarmService.EXTRA_RING_DURATION,  ringDuration);
        serviceIntent.putExtra(ZmanimAlarmService.EXTRA_RINGTONE_URI,   ringtoneUri);
        context.startForegroundService(serviceIntent);

        // תזמן מחדש ליום המחר לפי חישוב זמנים עדכני
        if (zmanLabel != null) {
            ZmanimRescheduler.INSTANCE.rescheduleNext(context, zmanLabel);
        }
    }
}
