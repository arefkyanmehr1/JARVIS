package com.example.data.repository

import com.example.data.local.dao.SmsDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.SmsMessageEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class SmsRepository(private val smsDao: SmsDao) {

    val allMessages: Flow<List<SmsMessageEntity>> = smsDao.getAllMessages()
    val allConversations: Flow<List<ConversationEntity>> = smsDao.getAllConversations()

    fun getMessagesForAddress(address: String): Flow<List<SmsMessageEntity>> =
        smsDao.getMessagesForAddress(address)

    suspend fun getRecentMessages(address: String, limit: Int = 5): List<SmsMessageEntity> =
        smsDao.getRecentMessagesForAddress(address, limit)

    suspend fun insertMessage(message: SmsMessageEntity): Long {
        val id = smsDao.insertMessage(message)
        // Also update conversation summary
        val existingConv = smsDao.getConversation(message.address)
        val updatedConv = existingConv?.copy(
            lastMessage = message.body,
            lastTimestamp = message.timestamp,
            unreadCount = if (message.direction == "INCOMING") existingConv.unreadCount + 1 else 0
        ) ?: ConversationEntity(
            address = message.address,
            contactName = message.contactName,
            lastMessage = message.body,
            lastTimestamp = message.timestamp,
            unreadCount = if (message.direction == "INCOMING") 1 else 0
        )
        smsDao.insertOrUpdateConversation(updatedConv)
        return id
    }

    suspend fun updateMessage(message: SmsMessageEntity) =
        smsDao.updateMessage(message)

    suspend fun deleteMessage(id: Long) =
        smsDao.deleteMessageById(id)

    suspend fun deleteConversation(address: String) =
        smsDao.deleteConversation(address)

    suspend fun clearAll() {
        smsDao.clearAllMessages()
        smsDao.clearAllConversations()
    }

    /**
     * Checks if this exact message has already been processed within duplicate protection window.
     */
    suspend fun isDuplicate(hash: String, windowMinutes: Int): Boolean {
        val cutoff = System.currentTimeMillis() - (windowMinutes * 60 * 1000L)
        return smsDao.findDuplicateMessage(hash, cutoff) != null
    }

    suspend fun hasMessageAround(
        address: String,
        body: String,
        direction: String,
        timestamp: Long,
        toleranceMs: Long = 2 * 60 * 1000L
    ): Boolean {
        return smsDao.findMessageAround(
            address = address,
            body = body,
            direction = direction,
            fromTimestamp = timestamp - toleranceMs,
            toTimestamp = timestamp + toleranceMs
        ) != null
    }

    suspend fun countAiRepliesToday(): Int {
        // Since start of today (midnight)
        val midnight = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        return smsDao.countAiRepliesSince(midnight)
    }

    fun countTodayMessages(): Flow<Int> {
        val midnight = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        return smsDao.countMessagesSince(midnight)
    }

    companion object {
        fun computeMessageHash(sender: String, body: String): String {
            val raw = "${sender.trim()}:${body.trim()}"
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(raw.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }
    }
}
