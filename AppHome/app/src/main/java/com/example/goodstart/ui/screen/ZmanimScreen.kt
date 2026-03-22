package com.example.goodstart.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodstart.ui.theme.*
import com.example.goodstart.ui.viewmodel.ZmanimViewModel
import com.example.goodstart.util.HebrewDate

@Composable
fun ZmanimScreen(
    onBack: () -> Unit,
    vm: ZmanimViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── Toolbar ─────────────────────────────────────────────────────────
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
                        "זמנים הלכתיים",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(Modifier.width(48.dp))
                }
            }
        }

        // ── City buttons ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vm.cityNames.forEachIndexed { idx, name ->
                val selected = idx == state.selectedCity
                OutlinedButton(
                    onClick = { vm.selectCity(idx) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Primary else Color.Transparent,
                        contentColor = if (selected) Color.White else Muted
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(name, fontSize = 13.sp, maxLines = 1)
                }
            }
        }

        // ── Date navigator ──────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 0.dp,
            color = Color(0xFFFDFBF7),
            border = androidx.compose.foundation.BorderStroke(1.dp, LineColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { vm.shiftDate(+1) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "הבא", tint = Primary)
                }
                Text(
                    text = HebrewDate.format(state.date),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BaHaYetzira,
                    color = Primary
                )
                IconButton(onClick = { vm.shiftDate(-1) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "הקודם", tint = Primary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Zmanim list (CLEAN VERSION) ─────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.error!!, color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // ASYMMETRIC PADDING: Left=8dp, Right=16dp
                        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, bottom = 32.dp)
                    ) {
                        items(state.zmanim) { zman ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = zman.label,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 17.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = zman.time,
                                    fontSize = 18.sp,
                                    color = Primary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(
                                color = LineColor.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
