package com.minetallest.mino.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.minetallest.mino.MainActivity
import com.minetallest.mino.R
import java.util.concurrent.atomic.AtomicInteger

/**
 * Posts the "reply ready" notification when a response finishes while Mino is
 * in the background. On HyperOS the same notification is also rendered in the
 * Xiaomi HyperIsland; everywhere else it stays a normal notification.
 */
class ReplyNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)
    private val idCounter = AtomicInteger(0)

    init {
        ensureChannel()
    }

    /** [chatTitle] is the user's own chat title; the reply body is never shown. */
    fun postReady(chatId: String, chatTitle: String) {
        if (!canPostNotifications()) return
        ensureChannel()

        // A fresh id per reply: HyperOS will not re-show a notification whose
        // previous instance was dismissed when the same id is reused.
        val notificationId = nextNotificationId()
        val title = context.getString(R.string.notification_ready_title)
        val text = context.getString(
            R.string.notification_ready_text,
            chatTitle.ifBlank { context.getString(R.string.notification_ready_fallback) },
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_CHAT_ID, chatId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setTicker(title)

        val island = HyperIsland.payload(context, chatTitle)
        if (island != null) builder.addExtras(island.extras)

        val notification = builder.build()
        if (island != null) {
            notification.extras.putString(HyperIsland.KEY_PARAM, island.json)
        }

        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }

    /** Called when Mino returns to the foreground: the reply has been seen. */
    fun clear() {
        try {
            manager.cancelAll()
        } catch (_: Throwable) {
            // No notification service on this device.
        }
    }

    private fun nextNotificationId(): Int {
        val base = (System.currentTimeMillis() and 0x7fffffffL).toInt()
        return (base + idCounter.getAndIncrement()) and 0x7fffffff
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return manager.areNotificationsEnabled()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_replies),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_replies_description)
            enableVibration(true)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "mino_replies"
    }
}
