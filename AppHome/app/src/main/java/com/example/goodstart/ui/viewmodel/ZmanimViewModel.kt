package com.example.goodstart.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodstart.ZmanimAlarmReceiver
import com.example.goodstart.alarm.AlarmConfig
import com.example.goodstart.util.HebrewDate
import com.google.gson.Gson
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ZmanEntry(val label: String, val time: String, val timeMillis: Long)

data class ZmanimState(
    val loading: Boolean = true,
    val zmanim: List<ZmanEntry> = emptyList(),
    val error: String? = null,
    val date: String = HebrewDate.today(),
    val selectedCity: Int = 0,
    val alarms: Map<String, AlarmConfig> = emptyMap()
)

private data class CityInfo(val name: String, val lat: Double, val lon: Double)

class ZmanimViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ZmanimState())
    val state = _state.asStateFlow()

    val cityNames = listOf("תל אביב", "ירושלים", "חיפה", "באר שבע")

    private val cityInfos = listOf(
        CityInfo("תל אביב", 32.0853, 34.7818),
        CityInfo("ירושלים", 31.7683, 35.2137),
        CityInfo("חיפה",    32.7940, 34.9896),
        CityInfo("באר שבע", 31.2518, 34.7913)
    )

    private val prefs = application.getSharedPreferences("ZmanimAlarms", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        loadAlarms()
        compute()
    }

    fun selectCity(idx: Int) {
        _state.value = _state.value.copy(selectedCity = idx, loading = true, error = null)
        compute()
    }

    fun shiftDate(days: Int) {
        _state.value = _state.value.copy(
            date = HebrewDate.shift(_state.value.date, days),
            loading = true, error = null
        )
        compute()
    }

    /** Returns true if alarm was scheduled, false if the time is already in the past. */
    fun scheduleAlarm(zman: ZmanEntry, config: AlarmConfig): Boolean {
        val ctx = getApplication<Application>()
        val offsetMs = config.offsetMinutes * 60_000L
        val alarmTimeMs = if (config.isBefore) zman.timeMillis - offsetMs else zman.timeMillis + offsetMs

        if (alarmTimeMs <= System.currentTimeMillis()) return false

        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ZmanimAlarmReceiver::class.java).apply {
            putExtra(ZmanimAlarmReceiver.EXTRA_ZMAN_LABEL,    zman.label)
            putExtra(ZmanimAlarmReceiver.EXTRA_RING_COUNT,    config.ringCount)
            putExtra(ZmanimAlarmReceiver.EXTRA_RINGTONE_URI,  config.ringtoneUri)
        }
        val pi = PendingIntent.getBroadcast(
            ctx, zman.label.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pi)

        prefs.edit().putString("alarm_${zman.label}", gson.toJson(config)).apply()
        _state.value = _state.value.copy(
            alarms = _state.value.alarms.toMutableMap().also { it[zman.label] = config }
        )
        return true
    }

    fun cancelAlarm(zmanLabel: String) {
        val ctx = getApplication<Application>()
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, zmanLabel.hashCode(),
            Intent(ctx, ZmanimAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        pi?.let { am.cancel(it); it.cancel() }

        prefs.edit().remove("alarm_${zmanLabel}").apply()
        _state.value = _state.value.copy(
            alarms = _state.value.alarms.toMutableMap().also { it.remove(zmanLabel) }
        )
    }

    private fun loadAlarms() {
        val alarms = prefs.all
            .filterKeys { it.startsWith("alarm_") }
            .mapNotNull { (_, v) ->
                try { gson.fromJson(v as String, AlarmConfig::class.java) } catch (_: Exception) { null }
            }
            .associateBy { it.zmanLabel }
        _state.value = _state.value.copy(alarms = alarms)
    }

    private fun compute() {
        val snap = _state.value
        val cityInfo = cityInfos[snap.selectedCity]
        val isoDate = snap.date
        viewModelScope.launch {
            val entries = withContext(Dispatchers.Default) { computeZmanim(cityInfo, isoDate) }
            if (entries == null) {
                _state.value = _state.value.copy(loading = false, error = "שגיאה בחישוב הזמנים")
            } else {
                _state.value = _state.value.copy(loading = false, zmanim = entries, error = null)
            }
        }
    }

    private fun computeZmanim(city: CityInfo, isoDate: String): List<ZmanEntry>? {
        return try {
            val tz  = TimeZone.getTimeZone("Asia/Jerusalem")
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
            val parsed: Date = sdf.parse(isoDate) ?: return null
            val cal = Calendar.getInstance(tz).apply { time = parsed }

            val geo = GeoLocation(city.name, city.lat, city.lon, 0.0, tz)
            val czc = ComplexZmanimCalendar(geo).apply { calendar = cal }

            val fmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }
            fun f(d: Date?) = d?.let { Pair(fmt.format(it), it.time) }

            listOfNotNull(
                f(czc.alos72)?.let                   { ZmanEntry("עלות השחר",                  it.first, it.second) },
                f(czc.misheyakir10Point2Degrees)?.let { ZmanEntry("משיכיר",                     it.first, it.second) },
                f(czc.sunrise)?.let                  { ZmanEntry("הנץ החמה",                   it.first, it.second) },
                f(czc.sofZmanShmaMGA)?.let           { ZmanEntry("סוף זמן ק\"ש (מג\"א)",       it.first, it.second) },
                f(czc.sofZmanShmaGRA)?.let           { ZmanEntry("סוף זמן ק\"ש (גר\"א)",       it.first, it.second) },
                f(czc.sofZmanTfilaMGA)?.let          { ZmanEntry("סוף זמן תפילה (מג\"א)",      it.first, it.second) },
                f(czc.sofZmanTfilaGRA)?.let          { ZmanEntry("סוף זמן תפילה (גר\"א)",      it.first, it.second) },
                f(czc.chatzos)?.let                  { ZmanEntry("חצות היום",                  it.first, it.second) },
                f(czc.minchaGedola)?.let             { ZmanEntry("מנחה גדולה",                 it.first, it.second) },
                f(czc.minchaKetana)?.let             { ZmanEntry("מנחה קטנה",                  it.first, it.second) },
                f(czc.plagHamincha)?.let             { ZmanEntry("פלג המנחה",                  it.first, it.second) },
                f(czc.sunset)?.let                   { ZmanEntry("שקיעת החמה",                 it.first, it.second) },
                f(czc.tzaisGeonim8Point5Degrees)?.let{ ZmanEntry("בין השמשות",                 it.first, it.second) },
                f(czc.tzais72)?.let                  { ZmanEntry("צאת הכוכבים",                it.first, it.second) },
                f(czc.solarMidnight)?.let            { ZmanEntry("חצות הלילה",                 it.first, it.second) }
            )
        } catch (_: Exception) { null }
    }
}
