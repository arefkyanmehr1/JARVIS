package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.ApiKeyDao
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.remote.AimlClient
import com.example.data.remote.DeepSeekClient
import com.example.data.remote.DeepSeekMessage
import com.example.data.remote.DeepSeekRequest
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiConfig
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRelayBridge
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GeminiResponse
import com.example.data.remote.OpenAiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AiResult {
    data class Success(
        val text: String,
        val keyName: String,
        val latencyMs: Long,
        val provider: String
    ) : AiResult()

    data class Error(
        val message: String,
        val rawError: String,
        val code: Int? = null
    ) : AiResult()
}

class AiRepository(
    private val apiKeyDao: ApiKeyDao,
    private val logDao: ActivityLogDao,
    private val settingsRepo: SettingsRepository
) {

    // Default pre-configured keys for out-of-the-box reliability
    private val defaultOpenAiKey = ""
    private val defaultAimlKey = ""

    private val fallbackDeepSeekKeys = emptyList<String>()

    /**
     * Executes generation with automatic API key rotation & failover.
     * Supports OpenAI/ChatGPT (gpt-4o-mini, gpt-5.6-luna), AIMLAPI (openai/gpt-5-5),
     * Gemini (gemini-3.8-flash, gemini-3.5-flash-lite, gemini-2.5-flash-lite) and DeepSeek (deepseek-chat).
     */
    suspend fun generateResponse(
        prompt: String,
        conversationContext: List<Pair<String, String>> = emptyList(),
        customSystemInstruction: String? = null
    ): AiResult = withContext(Dispatchers.IO) {
        val activeKeys = apiKeyDao.getActiveKeys().toMutableList()

        // Fallback to preconfigured keys if database has no active keys
        if (activeKeys.isEmpty()) {
            activeKeys.add(
                ApiKeyEntity(
                    id = -1,
                    name = "ع©ظ„غŒط¯ ط§طµظ„غŒ ChatGPT (OpenAI)",
                    apiKey = defaultOpenAiKey,
                    maskedKey = maskKey(defaultOpenAiKey),
                    provider = "OPENAI",
                    isEnabled = true,
                    priority = 10
                )
            )
            activeKeys.add(
                ApiKeyEntity(
                    id = -2,
                    name = "ع©ظ„غŒط¯ AIML API (ط³ط±ظˆغŒط³ ظ¾ط±ظˆع©ط³غŒ ع†ظ†ط¯ظ…ط¯ظ„غŒ)",
                    apiKey = defaultAimlKey,
                    maskedKey = maskKey(defaultAimlKey),
                    provider = "AIML",
                    isEnabled = true,
                    priority = 9
                )
            )
            fallbackDeepSeekKeys.forEachIndexed { index, key ->
                activeKeys.add(
                    ApiKeyEntity(
                        id = -(index + 10).toLong(),
                        name = "ع©ظ„غŒط¯ DeepSeek ${index + 1}",
                        apiKey = key,
                        maskedKey = maskKey(key),
                        provider = "DEEPSEEK",
                        isEnabled = true,
                        priority = 5 - index
                    )
                )
            }

            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                activeKeys.add(
                    ApiKeyEntity(
                        id = -99,
                        name = "ع©ظ„غŒط¯ Gemini ط³غŒط³طھظ… (BuildConfig)",
                        apiKey = buildConfigKey,
                        maskedKey = maskKey(buildConfigKey),
                        provider = "GEMINI",
                        isEnabled = true,
                        priority = 4
                    )
                )
            }
        }

        if (activeKeys.isEmpty()) {
            return@withContext AiResult.Error(
                message = "ظ‡غŒع† ع©ظ„غŒط¯ API ظپط¹ط§ظ„غŒ ط¯ط± ط³غŒط³طھظ… طھط¹ط±غŒظپ ظ†ط´ط¯ظ‡ ط§ط³طھ.",
                rawError = "No API keys configured"
            )
        }

        val configuredModel = settingsRepo.getAiModel()
        val systemInstruction = customSystemInstruction ?: settingsRepo.getSystemPrompt()
        val maxTokens = settingsRepo.getMaxSmsLength() + 100

        var lastErrorMessage = "ط®ط·ط§غŒ ظ†ط§ط´ظ†ط§ط®طھظ‡"
        var lastErrorCode: Int? = null

        // Sequential failover loop over active keys
        for (keyEntity in activeKeys) {
            val isOpenAi = keyEntity.isOpenAi()
            val isAiml = keyEntity.isAiml()
            val isGemini = keyEntity.isGemini()
            val isDeepSeek = keyEntity.isDeepSeek()

            when {
                isOpenAi -> {
                    // OpenAI / ChatGPT Endpoint
                    val targetModel = when {
                        configuredModel.startsWith("gpt", ignoreCase = true) -> configuredModel
                        configuredModel.contains("luna", ignoreCase = true) -> "gpt-5.6-luna"
                        else -> "gpt-4o-mini" // Default low-cost, lightning-fast
                    }

                    val messages = mutableListOf<DeepSeekMessage>()
                    if (systemInstruction.isNotBlank()) {
                        messages.add(DeepSeekMessage(role = "system", content = systemInstruction))
                    }
                    for ((role, text) in conversationContext) {
                        val dsRole = if (role.equals("USER", ignoreCase = true)) "user" else "assistant"
                        messages.add(DeepSeekMessage(role = dsRole, content = text))
                    }
                    messages.add(DeepSeekMessage(role = "user", content = prompt))

                    val request = DeepSeekRequest(
                        model = targetModel,
                        messages = messages,
                        temperature = 0.7f,
                        maxTokens = maxTokens,
                        stream = false
                    )

                    try {
                        val callStart = System.currentTimeMillis()
                        val authHeader = if (keyEntity.apiKey.startsWith("Bearer ", ignoreCase = true)) {
                            keyEntity.apiKey
                        } else {
                            "Bearer ${keyEntity.apiKey}"
                        }

                        val response = OpenAiClient.apiService.createChatCompletion(
                            authorization = authHeader,
                            request = request
                        )

                        if (response.isSuccessful) {
                            val choiceText = response.body()?.choices?.firstOrNull()?.message?.content
                            if (!choiceText.isNullOrBlank()) {
                                val latency = System.currentTimeMillis() - callStart
                                if (keyEntity.id > 0) apiKeyDao.recordSuccess(keyEntity.id)
                                logDao.insertLog(
                                    ActivityLogEntity(
                                        type = "AI",
                                        title = "ظ¾ط§ط³ط® ط§ط² OpenAI ($targetModel)",
                                        description = "ظ¾ط§ط³ط® ظ‡ظˆط´ظ…ظ†ط¯ ط¨ط§ ع©ظ„غŒط¯ ${keyEntity.name} ط¯ط± $latency ظ…غŒظ„غŒâ€Œط«ط§ظ†غŒظ‡ ط¯ط±غŒط§ظپطھ ط´ط¯.",
                                        status = "SUCCESS"
                                    )
                                )
                                return@withContext AiResult.Success(
                                    text = choiceText.trim(),
                                    keyName = keyEntity.name,
                                    latencyMs = latency,
                                    provider = "OPENAI"
                                )
                            }
                        } else {
                            val code = response.code()
                            lastErrorCode = code
                            val errorBody = response.errorBody()?.string()
                            val friendlyMsg = OpenAiClient.parseHttpError(code, errorBody)
                            lastErrorMessage = friendlyMsg

                            if (keyEntity.id > 0) {
                                if (code == 429) apiKeyDao.recordRateLimit(keyEntity.id, friendlyMsg)
                                else apiKeyDao.recordError(keyEntity.id, friendlyMsg)
                            }
                            logDao.insertLog(
                                ActivityLogEntity(
                                    type = "API",
                                    title = "ط®ط·ط§غŒ OpenAI (${keyEntity.name} - ع©ط¯ $code)",
                                    description = friendlyMsg,
                                    status = "WARNING"
                                )
                            )
                            continue
                        }
                    } catch (e: Exception) {
                        val friendlyEx = OpenAiClient.parseException(e)
                        lastErrorMessage = friendlyEx
                        if (keyEntity.id > 0) apiKeyDao.recordError(keyEntity.id, friendlyEx)
                        logDao.insertLog(
                            ActivityLogEntity(
                                type = "API",
                                title = "ط§ط³طھط«ظ†ط§ ط¯ط± ط§ط±طھط¨ط§ط· ط¨ط§ OpenAI (${keyEntity.name})",
                                description = friendlyEx,
                                status = "FAILED"
                            )
                        )
                        continue
                    }
                }

                isAiml -> {
                    // AIML API Endpoint (OpenAI schema, models like openai/gpt-5-5, gpt-4o-mini, etc.)
                    val targetModel = when {
                        configuredModel.contains("/") -> configuredModel
                        configuredModel.startsWith("gpt", ignoreCase = true) -> "openai/$configuredModel"
                        else -> "openai/gpt-5-5"
                    }

                    val messages = mutableListOf<DeepSeekMessage>()
                    if (systemInstruction.isNotBlank()) {
                        messages.add(DeepSeekMessage(role = "system", content = systemInstruction))
                    }
                    for ((role, text) in conversationContext) {
                        val dsRole = if (role.equals("USER", ignoreCase = true)) "user" else "assistant"
                        messages.add(DeepSeekMessage(role = dsRole, content = text))
                    }
                    messages.add(DeepSeekMessage(role = "user", content = prompt))

                    val request = DeepSeekRequest(
                        model = targetModel,
                        messages = messages,
                        temperature = 0.7f,
                        maxTokens = maxTokens,
                        stream = false
                    )

                    try {
                        val callStart = System.currentTimeMillis()
                        val authHeader = if (keyEntity.apiKey.startsWith("Bearer ", ignoreCase = true)) {
                            keyEntity.apiKey
                        } else {
                            "Bearer ${keyEntity.apiKey}"
                        }

                        val response = AimlClient.apiService.createChatCompletion(
                            authorization = authHeader,
                            request = request
                        )

                        if (response.isSuccessful) {
                            val choiceText = response.body()?.choices?.firstOrNull()?.message?.content
                            if (!choiceText.isNullOrBlank()) {
                                val latency = System.currentTimeMillis() - callStart
                                if (keyEntity.id > 0) apiKeyDao.recordSuccess(keyEntity.id)
                                logDao.insertLog(
                                    ActivityLogEntity(
                                        type = "AI",
                                        title = "ظ¾ط§ط³ط® ط§ط² AIML API ($targetModel)",
                                        description = "ظ¾ط§ط³ط® ط¨ط§ ع©ظ„غŒط¯ ${keyEntity.name} ط¯ط± $latency ظ…غŒظ„غŒâ€Œط«ط§ظ†غŒظ‡ ط¯ط±غŒط§ظپطھ ط´ط¯.",
                                        status = "SUCCESS"
                                    )
                                )
                                return@withContext AiResult.Success(
                                    text = choiceText.trim(),
                                    keyName = keyEntity.name,
                                    latencyMs = latency,
                                    provider = "AIML"
                                )
                            }
                        } else {
                            val code = response.code()
                            lastErrorCode = code
                            val errorBody = response.errorBody()?.string()
                            val friendlyMsg = AimlClient.parseHttpError(code, errorBody)
                            lastErrorMessage = friendlyMsg
                            if (keyEntity.id > 0) {
                                if (code == 429) apiKeyDao.recordRateLimit(keyEntity.id, friendlyMsg)
                                else apiKeyDao.recordError(keyEntity.id, friendlyMsg)
                            }
                            continue
                        }
                    } catch (e: Exception) {
                        val friendlyEx = AimlClient.parseException(e)
                        lastErrorMessage = friendlyEx
                        if (keyEntity.id > 0) apiKeyDao.recordError(keyEntity.id, friendlyEx)
                        continue
                    }
                }

                isGemini -> {
                    // Google Gemini API (official models: gemini-flash-lite-latest, gemini-flash-latest, gemini-3.5-flash-lite)
                    val preferredModel = when {
                        configuredModel.startsWith("gemini", ignoreCase = true) -> configuredModel.removePrefix("models/").trim()
                        else -> "gemini-flash-lite-latest"
                    }
                    val candidateModels = linkedSetOf(
                        preferredModel,
                        "gemini-flash-lite-latest",
                        "gemini-flash-latest",
                        "gemini-3.5-flash-lite",
                        "gemini-3.1-flash-lite-preview"
                    ).toList()

                    val contentList = mutableListOf<GeminiContent>()
                    for ((role, text) in conversationContext) {
                        val apiRole = if (role.equals("USER", ignoreCase = true)) "user" else "model"
                        contentList.add(GeminiContent(role = apiRole, parts = listOf(GeminiPart(text = text))))
                    }
                    contentList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt))))

                    val request = GeminiRequest(
                        contents = contentList,
                        systemInstruction = if (systemInstruction.isNotBlank()) GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))) else null,
                        generationConfig = GeminiConfig(
                            temperature = 0.7f,
                            maxOutputTokens = maxTokens
                        )
                    )

                    val cleanKey = keyEntity.apiKey.removeSurrounding("\"").removeSurrounding("'").removePrefix("Bearer ").trim()
                    val useRelay = settingsRepo.isGeminiRelayEnabled()
                    val relayUrl = settingsRepo.getGeminiRelayUrl()
                    var succeeded = false

                    for (targetModel in candidateModels) {
                        try {
                            val callStart = System.currentTimeMillis()

                            // If relay is enabled (default), send through user's host bridge to bypass Iranian filtering
                            val geminiResponse: GeminiResponse? = if (useRelay && relayUrl.isNotBlank()) {
                                val relayRes = GeminiRelayBridge.generateContentViaRelay(
                                    relayUrl = relayUrl,
                                    model = targetModel,
                                    apiKey = cleanKey,
                                    geminiRequest = request
                                )
                                if (relayRes.isSuccess) {
                                    relayRes.getOrNull()
                                } else {
                                    // Fallback to direct call if relay errored
                                    val err = relayRes.exceptionOrNull()?.message ?: "ط®ط·ط§غŒ ظ†ط§ط´ظ†ط§ط®طھظ‡ ط¯ط± ط±ظ„ظ‡"
                                    lastErrorMessage = err
                                    val directResponse = GeminiClient.apiService.generateContent(
                                        model = targetModel,
                                        apiKey = cleanKey,
                                        request = request
                                    )
                                    if (directResponse.isSuccessful) directResponse.body() else null
                                }
                            } else {
                                val directResponse = GeminiClient.apiService.generateContent(
                                    model = targetModel,
                                    apiKey = cleanKey,
                                    request = request
                                )
                                if (directResponse.isSuccessful) {
                                    directResponse.body()
                                } else {
                                    val code = directResponse.code()
                                    lastErrorCode = code
                                    lastErrorMessage = GeminiClient.parseHttpError(code, directResponse.errorBody()?.string())
                                    null
                                }
                            }

                            val candidateText = geminiResponse?.candidates
                                ?.firstOrNull()
                                ?.content
                                ?.parts
                                ?.firstOrNull()
                                ?.text

                            if (!candidateText.isNullOrBlank()) {
                                val latency = System.currentTimeMillis() - callStart
                                if (keyEntity.id > 0) apiKeyDao.recordSuccess(keyEntity.id)
                                val bridgeInfo = if (useRelay && relayUrl.isNotBlank()) "ط§ط² ط·ط±غŒظ‚ ط±ظ„ظ‡ ظ‡ط§ط³طھ ط¨ط¯ظˆظ† ظپغŒظ„طھط±ط´ع©ظ†" else "ظ…ط³طھظ‚غŒظ…"
                                logDao.insertLog(
                                    ActivityLogEntity(
                                        type = "AI",
                                        title = "طھظˆظ„غŒط¯ ظ¾ط§ط³ط® ط¨ط§ Gemini ($targetModel)",
                                        description = "ظ¾ط§ط³ط® ط¨ط§ ظ…ط¯ظ„ $targetModel ظˆ ع©ظ„غŒط¯ ${keyEntity.name} ($bridgeInfo) ط¯ط± $latency ظ…غŒظ„غŒâ€Œط«ط§ظ†غŒظ‡ ط¯ط±غŒط§ظپطھ ط´ط¯.",
                                        status = "SUCCESS"
                                    )
                                )
                                succeeded = true
                                return@withContext AiResult.Success(
                                    text = candidateText.trim(),
                                    keyName = keyEntity.name,
                                    latencyMs = latency,
                                    provider = "GEMINI"
                                )
                            } else {
                                if (keyEntity.id > 0) {
                                    apiKeyDao.recordError(keyEntity.id, lastErrorMessage)
                                }
                                continue
                            }
                        } catch (e: Exception) {
                            val friendlyEx = GeminiClient.parseException(e)
                            lastErrorMessage = friendlyEx
                            if (keyEntity.id > 0) apiKeyDao.recordError(keyEntity.id, friendlyEx)
                            break
                        }
                    }
                    if (!succeeded) {
                        continue
                    }
                }

                else -> {
                    // DeepSeek (Official model name: deepseek-chat)
                    val targetModel = when {
                        configuredModel.startsWith("deepseek", ignoreCase = true) && !configuredModel.contains("flash") -> configuredModel
                        else -> "deepseek-chat" // Fix 404 issue: standard official endpoint name
                    }

                    val messages = mutableListOf<DeepSeekMessage>()
                    if (systemInstruction.isNotBlank()) {
                        messages.add(DeepSeekMessage(role = "system", content = systemInstruction))
                    }
                    for ((role, text) in conversationContext) {
                        val dsRole = if (role.equals("USER", ignoreCase = true)) "user" else "assistant"
                        messages.add(DeepSeekMessage(role = dsRole, content = text))
                    }
                    messages.add(DeepSeekMessage(role = "user", content = prompt))

                    val request = DeepSeekRequest(
                        model = targetModel,
                        messages = messages,
                        temperature = 0.7f,
                        maxTokens = maxTokens,
                        stream = false
                    )

                    try {
                        val callStart = System.currentTimeMillis()
                        val authHeader = if (keyEntity.apiKey.startsWith("Bearer ", ignoreCase = true)) {
                            keyEntity.apiKey
                        } else {
                            "Bearer ${keyEntity.apiKey}"
                        }

                        val response = DeepSeekClient.apiService.createChatCompletion(
                            authorization = authHeader,
                            request = request
                        )

                        if (response.isSuccessful) {
                            val choiceText = response.body()?.choices?.firstOrNull()?.message?.content
                            if (!choiceText.isNullOrBlank()) {
                                val latency = System.currentTimeMillis() - callStart
                                if (keyEntity.id > 0) apiKeyDao.recordSuccess(keyEntity.id)
                                logDao.insertLog(
                                    ActivityLogEntity(
                                        type = "AI",
                                        title = "طھظˆظ„غŒط¯ ظ¾ط§ط³ط® ط¨ط§ DeepSeek ($targetModel)",
                                        description = "ظ¾ط§ط³ط® ط¨ط§ ظ…ط¯ظ„ $targetModel ظˆ ع©ظ„غŒط¯ ${keyEntity.name} ط¯ط± $latency ظ…غŒظ„غŒâ€Œط«ط§ظ†غŒظ‡ ط¯ط±غŒط§ظپطھ ط´ط¯.",
                                        status = "SUCCESS"
                                    )
                                )
                                return@withContext AiResult.Success(
                                    text = choiceText.trim(),
                                    keyName = keyEntity.name,
                                    latencyMs = latency,
                                    provider = "DEEPSEEK"
                                )
                            }
                        } else {
                            val code = response.code()
                            lastErrorCode = code
                            val errorBody = response.errorBody()?.string()
                            val friendlyMsg = DeepSeekClient.parseHttpError(code, errorBody)
                            lastErrorMessage = friendlyMsg
                            if (keyEntity.id > 0) {
                                if (code == 429) apiKeyDao.recordRateLimit(keyEntity.id, friendlyMsg)
                                else apiKeyDao.recordError(keyEntity.id, friendlyMsg)
                            }
                            continue
                        }
                    } catch (e: Exception) {
                        val friendlyEx = DeepSeekClient.parseException(e)
                        lastErrorMessage = friendlyEx
                        if (keyEntity.id > 0) apiKeyDao.recordError(keyEntity.id, friendlyEx)
                        continue
                    }
                }
            }
        }

        return@withContext AiResult.Error(
            message = lastErrorMessage,
            rawError = "All available API keys failed",
            code = lastErrorCode
        )
    }

    suspend fun testApiKey(
        apiKey: String,
        model: String? = null,
        provider: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        val resolvedProvider = when {
            provider.equals("OPENAI", ignoreCase = true) || trimmedKey.startsWith("sk-proj-") -> "OPENAI"
            provider.equals("AIML", ignoreCase = true) || (trimmedKey.length == 32 && !trimmedKey.startsWith("sk-")) -> "AIML"
            provider.equals("GEMINI", ignoreCase = true) || trimmedKey.startsWith("AIza") -> "GEMINI"
            else -> "DEEPSEEK"
        }

        when (resolvedProvider) {
            "OPENAI" -> {
                val targetModel = if (!model.isNullOrBlank() && model.startsWith("gpt", ignoreCase = true)) model else "gpt-4o-mini"
                try {
                    val authHeader = if (trimmedKey.startsWith("Bearer ", ignoreCase = true)) trimmedKey else "Bearer $trimmedKey"
                    val request = DeepSeekRequest(
                        model = targetModel,
                        messages = listOf(DeepSeekMessage(role = "user", content = "ط³ظ„ط§ظ…! ط¯ط± غŒع© ع©ظ„ظ…ظ‡ ظ¾ط§ط³ط® ط¨ط¯ظ‡: ط¢ظ…ط§ط¯ظ‡")),
                        maxTokens = 25
                    )
                    val response = OpenAiClient.apiService.createChatCompletion(authHeader, request)
                    if (response.isSuccessful) {
                        val text = response.body()?.choices?.firstOrNull()?.message?.content
                        if (!text.isNullOrBlank()) Result.success(text.trim())
                        else Result.failure(Exception("ظ¾ط§ط³ط® ط¯ط±غŒط§ظپطھغŒ ط®ط§ظ„غŒ ط¨ظˆط¯."))
                    } else {
                        Result.failure(Exception(OpenAiClient.parseHttpError(response.code(), response.errorBody()?.string())))
                    }
                } catch (e: Exception) {
                    Result.failure(Exception(OpenAiClient.parseException(e)))
                }
            }

            "AIML" -> {
                val targetModel = if (!model.isNullOrBlank()) model else "openai/gpt-5-5"
                try {
                    val authHeader = if (trimmedKey.startsWith("Bearer ", ignoreCase = true)) trimmedKey else "Bearer $trimmedKey"
                    val request = DeepSeekRequest(
                        model = targetModel,
                        messages = listOf(DeepSeekMessage(role = "user", content = "ظ¾ط§ط³ط® غŒع© ع©ظ„ظ…ظ‡â€Œط§غŒ: ط¢ظ…ط§ط¯ظ‡")),
                        maxTokens = 25
                    )
                    val response = AimlClient.apiService.createChatCompletion(authHeader, request)
                    if (response.isSuccessful) {
                        val text = response.body()?.choices?.firstOrNull()?.message?.content
                        if (!text.isNullOrBlank()) Result.success(text.trim())
                        else Result.failure(Exception("ظ¾ط§ط³ط® ط¯ط±غŒط§ظپطھغŒ ط®ط§ظ„غŒ ط¨ظˆط¯."))
                    } else {
                        Result.failure(Exception(AimlClient.parseHttpError(response.code(), response.errorBody()?.string())))
                    }
                } catch (e: Exception) {
                    Result.failure(Exception(AimlClient.parseException(e)))
                }
            }

            "GEMINI" -> {
                val cleanKey = trimmedKey.removeSurrounding("\"").removeSurrounding("'").removePrefix("Bearer ").trim()
                val useRelay = settingsRepo.isGeminiRelayEnabled()
                val relayUrl = settingsRepo.getGeminiRelayUrl()

                val preferredModel = if (!model.isNullOrBlank() && model.startsWith("gemini", ignoreCase = true)) {
                    model.removePrefix("models/").trim()
                } else {
                    "gemini-flash-lite-latest"
                }
                val candidateModels = linkedSetOf(
                    preferredModel,
                    "gemini-flash-lite-latest",
                    "gemini-flash-latest",
                    "gemini-3.5-flash-lite",
                    "gemini-3.1-flash-lite-preview"
                ).toList()

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "ظ¾ط§ط³ط® ع©ظˆطھط§ظ‡ طھع©â€Œع©ظ„ظ…ظ‡â€Œط§غŒ ط¨ط¯ظ‡: ط¢ظ†ظ„ط§غŒظ†")))),
                    generationConfig = GeminiConfig(maxOutputTokens = 20)
                )

                var lastException: Exception? = null
                var successfulModel: String? = null
                var responseText: String? = null
                var viaRelaySuccess = false

                for (targetModel in candidateModels) {
                    try {
                        if (useRelay && relayUrl.isNotBlank()) {
                            val relayResult = GeminiRelayBridge.generateContentViaRelay(
                                relayUrl = relayUrl,
                                model = targetModel,
                                apiKey = cleanKey,
                                geminiRequest = request
                            )
                            if (relayResult.isSuccess) {
                                val text = relayResult.getOrNull()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                if (!text.isNullOrBlank()) {
                                    successfulModel = targetModel
                                    responseText = text.trim()
                                    viaRelaySuccess = true
                                    break
                                }
                            } else {
                                lastException = Exception(relayResult.exceptionOrNull()?.message)
                            }
                        }

                        // Direct fallback or primary if relay disabled
                        val response = GeminiClient.apiService.generateContent(targetModel, cleanKey, request)
                        if (response.isSuccessful) {
                            val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!text.isNullOrBlank()) {
                                successfulModel = targetModel
                                responseText = text.trim()
                                break
                            }
                        } else {
                            val code = response.code()
                            val errStr = GeminiClient.parseHttpError(code, response.errorBody()?.string())
                            lastException = Exception(errStr)
                            if (code == 404 || code == 503) {
                                continue
                            } else {
                                break
                            }
                        }
                    } catch (e: Exception) {
                        lastException = Exception(GeminiClient.parseException(e))
                        break
                    }
                }

                if (!responseText.isNullOrBlank()) {
                    val viaStr = if (viaRelaySuccess) "ط¨ط¯ظˆظ† ظپغŒظ„طھط±ط´ع©ظ† ط§ط² ط·ط±غŒظ‚ ط±ظ„ظ‡ ظ‡ط§ط³طھ" else "ظ…ط³طھظ‚غŒظ…"
                    Result.success("ط¢ظ…ط§ط¯ظ‡ ظ¾ط§ط³ط®ع¯ظˆغŒغŒ ($viaStr - ظ…ط¯ظ„: $successfulModel) -> $responseText")
                } else {
                    Result.failure(lastException ?: Exception("ط¹ط¯ظ… ط¨ط±ظ‚ط±ط§ط±غŒ ط§طھطµط§ظ„ ط¨ظ‡ ط³ط±ظˆط± Gemini"))
                }
            }

            else -> {
                // DEEPSEEK
                val targetModel = if (!model.isNullOrBlank() && !model.contains("flash")) model else "deepseek-chat"
                try {
                    val authHeader = if (trimmedKey.startsWith("Bearer ", ignoreCase = true)) trimmedKey else "Bearer $trimmedKey"
                    val request = DeepSeekRequest(
                        model = targetModel,
                        messages = listOf(DeepSeekMessage(role = "user", content = "ظ¾ط§ط³ط® ع©ظˆطھط§ظ‡ طھع©â€Œع©ظ„ظ…ظ‡â€Œط§غŒ ط¨ط¯ظ‡: ط¢ظ†ظ„ط§غŒظ†")),
                        maxTokens = 25
                    )
                    val response = DeepSeekClient.apiService.createChatCompletion(authHeader, request)
                    if (response.isSuccessful) {
                        val text = response.body()?.choices?.firstOrNull()?.message?.content
                        if (!text.isNullOrBlank()) Result.success(text.trim())
                        else Result.failure(Exception("ظ¾ط§ط³ط® ط¯ط±غŒط§ظپطھغŒ ط§ط² ط³ط±ظˆط± DeepSeek ط®ط§ظ„غŒ ط¨ظˆط¯."))
                    } else {
                        Result.failure(Exception(DeepSeekClient.parseHttpError(response.code(), response.errorBody()?.string())))
                    }
                } catch (e: Exception) {
                    Result.failure(Exception(DeepSeekClient.parseException(e)))
                }
            }
        }
    }

    companion object {
        fun maskKey(key: String): String {
            if (key.length <= 8) return "****"
            return key.take(6) + "..." + key.takeLast(4)
        }
    }
}

