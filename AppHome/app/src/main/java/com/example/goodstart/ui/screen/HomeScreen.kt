package com.example.goodstart.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
    "chumash", "tanya", "rambam", "rambamOne", "tehillim", "seferHamitzvot", "shnayimMikra"
)

@Composable
fun HomeScreen(
    onStudyClick: (key: String, date: String, title: String, label: String) -> Unit,
    onZmanimClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLocationZonesClick: () -> Unit = {},
    onRabbenuTamClick: () -> Unit = {},
    onPdfLibraryClick: () -> Unit = {},
    onMamaarimClick: () -> Unit = {},
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
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    PdfLibraryCard(onClick = onPdfLibraryClick)
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    MamaarimCard(onClick = onMamaarimClick)
                }
            }
        }
    }
}

@Composable
private fun StudyCard(study: Study, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        color = CardBg,
        border = BorderStroke(1.dp, LineColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // אייקון/עיגול צבעוני ליד הטקסט במקום הפס בצד
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                // מציג את האות הראשונה של השיעור כסמליל יפהפה
                Text(
                    text = study.title?.firstOrNull()?.toString() ?: "",
                    color = Primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BaHaYetzira
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = study.title ?: "", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold, 
                    fontFamily = BaHaYetzira, 
                    color = Ink, 
                    maxLines = 1
                )
                if (!study.label.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = study.label, 
                        fontSize = 13.sp, 
                        color = Muted, 
                        maxLines = 1
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronLeft, 
                    contentDescription = null, 
                    tint = Primary, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RabbenuTamCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        color = CardBg,
        border = BorderStroke(1.dp, LineColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("ק", color = Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = BaHaYetzira)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("קריאת שמע", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = BaHaYetzira, color = Ink, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text("לפי רבינו תם", fontSize = 13.sp, color = Muted, maxLines = 1)
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(BgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronLeft, null, tint = Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PdfLibraryCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        color = Color(0xFFF0F5F9), // רקע עדין ובהיר יותר
        border = BorderStroke(1.dp, Color(0xFFD0DCE5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF263238).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF263238), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("ספרייה אישית", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = BaHaYetzira, color = Color(0xFF263238), maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text("המשך לימוד אישי מתוך קובץ PDF (גמרא / שיחות)", fontSize = 13.sp, color = Color(0xFF546E7A), maxLines = 2, lineHeight = 18.sp)
            }
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color(0xFF263238), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MamaarimCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        color = Color(0xFFF2F9F2), // רקע ירוק בהיר מאוד
        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("מאמרים", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = BaHaYetzira, color = Color(0xFF1B5E20), maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text("ייבא מאמר מ-PDF · הטקסט יחולץ לקריאה אופליין", fontSize = 13.sp, color = Color(0xFF388E3C), maxLines = 2, lineHeight = 18.sp)
            }
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
            }
        }
    }
}
