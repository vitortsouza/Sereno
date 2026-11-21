package com.opencode.sereno

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.ui.SerenoRoot
import com.opencode.sereno.ui.theme.SerenoTheme
import com.opencode.sereno.ui.theme.isDarkMode

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        boostRefreshRate()
        setContent {
            val mode by Prefs.themeMode.collectAsStateWithLifecycle()
            val dark = isDarkMode(mode, isSystemInDarkTheme())
            SerenoTheme(mode) {
                SerenoRoot()
            }
            LaunchedEffect(dark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!Prefs.notificationsAsked()) {
            Prefs.markNotificationsAsked()
            requestNotifications()
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun boostRefreshRate() {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                window.windowManager.defaultDisplay
            }
            display?.supportedModes?.maxByOrNull { it.refreshRate }?.let {
                window.attributes = window.attributes.apply { preferredDisplayModeId = it.modeId }
            }
        } catch (_: Throwable) {
        }
    }
}
