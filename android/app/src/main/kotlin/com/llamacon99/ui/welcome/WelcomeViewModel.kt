package com.llamacon99.ui.welcome

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamacon99.data.SettingsRepository
import com.llamacon99.monitor.CallMonitorService
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps caller-ID setup visible until its guided permission flow is completed.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _shouldShow = MutableStateFlow(false)
    val shouldShow: StateFlow<Boolean> = _shouldShow.asStateFlow()
    private val _setupStep = MutableStateFlow(0)
    val setupStep: StateFlow<Int> = _setupStep.asStateFlow()
    private val permissionRefreshTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.callMonitorEnabled,
                settingsRepository.welcomeSetupStarted,
                settingsRepository.welcomeSetupCompleted,
                permissionRefreshTrigger,
            ) { monitorEnabled, setupStarted, setupCompleted, _ ->
                val permissionsGranted = hasAllSetupPermissions()
                _setupStep.value = if (setupStarted || (setupCompleted && !permissionsGranted)) {
                    firstIncompleteStep()
                } else {
                    0
                }
                _shouldShow.value = (!monitorEnabled && !setupCompleted) || (setupCompleted && !permissionsGranted)
            }.collect { }
        }
    }

    fun startSetup() {
        viewModelScope.launch {
            settingsRepository.setWelcomeSetupStarted(true)
            refreshPermissionStatus()
        }
    }

    fun refreshPermissionStatus() {
        permissionRefreshTrigger.value += 1
    }

    fun skipOverlay() {
        _setupStep.value = 6
    }

    fun completeSetup() {
        viewModelScope.launch {
            settingsRepository.setCallMonitorEnabled(true)
            settingsRepository.setWelcomeSetupStarted(false)
            settingsRepository.setWelcomeSetupCompleted(true)
            settingsRepository.setLastWelcomeShownAtMillis(System.currentTimeMillis())
            _shouldShow.value = false
        }
    }

    private fun hasAllSetupPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            CallMonitorService.hasRequiredPermissions(context)

    private fun firstIncompleteStep(): Int {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return 1
        }
        val missingPermission = CallMonitorService.requiredPermissions().firstOrNull {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        return when (missingPermission) {
            Manifest.permission.READ_PHONE_STATE -> 2
            Manifest.permission.READ_CALL_LOG -> 3
            Manifest.permission.POST_NOTIFICATIONS -> 4
            null -> if (Settings.canDrawOverlays(context)) 6 else 5
            else -> 2
        }
    }
}
