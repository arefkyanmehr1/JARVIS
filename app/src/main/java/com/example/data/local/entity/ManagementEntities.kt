package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val apiKey: String,
    val maskedKey: String,
    val provider: String = "OPENAI", // "OPENAI", "AIML", "DEEPSEEK", or "GEMINI"
    val isEnabled: Boolean = true,
    val priority: Int = 1, // Higher priority attempted first
    val successCount: Int = 0,
    val errorCount: Int = 0,
    val rateLimitCount: Int = 0,
    val lastUsedTimestamp: Long = 0,
    val lastError: String? = null
) {
    fun isDeepSeek(): Boolean =
        provider.equals("DEEPSEEK", ignoreCase = true) ||
        (apiKey.startsWith("sk-") && !apiKey.startsWith("sk-proj-") && (provider.isBlank() || provider == "DEEPSEEK"))

    fun isOpenAi(): Boolean =
        provider.equals("OPENAI", ignoreCase = true) ||
        apiKey.startsWith("sk-proj-")

    fun isAiml(): Boolean =
        provider.equals("AIML", ignoreCase = true)

    fun isGemini(): Boolean =
        provider.equals("GEMINI", ignoreCase = true) || apiKey.startsWith("AIza")
}

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val priority: Int = 50, // 100 (high), 50 (medium), 10 (low)
    val senderPattern: String = "", // Specific phone number or prefix (e.g. "+98912")
    val keywordPatterns: String = "", // Comma-separated keywords or regex string
    val isRegex: Boolean = false,
    val actionType: String = "AUTO_REPLY", // "AUTO_REPLY", "IGNORE", "BLOCK", "FIXED_REPLY"
    val fixedReplyText: String? = null,
    val customAiPrompt: String? = null,
    val matchCount: Int = 0,
    val lastMatchedTimestamp: Long = 0
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val type: String = "LONG_TERM", // "SHORT_TERM", "LONG_TERM"
    val source: String = "USER", // "USER", "SMS", "AI"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "SMS", "AI", "API", "RULE", "ERROR", "SYSTEM"
    val title: String,
    val description: String,
    val status: String = "INFO", // "SUCCESS", "FAILED", "INFO", "WARNING"
    val relatedAddress: String? = null
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
