package com.example.service

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SmsMessageEntity
import com.example.data.repository.AiRepository
import com.example.data.repository.AiResult
import com.example.data.repository.LogRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.RuleRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisProcessor(
    private val context: Context,
    private val smsRepo: SmsRepository,
    private val aiRepo: AiRepository,
    private val ruleRepo: RuleRepository,
    private val memoryRepo: MemoryRepository,
    private val logRepo: LogRepository,
    private val settingsRepo: SettingsRepository,
    private val smsSender: SmsSenderHelper,
    private val notificationHelper: NotificationHelper
) {

    companion object {
        private val _liveAnalysisState = MutableStateFlow(AiAnalysisState())
        val liveAnalysisState: StateFlow<AiAnalysisState> = _liveAnalysisState.asStateFlow()

        @Volatile
        private var sendImmediatelyFlag = false
        @Volatile
        private var cancelScheduledReplyFlag = false

        fun triggerSendImmediately() {
            sendImmediatelyFlag = true
        }

        fun cancelScheduledReply() {
            cancelScheduledReplyFlag = true
        }

        fun updateLiveAnalysis(state: AiAnalysisState) {
            _liveAnalysisState.value = state
        }

        fun clearLiveAnalysis() {
            _liveAnalysisState.value = AiAnalysisState()
            sendImmediatelyFlag = false
            cancelScheduledReplyFlag = false
        }
    }

    suspend fun processIncomingSms(
        sender: String,
        messageBody: String,
        incomingSubId: Int = -1,
        incomingSlotIndex: Int = -1
    ) = withContext(Dispatchers.IO) {
        if (sender.isBlank() || messageBody.isBlank()) return@withContext

        // 0. SIM Filter Check: Check if user designated a specific SIM card
        val selectedSimId = settingsRepo.getSelectedSimId()
        if (selectedSimId >= 0) {
            // If incomingSubId is known and doesn't match selectedSimId, check if slotIndex matches
            val matchesSub = incomingSubId == selectedSimId
            val matchesSlot = incomingSlotIndex == selectedSimId
            if (incomingSubId != -1 && !matchesSub && !matchesSlot) {
                logRepo.log(
                    type = "SMS",
                    title = "پیام خط دیگر نادیده گرفته شد",
                    description = "پیام از $sender روی خط (Sub:$incomingSubId / سیم‌کارت ${incomingSlotIndex + 1}) دریافت شد اما تنظیم روی سیم‌کارت دیگر قرار دارد.",
                    status = "INFO",
                    relatedAddress = sender
                )
                return@withContext
            }
        }

        val hash = SmsRepository.computeMessageHash(sender, messageBody)

        // 1. Duplicate Protection Check
        val dupMinutes = settingsRepo.getDuplicateProtectionMinutes()
        if (smsRepo.isDuplicate(hash, dupMinutes)) {
            logRepo.log(
                type = "SMS",
                title = "پیام تکراری نادیده گرفته شد",
                description = "پیام از $sender قبلاً در بازه $dupMinutes دقیقه گذشته پردازش شده بود.",
                status = "INFO",
                relatedAddress = sender
            )
            return@withContext
        }

        // 2. Record Incoming Message
        val incomingId = smsRepo.insertMessage(
            SmsMessageEntity(
                address = sender,
                body = messageBody,
                timestamp = System.currentTimeMillis(),
                direction = "INCOMING",
                status = "RECEIVED",
                messageHash = hash
            )
        )

        logRepo.log(
            type = "SMS",
            title = "پیامک دریافت شد",
            description = "فرستنده: $sender | متن: $messageBody",
            status = "INFO",
            relatedAddress = sender
        )

        notificationHelper.showSmsReceivedNotification(sender, messageBody)

        // 3. Check Rule Engine
        val ruleResult = ruleRepo.evaluate(sender, messageBody)
        val matchedRuleName = ruleResult.matchedRule?.name

        when (ruleResult.action) {
            "IGNORE" -> {
                logRepo.log(
                    type = "RULE",
                    title = "قانون اجرا شد: نادیده گرفتن",
                    description = "پیام $sender مطابق قانون [$matchedRuleName] نادیده گرفته شد.",
                    status = "INFO",
                    relatedAddress = sender
                )
                return@withContext
            }
            "BLOCK" -> {
                logRepo.log(
                    type = "RULE",
                    title = "قانون اجرا شد: مسدودسازی",
                    description = "پردازش پیام $sender مطابق قانون [$matchedRuleName] مسدود شد.",
                    status = "WARNING",
                    relatedAddress = sender
                )
                return@withContext
            }
            "FIXED_REPLY" -> {
                val fixedText = ruleResult.fixedReplyText ?: "پیام شما دریافت شد. در اسرع وقت پاسخ خواهم داد."
                sendAndRecordReply(sender, fixedText, isAi = false, ruleName = matchedRuleName, targetSubId = selectedSimId)
                return@withContext
            }
        }

        // 4. Check Global Auto-Reply Switch (CRITICAL: Strictly respect toggle switch)
        val isAutoReplyOn = settingsRepo.isAutoReplyEnabledDirect()
        if (!isAutoReplyOn) {
            logRepo.log(
                type = "AI",
                title = "پاسخ خودکار غیرفعال است",
                description = "پیامک از $sender دریافت و ذخیره شد اما پاسخ خودکار توسط شما خاموش است.",
                status = "INFO",
                relatedAddress = sender
            )
            return@withContext
        }

        // 5. Rate Limiting Check
        val dailyLimit = settingsRepo.getDailyRateLimit()
        val todayReplies = smsRepo.countAiRepliesToday()
        if (todayReplies >= dailyLimit) {
            val limitMsg = "سقف مجاز پاسخ‌های خودکار روزانه ($dailyLimit عدد) تکمیل شده است."
            logRepo.log(
                type = "ERROR",
                title = "محدودیت تعداد پاسخ خودکار",
                description = limitMsg,
                status = "WARNING",
                relatedAddress = sender
            )
            notificationHelper.showErrorNotification("محدودیت پاسخ خودکار", limitMsg)
            return@withContext
        }

        // 6. Deep Conversation Context & Memory Integration
        // Fetch up to 50 recent messages from this sender to analyze conversation history & relationship
        val recentHistory = smsRepo.getRecentMessages(sender, limit = 50)
        // Order chronologically: oldest first, latest last
        // Exclude the very last incoming message if it's already in recentHistory to avoid duplicate prompt
        val historyPairs = recentHistory.reversed()
            .filterNot { it.id == incomingId || (it.body == messageBody && it.direction == "INCOMING") }
            .map { msg ->
                val role = if (msg.direction == "INCOMING") "user" else "model"
                Pair(role, msg.body)
            }

        val memoryContext = memoryRepo.getMemoryContextString()
        val basePrompt = settingsRepo.getSystemPrompt()
        val customRulePrompt = ruleResult.customAiPrompt?.let { "\n[دستور سفارشی برای این مخاطب]: $it\n" } ?: ""
        val tone = settingsRepo.getAiTone()

        // Contact name resolution via ContactHelper
        val contactName = ContactHelper.getContactName(context, sender) ?: ""
        val displayName = if (contactName.isNotBlank()) "$contactName ($sender)" else sender

        // User Personalization: Name, Bio & Current Live Status
        val userName = settingsRepo.getUserName()
        val userBio = settingsRepo.getUserBio()
        val currentStatus = settingsRepo.getCurrentStatus()
        val statusTimestamp = settingsRepo.getStatusTimestamp()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val recordedTimeStr = if (statusTimestamp > 0) timeFormat.format(Date(statusTimestamp)) else "نامشخص"
        val currentTimeStr = timeFormat.format(Date())
        val diffMinutes = if (statusTimestamp > 0) {
            ((System.currentTimeMillis() - statusTimestamp) / (60 * 1000)).coerceAtLeast(0)
        } else 0

        val delayMinutes = settingsRepo.getReplyDelayMinutes()
        val scheduledSendCal = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, delayMinutes)
        }
        val scheduledSendTimeStr = if (delayMinutes > 0) timeFormat.format(scheduledSendCal.time) else currentTimeStr

        // Step 1: Thinking Initialization - Scanning records
        updateLiveAnalysis(
            AiAnalysisState(
                isAnalyzing = true,
                sender = sender,
                contactName = contactName,
                incomingMessage = messageBody,
                analysisStep = "گام ۱: خواندن سوابق ۵۰ پیام و تحلیل لحن و روان‌شناختی مخاطب",
                historySummary = "در حال بازخوانی تاریخچه پیام‌ها، گویش، کلمات پرتکرار و لحن مخاطب...",
                personaInsight = "بررسی اطلاعات بیوگرافی و وضعیت زمانی «$userName»...",
                scheduledSendTimeStr = scheduledSendTimeStr,
                delayRemainingSeconds = delayMinutes * 60,
                timestamp = System.currentTimeMillis()
            )
        )

        // Stage 1: Generate Deep Cognitive Analysis of Contact & Context
        val analysisPrompt = buildString {
            append("وظیفه شما تحلیل روان‌شناختی، سوابق و لحن گفتگو برای پاسخ‌دهی در نقش «").append(userName).append("» است.\n")
            append("مخاطب پیامک: ").append(displayName).append("\n")
            append("اطلاعات صاحب خط:\n- نام: ").append(userName)
            if (userBio.isNotBlank()) append("\n- بیوگرافی/مشخصات: ").append(userBio)
            if (currentStatus.isNotBlank()) {
                append("\n- وضعیت فعلی: «").append(currentStatus).append("» (ثبت در ").append(recordedTimeStr).append(" - ").append(diffMinutes).append(" دقیقه قبل)")
            }
            append("\n\nپیام جدید دریافتی از مخاطب: «").append(messageBody).append("»\n")
            append("سوابق ۵۰ پیام اخیر در گفتگو پیوست شده‌اند.\n")
            append("لطفاً با دقت و به زبان فارسی در قالب ۳ بخش کوتاه تفکیک کن:\n")
            append("تاریخچه و لحن: (تحلیل کوتاه از لحن مخاطب؛ عامیانه، محلی/لری، رسمی، شوخ، یا پرخاشگر)\n")
            append("ارتباط با بیو/وضعیت: (آیا نیاز به اشاره به وضعیت یا بیو هست یا اصلاً ربطی ندارد و نباید گفته شود؟)\n")
            append("تصمیم نهایی: (استراتژی رفتار؛ مثلاً حاضر‌جوابی مؤدبانه با شوخ‌طبعی و رفاقت، بدون بی‌ادبی متقابل)")
        }

        val analysisResult = aiRepo.generateResponse(
            prompt = analysisPrompt,
            conversationContext = historyPairs.takeLast(20),
            customSystemInstruction = "شما موتور تحلیل ادراکی و فرایند تفکر (Thinking Engine) هوش مصنوعی هستید. استدلال دقیق، روان و تفکیک‌شده به زبان فارسی ارائه دهید."
        )

        val cognitiveRaw = if (analysisResult is AiResult.Success) {
            analysisResult.text.trim()
        } else {
            "تحلیل سریع انجام شد: تطبیق کامل با لحن مخاطب، حفظ متانت و صمیمیت."
        }

        // Parse cognitive parts for clear thinking UI
        var histPart = cognitiveRaw
        var personaPart = ""
        var decisionPart = ""
        if (cognitiveRaw.contains("تاریخچه و لحن:") || cognitiveRaw.contains("ارتباط با بیو/وضعیت:")) {
            val lines = cognitiveRaw.lines()
            histPart = lines.filter { it.contains("تاریخچه") || it.contains("لحن") }.joinToString("\n").ifBlank { cognitiveRaw.take(150) }
            personaPart = lines.filter { it.contains("بیو") || it.contains("وضعیت") }.joinToString("\n").ifBlank { "استفاده از وضعیت تنها در صورت ضرورت و با سنجش زمان گذشته." }
            decisionPart = lines.filter { it.contains("تصمیم") || it.contains("استراتژی") }.joinToString("\n").ifBlank { "پاسخ رفاقتی، مؤدبانه و صمیمی بدون اعلام ربات بودن." }
        }

        // Step 2: Update thinking with cognitive findings
        updateLiveAnalysis(
            AiAnalysisState(
                isAnalyzing = true,
                sender = sender,
                contactName = contactName,
                incomingMessage = messageBody,
                analysisStep = "گام ۲: تدوین متن پیامک نهایی بر اساس استدلال ادراکی",
                historySummary = histPart,
                personaInsight = personaPart,
                decisionReasoning = decisionPart,
                scheduledSendTimeStr = scheduledSendTimeStr,
                delayRemainingSeconds = delayMinutes * 60,
                timestamp = System.currentTimeMillis()
            )
        )

        // Stage 2: Compose natural human reply informed by cognitive analysis
        val fullSystemInstruction = buildString {
            append("دستورالعمل نقشی و هویتی (شما دقیقاً خودِ «").append(userName).append("» هستید):\n")
            append(basePrompt)
            append("\n\nنام صاحب خط (هویت شما): ").append(userName)
            if (contactName.isNotBlank()) {
                append("\nنام مخاطب در دفترچه تلفن شما: «").append(contactName).append("» است. در صورت نیاز می‌توانی با نام صدایش بزنی.")
            }
            if (userBio.isNotBlank()) {
                append("\nبیوگرافی و اطلاعات پس‌زمینه شما: ").append(userBio)
                append("\n[نکته مهم]: این اطلاعات صرفاً پس‌زمینه زندگی شماست. به هیچ وجه آنها را طوطی‌وار تکرار نکن! فقط و فقط اگر مخاطب صراحتاً درباره موضوعی مرتبط پرسید، مثل یک آدم معمولی در خلال حرفت اشاره کن.")
            }
            if (currentStatus.isNotBlank()) {
                append("\n\n[وضعیت و زمانبندی شما - بسیار مهم]:\n")
                append("متن وضعیت ثبت شده: «").append(currentStatus).append("»\n")
                append("ثبت شده در ساعت ").append(recordedTimeStr).append(" | الان ساعت: ").append(currentTimeStr).append(" (حدود ").append(diffMinutes).append(" دقیقه قبل)\n")
                append("قانون طلایی وضعیت: وضعیت را عینا کپی نکن! اگر به موضوع پیام مخاطب ربطی ندارد، اصلاً نگو. اگر ربط دارد یا احوالپرسی کرده، بر اساس زمان باقیمانده بازگو کن (مثلاً: «هنوز سالنم، یه ساعت دیگه کارم تموم میشه» یا «تو مسیرم»).")
            }

            append("\n\n[فرایند تفکر و تحلیل استدلالی انجام شده]:\n").append(cognitiveRaw)

            append("\n\nلحن و حس پاسخ: ").append(tone)
            append("\nاصول مهم رفتاری و گفتاری:")
            append("\n۱. شما یک انسان واقعی به نام «").append(userName).append("» هستی. هرگز و ابداً نگو ربات، هوش مصنوعی، مدل زبانی یا دستیار هستی.")
            append("\n۲. لحن طرف را بخوان: اگر با گویش یا لهجه محلی (مثلاً لری، لکی، کردی، ترکی و...)، عامیانه، یا حتی بی‌ادبی پیام داد: هرگز عصبانی نشو و هرگز بی‌ادبی نکن؛ با خنده، حاضر‌جوابی جذاب، شوخ‌طبعی و احترام صمیمانه جواب بده.")
            append("\n۳. پیام‌ها کوتاه و باورپذیر باشند؛ مثل یک اس‌ام‌اس واقعی در گوشی.")

            if (memoryContext.isNotBlank()) {
                append("\n\nاطلاعات و آموزش‌های ثبت شده قبلی:\n")
                append(memoryContext)
            }
            if (customRulePrompt.isNotBlank()) {
                append("\n").append(customRulePrompt)
            }
            if (historyPairs.isNotEmpty()) {
                append("\n\nسوابق ۵۰ پیام پیشین این گفتگو ارسال شده‌اند؛ جریان مکالمه را حفظ کن.")
            }
        }

        // 7. Request AI Response with full conversation context
        val aiResult = aiRepo.generateResponse(
            prompt = messageBody,
            conversationContext = historyPairs,
            customSystemInstruction = fullSystemInstruction
        )

        when (aiResult) {
            is AiResult.Success -> {
                val maxLen = settingsRepo.getMaxSmsLength()
                var replyText = aiResult.text.trim()
                // Strip common unwanted assistant disclaimers if generated
                replyText = replyText
                    .replace("من یک مدل زبان هستم", "")
                    .replace("به عنوان هوش مصنوعی", "")
                    .replace("من به عنوان دستیار", "")
                    .replace("من یک هوش مصنوعی هستم", "")
                    .trim()

                if (replyText.length > maxLen) {
                    replyText = replyText.take(maxLen) + "..."
                }

                // Reset flags
                sendImmediatelyFlag = false
                cancelScheduledReplyFlag = false

                // 8. Human-like Delay Mechanism (Configurable custom minutes with live countdown)
                if (delayMinutes > 0) {
                    val totalDelaySeconds = delayMinutes * 60
                    logRepo.log(
                        type = "AI",
                        title = "تحلیل ادراکی کامل شد؛ صف زمان‌بندی ارسال",
                        description = "پاسخ آماده شد و طبق زمان‌بندی مقرر در ساعت $scheduledSendTimeStr ارسال خواهد شد: «$replyText»",
                        status = "INFO",
                        relatedAddress = sender
                    )

                    for (remainingSec in totalDelaySeconds downTo 1) {
                        if (cancelScheduledReplyFlag) {
                            logRepo.log("AI", "ارسال پیامک توسط کاربر لغو شد", "پاسخ آماده ارسال به $displayName لغو گردید.", "INFO", sender)
                            clearLiveAnalysis()
                            return@withContext
                        }
                        if (sendImmediatelyFlag) {
                            logRepo.log("AI", "دستور ارسال فوری پیامک صادر شد", "ارسال بدون معطلی به $displayName", "INFO", sender)
                            break
                        }

                        updateLiveAnalysis(
                            AiAnalysisState(
                                isAnalyzing = true,
                                sender = sender,
                                contactName = contactName,
                                incomingMessage = messageBody,
                                analysisStep = "گام ۳: تحلیل و تدوین کامل شد - آماده ارسال در موعد مقرر",
                                historySummary = histPart,
                                personaInsight = personaPart,
                                decisionReasoning = decisionPart,
                                scheduledSendTimeStr = scheduledSendTimeStr,
                                delayRemainingSeconds = remainingSec,
                                plannedReply = replyText,
                                isReadyToSend = true,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        delay(1000L)
                    }
                }

                // Send SMS to recipient
                sendAndRecordReply(
                    recipient = sender,
                    replyText = replyText,
                    isAi = true,
                    ruleName = matchedRuleName,
                    keyName = aiResult.keyName,
                    targetSubId = selectedSimId
                )

                // Clear live analysis state
                clearLiveAnalysis()
            }
            is AiResult.Error -> {
                clearLiveAnalysis()
                logRepo.log(
                    type = "ERROR",
                    title = "خطای هوش مصنوعی در پاسخ به $sender",
                    description = aiResult.message,
                    status = "FAILED",
                    relatedAddress = sender
                )
                notificationHelper.showErrorNotification("خطا در پاسخ هوشمند", aiResult.message)
            }
        }
    }

    private suspend fun sendAndRecordReply(
        recipient: String,
        replyText: String,
        isAi: Boolean,
        ruleName: String?,
        keyName: String? = null,
        targetSubId: Int? = null
    ) {
        val sendResult = smsSender.sendSms(recipient, replyText, targetSubId)
        val status = when (sendResult) {
            is SmsSendResult.Success -> "SENT"
            is SmsSendResult.Failure -> "FAILED"
        }

        smsRepo.insertMessage(
            SmsMessageEntity(
                address = recipient,
                body = replyText,
                timestamp = System.currentTimeMillis(),
                direction = "OUTGOING",
                isAiReply = isAi,
                status = status,
                ruleAppliedName = ruleName
            )
        )

        if (sendResult is SmsSendResult.Success) {
            val keyInfo = keyName?.let { " با کلید $it" } ?: ""
            logRepo.log(
                type = if (isAi) "AI" else "SMS",
                title = if (isAi) "پاسخ هوشمند ارسال شد" else "پیامک ارسال شد",
                description = "به $recipient: $replyText$keyInfo",
                status = "SUCCESS",
                relatedAddress = recipient
            )
            notificationHelper.showAiReplySentNotification(recipient, replyText)
        } else if (sendResult is SmsSendResult.Failure) {
            logRepo.log(
                type = "ERROR",
                title = "خطا در ارسال پیامک به $recipient",
                description = sendResult.reason,
                status = "FAILED",
                relatedAddress = recipient
            )
            notificationHelper.showErrorNotification("خطا در ارسال پیامک", sendResult.reason)
        }
    }
}
