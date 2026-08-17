# Echo AI 助手更新日志 (Update Log)

## [v1.9.10] - 2026-08-17

### 1. 本次更新概述
本次更新针对设置界面窗口与输入框排版、对话思考与流式连接胶囊样式、二级菜单多层背景与边缘瑕疵、全方位 Markdown/LaTeX/标题/代码高亮与引用链接渲染优化、使用统计简约化与图标一致性、滑动手势拖影与列表精准滚动跟踪、历史消息模型名称独立固化绑定等 14 项关键体验与架构细节进行了系统性落地。

### 2. 需求实现与落地详情
1. **设置界面白色窗口背景彻底修复**：
   - 在 `EchoHaze.kt` 的 `EchoGlassDialog` 中通过 `DialogWindowProvider` 动态将 DecorView 及其 Window 背景置为透明，消除弹窗显示与退出时的 Android 系统默认白色底板。
2. **设置界面文字垂直居中排版优化**：
   - 提取并实现 `SettingsInputField`，将标题/说明移至输入框上方，采用 `singleLine = true` 并去除 Floating Label 导致的纵向偏移，确保输入文本垂直居中对齐。
3. **对话连接中胶囊统一为思考过程蓝色胶囊**：
   - `ChatScreen` 的连接中状态（`isConnecting`）采用与思考中胶囊完全统一的 `thinkingBubbleColor`、`thinkingContentColor` 和 `outlineSelected` 高光边框，保持视觉连贯。
4. **二级菜单多层背景与脏边修复**：
   - 根因：Material 3 1.2.x `DropdownMenu` 内部使用自带 `surface` 背景的 `Surface`，在 modifier 上再次添加 `.background()` 会引发双层半透明背景叠加与脏边。
   - 解决：创建 `EchoGlassDropdownMenu`，通过 `MaterialTheme(colorScheme = ...)` 直接定制 Menu 内部 Surface 的 `surface` 颜色与 18.dp 圆角高光边框，并在 8 个核心界面全面替换。
5. **模型输入/输出内容全方位渲染升级（Markdown/LaTeX/HTML/代码高亮/引用）**：
   - `MarkdownText.kt` 深度重构：
     - **LaTeX 公式**：支持行内 `$...$` 与块级 `$$...$$`，智能映射根号 `√`、积分 `∫/∬/∭/∮`、极限 `lim`、矩阵、导数、希腊字母、箭头与上标下标（`^2 -> ²`）；
     - **标题层级**：完整支持 1 到 6 级标题（解决 `#####` 五级与六级标题未解析问题）；
     - **粗斜体与 HTML**：支持 `***粗斜体***` 组合以及 HTML 标签与字符实体（`&nbsp;`, `&lt;`, `&gt;`, `&amp;`, `&quot;`, `&apos;`）安全转义；
     - **代码块高亮**：提供独立圆角代码卡片、语言徽章、复制按钮及基于关键词与符号的语法着色；
     - **关键小节强调**：「参考资料」、「要点概括」、「详细解答」关键字自动加大字号、加粗并呈斜体展示；
     - **参考资料与链接**：末尾 `[1]` 引用项展示为正常字号、移除冗余前导小点，并支持点击直接跳转网页或打开预览卡片。
6. **删除无法获取的缓存命中率指标**：
   - 从 `SummaryCard`、`ModernTrendChart` 和 `ModernModelStatsTable` 中彻底移除「缓存命中率」胶囊、曲线与排序选项。
7. **界面滑动到顶底拖影彻底修复**：
   - 在 `MainActivity.kt` 顶层注入 `CompositionLocalProvider(LocalOverscrollConfiguration provides null)`，彻底禁用 Android 12+ Stretch Overscroll 带来的 RenderNode 渲染失真与残影。
8. **使用统计表格完全重做为美观简约风**：
   - 在 `StatsScreen.kt` 中全新构建 `ModernModelStatsTable`，采用极简卡片流排版，支持 Tokens、请求次数、成功率、平均耗时 4 维快速排序，集成 Token 分布进度条与指标标签。
9. **使用统计界面图标与首页 100% 统一**：
   - `StatsScreen.kt` 的 `StatsHeaderIcon` 与 `HomeScreen.kt` 的三柱状递增徽章图参数与渲染逻辑完全统一。
10. **模型输出思考内容时的胶囊对齐与大小规范**：
    - `MessageBubble` 规范头像行与胶囊的尺寸约束，消除长文本溢出，并与下方的全宽思考内容卡片及正文内容精准对齐。
11. **思考中胶囊文案统一**：
    - 模型正在输出思考内容时（`isThinkingActive`），胶囊文本明确显示为「模型正在思考中」，思考结束后恢复为耗时与 Token 统计详情。
12. **删除消息后不触发滚动到底部修复**：
    - `ChatScreen` 维护 `prevMessagesCount`，当消息数减少（删除消息）时立即拦截滚动事件，保持当前视口位置不变。
