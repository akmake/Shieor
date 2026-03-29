package com.example.goodstart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.goodstart.geofence.GeofenceCheckWorker
import com.example.goodstart.geofence.GeofenceHelper
import com.example.goodstart.network.StudySyncWorker
import com.example.goodstart.sync.UserManager
import com.example.goodstart.sync.UserSyncWorker
import com.example.goodstart.ui.navigation.AppNavGraph
import com.example.goodstart.ui.theme.ShieorTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val CHANNEL_ID_MESSAGES = "messages_channel"
        const val CHANNEL_ID_TASKS    = "tasks_channel"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        PDFBoxResourceLoader.init(this)
        StudySyncWorker.enqueue(this)

        // Auto-register user and start periodic sync
        UserSyncWorker.enqueue(this)
        lifecycleScope.launch(Dispatchers.IO) {
            UserManager.ensureRegistered(this@MainActivity)
        }

        // Start periodic geofence check if silent zones are configured
        val hasActiveZones = GeofenceHelper.loadZones(this).any { it.active }
        if (hasActiveZones) {
            GeofenceHelper.registerActiveGeofences(this)
            GeofenceCheckWorker.enqueue(this)
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ShieorTheme {
                AppNavGraph()
            }
        }
    }
}
