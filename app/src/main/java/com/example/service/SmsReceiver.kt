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

            // Extract subscription/slot if available in intent extras
            val subscriptionId = intent.getIntExtra("subscription", -1).let {
                if (it != -1) it else intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
            }
            val slotIndex = intent.getIntExtra("slot", -1).let {
                if (it != -1) it else intent.getIntExtra("simSlot", -1)
            }

            // Group multipart messages by originating address
            val groupedBySender = mutableMapOf<String, StringBuilder>()
            for (sms in messages) {
                val address = sms.displayOriginatingAddress ?: sms.originatingAddress ?: continue
                val body = sms.displayMessageBody ?: sms.messageBody ?: ""
                groupedBySender.getOrPut(address) { StringBuilder() }.append(body)
            }

            val app = (context.applicationContext as? JarvisApplication) ?: JarvisApplication.instance

            // Ensure background service is running to guarantee countdown and thinking completion
            try {
                JarvisBackgroundService.start(context)
            } catch (_: Exception) {
            }

            for ((sender, bodyBuilder) in groupedBySender) {
                val fullMessage = bodyBuilder.toString()
                if (fullMessage.isNotBlank()) {
                    app.applicationScope.launch {
                        app.jarvisProcessor.processIncomingSms(
                            sender = sender,
                            messageBody = fullMessage,
                            incomingSubId = subscriptionId,
                            incomingSlotIndex = slotIndex
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Log receiver error
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
            } catch (_: Exception) {
            }
        }
    }
}
