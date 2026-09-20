package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class RelayBridgeRequest(
    @Json(name = "url") val url: String,
    @Json(name = "method") val method: String = "POST",
    @Json(name = "headers") val headers: Map<String, String> = mapOf("Content-Type" to "application/json"),
    @Json(name = "payload") val payload: Any? = null
)

/**
 * JARVIS Gemini Relay Bridge Client
 * Forwards Gemini requests through the user's host relay (https://arefkyanmehr.ir/gemini.php)
 * and Google Apps Script to bypass Iranian internet filtering & Google IP regional sanctions
 * without requiring any VPN or proxy on the device.
 */
object GeminiRelayBridge {
    const val DEFAULT_RELAY_URL = "https://arefkyanmehr.ir/gemini.php"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Checks if the relay server is online by performing a simple GET request.
     */
    suspend fun checkHealth(relayUrl: String = DEFAULT_RELAY_URL): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(relayUrl)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success("رله فعال و آماده است ($relayUrl)")
            } else {
                Result.failure(Exception("پاسخ ناموفق رله با کد ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseException(e)))
        }
    }

    /**
     * Executes a Gemini API generateContent call through the relay bridge.
     */
    suspend fun generateContentViaRelay(
        relayUrl: String,
        model: String,
        apiKey: String,
        geminiRequest: GeminiRequest
    ): Result<GeminiResponse> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val requestAdapter = moshi.adapter(GeminiRequest::class.java)
            val geminiJsonString = requestAdapter.toJson(geminiRequest)
            val payloadMap = moshi.adapter(Map::class.java).fromJson(geminiJsonString)

            val relayBodyObj = RelayBridgeRequest(
                url = targetUrl,
                method = "POST",
                headers = mapOf(
                    "Content-Type" to "application/json"
                ),
                payload = payloadMap
            )

            val relayAdapter = moshi.adapter(RelayBridgeRequest::class.java)
            val jsonPayload = relayAdapter.toJson(relayBodyObj)

            val httpRequest = Request.Builder()
                .url(relayUrl.ifBlank { DEFAULT_RELAY_URL })
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(httpRequest).execute()
            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val geminiResponse = try {
                    responseAdapter.fromJson(responseBody)
                } catch (_: Exception) {
                    null
                }

                if (geminiResponse != null && geminiResponse.candidates?.isNotEmpty() == true) {
                    Result.success(geminiResponse)
                } else if (geminiResponse?.error != null) {
                    Result.failure(Exception(geminiResponse.error.message ?: "خطای ناشناخته در پاسخ گوگل"))
                } else {
                    // Try parsing as a raw string error or fallback
                    Result.failure(Exception("پاسخ نامعتبر از رله: $responseBody"))
                }
            } else {
                val parsedError = GeminiClient.parseHttpError(responseCode, responseBody)
                Result.failure(Exception("خطای رله واسط (کد $responseCode): $parsedError"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseException(e)))
        }
    }

    fun parseException(exception: Throwable): String {
        return when (exception) {
            is UnknownHostException -> "اتصال اینترنت برقرار نیست یا آدرس هاست رله در دسترس نیست."
            is SocketTimeoutException -> "اتصال به هاست رله بیش از حد طول کشید (Timeout)."
            is IOException -> "خطای شبکه در ارتباط با هاست: ${exception.localizedMessage ?: "عدم پاسخگویی"}"
            else -> "خطای ارتباط با رله: ${exception.localizedMessage ?: "ناشناخته"}"
        }
    }
}
