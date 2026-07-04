# MiMo - AI API 助手

一个 Android 原生 AI API 客户端，支持 mimo、deepseek、OpenAI、Anthropic 等多种 AI 模型。

## 功能特性

- 🤖 多模型支持（OpenAI兼容 + Anthropic格式）
- 💬 流式响应实时显示
- 🧠 思考模式（DeepSeek R1等）
- 📎 文件/图片上传
- 📝 Markdown渲染
- 📁 文件夹分类管理
- 🔍 全文搜索
- 📊 使用统计
- 🔐 API Key加密存储

## 技术栈

- Kotlin
- Jetpack Compose
- Room Database
- Retrofit + OkHttp
- Coroutines + Flow

## 构建

```bash
# 设置环境
export JAVA_HOME="D:/Java/jdk-17.0.2"
export ANDROID_HOME="C:/Users/19376/Android/Sdk"

# 构建Debug APK
./gradlew assembleDebug

# 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 文档

- [PROJECT.md](PROJECT.md) - 项目详细说明
- [CHANGELOG.md](CHANGELOG.md) - 更新日志

## 当前版本

**v1.3.0** (2026-06-14)

## 许可证

MIT License
