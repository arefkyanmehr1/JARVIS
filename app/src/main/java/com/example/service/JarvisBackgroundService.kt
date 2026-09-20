package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class JarvisBackgroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processingMutex = Mutex()
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val ACTION_START = "com.example.service.START_JARVIS"
        private const val ACTION_STOP = "com.example.service.STOP_JARVIS"
        private const val ACTION_PROCESS_SMS = "com.example.service.PROCESS_SMS"
        private const val EXTRA_SENDER = "sender"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_SUB_ID = "sub_id"
        private const val EXTRA_SLOT_INDEX = "slot_index"
        private const val RESTART_REQUEST_CODE = 918271
        private const val RESTART_DELAY_MS = 1_500L

        fun start(context: Context) = startService(context, ACTION_START)

        fun processIncomingSms(context: Context, sender: String, body: String, subscriptionId: Int = -1, slotIndex: Int = -1) {
            val intent = Intent(context, JarvisBackgroundService::class.java).apply {
                action = ACTION_PROCESS_SMS
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_SUB_ID, subscriptionId)
                putExtra(EXTRA_SLOT_INDEX, slotIndex)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (_: Exception) { }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, JarvisBackgroundService::class.java).apply { action = ACTION_STOP })
        }

        private fun startService(context: Context, action: String) {
            val intent = Intent(context, JarvisBackgroundService::class.java).apply { this.action = action }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (_: Exception) { }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = (applicationContext as? JarvisApplication) ?: JarvisApplication.instance
        startForeground(
            NotificationHelper.NOTIF_ID_SERVICE,
            app.notificationHelper.buildForegroundNotification("JARVIS فعال است — پایش پیامک و پاسخ خودکار در پس‌زمینه")
        )
        serviceScope.launch {
            JarvisProcessor.liveAnalysisState.collectLatest { state ->
                val notifText = if (state.isAnalyzing) {
                    if (state.delayRemainingSeconds > 0) {
                        val m = state.delayRemainingSeconds / 60
                        val s = state.delayRemainingSeconds % 60
                        val countdown = String.format("%02d:%02d", m, s)
                        "پاسخ به " + state.sender + " در " + countdown
                    } else {
                        "در حال تحلیل و تدوین پاسخ به " + state.sender + "..."
                    }
                } else {
                    "JARVIS فعال است — منتظر پیامک"
                }
                val updatedNotif = app.notificationHelper.buildForegroundNotification(notifText)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NotificationHelper.NOTIF_ID_SERVICE, updatedNotif)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                cancelRestartAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PROCESS_SMS -> {
                val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)
                val slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, -1)
                if (sender.isNotBlank() && body.isNotBlank()) processSmsSafely(sender, body, subId, slotIndex)
            }
        }
        return START_STICKY
    }

    private fun processSmsSafely(sender: String, body: String, subId: Int, slotIndex: Int) {
        val app = (applicationContext as? JarvisApplication) ?: JarvisApplication.instance
        serviceScope.launch {
            processingMutex.withLock {
                acquireProcessingWakeLock()
                try {
                    app.jarvisProcessor.processIncomingSms(sender, body, subId, slotIndex)
                } catch (e: Exception) {
                    app.logRepository.log("ERROR", "خطا در پردازش پس‌زمینه پیامک", e.localizedMessage ?: e.javaClass.simpleName, "FAILED", sender)
                } finally {
                    releaseProcessingWakeLock()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        scheduleSelfRestart()
        super.onTaskRemoved(rootIntent)
    }

    private fun scheduleSelfRestart() {
        try {
            val restartIntent = Intent(this, JarvisBackgroundService::class.java).apply { action = ACTION_START }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getService(this, RESTART_REQUEST_CODE, restartIntent, flags)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + RESTART_DELAY_MS, pendingIntent)
        } catch (_: Exception) { }
    }

    private fun cancelRestartAlarm() {
        try {
            val intent = Intent(this, JarvisBackgroundService::class.java).apply { action = ACTION_START }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getService(this, RESTART_REQUEST_CODE, intent, flags)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) { }
    }

    private fun acquireProcessingWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock?.isHeld == true) return
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jarvis:BackgroundProcessingWakeLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) { }
    }

    private fun releaseProcessingWakeLock() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) { }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        releaseProcessingWakeLock()
        super.onDestroy()
    }
}