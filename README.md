# Echo - AI API 助手

Echo 是一个 Android 原生 AI API 客户端，用于统一调用 OpenAI 兼容接口、Anthropic 格式接口、DeepSeek、MiMo 等模型服务。项目重点是稳定的多模型对话、长上下文处理、本地数据管理和可交付的 APK 构建流程。

## 当前状态

- 当前版本：`v1.7.16`
- `versionCode`：`67`
- 版本码说明：当前测试包沿用 `67`，便于同签名旧版本回退安装。
- 应用包名：`com.aiassistant`
- Room 数据库版本：`17`
- 构建链：Kotlin `2.2.21`，AGP `8.10.1`，Gradle `8.11.1`，Compose BOM `2024.06.00`
- 当前 release ABI：`arm64-v8a`
- 最新本地安装包：`D:\Agent\app\releases\Echo-v1.7.16.apk`
- GitHub 仓库：`https://github.com/zhuqichen505-design/Api-.git`

## 核心功能

- 多 API 配置管理：支持 OpenAI 兼容格式、Anthropic 格式和常见第三方模型服务。
- 模型管理：支持模型列表、启用模型筛选、默认 API 与默认模型选择。
- 流式对话：实时显示模型输出，支持 Markdown 渲染、复制、编辑、删除和重新生成。
- 对话设置：模型切换、系统提示词、温度、最大输出 token、top-p、思考模式、思考强度、联网搜索统一在对话设置中管理。
- 长上下文：支持 token 预算、滚动摘要、长期记忆、上下文使用情况查看和主动压缩。
- 文件与图片：支持附件输入、图片处理、图片 OCR 和 PDF OCR 辅助。
- 界面个性化：支持自定义用户头像、模型头像、首页背景和对话页背景。
- 液态玻璃控件：首页搜索栏、创建对话按钮、对话页顶部工具栏和输入框使用真实背景模糊与高光材质，并按背景自动选择可读文字颜色。
- 对话管理：支持历史记录、文件夹、置顶、隐藏对话、隐私对话和对话分支。
- 使用统计：记录模型调用、token 用量、耗时和模型维度统计，并区分输入、输出、思考与未分项 token。
- 数据维护：支持 API Key 加密存储、环境变量管理、数据备份与恢复。

## 本次用户可见更新

- 首页设置、使用统计、历史记录和文件夹入口的液态玻璃颜色再次略微变淡。
- 文字可读性改为按顶部、内容区和底部背景分别判断黑色或白色，而不是只按主题模式判断。
- 对话页顶部工具栏和输入框改为与我的对话气泡一致的淡蓝玻璃色。
- 我的对话气泡和思考过程气泡加入液态玻璃效果。
- 输入框内附件、状态、按钮和智能搜索不再出现偏淡白块，智能搜索开关状态更明显。
- 使用统计、历史记录和对话设置增加玻璃承托底，文字和图表更清晰。
- 对话页工具栏中的对话名称字号调大一号。

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
$env:ECHO_RELEASE_KEYSTORE='C:\Users\19376\.android\debug.keystore'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --no-daemon assembleRelease
```

release 输出：

```text
app\build\outputs\apk\release\app-arm64-v8a-release.apk
```

交付给用户的 APK 复制到：

```text
D:\Agent\app\releases\Echo-v1.7.16.apk
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
