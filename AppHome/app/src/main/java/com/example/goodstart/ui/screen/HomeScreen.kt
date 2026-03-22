package com.example.goodstart.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodstart.model.Study
import com.example.goodstart.ui.theme.BaHaYetzira
import com.example.goodstart.ui.theme.BgColor
import com.example.goodstart.ui.theme.CardBg
import com.example.goodstart.ui.theme.Ink
import com.example.goodstart.ui.theme.LineColor
import com.example.goodstart.ui.theme.Muted
import com.example.goodstart.ui.theme.Primary
import com.example.goodstart.ui.viewmodel.HomeViewModel
import com.example.goodstart.util.HebrewDate

private val STUDY_ORDER = listOf(
    "chumash", "tanya", "rambam", "rambamOne", "seferHamitzvot", "shnayimMikra"
)

@Composable
fun HomeScreen(
    onStudyClick: (key: String, date: String, title: String, label: String) -> Unit,
    onZmanimClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLocationZonesClick: () -> Unit = {},
    onRabbenuTamClick: () -> Unit = {},
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── HERO HEADER ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgColor)
                .statusBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top action icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onLocationZonesClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onZmanimClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.AccessTime, null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Settings, null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                }

                // Day name (replacing "שיעור")
                Text(
                    text = HebrewDate.getDayName(state.date),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )

                Spacer(Modifier.height(4.dp))

                // Hebrew date + navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { vm.shiftDate(+1) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ChevronRight, null, tint = Primary, modifier = Modifier.size(26.dp))
                    }
                    Text(
                        text = HebrewDate.format(state.date),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BaHaYetzira,
                        color = Primary,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { vm.shiftDate(-1) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Primary, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        // ── CONTENT ──────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                    }
                }
            } else if (state.error != null) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error!!, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.retry() }) { Text("נסה שוב") }
                    }
                }
            } else {
                val studies = STUDY_ORDER.mapNotNull { key ->
                    state.day?.studies?.get(key)?.takeIf { it.available }
                }
                items(studies, key = { it.key ?: it.title ?: "" }) { study ->
                    StudyCard(study = study, onClick = {
                        onStudyClick(study.key ?: "", state.date, study.title ?: "", study.label ?: "")
                    })
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LineColor.copy(alpha = 0.5f))
                }
                item {
                    RabbenuTamCard(onClick = onRabbenuTamClick)
                }
            }
        }
    }
}

@Composable
private fun StudyCard(study: Study, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        color = CardBg
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(Primary))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.Center) {
                Text(study.title ?: "", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = BaHaYetzira, color = Ink, maxLines = 1)
                if (!study.label.isNullOrEmpty()) {
                    Text(study.label, fontSize = 12.sp, color = Muted, maxLines = 1)
                }
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Primary, modifier = Modifier.padding(end = 14.dp).size(20.dp))
        }
    }
}

@Composable
private fun RabbenuTamCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        color = CardBg
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(Primary))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.Center) {
                Text("קריאת שמע", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = BaHaYetzira, color = Ink, maxLines = 1)
                Text("לפי רבינו תם", fontSize = 12.sp, color = Muted, maxLines = 1)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Primary, modifier = Modifier.padding(end = 14.dp).size(20.dp))
        }
    }
}
