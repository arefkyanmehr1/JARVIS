package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.local.entity.SmsMessageEntity
import com.example.data.repository.SmsRepository

/**
 * Imports the existing SMS history for one contact from Android's SMS provider
 * into JARVIS' local Room database.
 *
 * This is intentionally separate from SmsReceiver: receiving a new SMS and
 * reading the already-existing conversation are two different operations.
 */
class SmsHistorySync(
    private val context: Context,
    private val smsRepo: SmsRepository
) {

    suspend fun syncConversation(address: String): Int {
        if (address.isBlank()) return 0

        val hasReadSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReadSms) return 0

        val addresses = buildAddressVariants(address)
        if (addresses.isEmpty()) return 0

        val placeholders = addresses.joinToString(",") { "?" }
        val selection = "address IN ($placeholders)"
        val selectionArgs = addresses.toTypedArray()

        var imported = 0

        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
                ),
                selection,
                selectionArgs,
                Telephony.Sms.DATE + " ASC"
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE)

                if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0 || typeIndex < 0) {
                    return@use
                }

                while (cursor.moveToNext()) {
                    val smsAddress = cursor.getString(addressIndex)?.trim().orEmpty()
                    val body = cursor.getString(bodyIndex)?.trim().orEmpty()
                    val timestamp = cursor.getLong(dateIndex)
                    val type = cursor.getInt(typeIndex)

                    if (smsAddress.isBlank() || body.isBlank() || timestamp <= 0L) continue

                    val direction = when (type) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> "INCOMING"
                        Telephony.Sms.MESSAGE_TYPE_SENT,
                        Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                        Telephony.Sms.MESSAGE_TYPE_FAILED,
                        Telephony.Sms.MESSAGE_TYPE_QUEUED -> "OUTGOING"
                        else -> continue
                    }

                    // Prevent duplicates without treating two identical messages
                    // sent at different times as the same conversation message.
                    if (smsRepo.hasMessageAround(
                            address = smsAddress,
                            body = body,
                            direction = direction,
                            timestamp = timestamp
                        )
                    ) {
                        continue
                    }

                    val contactName = ContactHelper.getContactName(context, smsAddress)
                    val hash = SmsRepository.computeHistoryMessageHash(
                        smsAddress,
                        body,
                        timestamp,
                        direction
                    )

                    smsRepo.insertMessage(
                        SmsMessageEntity(
                            address = smsAddress,
                            contactName = contactName,
                            body = body,
                            timestamp = timestamp,
                            direction = direction,
                            isAiReply = false,
                            status = when (type) {
                                Telephony.Sms.MESSAGE_TYPE_FAILED -> "FAILED"
                                Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                                Telephony.Sms.MESSAGE_TYPE_QUEUED -> "PENDING"
                                else -> if (direction == "INCOMING") "RECEIVED" else "SENT"
                            },
                            messageHash = hash
                        )
                    )
                    imported++
                }
            }
        } catch (_: SecurityException) {
            return imported
        } catch (_: Exception) {
            return imported
        }

        return imported
    }

    private fun buildAddressVariants(address: String): List<String> {
        val result = linkedSetOf<String>()
        val original = address.trim()
        if (original.isNotBlank()) result += original

        val digits = original.filter(Char::isDigit)
        if (digits.isBlank()) return result.toList()

        result += digits
        result += "+$digits"

        when {
            digits.startsWith("00") && digits.length > 2 -> {
                val country = digits.removePrefix("00")
                result += "+$country
                if (country.startsWith("98") && country.length > 2) {
                    result += "0" + country.removePrefix("98")
                }
            }

            digits.startsWith("98") && digits.length > 2 -> {
                result += "+$digits"
                result += "0" + digits.removePrefix("98")
            }

            digits.startsWith("9") && digits.length == 10 -> {
                result += "0$digits"
                result += "+98$digits"
            }

            digits.startsWith("0") && digits.length > 1 -> {
                val national = digits.removePrefix("0")
                if (national.startsWith("9") && national.length == 10) {
                    result += "+98$national"
                    result += "98$national"
                }
            }
        }

        return result.toList().take(8)
    }
}
