package com.example.goodstart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodstart.api.RetrofitClient
import com.example.goodstart.model.Mamaar
import com.example.goodstart.model.MamaarSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MamaarimState(
    val mamaarim: List<Mamaar> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MamaarimViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(MamaarimState())
    val state = _state.asStateFlow()

    private val _selectedMamaar = MutableStateFlow<Mamaar?>(null)
    val selectedMamaar = _selectedMamaar.asStateFlow()

    init { loadAll() }

    // ─── API Fetches ──────────────────────────────────────────────────────────

    fun loadAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val service = RetrofitClient.getStudyService()
                val response = service.getArticles().execute()
                if (response.isSuccessful && response.body() != null) {
                    val dtos = response.body()!!
                    val list = dtos.map { dto ->
                        Mamaar(
                            id = dto.id,
                            title = dto.title,
                            fileName = "",
                            sections = emptyList(), // Not fetched yet
                            createdAt = parseDate(dto.createdAt)
                        )
                    }
                    _state.update { it.copy(mamaarim = list, isLoading = false) }
                } else {
                    _state.update { it.copy(error = "שגיאה בטעינת מאמרים משרת", isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "שגיאת חיבור: ${e.message}", isLoading = false) }
            }
        }
    }

    fun loadArticleContent(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getStudyService()
                val response = service.getArticleById(id).execute()
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val fullText = dto.rawText ?: ""
                    val sections = splitIntoSections(fullText)
                    
                    val mamaar = Mamaar(
                        id = dto.id,
                        title = dto.title,
                        fileName = "",
                        sections = sections,
                        createdAt = parseDate(dto.createdAt)
                    )
                    _selectedMamaar.value = mamaar
                } else {
                    _selectedMamaar.value = null
                }
            } catch (e: Exception) {
                _selectedMamaar.value = null
            }
        }
    }

    fun clearSelectedMamaar() {
        _selectedMamaar.value = null
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun parseDate(iso: String?): Long {
        if (iso == null) return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(iso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    }

    // ─── Helper for splitting ────────────────────────────────────────────────
    private fun splitIntoSections(text: String): List<MamaarSection> {
        val markerRe = Regex("(?m)^([א-ת]\\)\\s)")
        val matches  = markerRe.findAll(text).toList()

        if (matches.isEmpty()) return splitByParagraphGroups(text)

        val sections = mutableListOf<MamaarSection>()

        val intro = text.substring(0, matches.first().range.first).trim()
        if (intro.isNotEmpty()) sections.add(MamaarSection(null, intro))

        for (i in matches.indices) {
            val start = matches[i].range.first
            val end   = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val chunk = text.substring(start, end).trim()
            val lines   = chunk.lines()
            val heading = lines.firstOrNull()?.trim()
            val body    = lines.drop(1).joinToString("\n").trim()
            sections.add(MamaarSection(heading, body))
        }

        return sections.ifEmpty { listOf(MamaarSection(null, text.trim())) }
    }

    private fun splitByParagraphGroups(text: String): List<MamaarSection> {
        val paragraphs = text.split(Regex("\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) return listOf(MamaarSection(null, text.trim()))

        val sections = mutableListOf<MamaarSection>()
        sections.add(MamaarSection(null, paragraphs.first()))
        paragraphs.drop(1).chunked(4).forEach { chunk ->
            sections.add(MamaarSection(null, chunk.joinToString("\n\n")))
        }
        return sections
    }
}
