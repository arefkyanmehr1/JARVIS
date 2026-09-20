package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    fun getMessages(conversationId: String = "assistant_main"): Flow<List<ChatMessageEntity>> =
        chatDao.getMessages(conversationId)

    suspend fun getRecentMessages(conversationId: String = "assistant_main", limit: Int = 10): List<ChatMessageEntity> =
        chatDao.getRecentMessages(conversationId, limit)

    suspend fun insertUserMessage(content: String, conversationId: String = "assistant_main"): Long =
        chatDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                sender = "USER",
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )

    suspend fun insertJarvisMessage(
        content: String,
        isError: Boolean = false,
        conversationId: String = "assistant_main"
    ): Long =
        chatDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                sender = "JARVIS",
                content = content,
                timestamp = System.currentTimeMillis(),
                isError = isError
            )
        )

    suspend fun deleteMessage(id: Long) = chatDao.deleteMessage(id)

    suspend fun clearConversation(conversationId: String = "assistant_main") =
        chatDao.clearConversation(conversationId)
}
