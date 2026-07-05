# Echo 更新日志

## v1.7.16 Hotfix (2026-07-05) - 玻璃遮罩源隔离与新对话入口统一
### 用户侧可见更新
- 修复使用统计、历史记录、文件夹管理、设置页和对话页中大块内容被玻璃遮罩泛白、模糊、遮挡的问题。
- 修复首页点击“置顶”筛选后，未选中的文件夹筛选按钮也跟着发生异常颜色变化的问题。
- 首页右下角入口、空状态入口和新建配置弹窗统一使用“新对话”文案。
- 首页空状态的“新对话”入口复用同一套液态玻璃按钮样式，不再和首页右下角入口割裂。
- 历史记录消息搜索结果的预览文本不再因为省略号拼接优先级错误而只显示 `...`。
- 将模型标签、默认 API 标签和附件状态从伪按钮改为非交互状态标签，避免出现点击无响应的控件。

### 技术实现细则
- `EchoWallpaperBackground` 改为仅把壁纸/主题背景层注册为 Haze 源，前景页面内容不再参与玻璃采样。
- `HomeScreen`、`ChatScreen`、`SettingsScreen` 都采用独立背景采样层，列表、消息和主要内容不再作为 Haze 源。
- `echoLiquidGlassOverlay` 的高光和折射层改为先绘制，内容后绘制，确保文字、图标和统计数据始终位于玻璃高光之上。
- 抽取 `NewConversationGlassButton`，统一首页浮动入口和空状态入口的玻璃材质、圆角、文字和图标。
- `HomeGlassChip` 的内容色区分选中/未选中状态，只有当前筛选项显示主题主色。

### 验证
- `:app:compileDebugKotlin` 通过。
- `:app:testDebugUnitTest` 通过（当前项目无 debug unit test 源，任务结果为 `NO-SOURCE`）。
- `:app:assembleDebug` 通过，生成 `app-arm64-v8a-debug.apk`。
- `:app:lintDebug` 运行到分析阶段后因 Android lint/Compose lint 工具链 metadata 版本不兼容崩溃，崩溃点为 `ComposableStateFlowValueDetector`，不是业务代码 lint 结果。

---

## v1.7.16 (2026-07-05) - 淡蓝玻璃体系、区域可读色与承托层修复
### 用户侧可见更新
- 首页设置、使用统计、历史记录和文件夹入口的液态玻璃颜色再次略微变淡。
- 文字可读性不再只按深色/浅色模式判断，会按顶部、内容区和底部背景分别选择黑色或白色。
- 对话页顶部工具栏和输入框改为与我的对话气泡一致的淡蓝玻璃色。
- 我的对话气泡和思考过程气泡加入液态玻璃效果。
- 输入框内附件、状态、按钮和智能搜索不再出现偏淡白块。
- 使用统计、历史记录和对话设置增加半透明承托底，避免只剩模糊导致文字和图表看不清。
- 智能搜索开关加入更明显的选中边框和蓝色状态。
- 对话页工具栏中的对话名称字号调大一号。

### 技术实现细则
- `ReadableColors.kt` 从单一整图平均采样升级为顶部、内容区、底部分区采样，分别服务工具栏、消息区和输入栏的可读色计算。
- `EchoHaze.echoHazePanel` 放宽低 alpha tint 支持，允许首页入口真正变淡；`EchoGlassDialog` 新增 `tint`、`containerColor`、`contentColor` 参数，用于保证弹窗玻璃可读性。
- `ChatScreen` 新增 `ChatUserGlassTint = #D9ECFF`，顶部工具栏、输入框、用户气泡、附件预览和输入栏内部控件统一使用同一淡蓝玻璃色系。
- 用户消息气泡和思考过程气泡接入 `echoHazePanel`，保留圆角和边框，同时获得真实背景模糊与液态玻璃高光。
- `ChatInputBar` 将智能搜索、附件状态、添加按钮、发送按钮禁用态和附件预览底色统一为淡蓝玻璃；智能搜索选中态增加蓝色承托和边框。
- `StatsScreen` 和 `HistoryScreen` 将主要卡片、搜索框、图表容器和列表项从透明玻璃改为半透明 `surface` 承托层叠加 Haze，解决只有模糊没有可读底的问题。
- `ChatSettingsDialog` 使用更高 alpha 的弹窗承托底，并为模型选择、系统提示词、最大 token 输入框等控件显式设置可读文字色和输入框容器色。
- `ChatHeaderTitle` 字号从 `14.5sp / 18sp` 调整到 `15.5sp / 19sp`。

