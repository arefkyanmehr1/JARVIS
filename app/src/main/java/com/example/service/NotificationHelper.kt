package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_SMS = "jarvis_sms_channel"
        const val CHANNEL_AI = "jarvis_ai_channel"
        const val CHANNEL_ERRORS = "jarvis_errors_channel"
        const val CHANNEL_SERVICE = "jarvis_service_channel"

        const val NOTIF_ID_SMS = 1001
        const val NOTIF_ID_AI = 1002
        const val NOTIF_ID_ERROR = 1003
        const val NOTIF_ID_SERVICE = 1004
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "سرویس پس‌زمینه JARVIS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "پایش و تفکر هوش مصنوعی در پس‌زمینه"
                setShowBadge(false)
            }

            val smsChannel = NotificationChannel(
                CHANNEL_SMS,
                "پیامک‌های دریافتی JARVIS",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "اعلان دریافت پیامک‌های جدید برای پردازش"
            }

            val aiChannel = NotificationChannel(
                CHANNEL_AI,
                "پاسخ‌های ارسالی JARVIS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان ارسال پاسخ‌های هوشمند توسط هوش مصنوعی"
            }

            val errorChannel = NotificationChannel(
                CHANNEL_ERRORS,
                "خطاها و هشدارهای JARVIS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان خطاهای مربوط به API یا محدودیت‌ها"
            }

            notificationManager.createNotificationChannels(listOf(serviceChannel, smsChannel, aiChannel, errorChannel))
        }
    }

    fun buildForegroundNotification(statusText: String): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("JARVIS: پایش و تحلیل هوشمند فعال است")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showSmsReceivedNotification(sender: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_SMS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("پیامک جدید از: $sender")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID_SMS, notification)
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    fun showAiReplySentNotification(recipient: String, replyText: String) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_AI)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("JARVIS: پاسخ هوشمند ارسال شد")
                .setContentText("به $recipient: $replyText")
                .setStyle(NotificationCompat.BigTextStyle().bigText("به $recipient:\n$replyText"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID_AI, notification)
        } catch (e: SecurityException) {
            // Android 13+ permission check
        }
    }

    fun showErrorNotification(title: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ERRORS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID_ERROR, notification)
        } catch (e: SecurityException) {
            // Permission check
        }
    }
}
