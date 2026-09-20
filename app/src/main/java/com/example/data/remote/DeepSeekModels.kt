package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeepSeekRequest(
    @Json(name = "model") val model: String = "deepseek-flash",
    @Json(name = "messages") val messages: List<DeepSeekMessage>,
    @Json(name = "temperature") val temperature: Float = 0.7f,
    @Json(name = "max_tokens") val maxTokens: Int = 400,
    @Json(name = "stream") val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DeepSeekMessage(
    @Json(name = "role") val role: String, // "system", "user", "assistant"
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class DeepSeekResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<DeepSeekChoice>? = null,
    @Json(name = "error") val error: DeepSeekErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class DeepSeekChoice(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "message") val message: DeepSeekMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class DeepSeekErrorDetail(
    @Json(name = "message") val message: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "code") val code: String? = null
)
