package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun parseHttpError(statusCode: Int, rawBody: String?): String {
        val serverMessage = try {
            if (!rawBody.isNullOrBlank()) {
                val adapter = moshi.adapter(GeminiResponse::class.java)
                adapter.fromJson(rawBody)?.error?.message ?: run {
                    // Fallback simple extract for "message": "..."
                    val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(rawBody)
                    match?.groupValues?.getOrNull(1)
                }
            } else null
        } catch (_: Exception) {
            null
        }

        return when {
            serverMessage?.contains("location", ignoreCase = true) == true ||
            serverMessage?.contains("not supported", ignoreCase = true) == true ||
            rawBody?.contains("User location is not supported", ignoreCase = true) == true -> {
                "خطای محدودیت منطقه جغرافیایی (User location not supported): گوگل دسترسی از لوکیشن IP فعلی را محدود کرده است. لطفاً فیلترشکن خود را فعال کرده و لوکیشن آن را روی کشور دیگری (مانند آلمان یا آمریکا) تنظیم نمایید."
            }

            serverMessage?.contains("API key not valid", ignoreCase = true) == true ||
            rawBody?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
            statusCode == 401 -> {
                "کلید Gemini نامعتبر است (API Key not valid). لطفاً کلید صحیح خود را از پنل Google AI Studio کپی کرده و مجدداً ثبت نمایید."
            }

            serverMessage?.contains("high demand", ignoreCase = true) == true || statusCode == 503 -> {
                "سرورهای این مدل گوگل موقتاً با ترافیک بالا مواجه هستند (High Demand). سیستم به مدل‌های دیگر سوئیچ خواهد کرد."
            }

            statusCode == 429 || serverMessage?.contains("quota", ignoreCase = true) == true -> {
                "محدودیت تعداد درخواست یا سقف استفاده رایگان (Quota Limit 429) روی این کلید به پایان رسیده است."
            }

            statusCode == 404 || serverMessage?.contains("not found", ignoreCase = true) == true -> {
                "مدل انتخابی در API گوگل یافت نشد یا منقضی شده است (کد ۴۰۴)."
            }

            statusCode == 400 -> {
                if (!serverMessage.isNullOrBlank()) "درخواست به Gemini نامعتبر است: $serverMessage"
                else "درخواست ارسال شده به هوش مصنوعی معتبر نیست (کد ۴۰۰)."
            }

            statusCode == 403 -> {
                if (!serverMessage.isNullOrBlank()) "دسترسی به Gemini مسدود شد (کد ۴۰۳): $serverMessage"
                else "اتصال به Gemini توسط سرور رد شد (کد ۴۰۳). اتصال اینترنت/VPN و دسترسی کلید را بررسی فرمایید."
            }

            !serverMessage.isNullOrBlank() -> {
                "خطای Gemini (کد $statusCode): $serverMessage"
            }

            else -> "خطای برقراری ارتباط با هوش مصنوعی گوگل (کد $statusCode)."
        }
    }

    fun parseException(exception: Throwable): String {
        return when (exception) {
            is UnknownHostException -> "اتصال اینترنت برقرار نیست. لطفاً دسترسی به شبکه را بررسی کنید."
            is SocketTimeoutException -> "اتصال به سرویس هوش مصنوعی بیش از حد طول کشید (Timeout)."
            is IOException -> "خطای ارتباط با شبکه: ${exception.localizedMessage ?: "عدم پاسخگویی"}"
            else -> "خطای پردازش هوش مصنوعی: ${exception.localizedMessage ?: "ناشناخته"}"
        }
    }
}
