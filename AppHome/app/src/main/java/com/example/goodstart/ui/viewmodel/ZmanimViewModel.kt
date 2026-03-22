package com.example.goodstart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodstart.util.HebrewDate
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ZmanEntry(val label: String, val time: String)

data class ZmanimState(
    val loading: Boolean = true,
    val zmanim: List<ZmanEntry> = emptyList(),
    val error: String? = null,
    val date: String = HebrewDate.today(),
    val selectedCity: Int = 0
)

private data class CityInfo(val name: String, val lat: Double, val lon: Double)

class ZmanimViewModel : ViewModel() {
    private val _state = MutableStateFlow(ZmanimState())
    val state = _state.asStateFlow()

    // Names exposed to UI
    val cityNames = listOf("תל אביב", "ירושלים", "חיפה", "באר שבע")

    private val cityInfos = listOf(
        CityInfo("תל אביב", 32.0853, 34.7818),
        CityInfo("ירושלים", 31.7683, 35.2137),
        CityInfo("חיפה",    32.7940, 34.9896),
        CityInfo("באר שבע", 31.2518, 34.7913)
    )

    init { compute() }

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

    private fun compute() {
        val snap = _state.value
        val cityInfo = cityInfos[snap.selectedCity]
        val isoDate = snap.date
        viewModelScope.launch {
            val entries = withContext(Dispatchers.Default) {
                computeZmanim(cityInfo, isoDate)
            }
            if (entries == null) {
                _state.value = _state.value.copy(loading = false, error = "שגיאה בחישוב הזמנים")
            } else {
                _state.value = _state.value.copy(loading = false, zmanim = entries, error = null)
            }
        }
    }

    private fun computeZmanim(city: CityInfo, isoDate: String): List<ZmanEntry>? {
        return try {
            val tz = TimeZone.getTimeZone("Asia/Jerusalem")
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
            val parsed: Date = sdf.parse(isoDate) ?: return null
            val cal = Calendar.getInstance(tz).apply { time = parsed }

            val geo = GeoLocation(city.name, city.lat, city.lon, 0.0, tz)
            val czc = ComplexZmanimCalendar(geo).apply { calendar = cal }

            val fmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }
            fun f(d: Date?) = if (d != null) fmt.format(d) else null

            listOfNotNull(
                f(czc.alos72)?.let                  { ZmanEntry("עלות השחר",                    it) },
                f(czc.misheyakir10Point2Degrees)?.let { ZmanEntry("משיכיר",                       it) },
                f(czc.sunrise)?.let                  { ZmanEntry("הנץ החמה",                     it) },
                f(czc.sofZmanShmaMGA)?.let           { ZmanEntry("סוף זמן ק\"ש (מג\"א)",         it) },
                f(czc.sofZmanShmaGRA)?.let           { ZmanEntry("סוף זמן ק\"ש (גר\"א)",         it) },
                f(czc.sofZmanTfilaMGA)?.let          { ZmanEntry("סוף זמן תפילה (מג\"א)",        it) },
                f(czc.sofZmanTfilaGRA)?.let          { ZmanEntry("סוף זמן תפילה (גר\"א)",        it) },
                f(czc.chatzos)?.let                  { ZmanEntry("חצות היום",                    it) },
                f(czc.minchaGedola)?.let             { ZmanEntry("מנחה גדולה",                   it) },
                f(czc.minchaKetana)?.let             { ZmanEntry("מנחה קטנה",                    it) },
                f(czc.plagHamincha)?.let             { ZmanEntry("פלג המנחה",                    it) },
                f(czc.sunset)?.let                   { ZmanEntry("שקיעת החמה",                   it) },
                f(czc.tzaisGeonim8Point5Degrees)?.let { ZmanEntry("בין השמשות",                  it) },
                f(czc.tzais72)?.let                  { ZmanEntry("צאת הכוכבים",                  it) },
                f(czc.solarMidnight)?.let            { ZmanEntry("חצות הלילה",                   it) }
            )
        } catch (_: Exception) { null }
    }
}
