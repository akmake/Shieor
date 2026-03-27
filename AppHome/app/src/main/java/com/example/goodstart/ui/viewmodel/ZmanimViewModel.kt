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
import com.example.goodstart.network.RetrofitClient
import com.example.goodstart.util.HebrewDate
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private data class CityInfo(val name: String, val locationId: Int)

// מיפוי סוגי זמנים מהשרת לתוויות עבריות (לפי סדר הצגה)
private val ZMANIM_ORDER = listOf(
    "AlosHashachar"    to "עלות השחר",
    "EarliestTefillin" to "משיכיר",
    "NetzHachamah"     to "הנץ החמה",
    "LatestShema"      to "סוף זמן ק\"ש",
    "LatestTefillah"   to "סוף זמן תפילה",
    "Chatzos"          to "חצות היום",
    "MinchahGedolah"   to "מנחה גדולה",
    "MinchahKetanah"   to "מנחה קטנה",
    "PlagHaminchah"    to "פלג המנחה",
    "CandleLighting"   to "הדלקת נרות",
    "Shkiah"           to "שקיעת החמה",
    "Tzeis"            to "צאת הכוכבים",
    "ChatzosNight"     to "חצות הלילה",
)

class ZmanimViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ZmanimState())
    val state = _state.asStateFlow()

    val cityNames = listOf("תל אביב", "ירושלים", "חיפה", "באר שבע")

    private val cityInfos = listOf(
        CityInfo("תל אביב", 531),
        CityInfo("ירושלים", 247),
        CityInfo("חיפה",    689),
        CityInfo("באר שבע", 688)
    )

    private val prefs = application.getSharedPreferences("ZmanimAlarms", Context.MODE_PRIVATE)
    private val gson  = Gson()

    init {
        loadAlarms()
        fetch()
    }

    fun selectCity(idx: Int) {
        _state.value = _state.value.copy(selectedCity = idx, loading = true, error = null)
        fetch()
    }

    fun shiftDate(days: Int) {
        _state.value = _state.value.copy(
            date = HebrewDate.shift(_state.value.date, days),
            loading = true, error = null
        )
        fetch()
    }

    /** Returns true if alarm was scheduled, false if the time is already in the past. */
    fun scheduleAlarm(zman: ZmanEntry, config: AlarmConfig): Boolean {
        val ctx     = getApplication<Application>()
        val offsetMs = config.offsetMinutes * 60_000L
        val alarmTimeMs = if (config.isBefore) zman.timeMillis - offsetMs else zman.timeMillis + offsetMs

        if (alarmTimeMs <= System.currentTimeMillis()) return false

        val am     = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ZmanimAlarmReceiver::class.java).apply {
            putExtra(ZmanimAlarmReceiver.EXTRA_ZMAN_LABEL,   zman.label)
            putExtra(ZmanimAlarmReceiver.EXTRA_RING_COUNT,   config.ringCount)
            putExtra(ZmanimAlarmReceiver.EXTRA_RINGTONE_URI, config.ringtoneUri)
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
        val am  = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi  = PendingIntent.getBroadcast(
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

    private fun fetch() {
        val snap     = _state.value
        val cityInfo = cityInfos[snap.selectedCity]
        val date     = snap.date

        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) {
                try {
                    val days = RetrofitClient.zmanimService.getZmanim(
                        locationId = cityInfo.locationId,
                        from       = date,
                        to         = date
                    )
                    val rawZmanim = days.firstOrNull()?.zmanim ?: return@withContext null
                    val byType    = rawZmanim.associateBy { it.type }

                    ZMANIM_ORDER.mapNotNull { (type, label) ->
                        val dto = byType[type] ?: return@mapNotNull null
                        ZmanEntry(
                            label      = label,
                            time       = dto.time,
                            timeMillis = timeStringToMillis(date, dto.time)
                        )
                    }
                } catch (_: Exception) { null }
            }

            if (entries == null) {
                _state.value = _state.value.copy(loading = false, error = "שגיאה בטעינת הזמנים")
            } else {
                _state.value = _state.value.copy(loading = false, zmanim = entries, error = null)
            }
        }
    }

    /** ממיר מחרוזת שעה "H:mm" + תאריך ISO ל-milliseconds ב-Asia/Jerusalem */
    private fun timeStringToMillis(isoDate: String, timeStr: String): Long {
        return try {
            val (year, month, day) = isoDate.split("-").map { it.toInt() }
            val parts  = timeStr.split(":")
            val hour   = parts[0].toInt()
            val minute = parts[1].toInt()
            Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem")).apply {
                set(year, month - 1, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (_: Exception) { 0L }
    }
}
