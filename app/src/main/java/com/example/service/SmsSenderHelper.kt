package com.example.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

sealed class SmsSendResult {
    data class Success(val partsCount: Int) : SmsSendResult()
    data class Failure(val reason: String) : SmsSendResult()
}

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String
)

class SmsSenderHelper(private val context: Context) {

    fun getAvailableSims(): List<SimInfo> {
        val list = mutableListOf<SimInfo>()
        try {
            val hasPhonePermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (hasPhonePermission && subManager != null) {
                val activeSubs = subManager.activeSubscriptionInfoList
                if (!activeSubs.isNullOrEmpty()) {
                    for (sub in activeSubs) {
                        list.add(
                            SimInfo(
                                subscriptionId = sub.subscriptionId,
                                slotIndex = sub.simSlotIndex,
                                displayName = sub.displayName?.toString() ?: "سیم‌کارت ${sub.simSlotIndex + 1}",
                                carrierName = sub.carrierName?.toString() ?: "اپراتور ${sub.simSlotIndex + 1}"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun sendSms(destinationAddress: String, text: String, targetSubId: Int? = null): SmsSendResult {
        if (destinationAddress.isBlank()) {
            return SmsSendResult.Failure("شماره مقصد نامعتبر است.")
        }
        if (text.isBlank()) {
            return SmsSendResult.Failure("متن پیامک ارسالی خالی است.")
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return SmsSendResult.Failure("دسترسی ارسال پیامک (SEND_SMS) به برنامه داده نشده است.")
        }

        return try {
            val smsManager: SmsManager = if (targetSubId != null && targetSubId >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(targetSubId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(targetSubId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            val parts = smsManager.divideMessage(text)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(destinationAddress, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(destinationAddress, null, text, null, null)
            }
            SmsSendResult.Success(parts.size)
        } catch (e: SecurityException) {
            SmsSendResult.Failure("عدم دسترسی امنیتی برای ارسال پیامک: ${e.message}")
        } catch (e: Exception) {
            SmsSendResult.Failure("خطا در ارسال پیامک: ${e.localizedMessage ?: "ناشناخته"}")
        }
    }
}

