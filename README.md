# Echo - AI API 助手

Echo 是一个 Android 原生 AI API 客户端，用于统一调用 OpenAI 兼容接口、Anthropic 格式接口、DeepSeek、MiMo 等模型服务。项目重点是稳定的多模型对话、长上下文处理、本地数据管理和可交付的 APK 构建流程。

## 当前状态

- 当前版本：`v1.7.5`
- `versionCode`：`67`
- 应用包名：`com.aiassistant`
- Room 数据库版本：`17`
- 当前 release ABI：`arm64-v8a`
- 最新本地安装包：`D:\Agent\app\releases\Echo-v1.7.5.apk`
- GitHub 仓库：`https://github.com/zhuqichen505-design/Api-.git`
- 最新版本标签：`v1.7.5`

## 核心功能

- 多 API 配置管理：支持 OpenAI 兼容格式、Anthropic 格式及常见第三方模型服务。
- 模型管理：支持模型列表、启用模型筛选、默认 API 与默认模型选择。
- 流式对话：实时显示模型输出，支持 Markdown 渲染、复制、编辑、删除和重新生成。
- 思考模式：支持 DeepSeek R1 等带推理内容的模型，并能单独展示思考内容。
- 联网搜索：支持 Tavily 搜索配置，在对话中按需启用联网搜索。
- 临时对话设置：温度、最大输出 token、top-p、思考力度、联网搜索等可对当前对话单独生效。
- 系统提示词与模板：支持当前对话系统提示词、全局提示词、提示词模板保存与复用。
- 文件与图片上传：支持附件输入、图片处理和 OCR 辅助。
- 对话管理：支持历史记录、文件夹、置顶、隐藏对话、隐私对话和对话分支。
- 使用统计：记录模型调用、token 用量、耗时和模型维度统计。
- 数据维护：支持 API Key 加密存储、环境变量管理、数据备份与恢复、自定义用户头像和模型头像。

## 长上下文能力

从 `v1.7.3` 开始，Echo 的对话上下文不再只依赖简单字符截断，而是按 token 预算组织输入：

- 近期原文：优先保留最近的用户与助手消息。
- 滚动摘要：较早消息会被压缩成可复用摘要，减少长对话中旧消息挤占预算。
- 长期记忆：保存明确的用户偏好、项目背景和长期事实，并在后续对话中按相关性注入。
- 上下文用量：`v1.7.5` 在对话页新增上下文用量入口，可查看预算占用、近期消息、较早消息、摘要和记忆占用。
- 主动压缩：用户可手动触发上下文压缩；该操作只刷新滚动摘要，不删除历史消息。

## v1.7.5 用户可见更新

- 对话页顶栏新增当前上下文用量百分比。
- 新增上下文使用情况弹窗，展示输入预算、摘要、记忆和压缩状态。
- 新增主动压缩按钮，可提前生成或刷新滚动摘要。
- 对话导航栏只在页面滑动时出现，展开后点击窗口外会自动收起。
- 调小使用统计入口图标，使其与设置按钮视觉大小更一致。
- 设置页“关于”中的功能特性已随当前版本更新，并在功能特性下方显示本次用户侧更新内容。
- release APK 命名改为短格式：`Echo-v版本号.apk`。

完整版本记录见 [CHANGELOG.md](CHANGELOG.md)。

## 技术栈

- Kotlin
- Jetpack Compose
- Room
- Retrofit + OkHttp
- Kotlin Coroutines + Flow
- Gradle Android Plugin

## 构建说明

项目根目录为：

```powershell
D:\Agent\app\AiApiAssistant
```

Windows PowerShell 下推荐使用 JDK 17 构建：

```powershell
$env:JAVA_HOME='D:\Java\jdk-17.0.2'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleRelease
```

release 构建输出：

```text
app\build\outputs\apk\release\app-arm64-v8a-release.apk
```

交付给用户的 APK 复制到：

```text
D:\Agent\app\releases\Echo-v1.7.5.apk
```

`releases/`、`*.apk`、`app/build/`、`.gradle/`、`local.properties`、keystore、`.env` 等本机文件和构建产物不会提交到 Git。

## 维护与网络备份规则

每次更新都必须完成以下收尾流程：

1. 更新代码和必要文档。
2. 提交 Git commit。
3. 自动推送到 GitHub：`git push origin main`。
4. 最终回复中必须说明 Git 提交号和是否已完成网络备份。

凡涉及应用功能、版本号或 APK 交付的更新，还必须额外完成：

1. 更新 [CHANGELOG.md](CHANGELOG.md)，写清用户侧可感知更新、技术细则、版本号、构建结果和 APK 信息。
2. 构建 release APK，并确认 `applicationId`、`versionCode`、`versionName` 和 ABI。
3. 将 APK 复制到 `D:\Agent\app\releases\`，文件名只保留版本号，例如 `Echo-v1.7.5.apk`。
4. 计算 APK SHA-256。
5. 创建对应版本标签，例如 `v1.7.5`。
6. 自动推送版本标签到 GitHub：`git push origin <tag>`。
7. 最终回复中必须说明 APK 路径、SHA-256、Git 提交号、版本标签和是否已完成网络备份。

## 相关文档

- [PROJECT.md](PROJECT.md)：较早的项目说明与维护规则记录，部分版本信息可能滞后。
- [CHANGELOG.md](CHANGELOG.md)：当前可信的版本更新记录。
- `design/stats-icons-preview.png`：`v1.7.5` 生成的 10 个使用统计图标预览方案。

## 许可证

MIT License
