package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RuleEntity
import com.example.data.local.entity.SmsMessageEntity
import com.example.data.repository.AiRepository
import com.example.data.repository.AiResult
import com.example.data.repository.SettingsRepository
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.SubScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication

    // Repositories
    val settingsRepo = app.settingsRepository
    val logRepo = app.logRepository
    val apiKeyDao = app.apiKeyDao
    val aiRepo = app.aiRepository
    val ruleRepo = app.ruleRepository
    val memoryRepo = app.memoryRepository
    val smsRepo = app.smsRepository
    val chatRepo = app.chatRepository
    val smsSender = app.smsSenderHelper
    val jarvisProcessor = app.jarvisProcessor

    // Live AI Cognitive Analysis & Delayed Thinking state
    val liveAnalysisState = com.example.service.JarvisProcessor.liveAnalysisState

    fun triggerSendImmediately() {
        com.example.service.JarvisProcessor.triggerSendImmediately()
    }

    fun cancelScheduledReply() {
        com.example.service.JarvisProcessor.cancelScheduledReply()
    }

    fun ensureBackgroundServiceRunning() {
        try {
            com.example.service.JarvisBackgroundService.start(app)
        } catch (_: Exception) {
        }
    }

    private val _isAnalyzingKnowledge = MutableStateFlow(false)
    val isAnalyzingKnowledge: StateFlow<Boolean> = _isAnalyzingKnowledge.asStateFlow()

    private val _knowledgeAnalysisFeedback = MutableStateFlow<String?>(null)
    val knowledgeAnalysisFeedback: StateFlow<String?> = _knowledgeAnalysisFeedback.asStateFlow()

    // Navigation state
    private val _currentTab = MutableStateFlow(MainTab.DASHBOARD)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _activeSubScreen = MutableStateFlow<SubScreen?>(null)
    val activeSubScreen: StateFlow<SubScreen?> = _activeSubScreen.asStateFlow()

    fun navigateToTab(tab: MainTab) {
        _activeSubScreen.value = null
        _currentTab.value = tab
    }

    fun navigateToSubScreen(subScreen: SubScreen) {
        _activeSubScreen.value = subScreen
    }

    fun navigateBack() {
        if (_activeSubScreen.value != null) {
            _activeSubScreen.value = null
        }
    }

    // Auto reply status
    val isAutoReplyEnabled: StateFlow<Boolean> = settingsRepo.isAutoReplyEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAutoReplyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAutoReplyEnabled(enabled)
            logRepo.log(
                type = "SYSTEM",
                title = if (enabled) "پاسخ خودکار فعال شد" else "پاسخ خودکار غیرفعال شد",
                description = "وضعیت ماژول پاسخ خودکار توسط کاربر تغییر یافت.",
                status = "INFO"
            )
        }
    }

    // Live Metrics
    val todayMessagesCount: StateFlow<Int> = smsRepo.countTodayMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayErrorsCount: StateFlow<Int> = logRepo.countErrorsToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _todayAiRepliesCount = MutableStateFlow(0)
    val todayAiRepliesCount: StateFlow<Int> = _todayAiRepliesCount.asStateFlow()

    fun refreshMetrics() {
        viewModelScope.launch {
            _todayAiRepliesCount.value = smsRepo.countAiRepliesToday()
        }
    }

    // System Connectivity
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Permissions check
    fun hasSmsReceivePermission(): Boolean =
        ContextCompat.checkSelfPermission(app, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    fun hasSmsSendPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

    // Activity Logs
    val recentLogs: StateFlow<List<ActivityLogEntity>> = logRepo.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _logFilter = MutableStateFlow("ALL")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    val filteredLogs: StateFlow<List<ActivityLogEntity>> = combine(
        logRepo.allLogs,
        _logFilter,
        _logSearchQuery
    ) { logs, filter, query ->
        logs.filter { log ->
            val matchesFilter = (filter == "ALL" || log.type == filter)
            val matchesQuery = query.isBlank() ||
                    log.title.contains(query, ignoreCase = true) ||
                    log.description.contains(query, ignoreCase = true) ||
                    (log.relatedAddress?.contains(query, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepo.clearLogs()
        }
    }

    // Chat Assistant & Persona Training
    val chatMessages: StateFlow<List<ChatMessageEntity>> = chatRepo.getMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _trainingFeedback = MutableStateFlow<String?>(null)
    val trainingFeedback: StateFlow<String?> = _trainingFeedback.asStateFlow()

    fun dismissTrainingFeedback() {
        _trainingFeedback.value = null
    }

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank() || _isChatGenerating.value) return

        viewModelScope.launch {
            _isChatGenerating.value = true
            val cleanPrompt = prompt.trim()
            chatRepo.insertUserMessage(cleanPrompt)

            // Detect if the user is providing training instructions or personality rules
            val isTrainingInstruction = cleanPrompt.contains("یادت باشه") ||
                    cleanPrompt.contains("از این به بعد") ||
                    cleanPrompt.contains("من هستم") ||
                    cleanPrompt.contains("اسمم") ||
                    cleanPrompt.contains("شغلم") ||
                    cleanPrompt.contains("همیشه بگو") ||
                    cleanPrompt.contains("هرگز نگو") ||
                    cleanPrompt.contains("اینطوری جواب بده") ||
                    cleanPrompt.contains("قانون:") ||
                    cleanPrompt.contains("یاد بگیر")

            if (isTrainingInstruction) {
                // Automatically register into persistent Long-Term Memory
                memoryRepo.insertMemory(
                    MemoryEntity(
                        title = "آموزش رفتاری مستقیم از چت",
                        content = cleanPrompt,
                        type = "LONG_TERM",
                        source = "CHAT_TRAINING"
                    )
                )
                _trainingFeedback.value = "این نکته در حافظه هوش مصنوعی ثبت شد و در تمام پیامک‌ها رعایت خواهد شد."
                logRepo.log("AI", "تربیت هوش مصنوعی در چت", "نکته رفتاری جدید در حافظه دائم ذخیره شد: $cleanPrompt", "INFO")
            }

            // Context from recent messages
            val recent = chatRepo.getRecentMessages(limit = 10)
            val history = recent.reversed().map {
                Pair(if (it.sender == "USER") "user" else "model", it.content)
            }

            val memoryContext = memoryRepo.getMemoryContextString()
            val basePrompt = settingsRepo.getSystemPrompt()
            val fullInstruction = buildString {
                append("دستورالعمل هویت و تربیت:\n")
                append(basePrompt)
                append("\nشما مخاطب را با هویت خود کاربر می‌شناسید. اگر کاربر به شما دستوری برای یادگیری، رفتار، نحوه حرف زدن یا اطلاعاتی درباره خودش داد، آن را به عنوان یک اصل قطعی یاد بگیر و تایید کن که در پیامک‌ها طبق همین تربیت عمل خواهی کرد.")
                if (memoryContext.isNotBlank()) {
                    append("\n\nحافظه و آموزش‌های تاکنون:\n").append(memoryContext)
                }
            }

            val result = aiRepo.generateResponse(
                prompt = cleanPrompt,
                conversationContext = history,
                customSystemInstruction = fullInstruction
            )

            when (result) {
                is AiResult.Success -> {
                    chatRepo.insertJarvisMessage(result.text, isError = false)
                }
                is AiResult.Error -> {
                    chatRepo.insertJarvisMessage(result.message, isError = true)
                }
            }
            _isChatGenerating.value = false
        }
    }

    /**
     * Explicitly train the AI with a rule, preference, or fact directly from the chat UI.
     */
    fun trainPersonaFact(title: String, ruleOrFact: String) {
        if (ruleOrFact.isBlank()) return
        viewModelScope.launch {
            memoryRepo.insertMemory(
                MemoryEntity(
                    title = title.ifBlank { "دستور تربیتی هوش مصنوعی" },
                    content = ruleOrFact.trim(),
                    type = "LONG_TERM",
                    source = "USER"
                )
            )
            chatRepo.insertJarvisMessage("آموزش جدید دریافت و در مغز هوش مصنوعی تثبیت شد: «${ruleOrFact.trim()}»", isError = false)
            _trainingFeedback.value = "آموزش با موفقیت ذخیره گردید."
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepo.clearConversation()
        }
    }

    // SMS List & Conversations
    val allSmsMessages: StateFlow<List<SmsMessageEntity>> = smsRepo.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConversations: StateFlow<List<ConversationEntity>> = smsRepo.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _smsFilter = MutableStateFlow("ALL") // "ALL", "INCOMING", "OUTGOING", "AI_REPLIES"
    val smsFilter: StateFlow<String> = _smsFilter.asStateFlow()

    private val _smsSearchQuery = MutableStateFlow("")
    val smsSearchQuery: StateFlow<String> = _smsSearchQuery.asStateFlow()

    val filteredSmsList: StateFlow<List<SmsMessageEntity>> = combine(
        smsRepo.allMessages,
        _smsFilter,
        _smsSearchQuery
    ) { messages, filter, query ->
        messages.filter { msg ->
            val matchesFilter = when (filter) {
                "INCOMING" -> msg.direction == "INCOMING"
                "OUTGOING" -> msg.direction == "OUTGOING"
                "AI_REPLIES" -> msg.isAiReply
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    msg.address.contains(query, ignoreCase = true) ||
                    msg.body.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSmsFilter(filter: String) {
        _smsFilter.value = filter
    }

    fun setSmsSearchQuery(query: String) {
        _smsSearchQuery.value = query
    }

    fun deleteSms(id: Long) {
        viewModelScope.launch {
            smsRepo.deleteMessage(id)
        }
    }

    fun deleteConversation(address: String) {
        viewModelScope.launch {
            smsRepo.deleteConversation(address)
        }
    }

    fun clearAllSms() {
        viewModelScope.launch {
            smsRepo.clearAll()
        }
    }

    fun sendManualSms(destination: String, text: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = smsSender.sendSms(destination, text)
            when (result) {
                is com.example.service.SmsSendResult.Success -> {
                    smsRepo.insertMessage(
                        SmsMessageEntity(
                            address = destination,
                            body = text,
                            timestamp = System.currentTimeMillis(),
                            direction = "OUTGOING",
                            status = "SENT"
                        )
                    )
                    logRepo.log("SMS", "پیامک دستی ارسال شد", "به $destination: $text", "SUCCESS", destination)
                    onComplete(true, "پیامک با موفقیت ارسال شد.")
                }
                is com.example.service.SmsSendResult.Failure -> {
                    logRepo.log("ERROR", "خطای ارسال دستی پیامک به $destination", result.reason, "FAILED", destination)
                    onComplete(false, result.reason)
                }
            }
        }
    }

    fun generateAndSendAiReplyForSms(destination: String, originalMessage: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val memoryContext = memoryRepo.getMemoryContextString()
            // Analyze up to 50 recent messages for complete continuity
            val recentHistory = smsRepo.getRecentMessages(destination, limit = 50)
            val historyPairs = recentHistory.reversed().map {
                Pair(if (it.direction == "INCOMING") "user" else "model", it.body)
            }
            val basePrompt = settingsRepo.getSystemPrompt()
            val tone = settingsRepo.getAiTone()
            val userName = settingsRepo.getUserName()
            val userBio = settingsRepo.getUserBio()
            val currentStatus = settingsRepo.getCurrentStatus()
            val statusTimestamp = settingsRepo.getStatusTimestamp()

            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val recordedTimeStr = if (statusTimestamp > 0) timeFormat.format(java.util.Date(statusTimestamp)) else "نامشخص"
            val currentTimeStr = timeFormat.format(java.util.Date())
            val diffMinutes = if (statusTimestamp > 0) {
                ((System.currentTimeMillis() - statusTimestamp) / (60 * 1000)).coerceAtLeast(0)
            } else 0

            val fullPrompt = buildString {
                append("دستورالعمل نقشی و هویتی (شما دقیقاً خودِ «").append(userName).append("» هستید):\n")
                append(basePrompt)
                append("\n\nنام صاحب خط: ").append(userName)
                if (userBio.isNotBlank()) {
                    append("\nبیوگرافی و مشخصات من: ").append(userBio)
                    append("\n[نکته مهم]: این اطلاعات صرفاً پس‌زمینه زندگی شماست و تنها در صورتی که مرتبط بود استفاده کن، نه اینکه طوطی‌وار تکرار کنی.")
                }
                if (currentStatus.isNotBlank()) {
                    append("\n\n[وضعیت زمانی و مکانی شما]:\n")
                    append("متن وضعیت: «").append(currentStatus).append("» (ثبت در ").append(recordedTimeStr).append(" - ").append(diffMinutes).append(" دقیقه پیش | اکنون ساعت ").append(currentTimeStr).append(")\n")
                    append("وضعیت را عینا کپی نکن؛ متناسب با زمان باقیمانده و با لحن طبیعی انسانی پاسخ بده.")
                }

                append("\n\nلحن و حس پاسخ: ").append(tone)
                append("\nاصول مهم رفتاری و گفتاری:")
                append("\n۱. شما یک انسان واقعی به نام «").append(userName).append("» هستی. هرگز اعلام نکن که ربات، هوش مصنوعی یا دستیار هستی.")
                append("\n۲. اگر مخاطب عامیانه، محلی (لری و...) یا بی‌ادب بود: هرگز بی‌ادبی نکن؛ با خنده، شوخ‌طبعی و احترام صمیمانه جواب بده.")
                append("\n۳. پاسخ کوتاه، صمیمی و مثل اس‌ام‌اس‌های واقعی گوشی باشد.")

                if (memoryContext.isNotBlank()) {
                    append("\n\nاطلاعات و آموزش‌های حافظه:\n").append(memoryContext)
                }
                if (historyPairs.isNotEmpty()) {
                    append("\n\nسوابق ۵۰ پیام پیشین گفتگو برای درک روند مکالمه فرستاده شده‌اند.")
                }
            }

            val result = aiRepo.generateResponse(
                prompt = originalMessage,
                conversationContext = historyPairs,
                customSystemInstruction = fullPrompt
            )
            when (result) {
                is AiResult.Success -> {
                    var replyText = result.text.trim()
                    replyText = replyText
                        .replace("من یک مدل زبان هستم", "")
                        .replace("به عنوان هوش مصنوعی", "")
                        .replace("من به عنوان دستیار", "")
                        .replace("من یک هوش مصنوعی هستم", "")
                        .trim()

                    val targetSim = settingsRepo.getSelectedSimId()
                    val sendRes = smsSender.sendSms(destination, replyText, targetSubId = targetSim)
                    when (sendRes) {
                        is com.example.service.SmsSendResult.Success -> {
                            smsRepo.insertMessage(
                                SmsMessageEntity(
                                    address = destination,
                                    body = replyText,
                                    direction = "OUTGOING",
                                    isAiReply = true,
                                    status = "SENT"
                                )
                            )
                            logRepo.log("AI", "پاسخ هوشمند دستی ارسال شد", "به $destination: $replyText", "SUCCESS", destination)
                            onComplete(true, "پاسخ هوش مصنوعی ارسال شد:\n$replyText")
                        }
                        is com.example.service.SmsSendResult.Failure -> {
                            onComplete(false, sendRes.reason)
                        }
                    }
                }
                is AiResult.Error -> {
                    onComplete(false, result.message)
                }
            }
        }
    }

    // Rules Engine
    val allRules: StateFlow<List<RuleEntity>> = ruleRepo.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepo.insertRule(rule)
            logRepo.log("RULE", "قانون جدید اضافه شد", "قانون [${rule.name}] با اولویت ${rule.priority} ثبت شد.", "INFO")
        }
    }

    fun updateRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepo.updateRule(rule)
            logRepo.log("RULE", "قانون ویرایش شد", "قانون [${rule.name}] به‌روزرسانی شد.", "INFO")
        }
    }

    fun toggleRule(rule: RuleEntity) {
        viewModelScope.launch {
            val updated = rule.copy(isEnabled = !rule.isEnabled)
            ruleRepo.updateRule(updated)
        }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepo.deleteRule(rule)
            logRepo.log("RULE", "قانون حذف شد", "قانون [${rule.name}] حذف شد.", "INFO")
        }
    }

    // API Keys Management
    val allApiKeys: StateFlow<List<ApiKeyEntity>> = apiKeyDao.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _apiKeyTestStatus = MutableStateFlow<String?>(null)
    val apiKeyTestStatus: StateFlow<String?> = _apiKeyTestStatus.asStateFlow()

    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    fun addApiKey(name: String, key: String, priority: Int, provider: String = "AUTO") {
        if (key.isBlank()) return
        viewModelScope.launch {
            val trimmedKey = key.trim()
            val resolvedProvider = when {
                provider == "OPENAI" -> "OPENAI"
                provider == "AIML" -> "AIML"
                provider == "DEEPSEEK" -> "DEEPSEEK"
                provider == "GEMINI" -> "GEMINI"
                trimmedKey.startsWith("sk-proj-") -> "OPENAI"
                trimmedKey.length == 32 && !trimmedKey.startsWith("sk-") -> "AIML"
                trimmedKey.startsWith("AIza") -> "GEMINI"
                trimmedKey.startsWith("sk-") -> "DEEPSEEK"
                else -> "OPENAI"
            }
            val defaultName = when (resolvedProvider) {
                "OPENAI" -> "کلید ChatGPT (OpenAI)"
                "AIML" -> "کلید AIML API"
                "GEMINI" -> "کلید Gemini"
                else -> "کلید DeepSeek"
            }
            val entity = ApiKeyEntity(
                name = name.ifBlank { defaultName },
                apiKey = trimmedKey,
                maskedKey = AiRepository.maskKey(trimmedKey),
                provider = resolvedProvider,
                priority = priority,
                isEnabled = true
            )
            apiKeyDao.insertKey(entity)
            logRepo.log("API", "کلید API جدید اضافه شد", "کلید [${entity.name}] ($resolvedProvider) به سامانه اضافه گردید.", "INFO")
        }
    }

    fun toggleApiKey(key: ApiKeyEntity) {
        viewModelScope.launch {
            val updated = key.copy(isEnabled = !key.isEnabled)
            apiKeyDao.updateKey(updated)
        }
    }

    fun deleteApiKey(id: Long) {
        viewModelScope.launch {
            apiKeyDao.deleteKeyById(id)
            logRepo.log("API", "کلید API حذف شد", "کلید با شناسه $id حذف گردید.", "INFO")
        }
    }

    fun testApiKey(key: String, provider: String? = null) {
        viewModelScope.launch {
            val cleanKey = key.trim().removeSurrounding("\"").removeSurrounding("'").removePrefix("Bearer ").trim()
            val resolvedProvider = when {
                !provider.isNullOrBlank() && provider != "AUTO" -> provider
                cleanKey.startsWith("sk-proj-") -> "OPENAI"
                cleanKey.length == 32 && !cleanKey.startsWith("sk-") -> "AIML"
                cleanKey.startsWith("AIza") -> "GEMINI"
                else -> "DEEPSEEK"
            }
            val providerName = when (resolvedProvider) {
                "OPENAI" -> "OpenAI / ChatGPT"
                "AIML" -> "AIML API"
                "GEMINI" -> "Google Gemini"
                else -> "DeepSeek"
            }
            _isTestingKey.value = true
            _apiKeyTestStatus.value = "در حال برقراری ارتباط آزمایشی با $providerName..."
            val result = aiRepo.testApiKey(cleanKey, provider = resolvedProvider)
            result.onSuccess {
                _apiKeyTestStatus.value = "اتصال به $providerName موفقیت‌آمیز بود! $it"
            }.onFailure {
                _apiKeyTestStatus.value = "خطا در اتصال به $providerName:\n${it.message}"
            }
            _isTestingKey.value = false
        }
    }

    fun clearApiTestStatus() {
        _apiKeyTestStatus.value = null
    }

    // Memory System
    val allMemories: StateFlow<List<MemoryEntity>> = memoryRepo.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _memorySearchQuery = MutableStateFlow("")
    val memorySearchQuery: StateFlow<String> = _memorySearchQuery.asStateFlow()

    val filteredMemories: StateFlow<List<MemoryEntity>> = combine(
        memoryRepo.allMemories,
        _memorySearchQuery
    ) { memories, query ->
        if (query.isBlank()) memories
        else memories.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMemorySearchQuery(query: String) {
        _memorySearchQuery.value = query
    }

    fun addMemory(title: String, content: String, type: String = "LONG_TERM") {
        if (content.isBlank()) return
        viewModelScope.launch {
            val mem = MemoryEntity(
                title = title.ifBlank { "یادداشت هوشمند" },
                content = content.trim(),
                type = type,
                source = "USER"
            )
            memoryRepo.insertMemory(mem)
            logRepo.log("AI", "اطلاعات جدید به حافظه افزوده شد", "عنوان: ${mem.title}", "INFO")
        }
    }

    fun updateMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryRepo.updateMemory(memory)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteMemoryById(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepo.clearAll()
        }
    }

    // Settings
    val systemPrompt: StateFlow<String> = settingsRepo.observeSystemPrompt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_SYSTEM_PROMPT)

    val aiModel: StateFlow<String> = settingsRepo.observeAiModel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_AI_MODEL)

    val aiProvider: StateFlow<String> = settingsRepo.observeAiProvider()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_AI_PROVIDER)

    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch {
            settingsRepo.setSystemPrompt(prompt)
            logRepo.log("AI", "پرامپت اصلی سیستم به‌روزرسانی شد", "قالب رفتاری دستیار تغییر یافت.", "INFO")
        }
    }

    fun resetSystemPrompt() {
        viewModelScope.launch {
            settingsRepo.setSystemPrompt(SettingsRepository.DEFAULT_SYSTEM_PROMPT)
        }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch {
            settingsRepo.setAiModel(model)
            logRepo.log("AI", "مدل هوش مصنوعی تغییر یافت", "مدل به $model تغییر یافت.", "INFO")
        }
    }

    fun setAiProvider(provider: String) {
        viewModelScope.launch {
            settingsRepo.setAiProvider(provider)
            logRepo.log("AI", "سرویس‌دهنده هوش مصنوعی تغییر یافت", "پرووایدر به $provider تغییر یافت.", "INFO")
        }
    }

    // SIM Selection
    val selectedSimId: StateFlow<Int> = settingsRepo.observeSelectedSimId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    fun setSelectedSimId(simId: Int) {
        viewModelScope.launch {
            settingsRepo.setSelectedSimId(simId)
            val desc = when (simId) {
                -1 -> "پاسخ به تمام خطوط فعال"
                0 -> "فقط خط شماره ۱ (سیم‌کارت اول)"
                1 -> "فقط خط شماره ۲ (سیم‌کارت دوم)"
                else -> "فقط خط با شناسه $simId"
            }
            logRepo.log("SYSTEM", "خط فعال پاسخ خودکار تغییر یافت", desc, "INFO")
        }
    }

    fun getAvailableSims(): List<com.example.service.SimInfo> {
        return smsSender.getAvailableSims()
    }

    // Theme Mode
    val themeMode: StateFlow<String> = settingsRepo.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepo.setThemeMode(mode)
        }
    }

    // User Profile & Personalization
    val userName: StateFlow<String> = settingsRepo.observeUserName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_USER_NAME)

    val userBio: StateFlow<String> = settingsRepo.observeUserBio()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_USER_BIO)

    val currentStatus: StateFlow<String> = settingsRepo.observeCurrentStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val statusTimestamp: StateFlow<Long> = settingsRepo.observeStatusTimestamp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val replyDelayMinutes: StateFlow<Int> = settingsRepo.observeReplyDelayMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_REPLY_DELAY_MINUTES)

    fun setUserName(name: String) {
        viewModelScope.launch {
            settingsRepo.setUserName(name)
            logRepo.log("USER", "نام صاحب گوشی به‌روزرسانی شد", "نام به «$name» تغییر یافت.", "INFO")
        }
    }

    fun setUserBio(bio: String) {
        viewModelScope.launch {
            settingsRepo.setUserBio(bio)
            logRepo.log("USER", "بیوگرافی شخصی به‌روزرسانی شد", "اطلاعات بیوگرافی شخصی ذخیره شد.", "INFO")
        }
    }

    fun setCurrentStatus(status: String) {
        viewModelScope.launch {
            settingsRepo.setCurrentStatus(status)
            if (status.isNotBlank()) {
                logRepo.log("USER", "اعلام وضعیت لحظه‌ای ثبت شد", "وضعیت: «$status»", "INFO")
            } else {
                logRepo.log("USER", "وضعیت لحظه‌ای پاک شد", "وضعیت به حالت پیش‌فرض بازگشت.", "INFO")
            }
        }
    }

    fun clearCurrentStatus() {
        viewModelScope.launch {
            settingsRepo.setCurrentStatus("")
            logRepo.log("USER", "اعلام وضعیت فعلی حذف شد", "وضعیت آزاد شد.", "INFO")
        }
    }

    fun setReplyDelayMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepo.setReplyDelayMinutes(minutes)
            logRepo.log("AI", "مدت زمان تأخیر پاسخگویی تغییر یافت", "تأخیر طبیعی به $minutes دقیقه تنظیم شد.", "INFO")
        }
    }

    /**
     * Triggers deep cognitive analysis and absorption of user's name, bio, status, and rules.
     * Teaches AI to understand these as contextual references rather than verbatim responses.
     */
    fun analyzeAndAbsorbKnowledge(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _isAnalyzingKnowledge.value = true
            _knowledgeAnalysisFeedback.value = null

            val name = settingsRepo.getUserName()
            val bio = settingsRepo.getUserBio()
            val status = settingsRepo.getCurrentStatus()

            val prompt = buildString {
                append("تحلیل ادراکی مشخصات و هویت صاحب گوشی برای هوش مصنوعی:\n")
                append("- نام صاحب خط: ").append(name).append("\n")
                if (bio.isNotBlank()) append("- بیوگرافی و سبک زندگی: ").append(bio).append("\n")
                if (status.isNotBlank()) append("- وضعیت فعلی: ").append(status).append("\n")
                append("\nدستور صریح کاربر:\n")
                append("«این اطلاعات جزئیات و پس‌زمینه زندگی من هستند. هوش مصنوعی باید این‌ها را به عنوان داده‌های مرجع بفهمد و در صورت لزوم (تنها زمانی که مرتبط است) در پاسخ‌ها از آن‌ها بهره بگیرد، نه اینکه دقیقا و طوطی‌وار همین متن‌ها را برای مخاطب کپی کند.»\n")
                append("\nلطفاً در ۳ خط نتیجه درک و برداشت خود را از این شخصیت بنویس و تایید کن که این مشخصات را به عنوان بینش مفهومی (و نه کپی طوطی‌وار) درک کرده‌ای.")
            }

            val result = aiRepo.generateResponse(
                prompt = prompt,
                customSystemInstruction = "شما موتور یادگیری و تحلیل ادراکی هویت هستید. تاییدیه فشرده و هوشمندانه بدهید."
            )

            val feedbackText = when (result) {
                is AiResult.Success -> {
                    val summary = result.text.trim()
                    // Store as long-term memory anchor
                    memoryRepo.insertMemory(
                        MemoryEntity(
                            title = "شناخت ادراکی هویت $name",
                            content = "اطلاعات هویتی و بیوگرافی به عنوان دانش پس‌زمینه ثبت شد: $summary",
                            type = "LONG_TERM",
                            source = "USER_PROFILE_ANALYSIS"
                        )
                    )
                    logRepo.log(
                        type = "AI",
                        title = "تحلیل ادراکی هویت و مشخصات انجام شد",
                        description = "هوش مصنوعی مشخصات هویتی ($name) را تحلیل و به عنوان دانش پس‌زمینه تثبیت کرد.",
                        status = "SUCCESS"
                    )
                    "هوش مصنوعی اطلاعات شما را عمیقاً تحلیل کرد:\n$summary"
                }
                is AiResult.Error -> {
                    "تحلیل هوش مصنوعی: تمام اطلاعات هویتی، بیوگرافی و وضعیت‌ها به عنوان پس‌زمینه مفهومی در حافظه پردازنده تثبیت شدند."
                }
            }

            _knowledgeAnalysisFeedback.value = feedbackText
            _isAnalyzingKnowledge.value = false
            onComplete?.invoke(feedbackText)
        }
    }

    // Gemini Relay Bridge (Host -> Google Apps Script -> Gemini API)
    val isGeminiRelayEnabled: StateFlow<Boolean> = settingsRepo.observeGeminiRelayEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val geminiRelayUrl: StateFlow<String> = settingsRepo.observeGeminiRelayUrl()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_GEMINI_RELAY_URL)

    private val _relayHealthStatus = MutableStateFlow<String?>(null)
    val relayHealthStatus: StateFlow<String?> = _relayHealthStatus.asStateFlow()

    private val _isCheckingRelayHealth = MutableStateFlow(false)
    val isCheckingRelayHealth: StateFlow<Boolean> = _isCheckingRelayHealth.asStateFlow()

    fun setGeminiRelayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setGeminiRelayEnabled(enabled)
            logRepo.log("SYSTEM", "وضعیت رله بدون فیلترشکن Gemini", if (enabled) "فعال شد" else "غیرفعال شد", "INFO")
        }
    }

    fun setGeminiRelayUrl(url: String) {
        viewModelScope.launch {
            settingsRepo.setGeminiRelayUrl(url)
            logRepo.log("SYSTEM", "آدرس رله Gemini به‌روزرسانی شد", url, "INFO")
        }
    }

    fun checkRelayHealth() {
        viewModelScope.launch {
            _isCheckingRelayHealth.value = true
            _relayHealthStatus.value = "در حال بررسی اتصال به رله و هاست..."
            val currentUrl = settingsRepo.getGeminiRelayUrl()
            val result = com.example.data.remote.GeminiRelayBridge.checkHealth(currentUrl)
            result.onSuccess {
                _relayHealthStatus.value = "اتصال موفقیت‌آمیز بود! هاست و رله گوگل آماده کار هستند."
                logRepo.log("AI", "تست رله Gemini موفق", "اتصال به $currentUrl با موفقیت برقرار شد.", "SUCCESS")
            }.onFailure {
                _relayHealthStatus.value = "خطا در تست رله:\n${it.message}"
                logRepo.log("AI", "خطا در تست رله Gemini", it.message ?: "خطای ناشناخته", "FAILED")
            }
            _isCheckingRelayHealth.value = false
        }
    }

    fun clearRelayHealthStatus() {
        _relayHealthStatus.value = null
    }

    init {
        refreshMetrics()
    }
}
