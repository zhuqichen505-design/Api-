# Echo - AI API 助手

Echo 是一个 Android 原生 AI API 客户端，用于统一调用 OpenAI 兼容接口、Anthropic 格式接口、DeepSeek、MiMo 等模型服务。项目重点是稳定的多模型对话、长上下文处理、本地数据管理和可交付的 APK 构建流程。

## 当前状态

- 当前版本：`v1.7.6`
- `versionCode`：`67`
- 版本码说明：当前测试包沿用 `67`，便于同签名旧版本回退安装。
- 应用包名：`com.aiassistant`
- Room 数据库版本：`17`
- 当前 release ABI：`arm64-v8a`
- 最新本地安装包：`D:\Agent\app\releases\Echo-v1.7.6.apk`
- GitHub 仓库：`https://github.com/zhuqichen505-design/Api-.git`

## 核心功能

- 多 API 配置管理：支持 OpenAI 兼容格式、Anthropic 格式和常见第三方模型服务。
- 模型管理：支持模型列表、启用模型筛选、默认 API 与默认模型选择。
- 流式对话：实时显示模型输出，支持 Markdown 渲染、复制、编辑、删除和重新生成。
- 对话设置：模型切换、系统提示词、温度、最大输出 token、top-p、思考模式、思考强度、联网搜索统一在对话设置中管理。
- 长上下文：支持 token 预算、滚动摘要、长期记忆、上下文使用情况查看和主动压缩。
- 文件与图片：支持附件输入、图片处理、图片 OCR 和 PDF OCR 辅助。
- 界面个性化：支持自定义用户头像、模型头像、首页背景和对话页背景。
- 对话管理：支持历史记录、文件夹、置顶、隐藏对话、隐私对话和对话分支。
- 使用统计：记录模型调用、token 用量、耗时和模型维度统计。
- 数据维护：支持 API Key 加密存储、环境变量管理、数据备份与恢复。

## 本次用户可见更新

- 系统提示词在对话设置中保存后，会在下一次发送或重新生成时立即作为本轮请求的最高优先级提示生效。
- 对话页上下文入口改为圆环图案，深色部分表示已占用比例，顶栏不再直接显示具体数值。
- 上下文详情中“输入预算”改为“最大上下文限制”，并优先使用模型列表返回的上下文窗口元数据。
- 模型切换移入对话设置顶部；系统提示词移入对话设置并放在温度上方。
- 输入框下方不再显示模型切换入口，保留智能搜索和附件入口。
- 消息底部耗时和思考 token 文案保持原样，排版改为可换行，避免文字被覆盖。
- Markdown 表格渲染修复列宽不一致、转义竖线和代码片段竖线导致的错列问题。
- 支持在设置的个性化页面分别上传首页背景和对话页背景图片，也可恢复默认纯色背景。
- 用户消息气泡改为淡蓝色、黑色文字，并接入磨砂背景效果。
- 对话页一键到顶/到底按钮会避让输入框磨砂区域。
- 设置关于页显示 Echo 版本号，并将“本次更新”和“功能特性”分区展示。

## OCR 现状与后续方向

当前 OCR 使用 Google ML Kit Text Recognition。它适合 Android 端直接选择图片后进行本地识别，接入成本低，稳定性好。

可继续评估的方向：

- PaddleOCR / RapidOCR：中文和复杂版面识别潜力更强，但会引入模型文件、ONNX/OpenCV 或 NDK 体积成本。
- Tesseract4Android：完全开源、离线可控，但中文识别体验和模型管理成本高。
- 可选 OCR 引擎设置：后续可以保留 ML Kit 为默认，在设置中增加“高级 OCR 引擎”选项。

## 构建说明

项目根目录：

```powershell
D:\Agent\app\AiApiAssistant
```

Windows PowerShell 推荐使用 JDK 17：

```powershell
$env:JAVA_HOME='D:\Java\jdk-17.0.2'
$env:GRADLE_USER_HOME='D:\Agent\app\.gradle-home'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleRelease
```

release 输出：

```text
app\build\outputs\apk\release\app-arm64-v8a-release.apk
```

交付给用户的 APK 复制到：

```text
D:\Agent\app\releases\Echo-v1.7.6.apk
```

## 维护规则

每次更新必须完成：

1. 更新代码和必要文档。
2. 构建 release APK。
3. 验证 APK 包名、版本号、ABI、签名和 SHA-256。
4. 提交 Git commit。
5. 推送到 GitHub：`git push origin main`。
6. 最终回复中说明 APK 路径、SHA-256、提交号和是否已完成网络备份。

## 相关文档

- [CHANGELOG.md](CHANGELOG.md)：版本更新记录。
- [PROJECT.md](PROJECT.md)：早期项目说明与维护记录，部分版本信息可能滞后。
- `design/stats-icons-preview.png`：使用统计图标预览方案。

## 许可

MIT License
