package com.llamacon99.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.llamacon99.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Disabled by default (see `AndroidManifest.xml`) — only enabled via `PackageManager` when the
 * user opts into "monitor calls from boot" in Settings. Restarts [CallMonitorService] after a
 * reboot so the caller-ID banner keeps working without the user having to reopen the app first.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val shouldStart = settingsRepository.callMonitorEnabled.first() &&
                    settingsRepository.monitorFromBootEnabled.first()
                if (shouldStart) {
                    ContextCompat.startForegroundService(context, Intent(context, CallMonitorService::class.java))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
