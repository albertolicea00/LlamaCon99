package com.llamacon99.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.llamacon99.R
import com.llamacon99.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts caller identity in the notification shade. Heads-up presentation is controlled by
 * Android and the user's channel settings; CallerIdOverlay provides the optional floating card.
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
            ).apply { setShowBadge(false) },
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                INCOMING_CALL_CHANNEL_ID,
                context.getString(R.string.notification_channel_incoming_call_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                setShowBadge(false)
            },
        )
    }

    fun foregroundNotification(): Notification =
        NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_foreground_title))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun showIncomingCallBanner(contactName: String, localNumber: String) {
        val details = context.getString(R.string.notification_incoming_call_text, contactName, localNumber)
        val notification = NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_incoming_call_title, contactName))
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    fun dismissIncomingCallBanner() {
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID)
    }
}
