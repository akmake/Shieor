package com.example.goodstart.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodstart.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed class UploadState {
    object Idle        : UploadState()
    object Extracting  : UploadState()
    data class Preview(val rawText: String, val pageCount: Int, val pdfUri: Uri) : UploadState()
    object Uploading   : UploadState()
    object Success     : UploadState()
    data class Error(val message: String) : UploadState()
}

class ArticleUploadViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state = _state.asStateFlow()

    var title = MutableStateFlow("")

    fun onPdfPicked(uri: Uri) {
        _state.value = UploadState.Extracting
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx         = getApplication<Application>()
                val inputStream = ctx.contentResolver.openInputStream(uri)
                    ?: throw Exception("לא ניתן לפתוח את הקובץ")
                val bytes       = inputStream.use { it.readBytes() }

                val pdfPart = MultipartBody.Part.createFormData(
                    name     = "pdf",
                    filename = "upload.pdf",
                    body     = bytes.toRequestBody("application/pdf".toMediaType())
                )

                val response = RetrofitClient.articleUploadService.extractText(pdfPart)

                if (response.rawText.isBlank()) {
                    _state.value = UploadState.Error("לא נמצא טקסט בקובץ")
                } else {
                    _state.value = UploadState.Preview(response.rawText, response.pageCount, uri)
                }
            } catch (e: Exception) {
                _state.value = UploadState.Error("שגיאה בחילוץ הטקסט: ${e.message}")
            }
        }
    }

    fun upload() {
        val preview = _state.value as? UploadState.Preview ?: return
        _state.value = UploadState.Uploading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx         = getApplication<Application>()
                val inputStream = ctx.contentResolver.openInputStream(preview.pdfUri)
                    ?: throw Exception("לא ניתן לפתוח את הקובץ")
                val bytes = inputStream.use { it.readBytes() }

                val pdfPart = MultipartBody.Part.createFormData(
                    name     = "pdf",
                    filename = "upload.pdf",
                    body     = bytes.toRequestBody("application/pdf".toMediaType())
                )
                val textType      = "text/plain".toMediaType()
                val rawTextBody   = preview.rawText.toRequestBody(textType)
                val pageCountBody = preview.pageCount.toString().toRequestBody(textType)
                val titleBody     = title.value.trim().ifEmpty { "מאמר חדש" }.toRequestBody(textType)

                RetrofitClient.articleUploadService.uploadArticle(
                    pdfPart, rawTextBody, pageCountBody, titleBody
                )
                _state.value = UploadState.Success
            } catch (e: Exception) {
                _state.value = UploadState.Error("שגיאה בשמירה: ${e.message}")
            }
        }
    }

    fun reset() {
        _state.value = UploadState.Idle
        title.value  = ""
    }
}