### 版本与构建
- `versionCode`: 67
- `versionName`: 1.7.16
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.15 (2026-07-04) - 玻璃可读性、背景对比与 Markdown 标题修复
### 用户侧可见更新
- 首页设置、使用统计、历史记录和文件夹入口的液态玻璃颜色略微变淡，保留蓝色图标体系。
- 消息、输入框、首页筛选、历史记录和使用统计页面文字会按当前背景自动选择黑色或白色中更易读的一种。
- 思考过程气泡中的文字、心理图标、复制图标和展开图标统一改为蓝色。
- 对话页顶部工具栏和输入框使用一致的玻璃颜色，颜色深度介于上一版两者之间。
- 修复模型输出标题中粗体 Markdown 可能显示为 `**标题**` 的问题。
- 输入框内的白色色块改为透明或同色玻璃底，不再和蓝色玻璃输入框冲突。
- 使用统计和历史记录页面的玻璃承托层更清晰，文字、搜索框、卡片和图表不再被壁纸背景吞掉。

### 技术实现细则
- `ReadableColors.kt` 新增壁纸采样能力，按当前背景图的采样色与透明玻璃 tint 合成后计算黑/白文字对比度。
- `ChatScreen` 将顶部工具栏与输入框统一使用 `ChatGlassTintAlpha = 0.22f`，并将工具栏、消息、输入框、附件预览的可读色底色切换为采样背景。
- `ChatInputBar` 内的智能搜索胶囊、附件状态、附件预览、禁用按钮底色改为输入框同色系透明玻璃，移除 `surfaceVariant` 白灰块。
- `MessageBubble` 的模型回复文本不再按主题 surface 判断可读色，而是按当前对话页背景判断；用户气泡按淡蓝气泡叠加背景后判断。
- `MarkdownText` 标题行改用 `InlineMarkdownText`，粗体分支递归解析内部 Markdown，避免标题中的 `**...**` 原样显示。
- `HomeScreen` 为创建对话按钮、搜索框和文件夹筛选 chip 接入采样背景可读色；首页入口玻璃 alpha 从上一版略微下调。
- `StatsScreen` 和 `HistoryScreen` 将主要面板改为半透明 `surface` 承托层，并为搜索框、历史卡、搜索结果卡、统计摘要、图表和模型明细设置更稳的内容色。

### 版本与构建
- `versionCode`: 67
- `versionName`: 1.7.15
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.14 (2026-07-04) - 可读文字颜色与玻璃细节修复

### 用户侧可见更新

- 思考过程气泡中的文字和图标改为更清晰的深蓝色。
- 消息文字与输入框文字不再只按深色/浅色主题判断，而是按当前背景颜色自动选择黑色或白色中更易读的一种。
- 首页设置、使用统计、历史记录和文件夹入口的液态玻璃颜色略微加深。
- 对话输入框内部文字区域改为透明，并移除输入框玻璃层中的白色高光矩形。

### 技术实现细则

- 新增 `ReadableColors.kt`，通过 WCAG 对比度公式在黑色/白色候选文字色之间选择更高对比度颜色。
- `MessageBubble` 的用户消息、模型消息文本改为按气泡背景计算可读文字色，不再只依赖主题深浅。
- 思考过程气泡保留淡蓝底色，文字、心理图标、复制与展开图标在浅背景下使用深蓝色；若背景变暗则自动切换到浅蓝白色。
- `ChatInputBar` 的输入文字、占位文字和光标颜色改为按输入框玻璃 tint 计算可读颜色。
- `ChatInputBar` 的 `BasicTextField` 与内部 `decorationBox` 显式使用透明背景，并关闭输入框玻璃层的顶部白色高光。
- 首页 `StatsIconButton`、`GlassHomeIconButton`、创建对话按钮和文件夹筛选 `HomeGlassChip` 的玻璃 tint 从浅表面色改为更深的主题蓝 tint。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.14
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.13 (2026-07-04) - 弹窗玻璃统一、统计优化与流式滚动修复

