package com.example.goodstart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodstart.cache.StudyCache
import com.example.goodstart.model.StudyDay
import com.example.goodstart.network.RetrofitClient
import com.example.goodstart.util.HebrewDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val day: StudyDay? = null,
    val date: String = HebrewDate.today(),
    val error: String? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init { load() }

    fun shiftDate(days: Int) {
        val newDate = HebrewDate.shift(_state.value.date, days)
        _state.value = _state.value.copy(date = newDate, loading = true, error = null, day = null)
        load()
    }

    fun retry() { load() }

    private fun load() {
        val date = _state.value.date
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val cached = StudyCache.get(ctx, date)
            if (cached?.studies?.isNotEmpty() == true) {
                _state.value = _state.value.copy(loading = false, day = cached)
                return@launch
            }
            try {
                val day = RetrofitClient.studyService.getDailyStudy(date)
                StudyCache.save(ctx, date, day)
                _state.value = _state.value.copy(loading = false, day = day)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "אין חיבור לאינטרנט")
            }
        }
    }
}
