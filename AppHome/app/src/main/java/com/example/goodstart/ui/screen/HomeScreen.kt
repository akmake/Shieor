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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodstart.model.Study
import com.example.goodstart.ui.theme.*
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
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0b5e57), Primary, Color(0xFF14a89d))
                    )
                )
                .statusBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top action icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End   // left in RTL = end
                ) {
                    IconButton(onClick = onLocationZonesClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.LocationOn, null,
                            tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onZmanimClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.AccessTime, null,
                            tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Settings, null,
                            tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                    }
                }

                // App name
                Text(
                    text = "שיעור",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f),
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(6.dp))

                // Hebrew date + navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { vm.shiftDate(+1) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, null,
                            tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Text(
                        text = HebrewDate.format(state.date),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BaHaYetzira,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = { vm.shiftDate(-1) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, null,
                            tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // ── CONTENT ──────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                state.loading -> item {
                    Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                state.error != null -> item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error!!, color = Muted, fontSize = 14.sp,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("נסה שוב") }
                    }
                }

                else -> {
                    val studies = STUDY_ORDER.mapNotNull { key ->
                        state.day?.studies?.get(key)?.takeIf { it.available }
                    }
                    items(studies, key = { it.key ?: it.title ?: "" }) { study ->
                        StudyCard(study = study, onClick = {
                            onStudyClick(
                                study.key ?: "",
                                state.date,
                                study.title ?: "",
                                study.label ?: ""
                            )
                        })
                    }
                }
            }

            // Divider before Rabbenu Tam
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = LineColor
                )
            }

            // Rabbenu Tam navigation card
            item {
                RabbenuTamCard(onClick = onRabbenuTamClick)
            }
        }
    }
}

// ── Study card ────────────────────────────────────────────────────────────────

@Composable
private fun StudyCard(study: Study, onClick: () -> Unit) {
    val accent = accentColor(study.accent)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        color = CardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent strip — physical right (RTL start = first child)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = study.title ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BaHaYetzira,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!study.label.isNullOrEmpty()) {
                    Text(
                        text = study.label,
                        fontSize = 12.sp,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = LineColor,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(20.dp)
            )
        }
    }
}

// ── Rabbenu Tam card ──────────────────────────────────────────────────────────

@Composable
private fun RabbenuTamCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        color = Primary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Primary)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "קריאת שמע",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BaHaYetzira,
                    color = Primary
                )
                Text(
                    "לפי רבינו תם",
                    fontSize = 12.sp,
                    color = Primary.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(20.dp)
            )
        }
    }
}