13. **流式输出仅在用户处于最底端时跟随滚动**：
    - 结合 `LazyListState.layoutInfo` 精确检测视口底端偏移；用户主动上滑阅读时立即停用跟随，不再强制抢夺滚动位置。
14. **每条历史消息独立固化实际调用模型名称**：
    - `ChatViewModel` 通过 `messageModelMap` 与 `ApiUsageStat` 将每条 assistant 消息与其生成时实际调用的模型名称永久绑定，后续切换模型不会影响已有历史消息上的模型徽章显示。

### 3. 修改文件列表
- `app/build.gradle.kts`
- `app/src/main/java/com/aiassistant/MainActivity.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/FolderManagerScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/history/HistoryScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/CharacterEditorScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/ScenarioEditorScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayStudioScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/stats/StatsScreen.kt`
- `app/src/test/java/com/aiassistant/ChatEnhancementsTest.kt`

## [v1.9.8] - 2026-08-17

### 1. 本次更新概述
本次更新针对模型思考记录中断保护、全局对话与故事标题字体排版、思考胶囊紧凑布局与个性化文案模板、设置页交互与层级重构、二级液态玻璃菜单深度美化、使用统计图表与数据真实度修复、弹窗白影拖影彻底修复等 11 项深度体验进行了全面系统性升级。

### 2. 需求实现与落地详情
1. **模型思考打断记录持久化**：
   - 在 `ChatViewModel.stopGeneration()` 与 `saveErrorReply()` 中优化打断与异常处理，当思考过程被用户打断时，完整保留 `thinkingContent` 并持久化到 Room 数据库，确保思考气泡可正常展开查看。
2. **对话和故事标题字体加粗与字号微调**：
   - `HomeScreen`、`ChatScreen`、`HistoryScreen`、`RoleplayStudioScreen` 统一应用方正无衬线字体（`FontFamily.SansSerif`），字号调大至 16.5sp ~ 17.0sp 并加粗展示。
3. **模型名称整合入思考胶囊并贴近头像**：
   - `ChatScreen` 的 `MessageBubble` 移除右推 Spacer，胶囊紧贴头像（`Spacer(width = 8.dp)`），模型名称直接置于胶囊内，彻底消除左侧空白。
4. **思考胶囊文案个性化自定义模板**：
   - `PersonalizationManager` 新增 `thinkingCapsuleTemplate` 存储；
   - 支持 `{model}`、`{status}`、`{time}`、`{tokens}` 占位变量；
   - 设置页提供默认、极简、叙述等多款预设模板与实时预览效果。
5. **个性化中调节各个地方字体大小**：
   - `PersonalizationManager` 新增 `chatFontSize`（13sp ~ 22sp 滑块调节）与 `fontSizeScale`（紧凑、标准、大、特大）；
   - 设置页个性化 Tab 提供实时文本渲染预览卡片。
6. **修复设置页添加 API 配置等返回时残留白色窗口**：
   - `EchoGlassDialog` 配置 `decorFitsSystemWindows = false` 并添加全屏平滑渐层遮罩，彻底杜绝 Dialog 退出时的系统 DecorView 白底残影。
7. **设置页层级重构与气泡尺寸统一**：
   - 主题切换整合至「个性化与全局设定」Tab；
   - 菜单调整为：API配置 -> 个性化与全局设定 -> 联网搜索 -> 其他对话 -> 数据备份 -> 关于；
   - 统一所有设置项的外观间距与圆角气泡规范。
8. **二级菜单质感升级**：
   - 输入栏加号菜单、首页对话右侧三点菜单全面升级为带毛玻璃滤镜背景（`EchoGlass`）、圆角高光边框的高质感二级菜单。
9. **使用统计页面左上角图标统一**：
   - `StatsScreen` 顶栏图标调整为与首页 `StatsIconButton` 100% 结构与尺寸一致的动态三柱状徽章图。
10. **使用统计表格展示优化与缓存命中率修正**：
    - `ModelStatsTable` 优化排版与信息对齐；
    - 针对模型未返回缓存 Token 的情况统一显示 `--`，避免产生误导性的 `0.0%`。
11. **使用统计滑动白影与遮罩错位修复**：
    - 扁平化 `StatsScreen` 内部嵌套卡片层级，将内部子组件从 `Surface` 转换为轻量 `Box + background`，消除快速滑动时的硬件加速混合拖影。

### 3. 修改文件列表
- `app/build.gradle.kts`
- `app/src/main/java/com/aiassistant/utils/PersonalizationManager.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/history/HistoryScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayStudioScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/stats/StatsScreen.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/test/java/com/aiassistant/ChatEnhancementsTest.kt`

### 4. 版本与发布产物
- **VersionCode**: 88
- **VersionName**: 1.9.8
- **APK 产物**: `releases/Echo-v1.9.8-arm64-v8a.apk`
- **SHA256**: `6D8DF338D764301C855503611E559D7C938C80CE6A86CFA0392BB6C7BEF0488F`
