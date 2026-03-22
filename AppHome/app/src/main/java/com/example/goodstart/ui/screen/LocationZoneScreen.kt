package com.example.goodstart.ui.screen

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodstart.ui.theme.*
import com.example.goodstart.ui.viewmodel.LocationZoneViewModel
import com.example.goodstart.ui.viewmodel.SilentZone
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun LocationZoneScreen(
    onBack: () -> Unit,
    vm: LocationZoneViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Check permissions on first composition and after returning from settings
    fun refreshPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        vm.updatePermissions(nm.isNotificationPolicyAccessGranted, hasFine)
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
        if (state.hasLocationPermission) vm.reregisterAll()
    }

    // Location permission launcher
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshPermissions() }

    // Map state
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingLatLng by remember { mutableStateOf<LatLng?>(null) }
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(31.7683, 35.2137), 9f) // Israel
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
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
                    Icon(Icons.Default.ArrowForward, contentDescription = "חזור", tint = Primary)
                }
                Text(
                    "אזורי שקט",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Spacer(Modifier.width(48.dp))
            }
        }

        // ── Permission banners ───────────────────────────────────────────────
        if (!state.hasLocationPermission) {
            PermissionBanner(
                text = "נדרשת הרשאת מיקום לפעולת האזורים",
                buttonText = "אפשר"
            ) {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                )
            }
        }
        if (!state.hasDndPermission) {
            PermissionBanner(
                text = "נדרשת הרשאת 'אל תפריע' להשתקה אוטומטית",
                buttonText = "אפשר"
            ) {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }

        // ── Map (60% height) ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                onMapLongClick = { latLng ->
                    pendingLatLng = latLng
                    showAddDialog = true
                }
            ) {
                state.zones.forEach { zone ->
                    val center = LatLng(zone.lat, zone.lng)
                    Circle(
                        center = center,
                        radius = zone.radiusMeters.toDouble(),
                        strokeColor = if (zone.active) Primary else Color.Gray,
                        fillColor = if (zone.active)
                            Primary.copy(alpha = 0.15f)
                        else Color.Gray.copy(alpha = 0.10f),
                        strokeWidth = 3f
                    )
                    Marker(
                        state = MarkerState(position = center),
                        title = zone.name,
                        snippet = "${zone.radiusMeters.toInt()} מטר"
                    )
                }
            }
            // Hint
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    "לחץ לחיצה ארוכה על המפה להוספת אזור",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        // ── Zone list (40% height) ────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .navigationBarsPadding(),
            shadowElevation = 4.dp,
            color = CardBg,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            if (state.zones.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "עדיין אין אזורים מוגדרים\nלחץ לחיצה ארוכה על המפה",
                        textAlign = TextAlign.Center,
                        color = Muted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.zones, key = { it.id }) { zone ->
                        ZoneRow(
                            zone = zone,
                            onToggle = { vm.toggleZone(zone.id) },
                            onDelete = { vm.removeZone(zone.id) }
                        )
                        HorizontalDivider(color = LineColor, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    // ── Add zone dialog ────────────────────────────────────────────────────
    if (showAddDialog && pendingLatLng != null) {
        AddZoneDialog(
            latLng = pendingLatLng!!,
            onConfirm = { name, radius ->
                vm.addZone(name, pendingLatLng!!, radius)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun PermissionBanner(text: String, buttonText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Amber.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, fontSize = 13.sp, color = Ink, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Text(buttonText, color = Primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZoneRow(zone: SilentZone, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = zone.active,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.4f))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(zone.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(
                "${zone.radiusMeters.toInt()} מ' • %.4f, %.4f".format(zone.lat, zone.lng),
                fontSize = 12.sp, color = Muted
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "מחק", tint = Muted)
        }
    }
}

@Composable
private fun AddZoneDialog(
    latLng: LatLng,
    onConfirm: (name: String, radius: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(100f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הוסף אזור שקט", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("שם האזור") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("רדיוס: ${radius.toInt()} מטר", fontSize = 13.sp, color = Muted)
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..500f,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), radius) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = name.isNotBlank()
            ) { Text("הוסף") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("בטל") }
        }
    )
}
