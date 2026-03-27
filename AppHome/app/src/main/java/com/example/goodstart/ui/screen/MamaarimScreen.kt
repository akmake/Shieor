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
import androidx.compose.runtime.*
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
import com.example.goodstart.model.Mamaar
import com.example.goodstart.ui.theme.*
import com.example.goodstart.ui.viewmodel.MamaarimViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MamaarimScreen(
    onBack:    () -> Unit,
    onOpen:    (String) -> Unit,
    onUpload:  () -> Unit = {},
    vm: MamaarimViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    // Show error snackbar
    val snackHost = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackHost.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackHost) },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onUpload,
                containerColor    = Primary,
                contentColor      = Color.White,
                shape             = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "העלה מאמר")
            }
        },
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "חזור", tint = Primary)
                        }
                        Text(
                            text = "מאמרים",
                            modifier = Modifier.weight(1f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BaHaYetzira,
                            color = Primary
                        )
                        IconButton(onClick = { vm.loadAll() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "רענן", tint = Primary)
                        }
                    }
                }
            }
        },
        containerColor = BgColor
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (state.mamaarim.isEmpty() && !state.isLoading) {
                EmptyState(onRefresh = { vm.loadAll() })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.mamaarim, key = { it.id }) { mamaar ->
                        MamaarCard(
                            mamaar  = mamaar,
                            onClick  = { onOpen(mamaar.id) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(Modifier.height(12.dp))
                        Text("טוען מאמרים...", color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ─── sub-composables ─────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.3f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "אין מאמרים עדיין",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BaHaYetzira,
                color = Ink
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "לחץ לרענון המאמרים מהשרת",
                fontSize = 14.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("רענן משרת")
            }
        }
    }
}

@Composable
private fun MamaarCard(mamaar: Mamaar, onClick: () -> Unit) {
    Surface(
        modifier      = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape         = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        color         = CardBg
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .background(Primary, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = mamaar.title,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BaHaYetzira,
                    color      = Ink,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = formatDate(mamaar.createdAt),
                    fontSize = 12.sp,
                    color    = Muted
                )
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return sdf.format(Date(ms))
}
