package com.example.goodstart.ui.screen

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodstart.model.Section
import com.example.goodstart.ui.theme.*
import com.example.goodstart.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val HE_NUMS = arrayOf(
    "", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט", "י",
    "יא", "יב", "יג", "יד", "טו", "טז", "יז", "יח", "יט", "כ",
    "כא", "כב", "כג", "כד", "כה", "כו", "כז", "כח", "כט", "ל",
    "לא", "לב", "לג", "לד", "לה", "לו", "לז", "לח", "לט", "מ",
    "מא", "מב", "מג", "מד", "מה", "מו", "מז", "מח", "מט", "נ",
    "נא", "נב", "נג", "נד", "נה", "נו", "נז", "נח", "נט", "ס",
    "סא", "סב", "סג", "סד", "סה", "סו", "סז", "סח", "סט", "ע",
    "עא", "עב", "עג", "עד", "עה", "עו"
)

private fun toHebNum(n: Int) = if (n in 1 until HE_NUMS.size) HE_NUMS[n] else n.toString()

@Composable
fun StudyReadingScreen(
    studyKey: String,
    date: String,
    title: String,
    label: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    vm: StudyViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(studyKey, date) { vm.load(studyKey, date, label) }

    val prefs = remember { context.getSharedPreferences("RambamPrefs", Context.MODE_PRIVATE) }
    val fontSize = remember { mutableIntStateOf(prefs.getInt("text_size_sp", 20)) }
    val scrollSpeed = remember { mutableIntStateOf(prefs.getInt("scroll_speed", 40)) }
    val isShnayim = studyKey == "shnayimMikra"
    val isTanya   = studyKey == "tanya"
    val isTehillim = studyKey == "tehillim"
    val connected = remember { context.getSharedPreferences("ShnayimPrefs", Context.MODE_PRIVATE).getBoolean("shnayim_mikra_connected", true) }

    var showCustomChaptersDialog by remember { mutableStateOf(false) }

    if (showCustomChaptersDialog) {
        CustomTehillimChaptersDialog(
            currentChapters = state.customChapters,
            onDismiss = { showCustomChaptersDialog = false },
            onSave = { chapters ->
                vm.saveCustomChapters(chapters)
                showCustomChaptersDialog = false
            }
        )
    }

    val listState = rememberLazyListState()
    var autoScrolling by remember { mutableStateOf(false) }

    val scrollKey = "scroll_${studyKey}_${date}"
    var scrollRestored by remember { mutableStateOf(false) }

    LaunchedEffect(state.sections.isNotEmpty()) {
        if (state.sections.isNotEmpty() && !scrollRestored) {
            val idx = prefs.getInt("${scrollKey}_idx", 0)
            val off = prefs.getInt("${scrollKey}_off", 0)
            if (idx > 0) listState.scrollToItem(idx, off)
            scrollRestored = true
        }
    }

    DisposableEffect(scrollKey) {
        onDispose {
            prefs.edit()
                .putInt("${scrollKey}_idx", listState.firstVisibleItemIndex)
                .putInt("${scrollKey}_off", listState.firstVisibleItemScrollOffset)
                .apply()
        }
    }

    LaunchedEffect(autoScrolling) {
        if (!autoScrolling) return@LaunchedEffect
        val msPerPx = (1000L / scrollSpeed.intValue.coerceAtLeast(1))
        while (isActive && autoScrolling) {
            delay(msPerPx)
            listState.scroll { scrollBy(1f) }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 0.dp, color = Color(0xFFFDFBF7)) {
                Column {
                    Spacer(Modifier.statusBarsPadding())
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "חזור", tint = Primary)
                        }
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            maxLines = 1
                        )
                        if (isTehillim) {
                            IconButton(onClick = { showCustomChaptersDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "הוסף פרקים", tint = Primary)
                            }
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "הגדרות", tint = Primary)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFFDFBF7),
        floatingActionButton = {
            Button(
                onClick = { autoScrolling = !autoScrolling },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.95f), contentColor = Primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f)),
                modifier = Modifier.height(48.dp).padding(bottom = 16.dp)
            ) {
                Icon(if (autoScrolling) Icons.Default.PauseCircle else Icons.Default.PlayCircle, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (autoScrolling) "עצור גלילה" else "גלילה אוטומטית", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
                state.error != null -> Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(state.error!!, color = Muted, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.load(studyKey, date, label) }) { Text("נסה שוב") }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                    ) {
                        if (state.subtitle.isNotEmpty()) {
                            item {
                                Text(
                                    text = state.subtitle,
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 12.dp, right = 16.dp, bottom = 20.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    color = Primary.copy(alpha = 0.7f),
                                    fontFamily = SblHebrew
                                )
                            }
                        }
                        items(state.sections) { section ->
                            // MODERATE BALANCE: 12dp left, 16dp right. Clean but not cut.
                            Box(modifier = Modifier.fillMaxWidth().absolutePadding(left = 12.dp, right = 16.dp)) {
                                SectionRow(section, isShnayim, isTanya, connected, fontSize.intValue)
                            }
                        }
                    }
                }
            }
            // Study Progress Map at the absolute bottom
            if (state.sections.isNotEmpty()) {
                StudyProgressBar(
                    sections = state.sections,
                    listState = listState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun StudyProgressBar(
    sections: List<Section>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val total = sections.size
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    
    // Calculate progress: how much of the list we have scrolled past
    val progress by remember {
        derivedStateOf {
            if (total > 1) {
                (firstVisible.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    // Find markers for headers (chapters, aliyot, etc.)
    val markers = remember(sections) {
        sections.mapIndexedNotNull { index, section ->
            if (section.isHeader || section.isChapterHeader || section.isAliyahHeader) {
                index.toFloat() / total.toFloat()
            } else null
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(Color(0xFFE5E7EB)) // Subtle gray background
    ) {
        val width = size.width
        val height = size.height

        // Since the app is RTL, the drawing logic needs to respect that.
        // In a Box with alignment, coordinates are standard, but we'll draw from right to left
        // or just let it be standard LTR for the bar itself if it feels more natural as a "loading" bar.
        // Usually, progress bars stay LTR. Let's stick to standard progress feel.
        
        // Progress fill (Primary Green)
        drawRect(
            color = Primary,
            size = size.copy(width = width * progress)
        )

        // Chapter/Section Markers
        markers.forEach { markerProgress ->
            val x = width * markerProgress
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

@Composable
private fun SectionRow(section: Section, isShnayim: Boolean, isTanya: Boolean, connected: Boolean, fontSize: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            section.isHeader -> HeaderRow(section, fontSize)
            !section.ordinal.isNullOrEmpty() -> HalachaRow(section, fontSize)
            else -> VerseRow(section, isShnayim, isTanya, connected, fontSize)
        }
    }
}

@Composable
private fun HeaderRow(section: Section, fontSize: Int) {
    val isAliyah = section.isAliyahHeader
    if (isAliyah) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Primary.copy(alpha = 0.15f)))
            Text(
                text = section.he ?: "",
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SblHebrew,
                color = Primary,
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Primary.copy(alpha = 0.15f)))
        }
    } else {
        Text(
            text = section.he ?: "",
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            textAlign = TextAlign.Center,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SblHebrew,
            color = Primary,
            style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
        )
    }
}

@Composable
private fun HalachaRow(section: Section, fontSize: Int) {
    val ordinal = (section.ordinal ?: "") + "\u00A0"
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold, fontSize = (fontSize + 6).sp)) { append(ordinal) }
        append(section.he ?: "")
    }
    Text(
        text = annotated,
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        fontSize = fontSize.sp,
        fontFamily = SblHebrew,
        color = Color.Black,
        lineHeight = (fontSize * 1.8f).sp,
        textAlign = TextAlign.Justify,
        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
    )
}

