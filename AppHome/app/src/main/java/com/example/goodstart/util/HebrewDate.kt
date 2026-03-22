package com.example.goodstart.util

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HebrewDate {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Format ISO date as "ג' בניסן תשפ״ו" */
    fun format(isoDate: String): String = try {
        val cal = calFrom(isoDate)
        val jc = JewishCalendar(cal)          // pass Calendar — not raw Gregorian ints
        val hdf = HebrewDateFormatter().apply { isHebrewFormat = true }
        val full = hdf.format(jc)             // "ג ניסן תשפ״ו"
        val parts = full.split(" ", limit = 3)
        if (parts.size == 3) "${parts[0]}' \u05D1${parts[1]} ${parts[2]}" else full
    } catch (_: Exception) { isoDate }

    fun shift(isoDate: String, days: Int): String = try {
        val cal = calFrom(isoDate)
        cal.add(Calendar.DAY_OF_YEAR, days)
        sdf.format(cal.time)
    } catch (_: Exception) { isoDate }

    fun today(): String = sdf.format(Date())

    private fun calFrom(isoDate: String): Calendar {
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(isoDate) ?: Date()
        return cal
    }
}
