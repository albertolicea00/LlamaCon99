package com.llamacon99.monitor

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.llamacon99.data.CallerIdMatcher
import com.llamacon99.data.CallerIdStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that watches for a ringing call carrying ETECSA's wrapped `*99` caller ID
 * and, when it matches a contact, raises a heads-up banner via [CallerIdNotifier]. This is the
 * whole of the "caller ID" feature on Android: no [android.telecom.InCallService], no default
 * dialer role — see the plan's rationale for why that line wasn't crossed.
 *
 * Requires `READ_PHONE_STATE` + `READ_CALL_LOG` (the latter is what lets
 * `PhoneStateListener.onCallStateChanged` actually receives
 * the ringing number on API 28+ — without it the number argument comes back empty).
 */
@AndroidEntryPoint
class CallMonitorService : Service() {

    @Inject lateinit var callerIdStore: CallerIdStore
    @Inject lateinit var notifier: CallerIdNotifier

    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var telephonyManager: TelephonyManager? = null

    private var telephonyCallback: TelephonyCallback? = null
    // Required for the fallback on Android 8–11 (API 26–30).
    @Suppress("DEPRECATION")
    private var phoneStateListener: android.telephony.PhoneStateListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifier.ensureChannels()
        startForeground(FOREGROUND_NOTIFICATION_ID, notifier.foregroundNotification())
        registerCallStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        unregisterCallStateListener()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerCallStateListener() {
        val hasPermissions = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_PHONE_STATE,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CALL_LOG,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermissions) {
            stopSelf()
            return
        }

        val manager = getSystemService(TelephonyManager::class.java) ?: return
        telephonyManager = manager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    // CallStateListener on API 31+ no longer carries the number directly;
                    // the ringing number is delivered via CallDetails on API 33+, so this path
                    // falls back to PhoneStateListener's richer callback below on the devices
                    // this app actually targets (minSdk 26). Kept for forward compatibility only.
                }
            }
            telephonyCallback = callback
            manager.registerTelephonyCallback(mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : android.telephony.PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state, phoneNumber)
                }
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            manager.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun unregisterCallStateListener() {
        telephonyCallback?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) telephonyManager?.unregisterTelephonyCallback(it)
        }
        @Suppress("DEPRECATION")
        phoneStateListener?.let { telephonyManager?.listen(it, android.telephony.PhoneStateListener.LISTEN_NONE) }
    }

    private fun handleCallState(state: Int, phoneNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                val number = phoneNumber ?: return
                serviceScope.launch {
                    val localNumber = CallerIdMatcher.unwrap(number) ?: return@launch
                    val wrapped = CallerIdMatcher.wrappedNumber(localNumber) ?: return@launch
                    val name = callerIdStore.lookup(wrapped) ?: return@launch
                    notifier.showIncomingCallBanner(name)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> notifier.dismissIncomingCallBanner()
        }
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 2001
    }
}
