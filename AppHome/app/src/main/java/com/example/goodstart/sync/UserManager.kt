package com.example.goodstart.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.goodstart.network.LoginRequest
import com.example.goodstart.network.RetrofitClient
import com.example.goodstart.network.SyncRequest
import com.example.goodstart.network.UserDataResponse

/**
 * Manages the user identity (8-digit code) and data sync with the server.
 *
 * On first launch:  auto-registers with /api/user/register → gets a code.
 * On every resume:  pushes local reading positions + prefs to server.
 * On "login" from another device: pulls server data into local storage.
 */
object UserManager {
    private const val TAG = "UserManager"
    private const val PREFS_NAME = "UserIdentity"
    private const val KEY_USER_ID = "user_id"

    // SharedPreferences names that hold syncable data
    private const val READING_PREFS = "RambamPrefs"

    // Keys inside RambamPrefs that are preferences (not reading positions)
    private val PREFERENCE_KEYS = setOf(
        "text_size_sp", "mamaar_text_size_sp", "scroll_speed", "auto_scroll_speed"
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The current user ID, or null if not yet registered. */
    fun getUserId(context: Context): String? =
        prefs(context).getString(KEY_USER_ID, null)

    /**
     * Ensures the device has a user ID. Registers with the server if needed.
     * Call from a background thread.
     */
    fun ensureRegistered(context: Context): String? {
        val existing = getUserId(context)
        if (existing != null) return existing

        return try {
            val resp = RetrofitClient.userService.register().execute()
            if (resp.isSuccessful) {
                val userId = resp.body()?.userId ?: return null
                prefs(context).edit().putString(KEY_USER_ID, userId).apply()
                Log.d(TAG, "Registered with userId=$userId")
                userId
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Registration failed: ${e.message}")
            null
        }
    }

    /**
     * Log into an existing account (from another device).
     * Downloads server data and merges into local storage.
     * Call from a background thread. Returns true on success.
     */
    fun loginWithCode(context: Context, code: String): Boolean {
        return try {
            val resp = RetrofitClient.userService.login(LoginRequest(code)).execute()
            if (resp.isSuccessful) {
                val data = resp.body() ?: return false
                prefs(context).edit().putString(KEY_USER_ID, code).apply()
                applyServerData(context, data)
                Log.d(TAG, "Logged in with code=$code")
                true
            } else false
        } catch (e: Exception) {
            Log.w(TAG, "Login failed: ${e.message}")
            false
        }
    }

    /**
     * Push local data to the server.
     * Call from a background thread (e.g. WorkManager or coroutine).
     */
    fun pushToServer(context: Context): Boolean {
        val userId = getUserId(context) ?: return false
        val readingPrefs = context.getSharedPreferences(READING_PREFS, Context.MODE_PRIVATE)

        val positions = mutableMapOf<String, Int>()
        val preferences = mutableMapOf<String, Any>()

        readingPrefs.all.forEach { (key, value) ->
            if (value is Int) {
                if (key in PREFERENCE_KEYS) {
                    preferences[key] = value
                } else {
                    positions[key] = value
                }
            }
        }

        // Also sync ShnayimPrefs
        val shnayimPrefs = context.getSharedPreferences("ShnayimPrefs", Context.MODE_PRIVATE)
        shnayimPrefs.all.forEach { (key, value) ->
            if (value is Boolean) preferences["shnayim_$key"] = value
        }

        return try {
            val body = SyncRequest(positions, preferences, null)
            val resp = RetrofitClient.userService.sync(userId, body).execute()
            if (resp.isSuccessful) {
                Log.d(TAG, "Pushed ${positions.size} positions + ${preferences.size} prefs")
                true
            } else false
        } catch (e: Exception) {
            Log.w(TAG, "Push failed: ${e.message}")
            false
        }
    }

    /**
     * Pull server data into local storage (used after login on a new device).
     */
    fun pullFromServer(context: Context): Boolean {
        val userId = getUserId(context) ?: return false
        return try {
            val resp = RetrofitClient.userService.getUserData(userId).execute()
            if (resp.isSuccessful) {
                val data = resp.body() ?: return false
                applyServerData(context, data)
                true
            } else false
        } catch (e: Exception) {
            Log.w(TAG, "Pull failed: ${e.message}")
            false
        }
    }

    private fun applyServerData(context: Context, data: UserDataResponse) {
        val readingPrefs = context.getSharedPreferences(READING_PREFS, Context.MODE_PRIVATE)
        val editor = readingPrefs.edit()

        // Apply reading positions
        data.readingPositions?.forEach { (key, value) ->
            editor.putInt(key, value)
        }

        // Apply preferences
        data.preferences?.forEach { (key, value) ->
            when (value) {
                is Number -> editor.putInt(key, value.toInt())
                is Boolean -> {
                    if (key.startsWith("shnayim_")) {
                        val realKey = key.removePrefix("shnayim_")
                        context.getSharedPreferences("ShnayimPrefs", Context.MODE_PRIVATE)
                            .edit().putBoolean(realKey, value).apply()
                    } else {
                        // Store as int (0/1) for SharedPreferences compatibility
                    }
                }
            }
        }

        editor.apply()
        Log.d(TAG, "Applied server data: ${data.readingPositions?.size ?: 0} positions")
    }
}
