package com.example.goodstart.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.UploadFile
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
import com.example.goodstart.ui.theme.*
import com.example.goodstart.ui.viewmodel.ArticleUploadViewModel
import com.example.goodstart.ui.viewmodel.UploadState

@Composable
fun ArticleUploadScreen(
    onBack:    () -> Unit,
    onSuccess: () -> Unit,
    vm: ArticleUploadViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val title by vm.title.collectAsState()

    // ניווט אחרי הצלחה
    LaunchedEffect(state) {
        if (state is UploadState.Success) {
            onSuccess()
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.onPdfPicked(it) } }

    Scaffold(
        topBar = {
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
                        IconButton(onClick = { vm.reset(); onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "חזור", tint = Primary)
                        }
                        Text(
                            text           = "העלאת מאמר",
                            modifier       = Modifier.weight(1f),
                            fontSize       = 20.sp,
                            fontWeight     = FontWeight.Bold,
                            fontFamily     = BaHaYetzira,
                            color          = Primary,
                            textAlign      = TextAlign.Center
                        )
                        Spacer(Modifier.width(48.dp))
                    }
                }
            }
        },
        containerColor = BgColor
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {

                // ── בחירת קובץ ────────────────────────────────────────────────
                is UploadState.Idle -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector     = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint            = Primary.copy(alpha = 0.4f),
                            modifier        = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text       = "בחר קובץ PDF להעלאה",
                            fontSize   = 17.sp,
                            color      = Ink,
                            fontFamily = BaHaYetzira,
                            textAlign  = TextAlign.Center
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { pdfPicker.launch("application/pdf") },
                            colors  = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape   = RoundedCornerShape(12.dp)
                        ) {
                            Text("בחר PDF", fontSize = 16.sp)
                        }
                    }
                }

                // ── חילוץ טקסט ────────────────────────────────────────────────
                is UploadState.Extracting -> {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(Modifier.height(16.dp))
                        Text("מחלץ טקסט מה-PDF...", color = Muted, fontSize = 15.sp)
                    }
                }

                // ── תצוגה מקדימה + שמירה ────────────────────────────────────
                is UploadState.Preview -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // כותרת
                        OutlinedTextField(
                            value         = title,
                            onValueChange = { vm.title.value = it },
                            label         = { Text("כותרת המאמר") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            shape         = RoundedCornerShape(10.dp)
                        )

                        // מידע
                        Text(
                            text     = "${s.pageCount} עמודים · ${s.rawText.length} תווים",
                            fontSize = 13.sp,
                            color    = Muted
                        )

                        // תצוגה מקדימה
                        Surface(
                            modifier        = Modifier.fillMaxWidth(),
                            shape           = RoundedCornerShape(12.dp),
                            color           = CardBg,
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text     = s.rawText.take(600) + if (s.rawText.length > 600) "…" else "",
                                modifier = Modifier.padding(14.dp),
                                fontSize = 14.sp,
                                color    = Ink,
                                lineHeight = 22.sp
                            )
                        }

                        Row(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick  = { vm.reset() },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp)
                            ) { Text("בחר מחדש") }

                            Button(
                                onClick  = { vm.upload() },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape    = RoundedCornerShape(10.dp)
                            ) { Text("שמור מאמר") }
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }

                // ── שמירה ─────────────────────────────────────────────────────
                is UploadState.Uploading -> {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(Modifier.height(16.dp))
                        Text("שומר מאמר...", color = Muted, fontSize = 15.sp)
                    }
                }

                // ── שגיאה ─────────────────────────────────────────────────────
                is UploadState.Error -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text      = s.message,
                            color     = Color.Red,
                            fontSize  = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { vm.reset() },
                            colors  = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("נסה שוב") }
                    }
                }

                is UploadState.Success -> { /* handled by LaunchedEffect */ }
            }
        }
    }
}
