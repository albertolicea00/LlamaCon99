package com.llamacon99.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Counts active subscriptions, not SIM slots. Null means the count cannot be verified. */
@Singleton
class SimRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val subscriptions = context.getSystemService(SubscriptionManager::class.java)
    private val count = MutableStateFlow(readActiveSimCount())
    val activeSimCount = count.asStateFlow()

    private val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() { refresh() }
    }

    init {
        // This singleton and its subscription listener live for the application process.
        Handler(Looper.getMainLooper()).post {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    subscriptions?.addOnSubscriptionsChangedListener(context.mainExecutor, listener)
                } else {
                    @Suppress("DEPRECATION")
                    subscriptions?.addOnSubscriptionsChangedListener(listener)
                }
            } catch (_: RuntimeException) {
                refresh()
            }
        }
    }

    fun refresh(): Int? = readActiveSimCount().also { count.value = it }

    private fun readActiveSimCount(): Int? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            subscriptions?.activeSubscriptionInfoCount
        } catch (_: RuntimeException) {
            null
        }
    }
}

/** Direct calls require exactly one verified active SIM. */
internal fun effectiveDialMode(
    preferred: SettingsRepository.DialMode,
    activeSimCount: Int?,
): SettingsRepository.DialMode =
    if (preferred == SettingsRepository.DialMode.DIRECT && activeSimCount == 1) preferred
    else SettingsRepository.DialMode.PREFILL
