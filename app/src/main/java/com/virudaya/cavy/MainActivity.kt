package com.virudaya.cavy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.virudaya.cavy.ui.navigation.CavyNavGraph
import com.virudaya.cavy.ui.theme.CavyTheme
import com.virudaya.cavy.util.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.requestNotificationPermissionIfNeeded(this)

        setContent {
            CavyTheme {
                CavyNavGraph()
            }
        }
    }
}
