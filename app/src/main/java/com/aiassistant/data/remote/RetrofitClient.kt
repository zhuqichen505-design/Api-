package com.aiassistant.data.remote

import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val DEFAULT_USER_AGENT = "Echo-Assistant/2.2.5 (Android; Mobile)"

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

    // 专用的 AI 请求连接池：保持最大 10 个空闲连接，空闲保活 45 秒（避免被反代/防火墙静默掐断后复用僵尸连接导致超时或断连）
    private val sharedConnectionPool = ConnectionPool(10, 45, TimeUnit.SECONDS)

    // 专用于长文本与深度思考 SSE 流式输出（开启专用连接池与 15s 心跳保活）
    val streamHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .connectionPool(sharedConnectionPool)
        .pingInterval(15, TimeUnit.SECONDS) // HTTP/2 长连接保活心跳，防止深度推理/思考长挂时被中间 NAT 掐断
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 专用于普通 REST 请求、模型拉取与摘要生成（设置明确超时与专用连接池，防止永久挂死）
    val restHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .connectionPool(sharedConnectionPool)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 专用于全量时间线提炼、长文本深度分析与设定提炼（设置 600s 充足超时与弹性连接，支持超长篇上下文深度推理）
    val longAnalysisHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .connectionPool(sharedConnectionPool)
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .callTimeout(600, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var currentAnalysisBaseUrl: String = ""

    @Volatile
    private var currentAnalysisService: AiApiService? = null

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

    fun getAnalysisService(baseUrl: String): AiApiService {
        val normalizedUrl = normalizeBaseUrl(baseUrl)

        if (normalizedUrl != currentAnalysisBaseUrl || currentAnalysisService == null) {
            synchronized(lock) {
                if (normalizedUrl != currentAnalysisBaseUrl || currentAnalysisService == null) {
                    currentAnalysisBaseUrl = normalizedUrl
                    val retrofit = Retrofit.Builder()
                        .baseUrl(normalizedUrl)
                        .client(longAnalysisHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    currentAnalysisService = retrofit.create(AiApiService::class.java)
                }
            }
        }
        return currentAnalysisService!!
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
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Connection", "keep-alive")

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
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Connection", "keep-alive")

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
