package com.example.data.repository

import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

class LogRepository(private val logDao: ActivityLogDao) {

    val allLogs: Flow<List<ActivityLogEntity>> = logDao.getAllLogs()
    val recentLogs: Flow<List<ActivityLogEntity>> = logDao.getRecentLogs(10)

    fun getLogsByType(type: String): Flow<List<ActivityLogEntity>> =
        if (type == "ALL") logDao.getAllLogs() else logDao.getLogsByType(type)

    fun searchLogs(query: String): Flow<List<ActivityLogEntity>> =
        logDao.searchLogs(query)

    fun countErrorsToday(): Flow<Int> {
        val midnight = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        return logDao.countErrorsSince(midnight)
    }

    suspend fun log(
        type: String, // "SMS", "AI", "API", "RULE", "ERROR", "SYSTEM"
        title: String,
        description: String,
        status: String = "INFO",
        relatedAddress: String? = null
    ): Long {
        return logDao.insertLog(
            ActivityLogEntity(
                type = type,
                title = title,
                description = description,
                status = status,
                relatedAddress = relatedAddress
            )
        )
    }

    suspend fun clearLogs() = logDao.clearLogs()
}
