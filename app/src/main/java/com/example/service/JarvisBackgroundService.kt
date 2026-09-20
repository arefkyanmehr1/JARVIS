package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.JarvisApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service guaranteeing that JARVIS stays active in background,
 * executes countdown timers, performs AI thinking, and sends scheduled replies even
 * when the app is closed or the device screen is off.
 */
class JarvisBackgroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val ACTION_START = "com.example.service.START_JARVIS"
        private const val ACTION_STOP = "com.example.service.STOP_JARVIS"

        fun start(context: Context) {
            val intent = Intent(context, JarvisBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        val app = (applicationContext as? JarvisApplication) ?: JarvisApplication.instance
        val notification = app.notificationHelper.buildForegroundNotification("سیستم هوشمند آماده پایش و پاسخگویی خودکار")
        startForeground(NotificationHelper.NOTIF_ID_SERVICE, notification)

        // Observe thinking and analysis state to update ongoing notification dynamically
        serviceScope.launch {
            JarvisProcessor.liveAnalysisState.collectLatest { state ->
                val notifText = if (state.isAnalyzing) {
                    if (state.delayRemainingSeconds > 0) {
                        val m = state.delayRemainingSeconds / 60
                        val s = state.delayRemainingSeconds % 60
                        val countdown = String.format("%02d:%02d", m, s)
                        "تحلیل سوابق و تأخیر هوشمند: ارسال به ${state.sender} در $countdown"
                    } else {
                        "در حال تحلیل سوابق و تدوین پاسخ به ${state.sender}..."
                    }
                } else {
                    "پایش پیامک‌ها فعال است - آماده تحلیل بلادرنگ"
                }

                val updatedNotif = app.notificationHelper.buildForegroundNotification(notifText)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NotificationHelper.NOTIF_ID_SERVICE, updatedNotif)
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Jarvis:BackgroundProcessingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // Safety timeout 10 mins per acquire
            }
        } catch (_: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }
}
