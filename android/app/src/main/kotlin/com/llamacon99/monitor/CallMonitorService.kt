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
import com.llamacon99.data.CallerIdLookup
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
    @Inject lateinit var callerIdLookup: CallerIdLookup
    @Inject lateinit var notifier: CallerIdNotifier
    @Inject lateinit var overlay: CallerIdOverlay
    @Inject lateinit var settingsRepository: SettingsRepository

    // Serialize call-state changes and banner delivery so an ended call cannot publish a late banner.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var telephonyManager: TelephonyManager? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: android.telephony.PhoneStateListener? = null
    private var lookupJob: Job? = null
    private var monitoringStarted = false
    private var ringingNumber: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating caller ID service")
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
            Log.i(TAG, "Monitoring started: call-state listener registered")
        } catch (error: SecurityException) {
            failToStart(error)
        } catch (error: IllegalStateException) {
            failToStart(error)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        if (monitoringStarted) START_STICKY else START_NOT_STICKY

    override fun onDestroy() {
        Log.i(TAG, "Destroying caller ID service")
        @Suppress("DEPRECATION")
        try {
            phoneStateListener?.let { telephonyManager?.listen(it, android.telephony.PhoneStateListener.LISTEN_NONE) }
        } catch (error: SecurityException) {
            Log.w(TAG, "Call permission was revoked before listener cleanup", error)
        }
        lookupJob?.cancel()
        notifier.dismissIncomingCallBanner()
        overlay.dismiss()
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
        // Log only state and length, never contact names or telephone digits.
        Log.d(TAG, "Call state=$state numberPresent=${!phoneNumber.isNullOrBlank()} digits=${phoneNumber?.count { it.isDigit() } ?: 0}")
        if (state != TelephonyManager.CALL_STATE_RINGING) {
            ringingNumber = null
            lookupJob?.cancel()
            notifier.dismissIncomingCallBanner()
            overlay.dismiss()
            return
        }
        // A numberless update must not cancel an identification already in progress.
        val number = phoneNumber?.takeIf { it.isNotBlank() } ?: run {
            Log.d(TAG, "Ringing without incoming number; waiting for numbered update")
            return
        }
        val localNumber = CallerIdMatcher.unwrap(number) ?: run {
            Log.d(TAG, "Incoming number does not match the collect-call format")
            return
        }
        if (ringingNumber == localNumber) return
        ringingNumber = localNumber
        lookupJob?.cancel()
        notifier.dismissIncomingCallBanner()
        overlay.dismiss()
        lookupJob = serviceScope.launch {
            Log.d(TAG, "Collect-call format recognized; looking up contact")
            val contactName = callerIdLookup.nameFor(localNumber)
            Log.d(TAG, "Contact lookup complete: matched=${contactName != null}")
            val name = contactName ?: getString(R.string.caller_id_unknown_contact)
            if (hasRequiredPermissions(this@CallMonitorService)) {
                notifier.showIncomingCallBanner(name, localNumber)
                Log.i(TAG, "Caller ID notification posted")
                overlay.show(name, localNumber)
            } else {
                Log.w(TAG, "Caller ID display skipped: required permissions missing")
            }
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
