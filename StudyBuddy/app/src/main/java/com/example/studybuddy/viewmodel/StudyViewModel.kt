package com.example.studybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studybuddy.data.*
import com.example.studybuddy.data.PrefsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 学习督促 ViewModel
 * 管理所有 UI 状态和后端 API 调用
 */
class StudyViewModel : ViewModel() {

    private val api = ApiClient.api

    // ===== 用户数据 =====

    /** 学习目标 */
    var studyGoal by mutableStateOf("")
        private set

    /** 最小启动动作 */
    var minAction by mutableStateOf("")
        private set

    /** 是否已设置目标 */
    var hasGoal by mutableStateOf(false)
        private set

    /** 提醒时间 */
    var reminderHour by mutableIntStateOf(21)
        private set
    var reminderMinute by mutableIntStateOf(30)
        private set

    // ===== 会话数据 =====

    /** 当前 session_id */
    var currentSessionId by mutableStateOf("")
        private set

    /** 用户选择的状态 */
    var selectedState by mutableStateOf("")
        private set

    /** AI 生成的引导话术 */
    var guidanceMessage by mutableStateOf("")
        private set

    /** AI 生成的行动建议 */
    var actionSuggestion by mutableStateOf("")
        private set

    /** 用户状态（从后端返回） */
    var userState by mutableStateOf("")
        private set

    // ===== 对话数据 =====

    /** 对话消息列表 */
    var chatMessages by mutableStateOf<List<com.example.studybuddy.ui.screens.ChatMessage>>(emptyList())
        private set

    /** 当前对话轮次 */
    var chatRound by mutableIntStateOf(0)
        private set

    /** 最大对话轮次 */
    val maxChatRounds = 3

    /** 对话是否正在加载 */
    var isChatLoading by mutableStateOf(false)
        private set

    /** 对话历史（发送给后端的格式） */
    private var chatHistoryForApi = mutableListOf<ChatMessageData>()

    // ===== 统计数据 =====

    /** 连续打卡天数 */
    var streakDays by mutableIntStateOf(0)
        private set

    /** 累计启动次数 */
    var totalSessions by mutableIntStateOf(0)
        private set

    // ===== 学习中状态 =====

    /** 是否正在学习中 */
    var isStudying by mutableStateOf(false)
        private set

    /** 学习已进行的秒数（用于界面显示） */
    var studyElapsedSeconds by mutableStateOf(0L)
        private set

    /** 上次学习时长（结束后显示） */
    var lastStudyDuration by mutableStateOf(0L)
        private set

    /** 计时协程 */
    private var timerJob: Job? = null

    // ===== 向上之路（登山轨迹） =====

    /** 脚印列表 */
    var footprints by mutableStateOf<List<com.example.studybuddy.ui.screens.Footprint>>(emptyList())
        private set

    /** 是否刚新增了脚印（触发动画） */
    var newFootprintAdded by mutableStateOf(false)
        private set

    // ===== 历史记录 =====

    var historyRecords by mutableStateOf<List<HistoryRecord>>(emptyList())
        private set

    // ===== 加载状态 =====

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // ===== API 调用 =====

    /** 加载用户目标（App 启动时调用） */
    fun loadGoal(userId: String) {
        // 只有本地标记了设置完成，才从服务器加载
        if (!PrefsManager.isSetupComplete()) return

        reminderHour = PrefsManager.getReminderHour()
        reminderMinute = PrefsManager.getReminderMinute()

        // 检查是否有上次未结束的学习
        if (PrefsManager.isStudying()) {
            isStudying = true
            val startTime = PrefsManager.getStudyStartTime()
            studyElapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            startStudyTimer()
        }

        viewModelScope.launch {
            try {
                val response = api.getGoal(userId)
                if (response.study_goal.isNotEmpty()) {
                    studyGoal = response.study_goal
                    minAction = response.min_action
                    hasGoal = true
                }
            } catch (_: Exception) {
                // 新用户没有目标，忽略
            }

            // 加载历史记录来计算统计数据和脚印（过滤掉重置前的记录）
            try {
                val history = api.getHistory(userId)
                val resetTime = PrefsManager.getResetTime()
                historyRecords = if (resetTime != null) {
                    history.history.filter { it.timestamp > resetTime }
                } else {
                    history.history
                }
                calculateStats()
                // 从历史记录构建脚印数据
                footprints = historyRecords
                    .filter { it.user_feedback == "started" }
                    .mapIndexed { index, record ->
                        com.example.studybuddy.ui.screens.Footprint(
                            timestamp = record.timestamp,
                            index = index + 1
                        )
                    }
            } catch (_: Exception) {
                // 忽略
            }
        }
    }

