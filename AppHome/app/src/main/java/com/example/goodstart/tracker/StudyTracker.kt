package com.example.goodstart.tracker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tracks daily study completion.
 *
 * Storage format:
 *   SharedPreferences "StudyTracker"
 *     "enabled_studies"     → Set<String> of study keys the user wants to track
 *     "done_<date>_<key>"   → Boolean  (true = completed that day)
 *     "history_<date>"      → Set<String> of completed study keys for that date
 */
object StudyTracker {
    private const val TAG = "StudyTracker"
    private const val PREFS_NAME = "StudyTracker"
    private const val KEY_ENABLED = "enabled_studies"

    /** All possible study keys. */
    val ALL_STUDY_KEYS = listOf(
        "chumash", "tanya", "rambam", "rambamOne",
        "tehillim", "seferHamitzvot", "shnayimMikra"
    )

    val STUDY_LABELS = mapOf(
        "chumash"       to "חומש",
        "tanya"         to "תניא",
        "rambam"        to "רמב\"ם ג׳ פרקים",
        "rambamOne"     to "רמב\"ם פרק אחד",
        "tehillim"      to "תהילים",
        "seferHamitzvot" to "ספר המצוות",
        "shnayimMikra"  to "שניים מקרא"
    )

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ── Enabled Studies ────────────────────────────────────────────────────

    fun getEnabledStudies(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_ENABLED, ALL_STUDY_KEYS.toSet()) ?: ALL_STUDY_KEYS.toSet()

    fun setEnabledStudies(ctx: Context, keys: Set<String>) {
        prefs(ctx).edit().putStringSet(KEY_ENABLED, keys).apply()
    }

    // ── Daily Completion ───────────────────────────────────────────────────

    fun isCompleted(ctx: Context, date: String, key: String): Boolean =
        prefs(ctx).getBoolean("done_${date}_$key", false)

    fun setCompleted(ctx: Context, date: String, key: String, done: Boolean) {
        val p = prefs(ctx)
        p.edit().putBoolean("done_${date}_$key", done).apply()

        // Also maintain history set for easy querying
        val histKey = "history_$date"
        val history = p.getStringSet(histKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (done) history.add(key) else history.remove(key)
        p.edit().putStringSet(histKey, history).apply()
    }

    fun toggleCompleted(ctx: Context, key: String): Boolean {
        val date = today()
        val current = isCompleted(ctx, date, key)
        setCompleted(ctx, date, key, !current)
        return !current
    }

    // ── Queries ────────────────────────────────────────────────────────────

    /** Returns today's enabled studies with completion status. */
    fun getTodayStatus(ctx: Context): List<Pair<String, Boolean>> {
        val date = today()
        val enabled = getEnabledStudies(ctx)
        return ALL_STUDY_KEYS.filter { it in enabled }.map { key ->
            key to isCompleted(ctx, date, key)
        }
    }

    /** Returns how many enabled studies are completed today. */
    fun completedCount(ctx: Context): Int {
        val date = today()
        val enabled = getEnabledStudies(ctx)
        return enabled.count { isCompleted(ctx, date, it) }
    }

    fun totalEnabled(ctx: Context): Int = getEnabledStudies(ctx).size

    /** Returns the uncompleted study keys for today. */
    fun getUncompleted(ctx: Context): List<String> {
        val date = today()
        val enabled = getEnabledStudies(ctx)
        return ALL_STUDY_KEYS.filter { it in enabled && !isCompleted(ctx, date, it) }
    }

    /** Returns status for a specific date (history view). */
    fun getDateStatus(ctx: Context, date: String): List<Pair<String, Boolean>> {
        val enabled = getEnabledStudies(ctx)
        return ALL_STUDY_KEYS.filter { it in enabled }.map { key ->
            key to isCompleted(ctx, date, key)
        }
    }

    /** Returns the 30 most recent dates that have any history. */
    fun getRecentDates(ctx: Context, days: Int = 30): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val dates = mutableListOf<String>()
        repeat(days) {
            dates.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dates
    }
}
