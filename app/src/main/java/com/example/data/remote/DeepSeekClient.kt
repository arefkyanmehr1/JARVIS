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

object DeepSeekClient {
    private const val BASE_URL = "https://api.deepseek.com/"

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

    val apiService: DeepSeekApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DeepSeekApiService::class.java)
    }

    fun parseHttpError(statusCode: Int, rawBody: String?): String {
        return when (statusCode) {
            400 -> "فرمت درخواست ارسال شده به DeepSeek معتبر نیست (کد ۴۰۰)."
            401 -> "کلید DeepSeek API نامعتبر است (کد ۴۰۱). لطفاً کلید صحیح را بررسی کنید."
            402 -> "اعتبار حساب DeepSeek کافی نیست (Insufficient Balance). لطفاً موجودی حساب را شارژ کنید."
            403 -> "دسترسی به سرور DeepSeek محدود شده است (کد ۴۰۳)."
            429 -> "محدودیت تعداد درخواست روی این کلید DeepSeek پر شده است. سیستم خودکار به کلید بعدی سوئیچ می‌کند."
            500, 502, 503 -> "سرورهای هوش مصنوعی DeepSeek موقتاً با بار زیاد مواجه هستند (کد $statusCode)."
            else -> "خطا در ارتباط با DeepSeek (کد $statusCode): ${rawBody?.take(80) ?: ""}"
        }
    }

    fun parseException(exception: Throwable): String {
        return when (exception) {
            is UnknownHostException -> "عدم امکان اتصال به سرور DeepSeek. اتصال اینترنت را بررسی کنید."
            is SocketTimeoutException -> "مهلت پاسخگویی DeepSeek به پایان رسید (Timeout)."
            is IOException -> "خطای ارتباط شبکه با DeepSeek: ${exception.localizedMessage ?: "عدم پاسخگویی"}"
            else -> "خطای پردازش هوش مصنوعی: ${exception.localizedMessage ?: "ناشناخته"}"
        }
    }
}
