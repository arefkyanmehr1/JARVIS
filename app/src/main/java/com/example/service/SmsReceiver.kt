package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.JarvisApplication
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val subscriptionId = intent.getIntExtra("subscription", -1).let {
                if (it != -1) it else intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
            }
            val slotIndex = intent.getIntExtra("slot", -1).let {
                if (it != -1) it else intent.getIntExtra("simSlot", -1)
            }

            val groupedBySender = mutableMapOf<String, StringBuilder>()
            for (sms in messages) {
                val address = sms.displayOriginatingAddress ?: sms.originatingAddress ?: continue
                val body = sms.displayMessageBody ?: sms.messageBody ?: ""
                groupedBySender.getOrPut(address) { StringBuilder() }.append(body)
            }

            for ((sender, bodyBuilder) in groupedBySender) {
                val fullMessage = bodyBuilder.toString()
                if (fullMessage.isNotBlank()) {
                    try {
                        JarvisBackgroundService.processIncomingSms(
                            context = context.applicationContext,
                            sender = sender,
                            body = fullMessage,
                            subscriptionId = subscriptionId,
                            slotIndex = slotIndex
                        )
                    } catch (e: Exception) {
                        val app = (context.applicationContext as? JarvisApplication) ?: JarvisApplication.instance
                        app.applicationScope.launch {
                            app.logRepository.log(
                                type = "ERROR",
                                title = "خطا در شروع پردازش پس‌زمینه پیامک",
                                description = e.localizedMessage ?: e.javaClass.simpleName,
                                status = "FAILED",
                                relatedAddress = sender
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            try {
                val app = (context.applicationContext as? JarvisApplication) ?: JarvisApplication.instance
                app.applicationScope.launch {
                    app.logRepository.log(
                        type = "ERROR",
                        title = "خطا در دریافت برودکست پیامک",
                        description = e.localizedMessage ?: "استثنا در SmsReceiver",
                        status = "FAILED"
                    )
                }
            } catch (_: Exception) {}
        }
    }
}
