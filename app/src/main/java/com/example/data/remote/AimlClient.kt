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

interface AimlApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): Response<DeepSeekResponse>
}

object AimlClient {
    private const val BASE_URL = "https://api.aimlapi.com/"

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

    val apiService: AimlApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AimlApiService::class.java)
    }

    fun parseHttpError(statusCode: Int, rawBody: String?): String {
        return when (statusCode) {
            400 -> "خطای فرمت درخواست به AIML API (کد ۴۰۰)."
            401 -> "کلید AIML API نامعتبر است (کد ۴۰۱). لطفاً کلید صحیح را بررسی فرمایید."
            402 -> "موجودی حساب AIML کافی نیست (کد ۴۰۲)."
            429 -> "محدودیت تعداد درخواست روی این کلید AIML پر شده است."
            else -> "خطا در ارتباط با AIML API (کد $statusCode): ${rawBody?.take(100) ?: ""}"
        }
    }

    fun parseException(exception: Throwable): String {
        return when (exception) {
            is UnknownHostException -> "عدم امکان اتصال به سرور AIML. اتصال اینترنت را بررسی کنید."
            is SocketTimeoutException -> "مهلت پاسخگویی سرور AIML به پایان رسید (Timeout)."
            is IOException -> "خطای شبکه در ارتباط با AIML: ${exception.localizedMessage ?: ""}"
            else -> "خطای هوش مصنوعی: ${exception.localizedMessage ?: "ناشناخته"}"
        }
    }
}
