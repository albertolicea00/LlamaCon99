package com.llamacon99.service

import androidx.lifecycle.ViewModel
import com.llamacon99.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

private const val CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1_000L
private const val RETRY_INTERVAL_MILLIS = 60 * 60 * 1_000L

internal fun nextAutomaticUpdateCheckDelay(lastCheck: Long?, now: Long): Long {
    // A clock correction must not postpone checks indefinitely.
    if (lastCheck == null || lastCheck > now) return 0L
    return (CHECK_INTERVAL_MILLIS - (now - lastCheck)).coerceAtLeast(0L)
}

/** Temporary GitHub update prompts while distribution moves to Google Play. */
@HiltViewModel
class AutomaticUpdateViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _pendingUpdate = MutableStateFlow<UpdateInfo?>(null)
    val pendingUpdate = _pendingUpdate.asStateFlow()

    fun dismissUpdate() {
        _pendingUpdate.value = null
    }

    // Called only while the activity is resumed; no background service is needed.
    suspend fun checkWhileResumed() {
        while (currentCoroutineContext().isActive) {
            val wait = nextAutomaticUpdateCheckDelay(
                settingsRepository.lastUpdateCheckAtMillis.first(),
                System.currentTimeMillis(),
            )
            if (wait > 0L) {
                delay(wait)
                continue
            }

            val result = UpdateChecker.check()
            // Cancellation can happen during the blocking HTTP request.
            if (!currentCoroutineContext().isActive) return
            if (result is UpdateCheckResult.Failed) {
                // Keep the daily check due so reopening also retries after an offline attempt.
                delay(RETRY_INTERVAL_MILLIS)
                continue
            }
            settingsRepository.setLastUpdateCheckAtMillis(System.currentTimeMillis())
            if (result is UpdateCheckResult.Available) {
                _pendingUpdate.value = result.info
            }
        }
    }
}
