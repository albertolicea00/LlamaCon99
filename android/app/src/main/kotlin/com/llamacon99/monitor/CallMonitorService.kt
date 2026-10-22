package com.llamacon99.monitor

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.llamacon99.R
import com.llamacon99.data.CallerIdMatcher
import com.llamacon99.data.CallerIdStore
import com.llamacon99.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Passive caller ID monitoring, independent of the system dialer and self-managed calls. */
@AndroidEntryPoint
class CallMonitorService : Service() {
    @Inject lateinit var callerIdStore: CallerIdStore
    @Inject lateinit var notifier: CallerIdNotifier
    @Inject lateinit var settingsRepository: SettingsRepository

    // Serialize call-state changes and banner delivery so an ended call cannot publish a late banner.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var telephonyManager: TelephonyManager? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: android.telephony.PhoneStateListener? = null
    private var lookupJob: Job? = null
    private var monitoringStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!hasRequiredPermissions(this)) {
            failToStart(null)
            return
        }
        try {
            notifier.ensureChannels()
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notifier.foregroundNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )
            registerCallStateListener()
            monitoringStarted = true
        } catch (error: SecurityException) {
            failToStart(error)
        } catch (error: IllegalStateException) {
            failToStart(error)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        if (monitoringStarted) START_STICKY else START_NOT_STICKY

    override fun onDestroy() {
        @Suppress("DEPRECATION")
        try {
            phoneStateListener?.let { telephonyManager?.listen(it, android.telephony.PhoneStateListener.LISTEN_NONE) }
        } catch (error: SecurityException) {
            Log.w(TAG, "Call permission was revoked before listener cleanup", error)
        }
        lookupJob?.cancel()
        notifier.dismissIncomingCallBanner()
        serviceScope.cancel()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun registerCallStateListener() {
        val manager = getSystemService(TelephonyManager::class.java)
            ?: throw IllegalStateException("Telephony is unavailable")
        telephonyManager = manager
        // Unlike TelephonyCallback.CallStateListener, this callback includes the incoming
        // number when READ_CALL_LOG is granted, including on Android 12 and later.
        val listener = object : android.telephony.PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallState(state, phoneNumber)
            }
        }
        phoneStateListener = listener
        manager.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun failToStart(error: Exception?) {
        Log.w(TAG, "Caller ID monitoring could not start", error)
        Toast.makeText(this, R.string.settings_call_monitor_start_failed, Toast.LENGTH_LONG).show()
        serviceScope.launch {
            try {
                settingsRepository.setCallMonitorEnabled(false)
            } finally {
                withContext(Dispatchers.Main.immediate) { stopSelf() }
            }
        }
    }

    private fun handleCallState(state: Int, phoneNumber: String?) {
        if (state != TelephonyManager.CALL_STATE_RINGING) {
            lookupJob?.cancel()
            notifier.dismissIncomingCallBanner()
            return
        }
        // A numberless update must not cancel an identification already in progress.
        val number = phoneNumber?.takeIf { it.isNotBlank() } ?: return
        lookupJob?.cancel()
        lookupJob = serviceScope.launch {
            val localNumber = CallerIdMatcher.unwrap(number) ?: return@launch
            val wrapped = CallerIdMatcher.wrappedNumber(localNumber) ?: return@launch
            val name = callerIdStore.lookup(wrapped) ?: return@launch
            if (hasRequiredPermissions(this@CallMonitorService)) notifier.showIncomingCallBanner(name)
        }
    }

    companion object {
        private const val TAG = "CallMonitorService"
        private const val FOREGROUND_NOTIFICATION_ID = 2001

        fun hasRequiredPermissions(context: Context): Boolean =
            requiredPermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

        fun requiredPermissions(): Array<String> = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CALL_LOG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
}
