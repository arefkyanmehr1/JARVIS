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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class JarvisBackgroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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

        fun start(context: Context) = startService(context, ACTION_START)

        fun processIncomingSms(
            context: Context,
            sender: String,
            body: String,
            subscriptionId: Int = -1,
            slotIndex: Int = -1
        ) {
            val intent = Intent(context, JarvisBackgroundService::class.java).apply {
                action = ACTION_PROCESS_SMS
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_SUB_ID, subscriptionId)
                putExtra(EXTRA_SLOT_INDEX, slotIndex)
            }
            startService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, JarvisBackgroundService::class.java).apply {
                action = ACTION_STOP
            })
        }

        private fun startService(context: Context, action: String) {
            val intent = Intent(context, JarvisBackgroundService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = (applicationContext as? JarvisApplication) ?: JarvisApplication.instance
        startForeground(
            NotificationHelper.NOTIF_ID_SERVICE,
            app.notificationHelper.buildForegroundNotification("سیستم هوشمند آماده پایش و پاسخگویی خودکار")
        )

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PROCESS_SMS -> {
                val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)
                val slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, -1)

                if (sender.isNotBlank() && body.isNotBlank()) {
                    val app = (applicationContext as? JarvisApplication) ?: JarvisApplication.instance
                    serviceScope.launch {
                        processingMutex.withLock {
                            acquireProcessingWakeLock()
                            try {
                                app.jarvisProcessor.processIncomingSms(
                                    sender = sender,
                                    messageBody = body,
                                    incomingSubId = subId,
                                    incomingSlotIndex = slotIndex
                                )
                            } catch (e: Exception) {
                                app.logRepository.log(
                                    type = "ERROR",
                                    title = "خطا در پردازش پس‌زمینه پیامک",
                                    description = e.localizedMessage ?: e.javaClass.simpleName,
                                    status = "FAILED",
                                    relatedAddress = sender
                                )
                            } finally {
                                releaseProcessingWakeLock()
                            }
                        }
                    }
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun acquireProcessingWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock?.isHeld == true) return
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Jarvis:BackgroundProcessingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseProcessingWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        releaseProcessingWakeLock()
        super.onDestroy()
    }
}
