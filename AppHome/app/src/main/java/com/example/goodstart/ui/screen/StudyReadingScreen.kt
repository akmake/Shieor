package com.example.goodstart.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
    "לא", "לב", "לז", "לח", "לט", "מ", "נ", "ס"
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
    val connected = remember { context.getSharedPreferences("ShnayimPrefs", Context.MODE_PRIVATE).getBoolean("shnayim_mikra_connected", true) }

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
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 4.dp, right = 16.dp, bottom = 20.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    color = Primary.copy(alpha = 0.7f),
                                    fontFamily = SblHebrew
                                )
                            }
                        }
                        items(state.sections) { section ->
                            // Reduced Left padding to 2dp (even smaller than before for perfect fit), Right 20dp
                            Box(modifier = Modifier.fillMaxWidth().absolutePadding(left = 2.dp, right = 20.dp)) {
                                SectionRow(section, isShnayim, isTanya, connected, fontSize.intValue)
                            }
                        }
                    }
                }
            }
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
    Text(
        text = section.he ?: "",
        modifier = Modifier.fillMaxWidth().padding(top = if (isAliyah) 24.dp else 12.dp, bottom = 12.dp),
        textAlign = TextAlign.Center,
        fontSize = if (isAliyah) 21.sp else 32.sp,
        fontWeight = if (isAliyah) FontWeight.Normal else FontWeight.Bold,
        fontFamily = SblHebrew,
        color = Primary,
        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
    )
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
    val prefix = if (!isTanya && section.verseNum != null) "${toHebNum(section.verseNum)}.\u00A0" else ""
    val he = section.he ?: ""
    val en = section.en ?: ""

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (isShnayim && connected) {
            val annotated = buildAnnotatedString {
                append("$prefix$he $prefix$he")
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
            HebrewText("$prefix$he", fontSize)
            if (isShnayim) HebrewText("$prefix$he", fontSize)
            if (en.isNotEmpty()) Text(en, fontSize = maxOf(14, fontSize - 3).sp, color = Muted, lineHeight = (maxOf(14, fontSize - 3) * 1.3f).sp, modifier = Modifier.fillMaxWidth())
        }
        if (!section.rashi.isNullOrEmpty()) RashiBlock(section.rashi, fontSize)
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
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp).height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Primary.copy(alpha = 0.6f)))
        Column(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
            Text("רש\u05F4י", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber, modifier = Modifier.padding(bottom = 4.dp))
            rashiList.forEach { item ->
                if (!item.he.isNullOrEmpty()) {
                    Text(
                        text = item.he,
                        fontSize = rashiSize.sp,
                        fontFamily = SblHebrew,
                        color = Color.Black.copy(alpha = 0.85f),
                        lineHeight = (rashiSize * 1.4f).sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                    )
                }
            }
        }
    }
}
