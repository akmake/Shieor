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
import com.example.goodstart.network.ZmanDto
import com.example.goodstart.network.ZmanimDay
import com.example.goodstart.util.HebrewDate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

data class ZmanEntry(val label: String, val time: String, val timeMillis: Long)

data class ZmanimState(
    val loading: Boolean = true,
    val zmanim: List<ZmanEntry> = emptyList(),
    val error: String? = null,
    val date: String = HebrewDate.today(),
    val selectedCity: Int = 0,
    val alarms: Map<String, AlarmConfig> = emptyMap(),
    val syncing: Boolean = false   // background download in progress
)

private data class CityInfo(val name: String, val locationId: Int)

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

    private val alarmPrefs = application.getSharedPreferences("ZmanimAlarms", Context.MODE_PRIVATE)
    private val gson       = Gson()
    private val cacheDir   = File(application.filesDir, "zmanim_cache").also { it.mkdirs() }

    // In-memory: locationId → (date → zmanim list)
    private val memCache = mutableMapOf<Int, MutableMap<String, List<ZmanDto>>>()

    init {
        val savedCity = application.getSharedPreferences("ZmanimPrefs", Context.MODE_PRIVATE)
            .getInt("selected_city", 0).coerceIn(0, cityInfos.lastIndex)
        if (savedCity != 0) _state.value = _state.value.copy(selectedCity = savedCity)
        loadAlarms()
        viewModelScope.launch(Dispatchers.IO) {
            loadFileCacheIntoMemory()
            withContext(Dispatchers.Main) { fetch() }
            syncMissingCities()
        }
    }

    // ── public API ───────────────────────────────────────────────────────────

    fun selectCity(idx: Int) {
        _state.value = _state.value.copy(selectedCity = idx, loading = true, error = null)
        getApplication<Application>()
            .getSharedPreferences("ZmanimPrefs", Context.MODE_PRIVATE)
            .edit().putInt("selected_city", idx).apply()
        fetch()
    }

    fun shiftDate(days: Int) {
        _state.value = _state.value.copy(
            date = HebrewDate.shift(_state.value.date, days),
            loading = true, error = null
        )
        fetch()
    }

    /** Returns true if alarm was scheduled, false if time is already past. */
    fun scheduleAlarm(zman: ZmanEntry, config: AlarmConfig): Boolean {
        val ctx      = getApplication<Application>()
        val offsetMs = config.offsetMinutes * 60_000L
        val alarmMs  = if (config.isBefore) zman.timeMillis - offsetMs else zman.timeMillis + offsetMs
        if (alarmMs <= System.currentTimeMillis()) return false

        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return false

        val intent = Intent(ctx, ZmanimAlarmReceiver::class.java).apply {
            putExtra(ZmanimAlarmReceiver.EXTRA_ZMAN_LABEL,   zman.label)
            putExtra(ZmanimAlarmReceiver.EXTRA_RING_COUNT,   config.ringCount)
            putExtra(ZmanimAlarmReceiver.EXTRA_RING_DURATION, config.ringDurationSeconds)
            putExtra(ZmanimAlarmReceiver.EXTRA_RINGTONE_URI, config.ringtoneUri)
        }
        val pi = PendingIntent.getBroadcast(
            ctx, zman.label.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmMs, pi)
            alarmPrefs.edit().putString("alarm_${zman.label}", gson.toJson(config)).apply()
            _state.value = _state.value.copy(
                alarms = _state.value.alarms.toMutableMap().also { it[zman.label] = config }
            )
            true
        } catch (_: SecurityException) { false }
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
        alarmPrefs.edit().remove("alarm_${zmanLabel}").apply()
        _state.value = _state.value.copy(
            alarms = _state.value.alarms.toMutableMap().also { it.remove(zmanLabel) }
        )
    }

    // ── cache helpers ────────────────────────────────────────────────────────

    private fun loadFileCacheIntoMemory() {
        val listType = object : TypeToken<List<ZmanimDay>>() {}.type
        cityInfos.forEach { city ->
            val file = File(cacheDir, "city_${city.locationId}.json")
            if (!file.exists()) return@forEach
            try {
                val days: List<ZmanimDay> = gson.fromJson(file.readText(), listType) ?: return@forEach
                val map = days.associate { it.date to it.zmanim }.toMutableMap()
                synchronized(memCache) { memCache[city.locationId] = map }
            } catch (_: Exception) { /* corrupt cache — skip */ }
        }
    }

    private suspend fun downloadCity(locationId: Int, year: Int) {
        try {
            val days = RetrofitClient.zmanimService.getZmanim(
                locationId = locationId,
                from       = "$year-01-01",
                to         = "$year-12-31"
            )
            val incoming = days.associate { it.date to it.zmanim }
            synchronized(memCache) {
                val existing = memCache.getOrPut(locationId) { mutableMapOf() }
                existing.putAll(incoming)
            }
            // Persist full map to file
            val allDays = synchronized(memCache) {
                memCache[locationId]?.map { (d, z) -> ZmanimDay(d, z) } ?: emptyList()
            }
            File(cacheDir, "city_$locationId.json").writeText(gson.toJson(allDays))
        } catch (_: Exception) { /* no network — silent */ }
    }

    private fun hasCacheForYear(locationId: Int, year: Int): Boolean =
        synchronized(memCache) {
            memCache[locationId]?.keys?.any { it.startsWith("$year-") } == true
        }

    private suspend fun syncMissingCities() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val missing = cityInfos.filter { !hasCacheForYear(it.locationId, year) }
        if (missing.isEmpty()) return

        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(syncing = true)
        }
        missing.forEach { city ->
            downloadCity(city.locationId, year)
        }
        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(syncing = false)
            // Re-render current view now that data arrived
            fetch()
        }
    }

    // ── display ──────────────────────────────────────────────────────────────

    private fun fetch() {
        val snap     = _state.value
        val cityInfo = cityInfos[snap.selectedCity]
        val date     = snap.date

        val dayZmanim = synchronized(memCache) {
            memCache[cityInfo.locationId]?.get(date)
        }

        if (dayZmanim != null) {
            val byType  = dayZmanim.associateBy { it.type }
            val entries = ZMANIM_ORDER.mapNotNull { (type, label) ->
                val dto = byType[type] ?: return@mapNotNull null
                ZmanEntry(label, dto.time, timeStringToMillis(date, dto.time))
            }
            _state.value = snap.copy(loading = false, zmanim = entries, error = null)
        } else {
            // Data not cached yet — kick off a download for this specific date
            _state.value = snap.copy(loading = true)
            viewModelScope.launch(Dispatchers.IO) {
                downloadCity(cityInfo.locationId, date.substring(0, 4).toInt())
                withContext(Dispatchers.Main) { fetch() }
            }
        }
    }

    private fun loadAlarms() {
        val alarms = alarmPrefs.all
            .filterKeys { it.startsWith("alarm_") }
            .mapNotNull { (_, v) ->
                try { gson.fromJson(v as String, AlarmConfig::class.java) } catch (_: Exception) { null }
            }
            .associateBy { it.zmanLabel }
        _state.value = _state.value.copy(alarms = alarms)
    }

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
