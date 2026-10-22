package com.llamacon99

import android.graphics.Color
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import com.llamacon99.ui.navigation.LlamaCon99NavGraph
import com.llamacon99.ui.theme.LlamaCon99Theme
import dagger.hilt.android.AndroidEntryPoint
import com.llamacon99.data.SettingsRepository
import com.llamacon99.monitor.CallMonitorService
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.callMonitorEnabled.collect { enabled ->
                    if (enabled) restoreCallMonitor()
                }
            }
        }
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Remove the system scrim behind three-button navigation.
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        val quickDialMode = intent?.getStringExtra("dial_mode")
        setContent {
            val appTheme by settingsRepository.appTheme.collectAsState(initial = com.llamacon99.ui.theme.AppThemeCatalog.default)
            LlamaCon99Theme(appTheme = appTheme) {
                val navigationShade = if (isSystemInDarkTheme()) ComposeColor.Black else ComposeColor.White
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        LlamaCon99NavGraph(quickDialMode = quickDialMode)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(
                                Brush.verticalGradient(
                                    listOf(navigationShade.copy(alpha = 0.25f), navigationShade.copy(alpha = 0.9f)),
                                ),
                            ),
                    )
                }
            }
        }
    }

    private suspend fun restoreCallMonitor() {
        if (!CallMonitorService.hasRequiredPermissions(this)) {
            settingsRepository.setCallMonitorEnabled(false)
            return
        }
        try {
            ContextCompat.startForegroundService(this, Intent(this, CallMonitorService::class.java))
        } catch (error: SecurityException) {
            reportMonitorFailure(error)
        } catch (error: IllegalStateException) {
            reportMonitorFailure(error)
        }
    }

    private suspend fun reportMonitorFailure(error: Exception) {
        Log.w("MainActivity", "Caller ID monitoring could not resume", error)
        settingsRepository.setCallMonitorEnabled(false)
        Toast.makeText(this, R.string.settings_call_monitor_start_failed, Toast.LENGTH_LONG).show()
    }
}
