package com.example.studybuddy.data

/**
 * 数据模型，与后端 API 的请求/响应格式对应
 */

// ===== 请求模型 =====

/** 启动督促会话的请求 */
data class StartSessionRequest(
    val user_id: String
)

/** 提交反馈的请求 */
data class FeedbackRequest(
    val user_id: String,
    val session_id: String,
    val feedback: String  // "started" / "refused" / "postponed"
)

/** 保存学习目标的请求 */
data class GoalRequest(
    val user_id: String,
    val study_goal: String,
    val min_action: String
)

// ===== 响应模型 =====

/** 督促会话响应：AI 生成的建议和引导话术 */
data class SessionResponse(
    val session_id: String = "",
    val user_state: String = "",
    val state_reason: String = "",
    val strategy: String = "",
    val action_suggestion: String = "",
    val guidance_message: String = ""
)

/** 反馈响应 */
data class FeedbackResponse(
    val is_effective: Boolean = false,
    val should_retry: Boolean = false,
    val report_summary: String = ""
)

/** 目标响应 */
data class GoalResponse(
    val user_id: String = "",
    val study_goal: String = "",
    val min_action: String = ""
)

/** 保存目标的响应 */
data class MessageResponse(
    val message: String = ""
)

/** 单条历史记录 */
data class HistoryRecord(
    val timestamp: String = "",
    val session_id: String = "",
    val user_state: String = "",
    val strategy: String = "",
    val action_suggestion: String = "",
    val guidance_message: String = "",
    val user_feedback: String = "",
    val is_effective: Boolean = false,
    val report_summary: String = ""
)

/** 历史记录响应 */
data class HistoryResponse(
    val user_id: String = "",
    val history: List<HistoryRecord> = emptyList()
)

/** 聊天请求 */
data class ChatRequest(
    val user_id: String,
    val session_id: String,
    val message: String,
    val user_state: String,
    val chat_history: List<ChatMessageData> = emptyList()
)

/** 聊天历史中的单条消息 */
data class ChatMessageData(
    val role: String,      // "user" 或 "assistant"
    val content: String
)

/** 聊天响应 */
data class ChatResponse(
    val reply: String = "",
    val should_start: Boolean = false  // AI 是否建议用户立即开始
)