@Composable
private fun VerseRow(section: Section, isShnayim: Boolean, isTanya: Boolean, connected: Boolean, fontSize: Int) {
    val hasRashi = !section.rashi.isNullOrEmpty()
    val he = section.he ?: ""
    val en = section.en ?: ""
    val showPrefix = !isTanya && section.verseNum != null

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (hasRashi) 20.dp else 6.dp)) {
        if (isShnayim && connected) {
            val annotated = buildAnnotatedString {
                if (showPrefix) { withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold, fontSize = (fontSize - 1).sp)) { append(toHebNum(section.verseNum!!)); append("\u00A0") } }
                append("$he ")
                if (showPrefix) { withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold, fontSize = (fontSize - 1).sp)) { append(toHebNum(section.verseNum!!)); append("\u00A0") } }
                append(he)
                if (en.isNotEmpty()) { append(" "); withStyle(SpanStyle(color = Muted)) { append(en) } }
            }
            Text(
                text = annotated,
                fontSize = fontSize.sp,
                fontFamily = SblHebrew,
                color = Color.Black,
                lineHeight = (fontSize * 1.8f).sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
            )
        } else {
            val annotated = buildAnnotatedString {
                if (showPrefix) {
                    withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold, fontSize = (fontSize - 1).sp)) {
                        append(toHebNum(section.verseNum!!))
                    }
                    append(" ")
                }
                append(he)
            }
            Text(
                text = annotated,
                fontSize = fontSize.sp,
                fontFamily = SblHebrew,
                color = Color.Black,
                lineHeight = (fontSize * 2f).sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
            )
            if (isShnayim) HebrewText(he, fontSize)
            if (en.isNotEmpty()) Text(en, fontSize = maxOf(14, fontSize - 3).sp, color = Muted, lineHeight = (maxOf(14, fontSize - 3) * 1.3f).sp, modifier = Modifier.fillMaxWidth())
        }
        if (hasRashi) RashiBlock(section.rashi!!, fontSize)
    }
}

