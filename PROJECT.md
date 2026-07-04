# MiMo - AI API 助手 Android 应用

## 项目概述

MiMo 是一个 Android 原生 AI API 客户端应用，用于调用 mimo、deepseek、OpenAI、Anthropic 等 AI 模型的 API。

**当前版本**: v1.4.0  
**数据库版本**: 11  
**技术栈**: Kotlin + Jetpack Compose + Room + Retrofit

---

## 数据库结构（v11）

| 表名 | 说明 |
|------|------|
| folders | 文件夹 |
| api_configs | API配置 |
| conversations | 对话 |
| messages | 消息 |
| api_usage_stats | 使用统计 |
| environment_variables | 环境变量 |
| prompt_templates | 提示词模板 |
| conversation_branches | 会话分支（新增） |
| selected_models | 选择的模型（新增） |

---

## 核心功能

1. **API配置管理** - 支持OpenAI兼容格式 + Anthropic格式
2. **对话功能** - 流式响应、思考模式、文件上传、Markdown渲染
3. **会话分支** - 从任意消息点创建分支对话
4. **模型列表选择** - 可选择显示哪些模型
5. **使用统计** - Token使用量、缓存命中率
6. **数据备份** - 自动备份、手动备份、恢复备份
7. **提示词系统** - 预设模板、全局提示词
8. **环境变量** - 加密存储、变量引用

---

## 版本历史

| 版本 | 数据库 | 主要更新 |
|------|--------|----------|
| v1.3.0 | v5 | 基础稳定版本 |
| v1.3.6 | v10 | 修复闪退、数据备份 |
| **v1.4.0** | **v11** | **会话分支、模型选择、缓存命中率** |

---

## 构建命令

```bash
cd d:/Agent/app/AiApiAssistant
export JAVA_HOME="D:/Java/jdk-17.0.2"
export ANDROID_HOME="C:/Users/19376/Android/Sdk"
./gradlew assembleDebug --no-daemon
```
