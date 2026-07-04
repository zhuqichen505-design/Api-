package com.aiassistant.data.remote

import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    @Volatile
    private var currentBaseUrl: String = ""

    @Volatile
    private var currentRetrofit: Retrofit? = null

    @Volatile
    private var currentService: AiApiService? = null

    private val lock = Any()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // BODY 日志会完整读取响应体，SSE 会因此等到结束才交给界面。
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getService(baseUrl: String): AiApiService {
        val normalizedUrl = normalizeBaseUrl(baseUrl)

        if (normalizedUrl != currentBaseUrl || currentService == null) {
            synchronized(lock) {
                if (normalizedUrl != currentBaseUrl || currentService == null) {
                    currentBaseUrl = normalizedUrl
                    currentRetrofit = Retrofit.Builder()
                        .baseUrl(normalizedUrl)
                        .client(httpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    currentService = currentRetrofit!!.create(AiApiService::class.java)
                }
            }
        }
        return currentService!!
    }

    fun postJson(
        baseUrl: String,
        path: String,
        headers: Map<String, String>,
        json: String,
        onCallCreated: ((Call) -> Unit)? = null
    ): okhttp3.Response {
        val url = normalizeBaseUrl(baseUrl).trimEnd('/') + "/" + path.trimStart('/')
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)

        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        val call = httpClient.newCall(requestBuilder.build())
        onCallCreated?.invoke(call)
        return call.execute()
    }

    fun getJson(baseUrl: String, path: String, headers: Map<String, String>): okhttp3.Response {
        val url = normalizeBaseUrl(baseUrl).trimEnd('/') + "/" + path.trimStart('/')
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        return httpClient.newCall(requestBuilder.build()).execute()
    }

    private fun normalizeBaseUrl(url: String): String {
        var normalized = url.trim()

        // 确保以http://或https://开头
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }

        // 移除末尾的斜杠
        if (normalized.endsWith("/")) {
            normalized = normalized.dropLast(1)
        }

        // 确保最终以斜杠结尾（Retrofit要求）
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }

        return normalized
    }

    fun formatApiKey(apiKey: String): String {
        return if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
    }
}
