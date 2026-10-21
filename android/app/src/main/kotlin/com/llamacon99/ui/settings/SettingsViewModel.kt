package com.llamacon99.ui.settings

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamacon99.data.SettingsRepository
import com.llamacon99.R
import com.llamacon99.monitor.CallMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val dialMode: SettingsRepository.DialMode = SettingsRepository.DialMode.PREFILL,
    val anonymousSwipeEnabled: Boolean = false,
    val callMonitorEnabled: Boolean = false,
    val monitorFromBootEnabled: Boolean = false,
    val hasCallMonitorPermissions: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val permissionsRefreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.dialMode,
        settingsRepository.anonymousSwipeEnabled,
        settingsRepository.callMonitorEnabled,
        settingsRepository.monitorFromBootEnabled,
        permissionsRefreshTrigger,
    ) { dialMode, anonymousSwipeEnabled, callMonitorEnabled, monitorFromBootEnabled, _ ->
        SettingsUiState(
            dialMode = dialMode,
            anonymousSwipeEnabled = anonymousSwipeEnabled,
            callMonitorEnabled = callMonitorEnabled,
            monitorFromBootEnabled = monitorFromBootEnabled,
            hasCallMonitorPermissions = hasCallMonitorPermissions(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshPermissionStatus() {
        permissionsRefreshTrigger.value += 1
    }

    fun setDialMode(mode: SettingsRepository.DialMode) {
        viewModelScope.launch { settingsRepository.setDialMode(mode) }
    }

    fun setAnonymousSwipeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAnonymousSwipeEnabled(enabled) }
    }

    fun setCallMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                settingsRepository.setCallMonitorEnabled(false)
                context.stopService(Intent(context, CallMonitorService::class.java))
                return@launch
            }
            if (!hasCallMonitorPermissions()) {
                settingsRepository.setCallMonitorEnabled(false)
                refreshPermissionStatus()
                return@launch
            }
            settingsRepository.setCallMonitorEnabled(true)
            try {
                ContextCompat.startForegroundService(context, Intent(context, CallMonitorService::class.java))
            } catch (error: SecurityException) {
                monitorStartFailed(error)
            } catch (error: IllegalStateException) {
                monitorStartFailed(error)
            }
        }
    }

    fun setMonitorFromBootEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMonitorFromBootEnabled(enabled) }
    }

    private suspend fun monitorStartFailed(error: Exception) {
        Log.w("SettingsViewModel", "Caller ID monitoring could not start", error)
        settingsRepository.setCallMonitorEnabled(false)
        Toast.makeText(context, R.string.settings_call_monitor_start_failed, Toast.LENGTH_LONG).show()
    }

    private fun hasCallMonitorPermissions(): Boolean = CallMonitorService.hasRequiredPermissions(context)
}
