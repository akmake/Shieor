package com.example.goodstart.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    "לא", "לב", "לג", "לד", "לה", "לו", "לז", "לח", "לט", "מ",
    "מא", "מב", "מג", "מד", "מה", "מו", "מז", "מח", "מט", "נ",
    "נא", "נב", "נג", "נד", "נה", "נו", "נז", "נח", "נט", "ס"
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
    val shnayimPrefs = remember { context.getSharedPreferences("ShnayimPrefs", Context.MODE_PRIVATE) }
    val fontSize by remember { mutableIntStateOf(prefs.getInt("text_size_sp", 20)) }
    val scrollSpeed by remember { mutableIntStateOf(prefs.getInt("scroll_speed", 40)) }
    val isShnayim = studyKey == "shnayimMikra"
    val isTanya   = studyKey == "tanya"
    val connected = remember { shnayimPrefs.getBoolean("shnayim_mikra_connected", true) }

    val listState = rememberLazyListState()
    var autoScrolling by remember { mutableStateOf(false) }

    // ── Scroll-position memory ───────────────────────────────────────────────
    val scrollKey = "scroll_${studyKey}_${date}"
    var scrollRestored by remember { mutableStateOf(false) }

    // Restore saved position once after sections load
    LaunchedEffect(state.sections.isNotEmpty()) {
        if (state.sections.isNotEmpty() && !scrollRestored) {
            val idx = prefs.getInt("${scrollKey}_idx", 0)
            val off = prefs.getInt("${scrollKey}_off", 0)
            if (idx > 0) listState.scrollToItem(idx, off)
            scrollRestored = true
        }
    }

    // Save position when leaving this screen
    DisposableEffect(scrollKey) {
        onDispose {
            prefs.edit()
                .putInt("${scrollKey}_idx", listState.firstVisibleItemIndex)
                .putInt("${scrollKey}_off", listState.firstVisibleItemScrollOffset)
                .apply()
        }
    }

    // Auto-scroll
    LaunchedEffect(autoScrolling) {
        if (!autoScrolling) return@LaunchedEffect
        val msPerPx = (1000L / scrollSpeed.coerceAtLeast(1))
        while (isActive && autoScrolling) {
            delay(msPerPx)
            listState.scroll { scrollBy(1f) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBF7))
            .statusBarsPadding()
    ) {
        // ── Toolbar (sticky) ────────────────────────────────────────────────
        Surface(shadowElevation = 2.dp, color = Color(0xFFFDFBF7)) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "חזור", tint = Primary)
                    }
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 1
                    )
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "הגדרות", tint = Primary)
                    }
                }
                Divider(color = Primary.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }

        // ── Content ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                state.error != null -> Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = Muted, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.load(studyKey, date, label) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("נסה שוב") }
                }
                else -> {
                    // Dynamic bottom padding: bottom bar (54dp) + nav bar insets
                    val navBarBottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 54.dp + navBarBottom + 16.dp)
                    ) {
                        // Subtitle as first scrollable item (not sticky)
                        if (state.subtitle.isNotEmpty()) {
                            item(key = "subtitle") {
                                Text(
                                    text = state.subtitle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // direction-aware: start=right=16dp, end=left=8dp in RTL
                                        .padding(start = 16.dp, end = 8.dp, bottom = 10.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = Primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        items(state.sections) { section ->
                            // direction-aware padding: start=right=16dp, end=left=8dp in RTL
                            Box(modifier = Modifier.padding(start = 16.dp, end = 8.dp)) {
                                SectionRow(
                                    section = section,
                                    isShnayim = isShnayim,
                                    isTanya = isTanya,
                                    connected = connected,
                                    fontSize = fontSize
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom bar ──────────────────────────────────────────────────────
        Surface(shadowElevation = 4.dp, color = Color(0xFFFDFBF7),
            modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { autoScrolling = !autoScrolling },
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Icon(
                        if (autoScrolling) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (autoScrolling) "עצור גלילה" else "גלילה אוטומטית", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: Section,
    isShnayim: Boolean,
    isTanya: Boolean,
    connected: Boolean,
    fontSize: Int
) {
    when {
        section.isHeader -> HeaderRow(section, fontSize)
        !section.ordinal.isNullOrEmpty() -> HalachaRow(section, fontSize)
        else -> VerseRow(section, isShnayim, isTanya, connected, fontSize)
    }
}

@Composable
private fun HeaderRow(section: Section, fontSize: Int) {
    val isAliyah = section.isAliyahHeader
    Text(
        text = section.he ?: "",
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isAliyah) 24.dp else 8.dp,
                bottom = if (isAliyah) 6.dp else 4.dp
            ),
        textAlign = TextAlign.Center,
        fontSize = if (isAliyah) 21.sp else 30.sp,
        fontWeight = if (isAliyah) FontWeight.Normal else FontWeight.Bold,
        fontFamily = SblHebrew,
        color = Primary,
        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
    )
}

@Composable
private fun HalachaRow(section: Section, fontSize: Int) {
    val ordinal = (section.ordinal ?: "") + "\u00A0"
    val he = section.he ?: ""
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(
            color = Primary,
            fontWeight = FontWeight.Bold,
            fontSize = (fontSize + 6).sp
        )) { append(ordinal) }
        append(he)
    }
    Text(
        text = annotated,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        fontSize = fontSize.sp,
        fontFamily = SblHebrew,
        color = Color.Black,
        lineHeight = (fontSize * 1.3f).sp,
        textAlign = TextAlign.Justify,
        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
    )
}

@Composable
private fun VerseRow(
    section: Section,
    isShnayim: Boolean,
    isTanya: Boolean,
    connected: Boolean,
    fontSize: Int
) {
    val prefix = if (!isTanya && section.verseNum != null) "${toHebNum(section.verseNum)}.\u00A0" else ""
    val he = section.he ?: ""
    val en = section.en ?: ""

    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        if (isShnayim && connected) {
            // Two readings + translation on one block
            val annotated = buildAnnotatedString {
                append("$prefix$he $prefix$he")
                if (en.isNotEmpty()) {
                    append(" ")
                    withStyle(SpanStyle(color = Muted)) { append(en) }
                }
            }
            Text(
                text = annotated,
                fontSize = fontSize.sp,
                fontFamily = SblHebrew,
                color = Color.Black,
                lineHeight = (fontSize * 1.3f).sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
            )
        } else if (isShnayim) {
            // Separated
            HebrewText("$prefix$he", fontSize)
            HebrewText("$prefix$he", fontSize)
            if (en.isNotEmpty()) Text(en, fontSize = (fontSize - 3).sp, color = Muted,
                lineHeight = ((fontSize - 3) * 1.3f).sp, modifier = Modifier.fillMaxWidth())
        } else {
            HebrewText("$prefix$he", fontSize)
            if (en.isNotEmpty()) Text(
                en,
                fontSize = maxOf(14, fontSize - 3).sp,
                color = Muted,
                lineHeight = (maxOf(14, fontSize - 3) * 1.3f).sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Rashi block
        if (!section.rashi.isNullOrEmpty()) {
            RashiBlock(section.rashi, fontSize)
        }
    }
}

@Composable
private fun HebrewText(text: String, fontSize: Int) {
    Text(
        text = text,
        fontSize = fontSize.sp,
        fontFamily = SblHebrew,
        color = Color.Black,
        lineHeight = (fontSize * 1.3f).sp,
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
            .padding(top = 6.dp, bottom = 4.dp)
            .height(IntrinsicSize.Min)
    ) {
        // Right border (start in RTL = right)
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Primary)
        )
        Column(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
            Text(
                "רש\u05F4י",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Amber,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )
            rashiList.forEach { item ->
                if (!item.he.isNullOrEmpty()) {
                    Text(
                        text = item.he,
                        fontSize = rashiSize.sp,
                        fontFamily = SblHebrew,
                        color = Color.Black,
                        lineHeight = (rashiSize * 1.3f).sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                    )
                }
            }
        }
    }
}
