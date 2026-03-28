package com.example.goodstart.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodstart.cache.StudyCache
import com.example.goodstart.model.Section
import com.example.goodstart.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TEHILLIM_PREFS = "TehillimPrefs"
private const val KEY_CUSTOM_CHAPTERS = "custom_chapters"
private const val KEY_LAST_DATE = "last_tehillim_date"
private const val KEY_LAST_LABEL = "last_tehillim_label"

data class StudyState(
    val loading: Boolean = true,
    val sections: List<Section> = emptyList(),
    val subtitle: String = "",
    val error: String? = null,
    val customChapters: List<Int> = emptyList()
)

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(StudyState())
    val state = _state.asStateFlow()

    private fun tehillimPrefs() =
        getApplication<Application>().getSharedPreferences(TEHILLIM_PREFS, Context.MODE_PRIVATE)

    fun getCustomChapters(): List<Int> {
        val str = tehillimPrefs().getString(KEY_CUSTOM_CHAPTERS, "") ?: ""
        return str.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..150 }
    }

    fun saveCustomChapters(chapters: List<Int>) {
        tehillimPrefs().edit()
            .putString(KEY_CUSTOM_CHAPTERS, chapters.joinToString(","))
            .apply()
        // Reload with updated custom chapters
        val date  = tehillimPrefs().getString(KEY_LAST_DATE, "")  ?: ""
        val label = tehillimPrefs().getString(KEY_LAST_LABEL, "") ?: ""
        if (date.isNotEmpty()) load("tehillim", date, label)
    }

    fun load(key: String, date: String, label: String) {
        val ctx = getApplication<Application>()
        if (key == "tehillim") {
            tehillimPrefs().edit()
                .putString(KEY_LAST_DATE, date)
                .putString(KEY_LAST_LABEL, label)
                .apply()
        }

        viewModelScope.launch {
            _state.value = StudyState(loading = true)

            val customChapters = if (key == "tehillim") getCustomChapters() else emptyList()

            // Load daily sections from cache or network
            var dailySections: List<Section> = emptyList()
            val cached = StudyCache.get(ctx, date)
            val cachedStudy = cached?.studies?.get(key)
            if (cachedStudy?.sections?.isNotEmpty() == true) {
                dailySections = cachedStudy.sections!!
            } else {
                try {
                    val day = RetrofitClient.studyService.getDailyStudy(date)
                    StudyCache.save(ctx, date, day)
                    dailySections = day.studies?.get(key)?.sections ?: emptyList()
                } catch (e: Exception) {
                    _state.value = StudyState(loading = false, error = "אין חיבור לאינטרנט: ${e.message}")
                    return@launch
                }
            }

            // Fetch and append custom chapters if tehillim
            var allSections = dailySections
            if (key == "tehillim" && customChapters.isNotEmpty()) {
                try {
                    val chaptersStr = customChapters.joinToString(",")
                    val customSections = RetrofitClient.studyService
                        .getTehillimChapters(chaptersStr).sections ?: emptyList()
                    if (customSections.isNotEmpty()) {
                        val separator = Section(
                            id = "custom_sep",
                            isHeader = true,
                            isAliyahHeader = true, // use smaller header style
                            he = "— פרקים אישיים —",
                            en = ""
                        )
                        allSections = dailySections + separator + customSections
                    }
                } catch (_: Exception) {
                    // custom chapters failed silently — still show daily
                }
            }

            if (allSections.isEmpty()) {
                _state.value = StudyState(loading = false, error = "אין תוכן ללימוד זה", customChapters = customChapters)
            } else {
                _state.value = StudyState(
                    loading = false,
                    sections = allSections,
                    subtitle = label.ifEmpty { "" },
                    customChapters = customChapters
                )
            }
        }
    }
}