@Composable
private fun HebrewText(text: String, fontSize: Int) {
    Text(
        text = text,
        fontSize = fontSize.sp,
        fontFamily = SblHebrew,
        color = Color.Black,
        lineHeight = (fontSize * 1.8f).sp,
        textAlign = TextAlign.Justify,
        modifier = Modifier.fillMaxWidth(),
        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
    )
}

@Composable
private fun RashiBlock(rashiList: List<Section.RashiItem>, fontSize: Int) {
    val rashiSize = maxOf(14, fontSize - 3)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
            .background(
                color = Amber.copy(alpha = 0.04f),
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
            )
            .height(IntrinsicSize.Min)
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Amber.copy(alpha = 0.55f)))
        Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 6.dp)) {
            Text("רש\u05F4י", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber, modifier = Modifier.padding(bottom = 6.dp))
            rashiList.forEach { item ->
                if (!item.he.isNullOrEmpty()) {
                    Text(
                        text = item.he,
                        fontSize = rashiSize.sp,
                        fontFamily = SblHebrew,
                        color = Color.Black.copy(alpha = 0.85f),
                        lineHeight = (rashiSize * 1.7f).sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                    )
                }
            }
        }
    }
}

// ── דיאלוג פרקי תהלים אישיים ───────────────────────────────────────────────
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CustomTehillimChaptersDialog(
    currentChapters: List<Int>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit
) {
    var chapters by remember { mutableStateOf(currentChapters.toMutableList()) }
    var inputText by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFDFBF7),
        title = {
            Text(
                "פרקי תהלים אישיים",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "הוסף פרקים שיוצגו בכל יום בסוף השיעור היומי",
                    fontSize = 13.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                )

                // Existing chapters as chips
                if (chapters.isNotEmpty()) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chapters.forEach { ch ->
                            InputChip(
                                selected = false,
                                onClick = { chapters = chapters.toMutableList().also { it.remove(ch) } },
                                label = { Text(com.example.goodstart.util.HebrewDate.formatTehillimLabel(ch.toString()), fontSize = 13.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                colors = InputChipDefaults.inputChipColors(containerColor = Primary.copy(alpha = 0.1f))
                            )
                        }
                    }
                }

                // Input row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it.filter { c -> c.isDigit() }; inputError = false },
                        label = { Text("מספר פרק (1-150)", fontSize = 12.sp) },
                        singleLine = true,
                        isError = inputError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                    )
                    Button(
                        onClick = {
                            val n = inputText.toIntOrNull()
                            if (n != null && n in 1..150 && n !in chapters) {
                                chapters = chapters.toMutableList().also { it.add(n); it.sort() }
                                inputText = ""
                            } else {
                                inputError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("הוסף") }
                }
                if (inputError) {
                    Text("פרק לא תקין (1-150)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(chapters.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("שמור") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול", color = Muted) }
        }
    )
}