### 用户侧可见更新

- 对话设置弹窗、上下文弹窗、备份恢复/删除确认弹窗统一为更圆润的玻璃风格。
- 历史记录页、文件夹管理页、使用统计页统一使用首页壁纸和玻璃卡片体系。
- 使用统计图表可读性提升，Token 消耗图新增“其他”分类，避免未知分项被误显示为输出。
- 设置系统提示词时，点击输入框不再自动把对话设置列表滚回顶部。
- 对话页顶部对话名称字号进一步减小。
- 流式输出只有当前页面停留在底部时才自动跟随；用户上滑阅读时不会被新 token 强制拉回底部。
- 独立“生成中”标志已删除，流式输出三点颜色改为淡蓝色。

### 技术实现细则

- 新增 `EchoWallpaperBackground`、`EchoGlassDialog`、`EchoGlassDialogShape`、`EchoGlassPagePanelShape`，统一页面壁纸、玻璃弹窗和页面卡片圆角。
- `ChatSettingsDialog` 与 `ContextUsageDialog` 从默认 `AlertDialog` 迁移到统一玻璃 `Dialog + Surface` 容器。
- `BackupItemCard` 新增恢复与删除确认弹窗，并复用 `EchoGlassDialog`，备份说明卡和备份项接入 `echoHazePanel`。
- `HistoryScreen`、`FolderManagerScreen`、`StatsScreen` 接入首页壁纸和 Haze 背景采样，顶栏改为透明，主要列表项改为玻璃 `Surface`。
- `ChatSettingsDialog` 为设置列表增加稳定 `LazyListState`，避免系统提示词输入框聚焦/编辑时因重组回到顶部。
- `ChatScreen` 的流式自动滚动改为底部跟随策略：手动滑动时立即停止自动跟随，当前位置离底部时不触发滚动，流式更新使用节流后的 `animateScrollToItem`。
- 删除顶部 `ChatHeaderTitle` 中的“生成中”状态胶囊，标题字号调整为 `14.5sp / 18sp`。
- 删除空响应阶段独立加载气泡，保留消息内流式三点，并将三点颜色改为 `#93C5FD`。
- `StatsScreen` 读取统计数据时对负数 token 清零、缓存 token 限制在输入 token 范围内、总 token 取记录总数与已知分项之和的较大值。
- 对无法归入输入/输出/思考的 token 增加 `otherTokens`，Token 柱状图单独绘制“其他”分段，并使用更清晰的整数刻度上限。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.13
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.12 (2026-07-04) - 首页与设置 UI 统一、主题选择

### 用户侧可见更新

- 首页顶部不再固定白色背景，会跟随首页背景图片一起显示。
- 首页设置、使用统计、历史记录、对话和选中文件夹入口统一为液态玻璃效果。
- 首页玻璃入口中的图标保持原来的样式，颜色统一改为协调蓝色。
- 设置界面使用和首页一致的背景图片。
- 设置界面一级菜单改为液态玻璃卡片。
- 设置界面二级菜单圆角统一。
- 新增应用主题选择：浅色、深色和跟随系统。

### 技术实现细则

- `HomeScreen` 的 `Scaffold` 与顶部 `HomeDashboardHeader` 去除硬编码白底，改为透明层叠在首页背景之上。
- 首页统计、设置、历史与文件夹筛选入口复用 `echoHazePanel`，入口内容颜色统一使用主题 `primary` 蓝色。
- `SettingsScreen` 增加全屏背景层，读取 `BackgroundImageManager.getHomeBackgroundBitmap()`，与首页共用同一张壁纸。
- `SettingsMenuItem` 从普通 `Card` 改为液态玻璃 `Surface`，使用统一 `SettingsPanelShape`。
- 设置二级页常见 `Card` 统一使用 `SettingsPanelShape`，输入框与背景选择行统一使用 `SettingsInnerShape`。
- 新增 `ThemePreferenceManager` 与 `AppThemeMode`，使用 SharedPreferences 保存主题模式。
- `MainActivity` 用主题偏好驱动 `AiApiAssistantTheme`，支持浅色、深色和跟随系统即时切换。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.12
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.11 (2026-07-04) - 玻璃性能、1M 上下文与对话细节修复

