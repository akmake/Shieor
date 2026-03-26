package com.example.goodstart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.goodstart.network.StudySyncWorker
import com.example.goodstart.ui.navigation.AppNavGraph
import com.example.goodstart.ui.theme.ShieorTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val CHANNEL_ID_MESSAGES = "messages_channel"
        const val CHANNEL_ID_TASKS    = "tasks_channel"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start background sync for study materials
        StudySyncWorker.enqueue(this)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ShieorTheme {
                AppNavGraph()
            }
        }
    }
}
