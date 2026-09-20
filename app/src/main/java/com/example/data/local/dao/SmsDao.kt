package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.SmsMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE address = :address ORDER BY timestamp ASC")
    fun getMessagesForAddress(address: String): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE address = :address ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesForAddress(address: String, limit: Int = 50): List<SmsMessageEntity>

    @Query("SELECT * FROM sms_messages WHERE messageHash = :hash AND timestamp > :sinceTimestamp LIMIT 1")
    suspend fun findDuplicateMessage(hash: String, sinceTimestamp: Long): SmsMessageEntity?

    @Query("SELECT COUNT(*) FROM sms_messages WHERE isAiReply = 1 AND timestamp > :sinceTimestamp")
    suspend fun countAiRepliesSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM sms_messages WHERE timestamp > :sinceTimestamp")
    fun countMessagesSince(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessageEntity): Long

    @Update
    suspend fun updateMessage(message: SmsMessageEntity)

    @Query("DELETE FROM sms_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM sms_messages")
    suspend fun clearAllMessages()

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE address = :address LIMIT 1")
    suspend fun getConversation(address: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE address = :address")
    suspend fun deleteConversation(address: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}
