package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY priority DESC, id ASC")
    fun getAllKeys(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE isEnabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getActiveKeys(): List<ApiKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ApiKeyEntity): Long

    @Update
    suspend fun updateKey(key: ApiKeyEntity)

    @Delete
    suspend fun deleteKey(key: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteKeyById(id: Long)

    @Query("UPDATE api_keys SET successCount = successCount + 1, lastUsedTimestamp = :timestamp, lastError = null WHERE id = :id")
    suspend fun recordSuccess(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE api_keys SET errorCount = errorCount + 1, lastUsedTimestamp = :timestamp, lastError = :error WHERE id = :id")
    suspend fun recordError(id: Long, error: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE api_keys SET rateLimitCount = rateLimitCount + 1, lastUsedTimestamp = :timestamp, lastError = :error WHERE id = :id")
    suspend fun recordRateLimit(id: Long, error: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority DESC, id ASC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getActiveRules(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("UPDATE rules SET matchCount = matchCount + 1, lastMatchedTimestamp = :timestamp WHERE id = :id")
    suspend fun recordMatch(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE type = :type ORDER BY timestamp DESC")
    fun getMemoriesByType(type: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 20): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 10): Flow<List<ActivityLogEntity>>

    @Query("SELECT COUNT(*) FROM activity_logs WHERE type = 'ERROR' AND timestamp > :sinceTimestamp")
    fun countErrorsSince(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingEntity>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun observeSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)
}
