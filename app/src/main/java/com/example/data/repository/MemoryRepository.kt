package com.example.data.repository

import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {

    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    fun searchMemories(query: String): Flow<List<MemoryEntity>> =
        memoryDao.searchMemories(query)

    suspend fun insertMemory(memory: MemoryEntity): Long =
        memoryDao.insertMemory(memory)

    suspend fun updateMemory(memory: MemoryEntity) =
        memoryDao.updateMemory(memory)

    suspend fun deleteMemory(memory: MemoryEntity) =
        memoryDao.deleteMemory(memory)

    suspend fun deleteMemoryById(id: Long) =
        memoryDao.deleteMemoryById(id)

    suspend fun clearAll() =
        memoryDao.clearAllMemories()

    /**
     * Builds a structured context string from user memories to feed into Gemini.
     */
    suspend fun getMemoryContextString(limit: Int = 15): String {
        val memories = memoryDao.getRecentMemories(limit)
        if (memories.isEmpty()) return ""

        val sb = StringBuilder("\nاطلاعات ذخیره شده در حافظه دائمی:\n")
        memories.forEachIndexed { index, mem ->
            sb.append("${index + 1}. [${mem.title}]: ${mem.content}\n")
        }
        return sb.toString()
    }
}
