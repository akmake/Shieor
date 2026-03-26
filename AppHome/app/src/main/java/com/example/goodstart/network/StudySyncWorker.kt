package com.example.goodstart.network

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.goodstart.cache.StudyCache
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class StudySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        
        var successCount = 0
        val totalToDownload = 30

        Log.d("StudySyncWorker", "Starting sync for the next $totalToDownload days")

        for (i in 0 until totalToDownload) {
            val dateStr = sdf.format(calendar.time)
            
            // Check if already cached
            if (StudyCache.get(applicationContext, dateStr) == null) {
                var attemptSuccess = false
                // Attempt 1
                try {
                    val day = RetrofitClient.studyService.getDailyStudy(dateStr)
                    StudyCache.save(applicationContext, dateStr, day)
                    attemptSuccess = true
                    Log.d("StudySyncWorker", "Downloaded study for $dateStr on 1st attempt")
                } catch (e: Exception) {
                    Log.w("StudySyncWorker", "1st attempt failed for $dateStr: ${e.message}")
                }

                if (attemptSuccess) {
                    successCount++
                }
            } else {
                successCount++
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // According to your requirement: if not all 30 days are synced, it will retry.
        // The WorkManager retry policy we set (6 mins) will handle the "Attempt 2".
        return if (successCount >= totalToDownload) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "StudySyncWork"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Run every 6 hours
            val repeatingRequest = PeriodicWorkRequestBuilder<StudySyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 6, TimeUnit.MINUTES) // Retry after 6 mins on failure
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
            )
        }
    }
}
