# Study Buddy

一款帮你克服拖延、从「想学」到「开始学」的 AI 学习督促 App。

Study Buddy 不会帮你制定复杂的学习计划，它只做一件事：**在你不想学的时候，温柔地推你一把。**

## 功能

### AI 对话引导
选择你当前的状态（不想动 / 迷茫 / 有点累 / 可以开始），AI 会根据你的状态用朋友聊天的语气引导你开始学习。支持自由文字对话，最多 3 轮。

### 5 分钟启动法
不要求你学 1 小时，只要求你做 5 分钟。大多数时候，开始之后你会发现没那么难。

### 学习中模式
完成 5 分钟后可以选择「继续学习」，App 会进入后台计时模式，记录你的学习时长。随时可以结束。

### 向上之路
每次开始学习都会在登山轨迹上留下一个脚印。看着脚印越来越多，你会发现自己其实一直在前进。

### 设置
随时修改学习目标、最小启动动作和提醒时间。

## 截图

| 首页 | AI 对话 | 专注计时 | 完成 |
|------|---------|----------|------|
| 目标卡片 + 统计 + 向上之路 | 自由文字聊天 | 5 分钟倒计时 | 庆祝 + 继续学习 |

## 技术栈

### Android 前端
- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: ViewModel + mutableStateOf
- **网络**: Retrofit + OkHttp + Gson
- **本地存储**: SharedPreferences

### 后端
- **框架**: FastAPI + LangGraph
- **语言**: Python 3.11
- **AI 模型**: DeepSeek V3 (via OpenAI-compatible API)
- **部署**: 阿里云 ECS + systemd

## 项目结构

```
app/src/main/java/com/example/studybuddy/
├── MainActivity.kt                 # 导航主入口
├── data/
│   ├── ApiService.kt              # API 接口定义 + Retrofit 客户端
│   ├── Models.kt                  # 数据模型
│   └── PrefsManager.kt           # 本地存储管理
├── viewmodel/
│   └── StudyViewModel.kt          # 状态管理 + 业务逻辑
└── ui/
    ├── theme/
    │   ├── Color.kt              # 配色方案（浅色 + 深色）
    │   ├── Theme.kt              # Material 3 主题
    │   └── Type.kt               # 字体层级
    └── screens/
        ├── GoalScreen.kt         # 目标设置（首次使用）
        ├── HomeScreen.kt         # 首页
        ├── StateScreen.kt        # 状态选择
        ├── SuggestionScreen.kt   # AI 对话
        ├── FocusScreen.kt        # 5 分钟专注计时
        ├── CompleteScreen.kt     # 完成庆祝
        ├── ClimbingTrail.kt      # 向上之路组件
        ├── HistoryScreen.kt      # 历史记录
        └── SettingsScreen.kt     # 设置
```

## 后端 API

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/session/start` | 启动督促会话，AI 分析状态并生成建议 |
| POST | `/api/session/feedback` | 提交用户反馈（started/refused/postponed） |
| POST | `/api/goal` | 保存/更新学习目标 |
| GET | `/api/goal/{user_id}` | 获取学习目标 |
| GET | `/api/history/{user_id}` | 获取历史记录 |
| POST | `/api/chat` | AI 自由对话 |
| GET | `/health` | 健康检查 |

## 用户流程

```
首次使用 → 设置目标 → 首页
                        ↓
                    准备开始
                        ↓
                  选择当前状态
                        ↓
               AI 对话引导（1-3 轮）
                        ↓
                  点击「我开始了」
                        ↓
                  5 分钟专注计时
                        ↓
                    完成页面
                   ↙        ↘
            继续学习          今天就到这里
          （后台计时）          （回到首页）
```

## 本地运行

### 前端
1. 用 Android Studio 打开项目
2. 修改 `ApiService.kt` 中的 `BASE_URL` 为你的后端地址
3. 运行到模拟器或真机

### 后端
1. 安装依赖：`pip install fastapi uvicorn langchain-openai langgraph python-dotenv`
2. 创建 `.env` 文件：
   ```
   LLM_API_KEY=你的API密钥
   LLM_BASE_URL=https://api.deepseek.com/v1
   LLM_MODEL_NAME=deepseek-chat
   ```
3. 启动：`uvicorn main:app --host 0.0.0.0 --port 8000`

## License

MIT
