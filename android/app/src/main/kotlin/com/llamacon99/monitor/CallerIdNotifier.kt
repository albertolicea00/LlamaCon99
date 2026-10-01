package com.llamacon99.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.llamacon99.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the heads-up "who's really calling" banner shown over the system's native incoming-call
 * screen. This is the ceiling of what a non-default-dialer app can do on Android — it cannot
 * replace or relabel the system call screen itself (that's what iOS's Call Directory Extension
 * does), only overlay a notification on top of it while the phone rings.
 */
@Singleton
class CallerIdNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val FOREGROUND_CHANNEL_ID = "call_monitor_foreground"
        const val INCOMING_CALL_CHANNEL_ID = "incoming_call_banner"
        const val INCOMING_CALL_NOTIFICATION_ID = 1001
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                context.getString(R.string.notification_channel_foreground_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                INCOMING_CALL_CHANNEL_ID,
                context.getString(R.string.notification_channel_incoming_call_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { setSound(null, null) },
        )
    }

    fun foregroundNotification(): Notification =
        NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_foreground_title))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun showIncomingCallBanner(contactName: String) {
        val notification = NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_incoming_call_title, contactName))
            .setContentText(context.getString(R.string.notification_incoming_call_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    fun dismissIncomingCallBanner() {
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID)
    }
}
