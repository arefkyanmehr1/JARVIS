package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.JarvisApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as JarvisApplication
                if (app.settingsRepository.isAutoReplyEnabledDirect()) {
                    JarvisBackgroundService.start(app)
                    app.logRepository.log(
                        type = "SYSTEM",
                        title = "JARVIS در پس‌زمینه بازیابی شد",
                        description = "سرویس پایش پیامک پس از راه‌اندازی مجدد دستگاه یا به‌روزرسانی برنامه فعال شد.",
                        status = "SUCCESS"
                    )
                }
            } catch (e: Exception) {
                try {
                    val app = context.applicationContext as JarvisApplication
                    app.logRepository.log(
                        type = "ERROR",
                        title = "بازیابی سرویس پس‌زمینه ناموفق بود",
                        description = e.localizedMessage ?: e.javaClass.simpleName,
                        status = "FAILED"
                    )
                } catch (_: Exception) {
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
