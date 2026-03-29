package com.example.goodstart.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that pushes local reading progress to the server.
 * Runs every 1 hour, only when connected to a network.
 */
class UserSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        UserManager.ensureRegistered(applicationContext)
        val ok = UserManager.pushToServer(applicationContext)
        Log.d(TAG, "Sync result: $ok")
        return Result.success()
    }

    companion object {
        private const val TAG = "UserSyncWorker"
        private const val WORK_NAME = "UserSyncWork"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UserSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