### 用户侧可见更新

- 液态玻璃颜色加深，不再过白，同时降低模糊采样压力以改善滚动帧率。
- 对话页标题进一步缩小。
- 思考过程气泡内文字和图标改为黑色，和用户气泡风格一致。
- 思考过程气泡与模型头像顶部对齐。
- 上下文限制识别不再把未知 1M 模型误判为 32k 或 64k。
- 输入框内不和谐的白色方块已移除。
- 我的输入气泡下方时间与右侧功能图标重新对齐。
- 输入和输出消息时间显示到月日。
- 使用统计页面不再从状态栏顶部开始显示。
- 首页文件夹、历史记录、设置和使用统计入口改为液态玻璃效果。

### 技术实现细则

- `EchoHaze` 默认 tint 改为更深的 `surfaceVariant`，默认 blur 从 32dp 降到 22dp，noise 降到 0.018。
- 对话输入栏单独使用 18dp blur，并移除输入框内部 `BasicTextField` decoration 的额外 clip 层，避免产生白色矩形感。
- `ChatHeaderTitle` 标题文字调整为 16sp / 20sp lineHeight。
- `MessageBubble` 取消模型消息内容顶部 2dp 偏移，让思考气泡与模型头像顶部对齐。
- `MessageFooter` 改为垂直居中布局，并将消息时间格式从 `HH:mm` 改为 `MM-dd HH:mm`。
- `AiRepository` 拆分模型列表上下文缓存与运行时超限降级缓存；未知或低置信度模型不再按模型族硬编码成 32k/64k。
- `parseContextWindowFromText` 在同一模型名中优先取最大的上下文数值，避免 `1m` 与 `64k` 同时出现时被小值覆盖。
- `StatsScreen` 顶栏增加 `statusBarsPadding()`。
- `HomeScreen` 顶部设置/统计入口、历史入口和文件夹筛选栏统一接入液态玻璃胶囊/圆形按钮。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.11
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.10 (2026-07-04) - 液态玻璃透明度与对话页细节修复

### 用户侧可见更新

- 液态玻璃控件整体更透明，输入框内不再出现明显白色矩形感。
- 首页对话卡片和设置菜单选项改为更圆润的边缘。
- 思考过程气泡改为和我的对话气泡一致的淡蓝色。
- 思考耗时和思考 token 都移动到思考过程气泡标题行内显示。
- 思考过程、滑动导航栏等点击反馈改为控件自身响应，不再出现不协调的矩形色块。
- 修复一键到顶/到底按钮和导航栏展开面板的圆角阴影表现。
- 对话页上下文圆环去除蓝色背景，并和返回键、设置键在顶栏内居中。
- 首页导航栏展开后，点击 Echo 标志等顶部区域也会自动收起。

### 技术实现细则

- `EchoHaze` 降低玻璃 tint、噪声和高光 alpha，并把顶部/底部高光从矩形改为圆角高光。
- 新增 `echoShapeClick`，使用控件 shape 内的按压高光替代默认矩形 ripple。
- `ChatScreen` 顶部栏从 `TopAppBar` 槽位改为显式居中的 56dp 胶囊 Row，保证返回、上下文、设置按钮垂直居中。
- `ContextUsageButton` 去除外层蓝色背景，仅保留圆环和主动压缩提示点。
- `MessageBubble` 将思考过程气泡改为用户气泡同色系，并把 `responseTime` 与 `thinkingTokens` 放入气泡标题行。
- `MessageFooter` 对已进入思考气泡的信息不再重复显示思考 token。
- `SideAnchorNavigator` 的收起态、展开项和收起按钮改为圆角内响应；展开面板阴影按圆角 shape 绘制。
- 首页导航栏浮层提升到首页根层，展开后顶部 header 区域也能点击收起。
- 首页对话卡片圆角从 18dp 调整到 30dp，设置菜单主选项圆角调整到 28dp。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.10
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.9 (2026-07-04) - Kotlin/Compose 升级与液态玻璃控件

