package com.example.studybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studybuddy.data.PrefsManager
import com.example.studybuddy.ui.screens.*
import com.example.studybuddy.ui.theme.StudyBuddyTheme
import com.example.studybuddy.viewmodel.StudyViewModel

/**
 * 主 Activity
 * 页面流程：Goal → Home → State → Suggestion → Focus → Complete → Home
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsManager.init(this)
        enableEdgeToEdge()
        setContent {
            StudyBuddyTheme {
                StudyBuddyApp()
            }
        }
    }
}

@Composable
fun StudyBuddyApp(viewModel: StudyViewModel = viewModel()) {
    val navController = rememberNavController()
    val userId = remember { PrefsManager.getUserId() }

    // App 启动时加载用户数据
    LaunchedEffect(userId) {
        viewModel.loadGoal(userId)
    }

    // 错误提示
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "goal",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. 设置阶段：首次使用设置目标
            composable("goal") {
                // 如果已有目标，自动跳到首页
                LaunchedEffect(viewModel.hasGoal) {
                    if (viewModel.hasGoal) {
                        navController.navigate("home") {
                            popUpTo("goal") { inclusive = true }
                        }
                    }
                }

                GoalScreen(
                    isLoading = viewModel.isLoading,
                    onSaveGoal = { goal, action, hour, minute ->
                        viewModel.saveGoal(userId, goal, action, hour, minute)
                    }
                )
            }

            // 2. 每日首页：显示目标和统计
            composable("home") {
                HomeScreen(
                    studyGoal = viewModel.studyGoal,
                    minAction = viewModel.minAction,
                    reminderTime = viewModel.getReminderTimeString(),
                    streakDays = viewModel.streakDays,
                    totalSessions = viewModel.totalSessions,
                    footprints = viewModel.footprints,
                    newFootprintAdded = viewModel.newFootprintAdded,
                    isStudying = viewModel.isStudying,
                    studyElapsedSeconds = viewModel.studyElapsedSeconds,
                    formattedDuration = viewModel.formatDuration(viewModel.studyElapsedSeconds),
                    lastStudyDuration = viewModel.lastStudyDuration,
                    lastDurationFormatted = viewModel.formatDuration(viewModel.lastStudyDuration),
                    onStartStudy = {
                        viewModel.resetSession()
                        navController.navigate("state")
                    },
                    onEndStudy = {
                        viewModel.exitStudyingMode()
                    },
                    onGoToHistory = {
                        viewModel.loadHistory(userId)
                        navController.navigate("history")
                    },
                    onGoToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            // 3. 引导阶段：选择当前状态
            composable("state") {
                StateScreen(
                    onStateSelected = { state ->
                        viewModel.startSession(userId, state)
                        navController.navigate("suggestion")
                    }
                )
            }

            // 4. AI 建议 + 自由对话页
            composable("suggestion") {
                SuggestionScreen(
                    guidanceMessage = viewModel.guidanceMessage,
                    actionSuggestion = viewModel.actionSuggestion,
                    userState = viewModel.userState,
                    isLoading = viewModel.isLoading,
                    isChatLoading = viewModel.isChatLoading,
                    chatRound = viewModel.chatRound,
                    maxChatRounds = viewModel.maxChatRounds,
                    chatMessages = viewModel.chatMessages,
                    onSendMessage = { message ->
                        viewModel.sendChatMessage(userId, message)
                    },
                    onStartFocus = {
                        viewModel.submitStartedFeedback(userId)
                        navController.navigate("focus") {
                            popUpTo("home")
                        }
                    }
                )
            }

            // 5. 专注阶段：5分钟倒计时
            composable("focus") {
                FocusScreen(
                    actionSuggestion = viewModel.actionSuggestion,
                    onFocusComplete = {
                        viewModel.submitCompleteFeedback(userId)
                        navController.navigate("complete") {
                            popUpTo("home")
                        }
                    }
                )
            }

            // 6. 完成阶段：庆祝 + 继续/结束
            composable("complete") {
                CompleteScreen(
                    streakDays = viewModel.streakDays,
                    onContinueStudy = {
                        // 继续学习：进入学习中状态，回到首页显示计时器
                        viewModel.enterStudyingMode()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onFinish = {
                        // 结束：回到首页
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            // 历史记录页
            composable("history") {
                HistoryScreen(
                    records = viewModel.historyRecords,
                    isLoading = viewModel.isLoading,
                    onBack = { navController.popBackStack() }
                )
            }

            // 设置页
            composable("settings") {
                SettingsScreen(
                    currentGoal = viewModel.studyGoal,
                    currentMinAction = viewModel.minAction,
                    currentReminderHour = viewModel.reminderHour,
                    currentReminderMinute = viewModel.reminderMinute,
                    isLoading = viewModel.isLoading,
                    onSave = { goal, action, hour, minute ->
                        viewModel.updateSettings(userId, goal, action, hour, minute)
                        navController.popBackStack()
                    },
                    onResetData = {
                        viewModel.resetAllData()
                        navController.navigate("goal") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
