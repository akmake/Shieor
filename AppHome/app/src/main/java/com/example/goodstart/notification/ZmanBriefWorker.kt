package com.example.goodstart.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.goodstart.MainActivity
import com.example.goodstart.R
import com.example.goodstart.tracker.StudyTracker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Shows a persistent notification with the next upcoming halachic zman.
 * Runs via a periodic WorkManager worker (every 30 min) that refreshes the notification.
 */
class ZmanBriefWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "zman_brief_channel_v2"
        const val NOTIFICATION_ID = 9001
        private const val WORK_NAME = "ZmanBriefWorker"
        private const val PREFS = "ZmanBriefPrefs"
        private const val KEY_ENABLED = "brief_enabled"

        fun enqueue(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<ZmanBriefWorker>(30, TimeUnit.MINUTES)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        }

        fun isEnabled(ctx: Context): Boolean =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

        fun setEnabled(ctx: Context, enabled: Boolean) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, enabled).apply()
            if (enabled) {
                enqueue(ctx)
                // Show immediately so user doesn't have to wait
                val oneTime = OneTimeWorkRequestBuilder<ZmanBriefWorker>().build()
                WorkManager.getInstance(ctx).enqueue(oneTime)
            } else {
                cancel(ctx)
            }
        }

        fun createChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "הזמן הבא (Brief)",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "מציג את הזמן ההלכתי הקרוב על מסך הנעילה"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(channel)
            }
        }
    }

    override fun doWork(): Result {
        createChannel(applicationContext)
        val nextZman = findNextZman(applicationContext)
        if (nextZman != null) {
            showNotification(applicationContext, nextZman.first, nextZman.second)
        }
        return Result.success()
    }

    private fun showNotification(ctx: Context, label: String, time: String) {
        val openIntent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            ctx, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("הזמן הבא: $label")
            .setContentText(time)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Reads the zmanim cache files (same format as ZmanimViewModel) and finds the next upcoming zman.
     */
    private fun findNextZman(ctx: Context): Pair<String, String>? {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())

        // Read selected city from prefs
        val selectedCity = ctx.getSharedPreferences("ZmanimPrefs", Context.MODE_PRIVATE)
            .getInt("selected_city", 0)

        val locationIds = listOf(531, 247, 689, 688)
        val locationId = locationIds.getOrElse(selectedCity) { 531 }

        val cacheFile = File(ctx.filesDir, "zmanim_cache/city_$locationId.json")
        if (!cacheFile.exists()) return null

        try {
            val gson = Gson()
            val listType = object : TypeToken<List<ZmanimDayBrief>>() {}.type
            val days: List<ZmanimDayBrief> = gson.fromJson(cacheFile.readText(), listType) ?: return null
            val todayData = days.find { it.date == today } ?: return null

            val zmanOrder = listOf(
                "AlosHashachar" to "עלות השחר",
                "EarliestTefillin" to "משיכיר",
                "NetzHachamah" to "הנץ החמה",
                "LatestShema" to "סוף זמן ק\"ש",
                "LatestTefillah" to "סוף זמן תפילה",
                "Chatzos" to "חצות היום",
                "MinchahGedolah" to "מנחה גדולה",
                "MinchahKetanah" to "מנחה קטנה",
                "PlagHaminchah" to "פלג המנחה",
                "CandleLighting" to "הדלקת נרות",
                "Shkiah" to "שקיעת החמה",
                "Tzeis" to "צאת הכוכבים",
                "ChatzosNight" to "חצות הלילה"
            )

            val byType = todayData.zmanim.associateBy { it.type }

            for ((type, label) in zmanOrder) {
                val dto = byType[type] ?: continue
                val millis = timeToMillis(today, dto.time)
                if (millis > now) {
                    return label to dto.time
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun timeToMillis(isoDate: String, timeStr: String): Long {
        return try {
            val (year, month, day) = isoDate.split("-").map { it.toInt() }
            val parts = timeStr.split(":")
            Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem")).apply {
                set(year, month - 1, day, parts[0].toInt(), parts[1].toInt(), 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (_: Exception) { 0L }
    }
}

// Minimal DTO to parse the cache files (same structure as ZmanimViewModel)
private data class ZmanDtoBrief(val type: String = "", val time: String = "")
private data class ZmanimDayBrief(val date: String = "", val zmanim: List<ZmanDtoBrief> = emptyList())