### 用户侧可见更新

- 首页搜索栏、创建对话按钮、对话页顶部工具栏和输入框升级为液态玻璃效果。
- 玻璃控件现在包含真实背景模糊、渐变高光、边缘亮线和轻微阴影，不再只是灰色边框。
- 应用构建链升级到 Kotlin 2.x 与新版 Compose BOM，为后续 Compose UI 能力升级打基础。

### 技术实现细则

- Kotlin Gradle 插件由 `1.9.20` 升级到 `2.2.21`。
- Android Gradle Plugin 由 `8.2.0` 升级到 `8.10.1`，Gradle Wrapper 由 `8.5` 升级到 `8.11.1`，以匹配 Kotlin 2.2 对 R8/D8 的最低版本要求。
- Kotlin 编译策略固定为 `in-process`，避免 Windows 受限用户目录下 Kotlin daemon 无权限后再回退。
- 新增 `org.jetbrains.kotlin.plugin.compose`，迁移到 Kotlin 2.x 推荐的 Compose Compiler Gradle 插件路径。
- KSP 插件同步升级到 `2.2.21-2.0.4`，避免 Room 注解处理停留在旧 Kotlin 1.9 编译链。
- Room 从 `2.6.1` 升级到 `2.8.4`，并启用 `room.generateKotlin=true`，修复旧 Room KSP 处理器在 Kotlin 2.x 下的签名解析崩溃。
- Compose BOM 从 `2023.10.01` 升级到 `2024.06.00`，测试依赖同步使用同一 BOM；该版本仍匹配当前已安装的 Android 34 SDK。
- 移除 `composeOptions.kotlinCompilerExtensionVersion = "1.5.5"`，避免 Kotlin 2.x 下继续维护旧式 Compose 编译器扩展版本。
- 继续使用现有 Haze 背景采样与模糊基础，避免引入要求更高 compileSdk/AGP 的新版传递依赖。
- `EchoHaze` 增加统一液态玻璃叠层：模糊采样、半透明 tint、非均匀高光、径向亮斑、底部轻阴影和渐变边缘亮线。
- 液态玻璃仅应用在稳定悬浮控件上，未重新加到滚动消息气泡，避免列表复用导致模糊层漂移。
- 首页创建对话按钮接入 Haze 背景采样，保持与搜索栏、对话顶部栏、输入框一致的玻璃材质。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.9
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat --no-daemon assembleRelease`

---

## v1.7.8 (2026-07-04) - 标题居中、上下文识别与思考信息修复

### 用户侧可见更新

- 对话页顶部标题恢复更大的字号，并在胶囊工具栏内垂直居中。
- 模型最大上下文识别改为动态读取模型列表元数据；识别不到时默认按 1M 上下文处理。
- 当实际模型上下文小于默认值并触发 API 上下文超限时，应用会自动缩小窗口、重组上下文并重试，尽量避免直接报错。
- 模型回复底部“思考”数字补充 `tokens` 单位。
- 模型响应耗时移入蓝色“思考过程”气泡，显示在“思考过程”右侧，不再挤在消息底栏。

### 技术实现细则

- `ChatHeaderTitle` 改为填满工具栏高度并使用 `titleLarge`，生成状态仅在需要时占用第二行。
- `/models` 原始 JSON 增加动态解析：
  - 支持 `data`、`models`、`model` 数组。
  - 支持 `context_length`、`context_window`、`max_context_length`、`max_model_len`、`max_position_embeddings`、`n_ctx` 等常见字段。
  - 支持数字字段和 `128k`、`1m` 等字符串字段。
  - 支持从 `metadata`、`limits`、`capabilities`、`model_info`、`config`、`parameters` 等嵌套对象继续提取。
- 未识别到上下文窗口时，默认模型窗口从保守 32k 调整为 1M。
- `ChatRequestOptions` 增加 `contextWindowOverrideTokens`，用于上下文超限后的运行时降级重试。
- 捕获上下文/token 超限类 API 错误后，会缓存较小上下文窗口、主动压缩上下文并重试一次。
- 蓝色思考气泡标题行右侧显示 `responseTime`，消息底栏对带思考内容的模型回复不再重复显示耗时。
- 消息底栏思考 token 文案由 `思考: 数字` 改为 `思考: 数字 tokens`。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.8
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat assembleRelease`

