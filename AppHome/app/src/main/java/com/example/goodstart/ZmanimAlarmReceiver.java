package com.example.goodstart;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ZmanimAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "zmanim_alerts";
    public static final String EXTRA_ZMAN_NAME = "zman_name";

    @Override
    public void onReceive(Context context, Intent intent) {
        String zmanName = intent.getStringExtra(EXTRA_ZMAN_NAME);
        if (zmanName == null) zmanName = "זמן הלכתי";

        showNotification(context, zmanName);
    }

    private void showNotification(Context context, String zmanName) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "התראות זמני היום",
                    NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_active)
                .setContentTitle("התראת זמנים")
                .setContentText("הגיע הזמן: " + zmanName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
