package com.example.goodstart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodstart.cache.StudyCache
import com.example.goodstart.model.Section
import com.example.goodstart.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudyState(
    val loading: Boolean = true,
    val sections: List<Section> = emptyList(),
    val subtitle: String = "",
    val error: String? = null
)

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(StudyState())
    val state = _state.asStateFlow()

    fun load(key: String, date: String, label: String) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _state.value = StudyState(loading = true)
            val cached = StudyCache.get(ctx, date)
            val cachedStudy = cached?.studies?.get(key)
            if (cachedStudy?.sections?.isNotEmpty() == true) {
                _state.value = StudyState(
                    loading = false,
                    sections = cachedStudy.sections!!,
                    subtitle = label.ifEmpty { cachedStudy.ref ?: "" }
                )
                return@launch
            }
            try {
                val day = RetrofitClient.studyService.getDailyStudy(date)
                StudyCache.save(ctx, date, day)
                val study = day.studies?.get(key)
                val sects = study?.sections
                if (sects.isNullOrEmpty()) {
                    _state.value = StudyState(loading = false, error = "אין תוכן ללימוד זה")
                } else {
                    _state.value = StudyState(
                        loading = false,
                        sections = sects,
                        subtitle = label.ifEmpty { study.ref ?: "" }
                    )
                }
            } catch (e: Exception) {
                _state.value = StudyState(loading = false, error = "אין חיבור לאינטרנט: ${e.message}")
            }
        }
    }
}
