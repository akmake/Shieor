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
        Surface(shadowElevation = 2.dp, color = CardBg) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "חזור", tint = Ink)
                }
                Text(
                    "זמנים הלכתיים",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Spacer(Modifier.width(48.dp))
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
                        containerColor = if (selected) Ink else Color.Transparent,
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
            shadowElevation = 1.dp,
            color = CardBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { vm.shiftDate(+1) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "הבא", tint = Ink)
                }
                Text(
                    text = HebrewDate.format(state.date),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BaHaYetzira,
                    color = Ink
                )
                IconButton(onClick = { vm.shiftDate(-1) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "הקודם", tint = Ink)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Zmanim list ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.error!!, color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
                else -> Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 2.dp,
                    color = CardBg
                ) {
                    LazyColumn {
                        items(state.zmanim) { zman ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = zman.label,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp,
                                    color = Ink
                                )
                                Text(
                                    text = zman.time,
                                    fontSize = 16.sp,
                                    color = Ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (state.zmanim.last() != zman) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = LineColor
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