---

## v1.7.7 (2026-07-04) - 对话 UI 稳定性与上下文修复

### 用户侧可见更新

- 修复对话页中用户消息气泡文字不可见的问题。
- 修复上下滑动时用户消息气泡模糊层错位、漂移的问题。
- 上下文使用情况入口外层背景恢复为正圆。
- 上下文预算不再被 64k 上限截断，长上下文模型的显示更准确。
- 主动压缩按钮任何时候都可点击，即使当前没有新的旧消息可压缩，也会刷新上下文状态。
- 模型输出底部信息重新排版，思考 token 不再挤压功能键；复制、重生成、编辑、删除按钮固定在底栏最右侧。
- 应用内容支持延伸到手机顶部状态栏区域。
- 首页创建对话按钮移除异常白色矩形。
- 首页对话导航栏改为仅在滑动时出现。

### 技术实现细则

- 用户消息气泡不再把 Haze 子层挂在滚动列表项上，改为稳定的半透明淡蓝 Surface 和浅色描边，避免滚动采样错位。
- 消息底栏拆分为左侧 `FlowRow` 元信息区和右侧固定操作区，避免长 token 文本覆盖按钮。
- 上下文圆环按钮调整 modifier 顺序，先留间距再固定 40dp 尺寸，保证外层为正圆。
- 主动压缩弹窗按钮只在压缩进行中禁用；仓库层移除 `canCompress=false` 时的提前返回。
- `estimatePromptBudgetTokens` 取消 64k 固定上限，并按当前模型上下文窗口动态计算可用输入预算。
- `gpt-4.1` 系列上下文窗口估算调整为百万级，其他模型仍优先使用模型列表返回的上下文窗口元数据。
- `MainActivity` 和主题层启用 edge-to-edge，状态栏/导航栏透明并按主题设置系统栏图标颜色。
- 首页创建对话按钮移除 Haze 子层，避免按钮内部出现异常白色矩形。
- 首页 `SideAnchorNavigator` 接入滚动可见状态，和对话页保持一致。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.7
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat assembleRelease`

---

## v1.7.6 (2026-07-04) - 系统提示词即时生效复核版

### 用户侧可见更新

- 系统提示词在对话设置中保存后，会在下一次发送或重新生成时立即生效。
- 关于页和 README 同步显示 `v1.7.6`。
- 继续保留 v1.7.5-ui3 的上下文圆环、对话设置收拢、自定义背景、消息气泡磨砂和表格修复能力。

### 技术实现细则

- `ChatRequestOptions` 新增系统提示词覆盖字段，本次请求优先使用界面中的最新系统提示词。
- 对话设置保存改为一次写入温度、输出 token、top-p、思考设置、联网搜索和系统提示词，避免异步保存乱序导致旧提示词回写。
- 任意对话设置写入都会保留当前系统提示词，避免切换联网搜索等操作覆盖提示词。
- 系统提示词包装文案明确“从本轮请求开始立即生效；若与较早对话内容冲突，以这里为准”。
- 模型列表返回上下文窗口元数据时，会优先使用该值作为当前模型最大上下文限制；无元数据时继续使用模型名解析和模型族估算。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.6
- 说明：`versionCode` 暂不递增，用于保持同签名测试包可回退安装。
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat assembleRelease`

---

## v1.7.5-ui3 (2026-07-04) - 对话设置收拢、上下文圆环与自定义背景

### 用户侧可见更新

