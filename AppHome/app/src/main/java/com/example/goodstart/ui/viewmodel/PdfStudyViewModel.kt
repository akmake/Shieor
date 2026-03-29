package com.example.goodstart.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

data class PdfBook(
    val id: String,
    val uriString: String,
    val title: String,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isLandscape: Boolean = false
)

data class PdfState(
    val books: List<PdfBook> = emptyList(),
    val currentBook: PdfBook? = null,
    val currentBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)

class PdfStudyViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val prefs = ctx.getSharedPreferences("PdfLibrary", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = MutableStateFlow(PdfState())
    val state = _state.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPdfPage: PdfRenderer.Page? = null

    init {
        loadBooks()
    }

    private fun loadBooks() {
        val json = prefs.getString("books_list", "[]")
        val type = object : TypeToken<List<PdfBook>>() {}.type
        val books: List<PdfBook> = try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
        _state.value = _state.value.copy(books = books)
    }

    private fun saveBooks(books: List<PdfBook>) {
        val jsonStr = gson.toJson(books)
        prefs.edit().putString("books_list", jsonStr).apply()
        _state.value = _state.value.copy(books = books)
    }

    fun addBook(uri: Uri, title: String, isLandscape: Boolean) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            ctx.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            Log.e("PdfStudyVM", "Could not take persistable perm", e)
        }
        val newBook = PdfBook(
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            title = title,
            isLandscape = isLandscape
        )
        val updatedList = _state.value.books + newBook
        saveBooks(updatedList)
    }

    fun removeBook(bookId: String) {
        val updatedList = _state.value.books.filter { it.id != bookId }
        saveBooks(updatedList)
    }

    fun openBook(bookId: String) {
        val book = _state.value.books.find { it.id == bookId } ?: return
        _state.value = _state.value.copy(currentBook = book, errorMsg = null)
        closePdf()
        loadPdf(Uri.parse(book.uriString), book.currentPage)
    }

    fun closeBook() {
        closePdf()
        _state.value = _state.value.copy(currentBook = null, currentBitmap = null)
        loadBooks()
    }

    private fun loadPdf(uri: Uri, startPage: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true)
            try {
                fileDescriptor = ctx.contentResolver.openFileDescriptor(uri, "r")
                if (fileDescriptor == null) throw IOException("Cannot open file")

                pdfRenderer = PdfRenderer(fileDescriptor!!)
                val total = pdfRenderer!!.pageCount
                val pageToLoad = if (startPage in 0 until total) startPage else 0
                
                val activeBook = _state.value.currentBook!!
                val updatedBook = activeBook.copy(totalPages = total)
                
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(currentBook = updatedBook)
                    updateBookInList(updatedBook)
                }

                renderPage(pageToLoad)

            } catch (e: Exception) {
                Log.e("PdfStudyVM", "Error loading PDF", e)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMsg = "Error loading",
                        currentBook = null
                    )
                }
            }
        }
    }

    private fun updateBookInList(book: PdfBook) {
        val currentBooks = _state.value.books.toMutableList()
        val index = currentBooks.indexOfFirst { it.id == book.id }
        if (index != -1) {
            currentBooks[index] = book
            saveBooks(currentBooks)
        }
    }

    fun nextPage() {
        val book = _state.value.currentBook ?: return
        if (book.currentPage < book.totalPages - 1) {
            renderPage(book.currentPage + 1)
        }
    }

    fun prevPage() {
        val book = _state.value.currentBook ?: return
        if (book.currentPage > 0) {
            renderPage(book.currentPage - 1)
        }
    }

    fun goToPage(pageIndex: Int) {
        val book = _state.value.currentBook ?: return
        val safePage = pageIndex.coerceIn(0, book.totalPages - 1)
        renderPage(safePage)
    }

    private fun renderPage(pageIndex: Int) {
        val activeBook = _state.value.currentBook ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true)
            try {
                currentPdfPage?.close()
                currentPdfPage = pdfRenderer?.openPage(pageIndex)

                val page = currentPdfPage
                if (page != null) {
                    val screenWidth = ctx.resources.displayMetrics.widthPixels
                    
                    // המרה חכמה ובטוחה של רזולוציה:
                    // במקום להכפיל פשוט ב-3 ולהקריס את הזיכרון (OOM), אנו מחשבים רזולוציה מקסימלית:
                    // בדרך כלל המקור של דף PDF הוא כ-600 פיקסלים.
                    // נשאף לאיכות טובה של פי 2 מרוחב המסך או מקסימום 2000 פיקסלים כדי לא לדרוס את ה-GPU.
                    val targetWidth = minOf(screenWidth * 2, 2000)
                    val targetHeight = (targetWidth.toFloat() * page.height / page.width).toInt()

                    // מניעת קריסות באנדרואידים עם מעט RAM - הגבלנו ל-2000 פיקסלים.
                    // PdfRenderer דורש ARGB_8888 ולכן החזרנו לזה, הפתרון לקריסה נמצא בחיתוך הגודל למעלה!
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val updatedBook = activeBook.copy(currentPage = pageIndex)

                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            currentBook = updatedBook,
                            currentBitmap = bitmap,
                            isLoading = false
                        )
                        updateBookInList(updatedBook)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isLoading = false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        closePdf()
    }

    private fun closePdf() {
        currentPdfPage?.close()
        pdfRenderer?.close()
        fileDescriptor?.close()
        currentPdfPage = null
        pdfRenderer = null
        fileDescriptor = null
    }
}
