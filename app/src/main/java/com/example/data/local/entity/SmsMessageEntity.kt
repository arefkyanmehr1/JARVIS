package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_messages",
    indices = [
        Index(value = ["address"]),
        Index(value = ["timestamp"]),
        Index(value = ["messageHash"], unique = false)
    ]
)
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val contactName: String? = null,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val direction: String, // "INCOMING" or "OUTGOING"
    val isAiReply: Boolean = false,
    val status: String = "RECEIVED", // "RECEIVED", "SENT", "DELIVERED", "FAILED", "PENDING", "IGNORED"
    val ruleAppliedName: String? = null,
    val messageHash: String = ""
)

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["address"], unique = true)]
)
data class ConversationEntity(
    @PrimaryKey
    val address: String,
    val contactName: String? = null,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isAiAutoReplyEnabled: Boolean = true
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String = "assistant_main",
    val sender: String, // "USER" or "JARVIS"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)
