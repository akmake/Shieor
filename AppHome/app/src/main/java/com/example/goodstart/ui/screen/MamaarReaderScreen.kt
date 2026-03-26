package com.example.goodstart.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodstart.model.Mamaar
import com.example.goodstart.model.MamaarSection
import com.example.goodstart.ui.theme.*
import com.example.goodstart.ui.viewmodel.MamaarimViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MamaarReaderScreen(
    mamaarId: String,
    onBack: () -> Unit,
    vm: MamaarimViewModel = viewModel()
) {
    val mamaar by vm.selectedMamaar.collectAsState()

    LaunchedEffect(mamaarId) {
        if (mamaar?.id != mamaarId) {
            vm.loadArticleContent(mamaarId)
        }
    }

    if (mamaar == null || mamaar?.id != mamaarId) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Primary)
                Spacer(Modifier.height(12.dp))
                Text("טוען מאמר משרת...", color = Muted)
            }
        }
        return
    }

    ReaderContent(mamaar = mamaar!!, onBack = {
        vm.clearSelectedMamaar()
        onBack()
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(mamaar: Mamaar, onBack: () -> Unit) {
    val listState    = rememberLazyListState()
    val coroutine    = rememberCoroutineScope()
    var showToc      by remember { mutableStateOf(false) }

    // Table-of-contents drawer
    if (showToc) {
        TocDrawer(
            sections  = mamaar.sections,
            onDismiss = { showToc = false },
            onJump    = { idx ->
                showToc = false
                coroutine.launch { listState.animateScrollToItem(idx + 1) } // +1 for title item
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Surface(shadowElevation = 2.dp, color = Color.White) {
            Column {
                Spacer(Modifier.statusBarsPadding())
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
                        text       = mamaar.title,
                        modifier   = Modifier.weight(1f),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BaHaYetzira,
                        color      = Primary,
                        maxLines   = 1
                    )
                    // Table of contents button – show only if there are headings
                    if (mamaar.sections.any { it.heading != null }) {
                        IconButton(onClick = { showToc = true }) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "תוכן עניינים", tint = Primary)
                        }
                    }
                }
            }
        }

        // ── Content ──────────────────────────────────────────────────────────
        ReaderBody(mamaar = mamaar, listState = listState)
    }
}

@Composable
private fun ReaderBody(mamaar: Mamaar, listState: LazyListState) {
    LazyColumn(
        state           = listState,
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Title block
        item {
            Text(
                text       = mamaar.title,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BaHaYetzira,
                color      = Primary,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            HorizontalDivider(color = LineColor, modifier = Modifier.padding(bottom = 24.dp))
        }

        // Sections
        itemsIndexed(mamaar.sections) { _, section ->
            SectionBlock(section)
            Spacer(Modifier.height(20.dp))
        }

        // Bottom padding for navigation bar
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun SectionBlock(section: MamaarSection) {
    Column {
        // Section heading (e.g. "ב) ויש לקשר…")
        if (!section.heading.isNullOrBlank()) {
            Text(
                text       = section.heading,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BaHaYetzira,
                color      = Primary,
                modifier   = Modifier.padding(bottom = 8.dp)
            )
        }

        // Body paragraphs — split by newline so each is rendered as its own text block
        val paragraphs = section.body
            .split(Regex("\\n{1,}"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        paragraphs.forEach { para ->
            Text(
                text      = para,
                fontSize  = 17.sp,
                fontFamily = BaHaYetzira,
                color     = Ink,
                lineHeight = 28.sp,
                textAlign = TextAlign.Start,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
        }
    }
}

// ─── Table of Contents sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TocDrawer(
    sections: List<MamaarSection>,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text       = "תוכן עניינים",
            modifier   = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = BaHaYetzira,
            color      = Primary
        )
        HorizontalDivider(color = LineColor)

        sections.forEachIndexed { idx, section ->
            val label = when {
                !section.heading.isNullOrBlank() -> section.heading
                idx == 0 -> "פתיחה"
                else     -> "פסקה ${idx + 1}"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJump(idx) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${idx + 1}", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text       = label,
                    fontSize   = 15.sp,
                    fontFamily = BaHaYetzira,
                    color      = Ink,
                    maxLines   = 2
                )
            }
            HorizontalDivider(color = LineColor.copy(alpha = 0.5f))
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}
