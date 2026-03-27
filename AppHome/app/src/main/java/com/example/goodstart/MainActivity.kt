package com.example.goodstart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.goodstart.network.StudySyncWorker
import com.example.goodstart.ui.navigation.AppNavGraph
import com.example.goodstart.ui.theme.ShieorTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    companion object {
        const val CHANNEL_ID_MESSAGES = "messages_channel"
        const val CHANNEL_ID_TASKS    = "tasks_channel"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        PDFBoxResourceLoader.init(this)
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
