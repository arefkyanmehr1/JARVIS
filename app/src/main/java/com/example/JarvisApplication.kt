package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RuleEntity
import com.example.data.repository.AiRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.LogRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.RuleRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SmsRepository
import com.example.service.JarvisProcessor
import com.example.service.NotificationHelper
import com.example.service.SmsSenderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JarvisApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getInstance(this) }

    val settingsRepository by lazy { SettingsRepository(database.appSettingDao()) }
    val logRepository by lazy { LogRepository(database.activityLogDao()) }
    val apiKeyDao by lazy { database.apiKeyDao() }
    val aiRepository by lazy { AiRepository(apiKeyDao, database.activityLogDao(), settingsRepository) }
    val ruleRepository by lazy { RuleRepository(database.ruleDao()) }
    val memoryRepository by lazy { MemoryRepository(database.memoryDao()) }
    val smsRepository by lazy { SmsRepository(database.smsDao()) }
    val chatRepository by lazy { ChatRepository(database.chatDao()) }

    val smsSenderHelper by lazy { SmsSenderHelper(this) }
    val notificationHelper by lazy { NotificationHelper(this) }

    val jarvisProcessor by lazy {
        JarvisProcessor(
            context = this,
            smsRepo = smsRepository,
            aiRepo = aiRepository,
            ruleRepo = ruleRepository,
            memoryRepo = memoryRepository,
            logRepo = logRepository,
            settingsRepo = settingsRepository,
            smsSender = smsSenderHelper,
            notificationHelper = notificationHelper
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeDefaultData()
    }

    private fun initializeDefaultData() {
        applicationScope.launch {
            // Seed initial memory if empty
            val initialMemories = database.memoryDao().getRecentMemories(1)
            if (initialMemories.isEmpty()) {
                database.memoryDao().insertMemory(
                    MemoryEntity(
                        title = "قالب پاسخگویی",
                        content = "پاسخ‌ها را همیشه به زبان فارسی روان، خلاصه، متین و مناسب پیامک ارائه کن.",
                        type = "LONG_TERM",
                        source = "USER"
                    )
                )
            }

            // Seed initial DeepSeek keys if empty
            val initialKeys = database.apiKeyDao().getActiveKeys()
            if (initialKeys.isEmpty()) {
                database.apiKeyDao().insertKey(
                    ApiKeyEntity(
                        name = "کلید اصلی DeepSeek (Flash)",
                        apiKey = "sk-d0833a00b40d4b3ebf6b1e55adf569b0",
                        maskedKey = AiRepository.maskKey("sk-d0833a00b40d4b3ebf6b1e55adf569b0"),
                        provider = "DEEPSEEK",
                        isEnabled = true,
                        priority = 3
                    )
                )
                database.apiKeyDao().insertKey(
                    ApiKeyEntity(
                        name = "کلید رزرو DeepSeek ۱",
                        apiKey = "sk-4ca4b46aa80b4e5ca990c028a5334057",
                        maskedKey = AiRepository.maskKey("sk-4ca4b46aa80b4e5ca990c028a5334057"),
                        provider = "DEEPSEEK",
                        isEnabled = true,
                        priority = 2
                    )
                )
                database.apiKeyDao().insertKey(
                    ApiKeyEntity(
                        name = "کلید رزرو DeepSeek ۲",
                        apiKey = "sk-cdf70e57116d4b419e2f4e44fb992250",
                        maskedKey = AiRepository.maskKey("sk-cdf70e57116d4b419e2f4e44fb992250"),
                        provider = "DEEPSEEK",
                        isEnabled = true,
                        priority = 1
                    )
                )
            }

            // Seed sample default rule if empty
            val rules = database.ruleDao().getActiveRules()
            if (rules.isEmpty()) {
                database.ruleDao().insertRule(
                    RuleEntity(
                        name = "پاسخ خودکار به احوالپرسی",
                        isEnabled = true,
                        priority = 80,
                        senderPattern = "",
                        keywordPatterns = "سلام, درود, وقت بخیر, سلام علیکم",
                        isRegex = false,
                        actionType = "AUTO_REPLY",
                        customAiPrompt = "با احترام و متانت به سلام و احوالپرسی پاسخ بده و اعلام کن در صورت داشتن کار فوری تماس بگیرند."
                    )
                )
                database.ruleDao().insertRule(
                    RuleEntity(
                        name = "نادیده گرفتن تبلیغات",
                        isEnabled = true,
                        priority = 90,
                        senderPattern = "",
                        keywordPatterns = "کد تخفیف, لغو11, ارسال 1, قرعه کشی, برنده شدید",
                        isRegex = false,
                        actionType = "IGNORE"
                    )
                )
            }

            // Initial startup log
            database.activityLogDao().insertLog(
                ActivityLogEntity(
                    type = "SYSTEM",
                    title = "هسته JARVIS راه‌اندازی شد",
                    description = "سامانه هوشمند پایش و پاسخ پیامک با موفقیت بارگذاری گردید.",
                    status = "SUCCESS"
                )
            )
        }
    }

    companion object {
        lateinit var instance: JarvisApplication
            private set
    }
}
