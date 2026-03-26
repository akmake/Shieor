package com.example.goodstart.ui.screen

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goodstart.alarm.AlarmConfig
import com.example.goodstart.ui.theme.Primary
import com.example.goodstart.ui.viewmodel.ZmanEntry
import com.example.goodstart.ui.viewmodel.ZmanimViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSetupBottomSheet(
    zman: ZmanEntry,
    existingAlarm: AlarmConfig?,
    vm: ZmanimViewModel,
    onDismiss: () -> Unit,
    onAlarmSetResult: (success: Boolean) -> Unit
) {
    var offsetMinutes by remember { mutableIntStateOf(existingAlarm?.offsetMinutes ?: 0) }
    var isBefore      by remember { mutableStateOf(existingAlarm?.isBefore ?: true) }
    var ringCount     by remember { mutableIntStateOf(existingAlarm?.ringCount ?: 3) }
    var ringtoneUri   by remember { mutableStateOf(existingAlarm?.ringtoneUri ?: "") }
    var ringtoneName  by remember { mutableStateOf(if (existingAlarm?.ringtoneUri?.isNotEmpty() == true) "צלצול נבחר" else "ברירת מחדל") }

    val context = LocalContext.current
    val tz  = remember { TimeZone.getTimeZone("Asia/Jerusalem") }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz } }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            else
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            ringtoneUri  = uri?.toString() ?: ""
            ringtoneName = if (uri != null)
                RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "צלצול נבחר"
            else "ברירת מחדל"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFDFBF7)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                text = "שעון מעורר — ${zman.label}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text("זמן: ${zman.time}", fontSize = 15.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))

            // Live preview of the actual alarm time
            val offsetMs    = offsetMinutes * 60_000L
            val alarmTimeMs = if (isBefore) zman.timeMillis - offsetMs else zman.timeMillis + offsetMs
            Text(
                text = "התראה תצלצל בשעה: ${fmt.format(Date(alarmTimeMs))}",
                fontSize = 14.sp,
                color = Primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Offset picker ────────────────────────────────────────────────
            Text("קיזוז זמן", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (offsetMinutes > 0) offsetMinutes-- }) {
                    Text("−", fontSize = 24.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$offsetMinutes דקות",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(100.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { if (offsetMinutes < 60) offsetMinutes++ }) {
                    Text("+", fontSize = 24.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Before / After chips ─────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = isBefore,
                    onClick  = { isBefore = true },
                    label    = { Text("לפני הזמן") }
                )
                FilterChip(
                    selected = !isBefore,
                    onClick  = { isBefore = false },
                    label    = { Text("אחרי הזמן") }
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Ring count ───────────────────────────────────────────────────
            Text("מספר צלצולים", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { n ->
                    val selected = n == ringCount
                    OutlinedButton(
                        onClick = { ringCount = n },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) Primary else Color.Transparent,
                            contentColor   = if (selected) Color.White else Primary
                        )
                    ) {
                        Text("$n", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Ringtone picker ──────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "בחר צלצול")
                        if (ringtoneUri.isNotEmpty())
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(ringtoneUri))
                    }
                    ringtoneLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Primary)
                Spacer(Modifier.width(8.dp))
                Text("צלצול: $ringtoneName", color = Primary)
            }

            Spacer(Modifier.height(20.dp))

            // ── Action buttons ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingAlarm != null) {
                    OutlinedButton(
                        onClick = { vm.cancelAlarm(zman.label); onDismiss() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("בטל התראה")
                    }
                }
                Button(
                    onClick = {
                        val config = AlarmConfig(
                            zmanLabel     = zman.label,
                            offsetMinutes = offsetMinutes,
                            isBefore      = isBefore,
                            ringCount     = ringCount,
                            ringtoneUri   = ringtoneUri
                        )
                        val ok = vm.scheduleAlarm(zman, config)
                        onAlarmSetResult(ok)
                        if (ok) onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("קבע התראה")
                }
            }
        }
    }
}
