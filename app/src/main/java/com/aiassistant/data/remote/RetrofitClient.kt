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
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // 专用于长文本与深度思考 SSE 流式输出（读超时为 0 无限等待）
    val streamHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 专用于普通 REST 请求、模型拉取与摘要生成（设置明确 30s 超时，防止永久挂死）
    val restHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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
                        .client(restHttpClient)
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

        val call = streamHttpClient.newCall(requestBuilder.build())
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

        return restHttpClient.newCall(requestBuilder.build()).execute()
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
