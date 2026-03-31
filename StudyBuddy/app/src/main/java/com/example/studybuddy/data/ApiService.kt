package com.example.studybuddy.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * 后端 API 接口定义
 * 对应后端 FastAPI 的 5 个接口
 */
interface ApiService {

    /** 启动督促会话（第一阶段：AI 分析状态 → 生成建议） */
    @POST("api/session/start")
    suspend fun startSession(@Body request: StartSessionRequest): SessionResponse

    /** 提交用户反馈（第二阶段：评估效果 → 记录报告） */
    @POST("api/session/feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest): FeedbackResponse

    /** 保存/更新学习目标 */
    @POST("api/goal")
    suspend fun saveGoal(@Body request: GoalRequest): MessageResponse

    /** 查询学习目标 */
    @GET("api/goal/{user_id}")
    suspend fun getGoal(@Path("user_id") userId: String): GoalResponse

    /** 查询历史记录 */
    @GET("api/history/{user_id}")
    suspend fun getHistory(@Path("user_id") userId: String): HistoryResponse

    /** AI 对话：用户自由输入，AI 引导回复 */
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

/**
 * API 客户端单例
 * ⚠️ 把 BASE_URL 改成你的阿里云服务器 IP
 */
object ApiClient {

    // TODO: 改成你的阿里云服务器公网 IP
    private const val BASE_URL = "http://8.136.195.194:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)   // 连接超时
        .readTimeout(60, TimeUnit.SECONDS)       // 读取超时（AI 生成可能较慢）
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY  // 打印请求日志，方便调试
            }
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** 获取 API 接口实例 */
    val api: ApiService = retrofit.create(ApiService::class.java)
}
