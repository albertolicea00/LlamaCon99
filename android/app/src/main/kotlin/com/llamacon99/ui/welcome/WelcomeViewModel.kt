package com.llamacon99.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamacon99.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val WEEKLY_REMINDER_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(7)

/**
 * Shows once on first launch, then resurfaces weekly until the user has set up call monitoring —
 * same reminder cadence as iOS's `WelcomeView` (`ios/LlamaCon99/Views.swift`).
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _shouldShow = MutableStateFlow(false)
    val shouldShow: StateFlow<Boolean> = _shouldShow.asStateFlow()

    init {
        viewModelScope.launch {
            val monitorEnabled = settingsRepository.callMonitorEnabled.first()
            val lastShown = settingsRepository.lastWelcomeShownAtMillis.first()
            val now = System.currentTimeMillis()
            val due = lastShown == null || now - lastShown >= WEEKLY_REMINDER_INTERVAL_MILLIS
            _shouldShow.value = !monitorEnabled && due
        }
    }

    fun onDismissed() {
        viewModelScope.launch {
            settingsRepository.setLastWelcomeShownAtMillis(System.currentTimeMillis())
            _shouldShow.value = false
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            settingsRepository.setMonitorFromBootEnabled(true)
            settingsRepository.setCallMonitorEnabled(true)
            settingsRepository.setLastWelcomeShownAtMillis(System.currentTimeMillis())
            _shouldShow.value = false
        }
    }
}
