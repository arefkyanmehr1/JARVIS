package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): Response<DeepSeekResponse>
}

object OpenAiClient {
    // Official OpenAI API base URL
    private const val BASE_URL = "https://api.openai.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestWithHeaders = original.newBuilder()
                .header("User-Agent", "JARVIS-Android/1.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(requestWithHeaders)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiApiService::class.java)
    }

    fun parseHttpError(statusCode: Int, rawBody: String?): String {
        return when (statusCode) {
            400 -> "درخواست ارسال شده به OpenAI دارای خطای فرمت است (کد ۴۰۰)."
            401 -> "کلید OpenAI API نامعتبر است یا منقضی شده است (کد ۴۰۱)."
            402, 429 -> "محدودیت تعداد درخواست یا اعتبار حساب OpenAI به اتمام رسیده است (کد $statusCode)."
            403 -> "دسترسی منطقه جغرافیایی یا حساب به OpenAI محدود شده است (کد ۴۰۳)."
            404 -> "مدل درخواستی OpenAI روی این اندپوینت یافت نشد (کد ۴۰۴)."
            500, 502, 503 -> "سرورهای OpenAI موقتاً با بار ترافیکی بالا مواجه هستند (کد $statusCode)."
            else -> "خطای ارتباط با OpenAI (کد $statusCode): ${rawBody?.take(100) ?: ""}"
        }
    }

    fun parseException(exception: Throwable): String {
        return when (exception) {
            is UnknownHostException -> "عدم اتصال به اینترنت یا مسدود بودن سرور OpenAI."
            is SocketTimeoutException -> "مهلت پاسخگویی سرور OpenAI به پایان رسید (Timeout)."
            is IOException -> "خطای شبکه در ارتباط با OpenAI: ${exception.localizedMessage ?: ""}"
            else -> "خطای پردازش هوش مصنوعی: ${exception.localizedMessage ?: "ناشناخته"}"
        }
    }
}
