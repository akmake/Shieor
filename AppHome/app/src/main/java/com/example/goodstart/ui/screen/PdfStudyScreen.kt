package com.example.goodstart.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodstart.ui.theme.Primary
import com.example.goodstart.ui.viewmodel.PdfStudyViewModel

fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                result = cursor.getString(index)
            }
        }
    }
    return result ?: uri.lastPathSegment ?: "ספר חדש"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfStudyScreen(onBack: () -> Unit, vm: PdfStudyViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf<Uri?>(null) }
    var newBookTitle by remember { mutableStateOf("") }
    var newBookLandscape by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            newBookTitle = getFileName(context, uri).replace(".pdf", "", ignoreCase = true)
            showAddDialog = uri
        }
    }

    if (showAddDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = null },
            title = { Text("הוסף ספר לספרייה") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newBookTitle,
                        onValueChange = { newBookTitle = it },
                        label = { Text("שם הספר") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newBookLandscape, onCheckedChange = { newBookLandscape = it })
                        Text("קריאה לרוחב (Landscape)")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addBook(showAddDialog!!, newBookTitle, newBookLandscape)
                    showAddDialog = null
                }) { Text("שמור") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = null }) { Text("ביטול") }
            }
        )
    }

    val currentBook = state.currentBook
    if (currentBook == null) {
        DisposableEffect(Unit) {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDispose { }
        }

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBF7))) {
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Column {
                    Spacer(Modifier.statusBarsPadding())
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowForward, "Back", tint = Primary)
                        }
                        Text(
                            text = "ארון הספרים שלי",
                            modifier = Modifier.weight(1f),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        IconButton(onClick = { launcher.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.Add, "Add Book", tint = Primary)
                        }
                    }
                }
            }

            if (state.books.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("הספרייה שלך ריקה", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { launcher.launch(arrayOf("application/pdf")) }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("הוסף קובץ PDF")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(state.books) { book ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { vm.openBook(book.id) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val pageText = if (book.totalPages > 0) "עמוד ${book.currentPage + 1} מתוך ${book.totalPages}" else "לא נפתח עדיין"
                                    val orientText = if (book.isLandscape) "תצוגה לרוחב" else "תצוגה לאורך"
                                    Text("$pageText • $orientText", fontSize = 14.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { vm.removeBook(book.id) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        DisposableEffect(currentBook.isLandscape) {
            val activity = context as? Activity
            if (currentBook.isLandscape) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        var showControls by remember { mutableStateOf(true) }
        var showPageJumpDialog by remember { mutableStateOf(false) }
        var pageJumpText by remember { mutableStateOf("") }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        // Page jump dialog
        if (showPageJumpDialog) {
            pageJumpText = (currentBook.currentPage + 1).toString()
            AlertDialog(
                onDismissRequest = { showPageJumpDialog = false },
                title = { Text("מעבר לעמוד") },
                text = {
                    Column {
                        Text("הקלד מספר עמוד (1-${currentBook.totalPages})")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pageJumpText,
                            onValueChange = { pageJumpText = it.filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(onGo = {
                                val page = pageJumpText.toIntOrNull()
                                if (page != null && page in 1..currentBook.totalPages) {
                                    vm.goToPage(page - 1)
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                }
                                showPageJumpDialog = false
                            }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val page = pageJumpText.toIntOrNull()
                        if (page != null && page in 1..currentBook.totalPages) {
                            vm.goToPage(page - 1)
                            scale = 1f; offsetX = 0f; offsetY = 0f
                        }
                        showPageJumpDialog = false
                    }) { Text("עבור", color = Primary) }
                },
                dismissButton = {
                    TextButton(onClick = { showPageJumpDialog = false }) { Text("ביטול") }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    // גלילה בין עמודים + זום — ללא ripple
                    .pointerInput(Unit) {
                        var swipeAccX = 0f
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (zoom != 1f) {
                                // מחווה של זום
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                                // Clamp offsets to keep image within bounds
                                val maxOffsetX = (containerSize.width * (scale - 1f)) / 2f
                                val maxOffsetY = (containerSize.height * (scale - 1f)) / 2f
                                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                swipeAccX = 0f
                            } else if (scale > 1f) {
                                // גרירה כשמוזם — עם הגבלת גבולות
                                offsetX += pan.x
                                offsetY += pan.y
                                val maxOffsetX = (containerSize.width * (scale - 1f)) / 2f
                                val maxOffsetY = (containerSize.height * (scale - 1f)) / 2f
                                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                swipeAccX = 0f
                            } else {
                                // החלקה לשינוי עמוד (scale = 1) — בלי תזוזה צידית!
                                // ב-RTL: ימינה = עמוד קודם, שמאלה = עמוד הבא
                                swipeAccX += pan.x
                                if (swipeAccX > 120f) {
                                    vm.nextPage()
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                    swipeAccX = 0f
                                } else if (swipeAccX < -120f) {
                                    vm.prevPage()
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                    swipeAccX = 0f
                                }
                            }
                        }
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showControls = !showControls },
                contentAlignment = Alignment.Center
            ) {
                if (state.currentBitmap != null) {
                    Image(
                        bitmap = state.currentBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            if (showControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.closeBook() }) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                    Text(
                        text = currentBook.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "${currentBook.currentPage + 1}/${currentBook.totalPages}",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { showPageJumpDialog = true }
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ב-RTL: הראשון בשורה מופיע מימין — עמוד קודם (ימין = אחורה בעברית)
                    FilledIconButton(
                        onClick = {
                            vm.prevPage()
                            scale = 1f; offsetX = 0f; offsetY = 0f
                        },
                        enabled = currentBook.currentPage > 0,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.ChevronRight, "Prev Page", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "עמוד ${currentBook.currentPage + 1}",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { showPageJumpDialog = true }
                        )
                        if (currentBook.totalPages > 1) {
                            Slider(
                                value = currentBook.currentPage.toFloat(),
                                onValueChange = { newVal ->
                                    val page = newVal.toInt().coerceIn(0, currentBook.totalPages - 1)
                                    vm.goToPage(page)
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                },
                                valueRange = 0f..(currentBook.totalPages - 1).toFloat(),
                                modifier = Modifier.fillMaxWidth().height(24.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Primary,
                                    activeTrackColor = Primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // האחרון בשורה מופיע משמאל — עמוד הבא (שמאל = קדימה בעברית)
                    FilledIconButton(
                        onClick = {
                            vm.nextPage()
                            scale = 1f; offsetX = 0f; offsetY = 0f
                        },
                        enabled = currentBook.currentPage < currentBook.totalPages - 1,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.ChevronLeft, "Next Page", tint = Color.White)
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
