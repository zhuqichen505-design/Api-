# Echo - AI API 助手 Android 应用

## 项目概述

Echo 是一个 Android 原生 AI API 客户端应用，用于调用 mimo、deepseek、OpenAI、Anthropic 等 AI 模型的 API。

**当前版本**: v2.6.0
**数据库版本**: 30
**技术栈**: Kotlin 2.2.21 + Jetpack Compose (BOM 2024.06.00) + Room 2.8.4 + Retrofit 2.9.0

> 工作流、最高准则与 APK 发布铁律的唯一权威版本在工作区根目录 `D:\Agent\APP-Echo\AGENTS.md`；本文件是项目事实卡，与其冲突时以 AGENTS.md 与代码为准。

---

## 维护规则（摘要，细则见 AGENTS.md）

1. 每次代码变动后都必须提交到 Git。
2. 每次版本更新后都必须创建对应版本标签，例如 `v1.7.2`。
3. 每次代码变动或版本更新后都必须推送到远程仓库：
   `https://github.com/zhuqichen505-design/Api-.git`
4. 每次交付时必须给出详细更新内容说明，包括：
   - 修改了哪些功能
   - 修复了哪些问题
   - 是否构建 APK
   - APK 路径
   - Git 提交号
   - 版本标签
   - 是否已完成网络备份
5. **APK 发布输出路径与历史版本安装包永久保留准则（最高铁律）**：
   - 以后构建和发布 APK 时，**统一只发布在 `D:\Agent\APP-Echo\app\releases` 这个路径**（严禁发布至其他路径）；
   - 绝对严禁删除、覆盖或清理 `D:\Agent\APP-Echo\app\releases` 目录下的任何历史版本安装包；
   - 每次发布新版本时只在 `D:\Agent\APP-Echo\app\releases` 目录下增量输出对应版本的唯一定名安装包（`Echo-v<version>.apk`），所有历史安装包必须永久保留。
6. 不允许提交本机敏感文件或构建产物，包括 `local.properties`、keystore、`.env`、`app/build/`、`.gradle/` 等。

---

## 数据库结构（v30，实体清单以 `data/local/AppDatabase.kt` 为准）

| 实体（表） | 说明 |
|------|------|
| Folder | 文件夹 |
| ApiConfig | API 配置 |
| Conversation / Message | 对话与消息 |
| ApiUsageStat | 使用统计 |
| EnvironmentVariable | 环境变量（加密存储） |
| PromptTemplate | 提示词模板 |
| MemoryItem | 全局记忆 |
| ConversationBranch / SelectedModel | 会话分支 / 模型选择与排序 |
| CharacterProfile / CharacterTag / CharacterTagCrossRef | 角色卡 / 角色标签及关联 |
| RoleplayScenario / RoleplaySession / RoleplayMemory | 角色扮演场景 / 会话 / 记忆 |
| WorldBook / WorldBookEntry | 世界书 |
| TimelineNode | 剧情时间线 |

迁移链至 `MIGRATION_29_30`（`data/local/migrations/AppDatabaseMigrations.kt`）；修改实体必须 version+1 并新增 Migration，禁止 destructive migration。

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
| **v1.7.2** | **v16** | **联网搜索、隐私对话、隐藏对话、模型默认选择、UI与流式输出优化** |
| **v1.7.18** | **v17** | **液态玻璃 token 统一、深色可读性、对话气泡/输入栏、统计表格与设置页 UI 修复** |
| **v1.8.0** | **v18** | **角色扮演系统架构与功能实现** |
| **v1.8.1** | **v18** | **规范文档与限制解除、角色扮演全链路闭环、图标与UI显示优化、支持原版本覆盖更新** |
| **v2.0.4** | **v23** | **分支功能完整重构与事务原子落库、分支生成弹窗确认与跳转、隐藏对话解锁会话维持** |
| **v2.0.5** | **v23** | **非破坏性增量备份导入、复制整个对话（普通与隐藏同步）、备份单对话（普通与隐藏同步）** |

> 完整版本历史见 `CHANGELOG.md` 与 `UPDATE_LOG.md`（v2.0.5 之后含角色扮演全链路、时间线、世界书、阶梯式上下文压缩等）。

---

## 构建命令（Git Bash，已核验）

```bash
cd /d/Agent/APP-Echo/app/AiApiAssistant
./gradlew.bat compileDebugKotlin --no-daemon   # 编译
./gradlew.bat testDebugUnitTest --no-daemon    # 单元测试
./gradlew.bat lintDebug --no-daemon            # Lint
./gradlew.bat assembleRelease --no-daemon      # 发布 APK
```

- JDK 17 由 `gradle.properties` 的 `org.gradle.java.home=D:/Java/jdk-17.0.2` 固定，无需手动 export。
- Android SDK 由 `local.properties` 的 `sdk.dir` 指定（本机 `D:\Agent\app\.android-sdk`，该文件不入库）。
- 发布产物须复制为 `D:\Agent\APP-Echo\app\releases\Echo-v<version>.apk`（详见 AGENTS.md §4 铁律）。
