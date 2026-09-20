package com.example.data.repository

import com.example.data.local.dao.AppSettingDao
import com.example.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val appSettingDao: AppSettingDao) {

    companion object {
        const val KEY_AUTO_REPLY_ENABLED = "auto_reply_enabled"
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_AI_PROVIDER = "ai_provider" // "OPENAI", "AIML", "DEEPSEEK", "GEMINI"
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_AI_TEMPERATURE = "ai_temperature"
        const val KEY_AI_TONE = "ai_tone"
        const val KEY_MAX_SMS_LENGTH = "max_sms_length"
        const val KEY_SPLIT_LONG_SMS = "split_long_sms"
        const val KEY_RATE_LIMIT_HOURLY = "rate_limit_hourly"
        const val KEY_RATE_LIMIT_DAILY = "rate_limit_daily"
        const val KEY_DUPLICATE_PROTECTION_MINUTES = "dup_protection_minutes"
        const val KEY_SETUP_COMPLETED = "setup_completed"
        const val KEY_ACTIVE_KEY_INDEX = "active_key_index"
        const val KEY_ACTIVE_SIM_SUBSCRIPTION = "active_sim_subscription" // -1: All SIMs, 0: SIM 1, 1: SIM 2
        const val KEY_THEME_MODE = "theme_mode" // "DARK", "LIGHT", "SYSTEM"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_BIO = "user_bio"
        const val KEY_CURRENT_STATUS = "current_status"
        const val KEY_STATUS_TIMESTAMP = "status_timestamp"
        const val KEY_REPLY_DELAY_MINUTES = "reply_delay_minutes"
        const val KEY_GEMINI_RELAY_ENABLED = "gemini_relay_enabled"
        const val KEY_GEMINI_RELAY_URL = "gemini_relay_url"

        const val DEFAULT_USER_NAME = "کاربر"
        const val DEFAULT_USER_BIO = ""
        const val DEFAULT_REPLY_DELAY_MINUTES = 10
        const val DEFAULT_GEMINI_RELAY_URL = "https://arefkyanmehr.ir/gemini.php"

        const val DEFAULT_SYSTEM_PROMPT =
            "تو خود «من» (صاحب گوشی) هستی و با هویت شخصی من مستقیماً به پیامک‌های مخاطب پاسخ می‌دهی. " +
            "اکیداً و تحت هیچ شرایطی نگو من جارویس یا هوش مصنوعی یا ربات هستم و هرگز نگو فلانی حضور ندارد! " +
            "پیام‌های پیشین و زمینه گفتگو را به دقت تحلیل کن و با لحنی طبیعی، خودمانی، محترمانه، هوشمند و کاملاً منطبق بر سبک پیامک (کوتاه و مفید بدون حاشیه) جواب بده. " +
            "هر آنچه در حافظه و آموزش‌ها ذکر شده را مو به مو رعایت کن."

        const val DEFAULT_AI_PROVIDER = "OPENAI"
        const val DEFAULT_AI_MODEL = "gpt-4o-mini" // Fast, ultra low-cost & official OpenAI model
    }

    fun isAutoReplyEnabled(): Flow<Boolean> =
        appSettingDao.observeSetting(KEY_AUTO_REPLY_ENABLED).map { it?.toBooleanStrictOrNull() ?: true }

    suspend fun isAutoReplyEnabledDirect(): Boolean =
        appSettingDao.getSetting(KEY_AUTO_REPLY_ENABLED)?.toBooleanStrictOrNull() ?: true

    suspend fun setAutoReplyEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSettingEntity(KEY_AUTO_REPLY_ENABLED, enabled.toString()))
    }

    suspend fun getSystemPrompt(): String {
        return appSettingDao.getSetting(KEY_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
    }

    fun observeSystemPrompt(): Flow<String> =
        appSettingDao.observeSetting(KEY_SYSTEM_PROMPT).map { it ?: DEFAULT_SYSTEM_PROMPT }

    suspend fun setSystemPrompt(prompt: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_SYSTEM_PROMPT, prompt))
    }

    suspend fun getAiProvider(): String {
        return appSettingDao.getSetting(KEY_AI_PROVIDER) ?: DEFAULT_AI_PROVIDER
    }

    fun observeAiProvider(): Flow<String> =
        appSettingDao.observeSetting(KEY_AI_PROVIDER).map { it ?: DEFAULT_AI_PROVIDER }

    suspend fun setAiProvider(provider: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_AI_PROVIDER, provider))
    }

    suspend fun getAiModel(): String {
        return appSettingDao.getSetting(KEY_AI_MODEL) ?: DEFAULT_AI_MODEL
    }

    fun observeAiModel(): Flow<String> =
        appSettingDao.observeSetting(KEY_AI_MODEL).map { it ?: DEFAULT_AI_MODEL }

    suspend fun setAiModel(model: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_AI_MODEL, model))
    }

    suspend fun getMaxSmsLength(): Int {
        return appSettingDao.getSetting(KEY_MAX_SMS_LENGTH)?.toIntOrNull() ?: 300
    }

    suspend fun setMaxSmsLength(length: Int) {
        appSettingDao.setSetting(AppSettingEntity(KEY_MAX_SMS_LENGTH, length.toString()))
    }

    suspend fun isSplitLongSms(): Boolean {
        return appSettingDao.getSetting(KEY_SPLIT_LONG_SMS)?.toBooleanStrictOrNull() ?: true
    }

    suspend fun setSplitLongSms(split: Boolean) {
        appSettingDao.setSetting(AppSettingEntity(KEY_SPLIT_LONG_SMS, split.toString()))
    }

    suspend fun getHourlyRateLimit(): Int {
        return appSettingDao.getSetting(KEY_RATE_LIMIT_HOURLY)?.toIntOrNull() ?: 30
    }

    suspend fun setHourlyRateLimit(limit: Int) {
        appSettingDao.setSetting(AppSettingEntity(KEY_RATE_LIMIT_HOURLY, limit.toString()))
    }

    suspend fun getDailyRateLimit(): Int {
        return appSettingDao.getSetting(KEY_RATE_LIMIT_DAILY)?.toIntOrNull() ?: 150
    }

    suspend fun setDailyRateLimit(limit: Int) {
        appSettingDao.setSetting(AppSettingEntity(KEY_RATE_LIMIT_DAILY, limit.toString()))
    }

    suspend fun getDuplicateProtectionMinutes(): Int {
        return appSettingDao.getSetting(KEY_DUPLICATE_PROTECTION_MINUTES)?.toIntOrNull() ?: 10
    }

    fun isSetupCompleted(): Flow<Boolean> =
        appSettingDao.observeSetting(KEY_SETUP_COMPLETED).map { it?.toBooleanStrictOrNull() ?: false }

    suspend fun setSetupCompleted(completed: Boolean) {
        appSettingDao.setSetting(AppSettingEntity(KEY_SETUP_COMPLETED, completed.toString()))
    }

    suspend fun getAiTone(): String {
        return appSettingDao.getSetting(KEY_AI_TONE) ?: "صمیمی و طبیعی"
    }

    suspend fun setAiTone(tone: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_AI_TONE, tone))
    }

    suspend fun getSelectedSimId(): Int {
        return appSettingDao.getSetting(KEY_ACTIVE_SIM_SUBSCRIPTION)?.toIntOrNull() ?: -1
    }

    fun observeSelectedSimId(): Flow<Int> =
        appSettingDao.observeSetting(KEY_ACTIVE_SIM_SUBSCRIPTION).map { it?.toIntOrNull() ?: -1 }

    suspend fun setSelectedSimId(simId: Int) {
        appSettingDao.setSetting(AppSettingEntity(KEY_ACTIVE_SIM_SUBSCRIPTION, simId.toString()))
    }

    suspend fun getThemeMode(): String {
        return appSettingDao.getSetting(KEY_THEME_MODE) ?: "DARK"
    }

    fun observeThemeMode(): Flow<String> =
        appSettingDao.observeSetting(KEY_THEME_MODE).map { it ?: "DARK" }

    suspend fun setThemeMode(mode: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_THEME_MODE, mode))
    }

    suspend fun getUserName(): String {
        return appSettingDao.getSetting(KEY_USER_NAME) ?: DEFAULT_USER_NAME
    }

    fun observeUserName(): Flow<String> =
        appSettingDao.observeSetting(KEY_USER_NAME).map { it ?: DEFAULT_USER_NAME }

    suspend fun setUserName(name: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_USER_NAME, name))
    }

    suspend fun getUserBio(): String {
        return appSettingDao.getSetting(KEY_USER_BIO) ?: DEFAULT_USER_BIO
    }

    fun observeUserBio(): Flow<String> =
        appSettingDao.observeSetting(KEY_USER_BIO).map { it ?: DEFAULT_USER_BIO }

    suspend fun setUserBio(bio: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_USER_BIO, bio))
    }

    suspend fun getCurrentStatus(): String {
        return appSettingDao.getSetting(KEY_CURRENT_STATUS) ?: ""
    }

    fun observeCurrentStatus(): Flow<String> =
        appSettingDao.observeSetting(KEY_CURRENT_STATUS).map { it ?: "" }

    suspend fun setCurrentStatus(status: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_CURRENT_STATUS, status))
        if (status.isNotBlank()) {
            appSettingDao.setSetting(AppSettingEntity(KEY_STATUS_TIMESTAMP, System.currentTimeMillis().toString()))
        } else {
            appSettingDao.setSetting(AppSettingEntity(KEY_STATUS_TIMESTAMP, "0"))
        }
    }

    suspend fun getStatusTimestamp(): Long {
        return appSettingDao.getSetting(KEY_STATUS_TIMESTAMP)?.toLongOrNull() ?: 0L
    }

    fun observeStatusTimestamp(): Flow<Long> =
        appSettingDao.observeSetting(KEY_STATUS_TIMESTAMP).map { it?.toLongOrNull() ?: 0L }

    suspend fun getReplyDelayMinutes(): Int {
        return appSettingDao.getSetting(KEY_REPLY_DELAY_MINUTES)?.toIntOrNull() ?: DEFAULT_REPLY_DELAY_MINUTES
    }

    fun observeReplyDelayMinutes(): Flow<Int> =
        appSettingDao.observeSetting(KEY_REPLY_DELAY_MINUTES).map { it?.toIntOrNull() ?: DEFAULT_REPLY_DELAY_MINUTES }

    suspend fun setReplyDelayMinutes(minutes: Int) {
        appSettingDao.setSetting(AppSettingEntity(KEY_REPLY_DELAY_MINUTES, minutes.toString()))
    }

    suspend fun isGeminiRelayEnabled(): Boolean {
        return appSettingDao.getSetting(KEY_GEMINI_RELAY_ENABLED)?.toBooleanStrictOrNull() ?: true
    }

    fun observeGeminiRelayEnabled(): Flow<Boolean> =
        appSettingDao.observeSetting(KEY_GEMINI_RELAY_ENABLED).map { it?.toBooleanStrictOrNull() ?: true }

    suspend fun setGeminiRelayEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSettingEntity(KEY_GEMINI_RELAY_ENABLED, enabled.toString()))
    }

    suspend fun getGeminiRelayUrl(): String {
        return appSettingDao.getSetting(KEY_GEMINI_RELAY_URL) ?: DEFAULT_GEMINI_RELAY_URL
    }

    fun observeGeminiRelayUrl(): Flow<String> =
        appSettingDao.observeSetting(KEY_GEMINI_RELAY_URL).map { it ?: DEFAULT_GEMINI_RELAY_URL }

    suspend fun setGeminiRelayUrl(url: String) {
        appSettingDao.setSetting(AppSettingEntity(KEY_GEMINI_RELAY_URL, url.trim()))
    }
}