- 对话页上下文使用情况改为圆环入口，深色圆弧表示已占用比例，顶栏不直接显示具体数值。
- 上下文详情弹窗将“输入预算”改为“最大上下文限制”，并显示当前模型估算上下文窗口。
- 模型切换从输入框下方移入“对话设置”顶部。
- 系统提示词移入“对话设置”，放在温度设置上方；对话设置默认生效，不再提供开关。
- 输入框下方删除模型切换入口，保留智能搜索、附件、图片和 OCR 入口。
- 模型回复底部耗时和思考 token 文案保持原样，仅将元信息行改为可换行排版，避免文字被覆盖。
- Markdown 表格渲染修复：
  - 支持 `\|` 转义竖线。
  - 避免代码片段中的 `|` 被误判为列分隔符。
  - 同一表格按统一列宽渲染，减少列错位。
- 设置的个性化页面新增首页背景和对话页背景图片设置，可分别选择和恢复默认。
- 用户消息气泡改为淡蓝色、黑色文字，并接入磨砂背景效果。
- 对话页一键到顶/一键到底按钮会避让底部输入框磨砂区域。
- 设置关于页调整为：
  - Echo 图标下方显示版本号。
  - “本次更新”逐行显示。
  - “功能特性”放在本次更新下方，并用独立卡片区分。

### 技术实现细则

- `ConversationContextUsage` 新增 `contextWindowTokens` 字段，用于区分模型最大上下文窗口和实际可用输入预算。
- `AiRepository` 增强模型上下文窗口估算：
  - 优先解析模型名里的 `k/m` 显式窗口标记。
  - 对 Gemini、Claude、GPT、Qwen、DeepSeek、Kimi、MiMo 等常见模型族做保守估算。
- `ChatScreen` 重构对话入口：
  - 顶栏移除独立系统提示词按钮。
  - `ChatSettingsDialog` 增加模型选择和系统提示词编辑区。
  - 输入栏移除模型选择器。
- `MarkdownText` 重写表格行拆分与列宽计算，减少表格渲染错列。
- 新增 `BackgroundImageManager`，将首页/对话页背景图压缩后保存到应用私有目录。
- `SettingsScreen` 个性化页新增背景图片选择与恢复入口。
- `README.md` 已按当前项目状态重写。

### 版本与构建

- `versionCode`: 67
- `versionName`: 1.7.5
- Room 数据库版本：17
- ABI：arm64-v8a
- 构建命令：`.\gradlew.bat assembleRelease`

### OCR 评估

- 当前仍保留 ML Kit Text Recognition 作为默认 OCR，实现简单、稳定，适合 Android 端直接选择图片识别。
- 后续可评估 PaddleOCR / RapidOCR / Tesseract4Android 作为高级可选 OCR 引擎，但不建议在本次更新中直接替换默认 OCR，以免引入包体、模型和兼容性风险。

---

## v1.7.5 (2026-07-04) - 上下文用量、主动压缩与导航优化

- 对话页新增上下文用量入口。
- 新增上下文使用情况弹窗和主动压缩按钮。
- 对话导航栏仅在页面滑动时出现，展开后点击窗口外可自动收起。
- 使用统计入口图标尺寸调整。
- APK 命名改为短格式：`Echo-v版本号.apk`。

---

## v1.7.4 (2026-07-04) - 可读性与更新细则补全

- 仅整理代码可读性、注释和更新说明。
- 不改变 API 调用逻辑、上下文策略、数据库结构、UI 流程或用户可见功能。

---

## v1.7.3 (2026-07-04) - Token 预算上下文、滚动摘要与长期记忆

- 新增 token 预算式上下文组装。
- 新增滚动摘要。
- 新增长期记忆表。
- Room 数据库版本升至 17。

---

## 维护要求

每次更新必须：

1. 更新代码和必要文档。
2. 构建 release APK。
3. 验证 APK 包名、版本号、ABI、签名和 SHA-256。
4. 提交 Git commit。
5. 推送到 GitHub。
6. 最终回复写明 APK 路径、SHA-256、提交号和网络备份状态。