    /** 保存学习目标 */
    fun saveGoal(userId: String, goal: String, action: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                api.saveGoal(GoalRequest(userId, goal, action))
            } catch (_: Exception) {
                // 即使云端保存失败也继续
            } finally {
                studyGoal = goal
                minAction = action
                reminderHour = hour
                reminderMinute = minute
                hasGoal = true
                PrefsManager.markSetupComplete()
                PrefsManager.saveReminderTime(hour, minute)
                isLoading = false
            }
        }
    }

    /** 启动督促会话（用户选择状态后调用） */
    fun startSession(userId: String, state: String) {
        selectedState = state
        userState = state
        chatMessages = emptyList()
        chatRound = 0
        chatHistoryForApi.clear()

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // 后台获取 session_id（不阻塞界面）
            launch {
                try {
                    val response = api.startSession(StartSessionRequest(userId))
                    currentSessionId = response.session_id
                    if (actionSuggestion.isEmpty()) {
                        actionSuggestion = response.action_suggestion
                    }
                } catch (_: Exception) {
                    currentSessionId = java.util.UUID.randomUUID().toString()
                }
            }

            // 用 /api/chat 快速生成第一条消息（单次 LLM 调用，快且自然）
            try {
                val firstMessage = getFirstChatPrompt(state)
                val response = api.chat(
                    ChatRequest(
                        user_id = userId,
                        session_id = "",
                        message = firstMessage,
                        user_state = state,
                        chat_history = emptyList()
                    )
                )
                guidanceMessage = response.reply
                actionSuggestion = minAction
            } catch (e: Exception) {
                guidanceMessage = getDefaultGuidance(state)
                actionSuggestion = minAction
            } finally {
                if (guidanceMessage.isNotEmpty()) {
                    chatMessages = listOf(
                        com.example.studybuddy.ui.screens.ChatMessage(guidanceMessage, isUser = false)
                    )
                    chatHistoryForApi.add(ChatMessageData("assistant", guidanceMessage))
                }
                isLoading = false
            }
        }
    }

    /** 根据用户选择的状态生成第一条聊天提示 */
    private fun getFirstChatPrompt(state: String): String {
        return when (state) {
            "no_energy" -> "我现在完全不想动，什么都不想做"
            "no_direction" -> "我想学习但不知道从哪开始"
            "tired" -> "我有点累，但还是想试试"
            "ready" -> "我准备好了，推我一把"
            else -> "我想开始学习"
        }
    }

    /** 用户发送聊天消息 */
    fun sendChatMessage(userId: String, message: String) {
        if (chatRound >= maxChatRounds || isChatLoading) return

        // 添加用户消息到界面
        chatMessages = chatMessages + com.example.studybuddy.ui.screens.ChatMessage(message, isUser = true)
        chatHistoryForApi.add(ChatMessageData("user", message))
        chatRound++

        viewModelScope.launch {
            isChatLoading = true
            try {
                val response = api.chat(
                    ChatRequest(
                        user_id = userId,
                        session_id = currentSessionId,
                        message = message,
                        user_state = userState,
                        chat_history = chatHistoryForApi.toList()
                    )
                )
                // 添加 AI 回复到界面
                chatMessages = chatMessages + com.example.studybuddy.ui.screens.ChatMessage(response.reply, isUser = false)
                chatHistoryForApi.add(ChatMessageData("assistant", response.reply))
            } catch (e: Exception) {
                // 后端不可用时用本地回复
                val fallbackReply = getLocalChatReply(message)
                chatMessages = chatMessages + com.example.studybuddy.ui.screens.ChatMessage(fallbackReply, isUser = false)
                chatHistoryForApi.add(ChatMessageData("assistant", fallbackReply))
            } finally {
                isChatLoading = false
            }
        }
    }

    /** 用户点击"我开始了"后，提交反馈 + 添加脚印 */
    fun submitStartedFeedback(userId: String) {
        // 添加脚印
        val newFootprint = com.example.studybuddy.ui.screens.Footprint(
            timestamp = java.time.LocalDateTime.now().toString(),
            index = footprints.size + 1
        )
        footprints = footprints + newFootprint
        newFootprintAdded = true
        totalSessions++

        viewModelScope.launch {
            try {
                api.submitFeedback(
                    FeedbackRequest(userId, currentSessionId, "started")
                )
            } catch (_: Exception) {
                // 静默失败，本地已计数
            }

            // 动画结束后重置标记
            kotlinx.coroutines.delay(2000)
            newFootprintAdded = false
        }
    }

    /** 完成专注后提交反馈 */
    fun submitCompleteFeedback(userId: String) {
        viewModelScope.launch {
            try {
                // 重新加载历史来更新统计
                val history = api.getHistory(userId)
                historyRecords = history.history
                calculateStats()
            } catch (_: Exception) {
                // 忽略
            }
        }
    }

    /** 进入"学习中"状态，开始后台计时 */
    fun enterStudyingMode() {
        isStudying = true
        studyElapsedSeconds = 0
        PrefsManager.startStudying()
        startStudyTimer()
    }

    /** 结束学习，记录时长 */
    fun exitStudyingMode() {
        timerJob?.cancel()
        lastStudyDuration = PrefsManager.stopStudying()
        isStudying = false
        studyElapsedSeconds = 0
    }

    /** 启动计时器（每秒更新界面） */
    private fun startStudyTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val startTime = PrefsManager.getStudyStartTime()
                if (startTime > 0) {
                    studyElapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                }
            }
        }
    }

    /** 格式化学习时长显示 */
    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    /** 加载历史记录（过滤重置前的记录） */
    fun loadHistory(userId: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = api.getHistory(userId)
                val resetTime = PrefsManager.getResetTime()
                historyRecords = if (resetTime != null) {
                    response.history.filter { it.timestamp > resetTime }
                } else {
                    response.history
                }
            } catch (e: Exception) {
                errorMessage = "加载失败：${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /** 更新设置（目标、最小动作、提醒时间） */
    fun updateSettings(userId: String, goal: String, action: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                api.saveGoal(GoalRequest(userId, goal, action))
            } catch (_: Exception) {
                // 即使云端保存失败也继续更新本地
            } finally {
                studyGoal = goal
                minAction = action
                reminderHour = hour
                reminderMinute = minute
                PrefsManager.saveReminderTime(hour, minute)
                isLoading = false
            }
        }
    }

    /** 重置所有数据 */
    fun resetAllData() {
        timerJob?.cancel()
        PrefsManager.clearAll()
        studyGoal = ""
        minAction = ""
        hasGoal = false
        streakDays = 0
        totalSessions = 0
        footprints = emptyList()
        historyRecords = emptyList()
        isStudying = false
        studyElapsedSeconds = 0
        lastStudyDuration = 0
        resetSession()
    }

    /** 重置会话状态（返回首页时调用） */
    fun resetSession() {
        currentSessionId = ""
        selectedState = ""
        guidanceMessage = ""
        actionSuggestion = ""
        chatMessages = emptyList()
        chatRound = 0
        chatHistoryForApi.clear()
        isChatLoading = false
    }

    fun clearError() {
        errorMessage = null
    }

    /** 获取格式化的提醒时间 */
    fun getReminderTimeString(): String {
        return String.format("%02d:%02d", reminderHour, reminderMinute)
    }

    // ===== 内部方法 =====

    /** 根据历史记录计算统计数据 */
    private fun calculateStats() {
        totalSessions = historyRecords.count { it.user_feedback == "started" }
        // 简单计算连续天数（统计最近连续有 started 记录的天数）
        streakDays = calculateStreak()
    }

    /** 计算连续打卡天数 */
    private fun calculateStreak(): Int {
        val successDates = historyRecords
            .filter { it.is_effective }
            .mapNotNull { it.timestamp.take(10) }  // 取日期部分 yyyy-MM-dd
            .distinct()
            .sortedDescending()

        if (successDates.isEmpty()) return 0

        var streak = 1
        for (i in 0 until successDates.size - 1) {
            // 简单判断：如果有连续的记录就计数
            streak++
        }
        return streak.coerceAtMost(successDates.size)
    }

    /** 后端不可用时的本地聊天回复兜底 */
    private fun getLocalChatReply(userMessage: String): String {
        return when {
            // 表达抗拒
            userMessage.contains("不想") || userMessage.contains("不行") ->
                "完全理解。其实你不用「想学」才能开始，只需要做一个动作就好。就像不用有食欲才能吃第一口饭。先打开看一眼？"
            userMessage.contains("太长") || userMessage.contains("5分钟") ->
                "5分钟只是个建议。哪怕就1分钟也行，打开看一眼就关掉也完全OK。重点不是时间，是「启动」这个动作本身。"
            userMessage.contains("为什么") ->
                "不为什么大道理。就是因为你之前设了这个目标，说明那个时候的你是想学的。现在帮那个你兑现一下，就这么简单。"
            // 表达迷茫
            userMessage.contains("不知道") || userMessage.contains("太多") ->
                "选择困难的时候，最好的办法就是不选——直接做最小的那个动作就好。不用想对不对，先动起来再说。"
            userMessage.contains("怕") || userMessage.contains("错") ->
                "学习没有错误方向，只有还没开始。哪怕今天只看了一页不相关的内容，你也比昨天多知道了一点。先开始。"
            userMessage.contains("帮我选") || userMessage.contains("方向") ->
                "好，我帮你选：就做你设定的最小动作「$minAction」。不多想，不犹豫，3秒内点下面的按钮。"
            // 表达疲惫
            userMessage.contains("累") ->
                "累是正常的。但你知道吗？很多时候「开始之后」反而没那么累了。就像出门前最不想动，走起来就好了。试试？"
            userMessage.contains("少做") || userMessage.contains("休息") ->
                "当然可以！做完5分钟就能休息，这是你和自己的约定。而且5分钟后你可能会发现：嘿，好像还能再来一会儿。"
            userMessage.contains("动力") ->
                "动力不是等来的，是做出来的。先开始1分钟，动力就会自己跑来找你。相信我，每次都是这样。"
            // 表达准备好了
            userMessage.contains("准备好") || userMessage.contains("冲") || userMessage.contains("好的") || userMessage.contains("好吧") || userMessage.contains("试试") ->
                "很好！就是现在。点击「我开始了」，5分钟后你会感谢现在的自己。我在这里陪你。"
            userMessage.contains("喝口水") || userMessage.contains("等我") ->
                "好的，喝完水就开始哦。我等你。准备好了就点下面的按钮。"
            userMessage.contains("行吧") || userMessage.contains("听你的") ->
                "就这么说定了。记住：不用做很多，只要「开始」就赢了。点击下面的按钮吧。"
            // 兜底
            else ->
                "不管现在感觉怎么样，先做一个最小的动作试试。开始之后的你，和现在犹豫的你，是完全不同的状态。"
        }
    }

    /** 后端不可用时的默认引导话术 */
    private fun getDefaultGuidance(state: String): String {
        return when (state) {
            "no_energy" -> "没关系，今天就做一件最小的事就好。打开看一眼，不想继续就可以关掉。零压力。"
            "no_direction" -> "别想太多，现在就做一个动作：$minAction。做完这一步再决定下一步。"
            "tired" -> "我知道你有点累了。但你之前也是这样坚持过来的。就 5 分钟，试试看？"
            "ready" -> "状态不错！别犹豫了，直接开始吧。5 分钟后你会感谢现在的自己。"
            else -> "准备好了吗？从最小的一步开始。"
        }
    }
}
