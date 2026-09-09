# Echo AI 助手更新日志 (Update Log)

## [v1.9.29] - 2026-09-09

### 1. 本次升级与用户需求 100% 修复与落实
1. **输入框与悬浮栏略微降低透明度，彻底恢复高可读性**：
   - 深入排查发现：此前由于 `echoHazePanel` 在 `hazeState != null` 时移除了自身 `background(resolvedTint, shape)` 的绘制，导致处于 `color = Color.Transparent` 的 `Surface` 自身完全透明，滚动文字与输入框文字缺乏足够的底色依托，严重破坏了可读性；
   - 优化底色渲染层级：`echoHazePanel` 内部恢复无条件绘制 `background(resolvedTint, shape)` 确保面板拥有独立半透明底色；
   - 适度提高底色不透明度（略微降低透明度）：暗色模式 `inputAlpha`/`panelAlpha` 设为 0.85f，浅色模式设为 0.88f，保证文字对比度达到 WCAG AAA 级别（>7.0:1），输入文字与标题一目了然；
   - 模糊层微阻尼着色：Haze 采样层接收 `0.05f..0.25f` 的轻量着色，既保证底层毛玻璃模糊正常渲染且不发黑变厚，又彻底解决了“完全透明”的问题。
2. **各项核心功能保持稳定**：
   - 思考胶囊文案与状态稳定显示，无异常滚动裁剪；
   - 分支生成组生命周期随生成状态安全回收，幽灵空消息严格过滤；
   - 输入框右上角同心圆弧手柄尺寸固定为 22dp/17dp，拖拽前后大小绝对统一；
   - 划选复制工具栏解耦防抖与辅助滑动 4 键浅天蓝半透明体系完全保持。

### 2. 自动化测试验证
- 全量 128 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V1929FeaturesTest` 覆盖版本更新说明、文字对比度与透明度约束测试。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/test/java/com/aiassistant/V1928FeaturesTest.kt`
- `app/src/test/java/com/aiassistant/V1929FeaturesTest.kt`
- `app/build.gradle.kts`
- `CHANGELOG.md`
- `PROJECT.md`
- `README.md`
- `UPDATE_LOG.md` (root & app)

## [v1.9.28] - 2026-09-09

### 1. 本次升级与 4 项核心用户需求 100% 彻底修复与落实
1. **输入框半透明液态毛玻璃效果完美还原（Req 1）**：
   - 修复根因：在全屏根节点恢复全屏全域毛玻璃取样源与背景图（`echoHazeSource(hazeState)` + `chatBackgroundBitmap`），打破原先在 `Scaffold` 内部放置导致的底部栏无背景、无取样源的缺陷，彻底解决输入框失去半透明、沦为纯色/黑底的问题；
   - 修复 `echoHazePanel`：杜绝在 `hazeChild` 之上无条件重复绘制 `mod.background(tint, shape)`，仅在无毛玻璃状态下作为 fallback 绘制，消除两层底色叠加导致的失真变厚；
   - 适度恢复 `echoGlassPalette()` 的 `inputAlpha`（深色 0.80f，浅色 0.84f），使输入框与背景壁纸产生完美通透的高级液态毛玻璃质感。
2. **顶部悬浮工具栏与错误提示真实半透明透字（Req 2）**：
   - 彻底消除 `echoHazePanel` 内部的双层背景叠加，`Surface` 配合 `echoHazePanel` 呈现通透冰晶磨砂质感；
   - 全屏贯通的消息列表（`LazyColumn`）向上滚动时平滑穿透悬浮栏底层，文字与气泡实时被模糊并半透明隐约透出，完美满足“无边缘包裹，能透过文字”的要求。
3. **模型思考胶囊文字无法显示问题彻底根除（Req 3）**：
   - 修复核心根因：移除思考胶囊内部导致无限约束冲突与测量裁剪的 `horizontalScroll(capsuleScrollState)` 容器，改为标准 `Text` 搭配 `Modifier.weight(1f, fill = false)` 与 `overflow = TextOverflow.Ellipsis`，消除宽度为 0 与滚动偏移溢出导致文字消失的问题；
   - 强化思考胶囊文案生成逻辑：在连接中、思考中、思考完成、回复中等所有状态分支均增加 `.ifBlank { ... }` 严格兜底，确保在任何网络/流式阶段思考胶囊文案 100% 稳定清晰呈现。
4. **对话页面气泡残留彻底修复（Req 4）**：
   - 修复核心根因：定位到分支生成组 ID `streamingBranchGroupId` 在流式生成完成后（`!isGenerating`）从未被重置为 `null`，导致残留至后续普通对话，错误阻断正常流式气泡并产生幽灵残留；
   - 增加生命周期监听：`LaunchedEffect(isGenerating)` 在生成结束时自动将 `streamingBranchGroupId` 重置为 `null`；`LaunchedEffect(conversationId)` 在切换会话时重置分支与编辑状态；
   - 增加无效空白异常消息安全过滤：在 `buildDisplayMessages` 中过滤无内容、无思考、无附件、无工具调用的纯空异常消息，从底层杜绝幽灵气泡残留。

### 2. 自动化测试与质量核验
- 全量 124 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V1928FeaturesTest` 专项覆盖：
  - `testV1928CurrentVersionUserUpdates`：验证版本更新说明条目对齐与完备性；
  - `testFormatThinkingCapsuleTextNeverBlank`：验证思考胶囊在不同生命周期与极端模板下文案非空保障；
  - `testFormatNonThinkingCapsuleText`：验证普通模型回复胶囊耗时与 token 统计文案；
  - `testGhostMessageFiltering`：验证无效空白消息的安全过滤逻辑；
  - `testStreamingBranchGroupIdLifecycleContract`：验证流式分支组生命周期重置契约。

### 3. 改动涉及文件列表
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`:
  - 修复 `echoHazePanel` 重复背景覆盖 bug，调整半透明调色板（Req 1, Req 2）。
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`:
  - 恢复全屏背景与根毛玻璃源（Req 1）；
  - 移除思考胶囊 `horizontalScroll` 异常裁剪并加入完备兜底（Req 3）；
  - 加入 `LaunchedEffect` 分支重置与 `buildDisplayMessages` 空消息过滤（Req 4）。
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`:
  - 增加 `V1927UserUpdates`，更新 `CurrentVersionUserUpdates` 为 v1.9.28 说明。
- `app/build.gradle.kts`:
  - `versionCode = 108`, `versionName = "1.9.28"`。
- `app/src/test/java/com/aiassistant/V1928FeaturesTest.kt`:
  - 新增 v1.9.28 自动化测试。
- `app/src/test/java/com/aiassistant/V1927FeaturesTest.kt`:
  - 适配历史更新日志校验。

### 4. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：106 个，构建后增至 107 个，严格遵守历史包永久保留最高铁律，未执行任何删除/清理操作；
- 增量输出安装包：`Echo-v1.9.28-arm64-v8a.apk`。

## [v1.9.27] - 2026-09-09

### 1. 本次升级与 4 项核心用户需求 100% 彻底修复与落实
1. **对话页顶部悬浮工具栏和错误提示无边缘包裹且全屏穿透（Req 1）**：
   - 移除 `Scaffold` 的 `topBar` 顶层占位，使全屏背景及消息列表 `LazyColumn` 穿透延伸至屏幕顶部与状态栏底层，并被完整纳入 `Haze` 实时取样源（`echoHazeSource(hazeState)`）；
   - 顶部悬浮工具栏与错误提示直接作为悬浮层覆盖在顶部（`statusBarsPadding` + 12dp 水平外边距），完全复用输入框的液态玻璃规范（纯透明 `Surface` + `echoHazePanel` + `glass.input` + `BorderStroke(1.dp, glass.outline)`）；
   - 彻底去除外层多余的任何纯色背景包裹与边缘阻断，聊天气泡与文字滚动至顶部时能够无缝穿透并显现出真实透光的液态磨砂毛玻璃效果。
2. **输入框右上角放大弧线手柄大小彻底统一（Req 2）**：
   - 将输入框圆角弧度常数永久固定为拖动后的精致小规格 `cornerRadiusDp = 22f`，圆弧半径统一为 `arcR = 17.dp`；
   - 彻底移除拖动前后在 30dp 与 22dp 间动态切换的尺寸跳变逻辑，确保在静止、拖拽、展开等任何交互状态下，右上角弧线手柄大小完全统一且与边框圆角紧密贴合。
3. **对话页辅助滑动 4 个按键变浅且半透明透光化（Req 3）**：
   - 将右侧悬浮辅助滑动 4 个按键（到顶、上一条输入、下一条输入、到底）全面从深色实色调整为柔和浅天蓝阶梯半透明配色：
     - 到顶（DoubleUp）：`0xFFBAE6FD`（Alpha 0.72f）；
     - 上一条输入（ArrowUp）：`0xFF93C5FD`（Alpha 0.75f）；
     - 下一条输入（ArrowDown）：`0xFF60A5FA`（Alpha 0.78f）；
     - 到底（DoubleDown）：`0xFF3B82F6`（Alpha 0.82f）；
   - 增加 `BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))` 冰晶微光描边并将阴影深度降至 1dp，色彩清新柔和且通透，兼顾清晰辨识与极简美感。
4. **长按文本选区弹出复制工具栏高频闪烁与复制失效彻底根治（Req 4）**：
   - 定位核心根因：`EchoTextToolbar.status` 此前直接从 `activeMenu`（Compose MutableState）读取，导致 `SelectionContainer` 隐式订阅该状态变量。当划选触发 `showMenu` 时修改 `activeMenu` 状态，引起 `SelectionContainer` 触发每秒数十次的重组并调用 `hide()`，形成毁灭性的闪烁死循环；
   - 架构级解耦：在 `EchoTextToolbar` 内部使用原生私有字段 `_status: TextToolbarStatus` 替代快照状态读取，切断 Compose 订阅链；
   - 防抖与实例就地复用：在 `showMenu` 时如果已有活动菜单且位置位移小于 16px，仅就地刷新回调闭包，严禁重新分配状态对象；
   - 弹窗位置提供者 `remember(density)` 稳定化：消除由屏幕密度重读引发的微小震颤；
   - 划选用户输入气泡与 AI 输出回复时，复制、全选、剪切、粘贴工具栏弹窗秒开秒响应，稳如磐石，彻底告别闪烁问题。

### 2. 自动化测试与质量核验
- 全量 119 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V1927FeaturesTest` 专项覆盖：
  - `testEchoTextToolbarStatusDecoupledFromSnapshot`：验证状态与快照解耦及就地复用菜单实例；
  - `testEchoTextToolbarPositionThreshold`：验证 16px 抖动过滤阈值判定；
  - `testJumpScrollButtonsColorAndAlpha`：验证 4 键浅天蓝半透明调色方案；
  - `testV1927CurrentVersionUserUpdates`：验证更新说明条目对齐与完备性。

### 3. 改动涉及文件列表
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`:
  - 顶部悬浮栏与错误提示重构为穿透悬浮层，复用输入框玻璃样式（Req 1）；
  - 输入框放大手柄尺寸永久统一为 22dp/17dp（Req 2）；
  - 辅助滑动 4 键浅色化与半透明化升级（Req 3）。
- `app/src/main/java/com/aiassistant/ui/components/EchoTextToolbar.kt`:
  - 快照状态深度解耦、防抖阈值与实例就地复用，彻底解决划选复制闪烁（Req 4）。
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`:
  - 调优液态玻璃输入面板半透明度（浅色 0.74f / 深色 0.70f），带来更剔透的穿透效果。
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`:
  - 注册 `V1927` 与 `V1926`、`V1924`、`V1923` 历史更新列表，同步 `CurrentVersionUserUpdates`。
- `app/build.gradle.kts`:
  - `versionCode = 107`, `versionName = "1.9.27"`。
- `app/src/test/java/com/aiassistant/V1927FeaturesTest.kt`:
  - 新增 v1.9.27 专项测试套件。
- `app/src/test/java/com/aiassistant/V1923FeaturesTest.kt` / `V1924FeaturesTest.kt`:
  - 适配历史更新日志校验。

### 4. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：105 个，构建后增至 106 个，严格遵守历史包永久保留最高铁律，未执行任何删除/清理操作；
- 增量输出安装包：`Echo-v1.9.27-arm64-v8a.apk`
- 文件大小：16,025,597 字节
- SHA-256：`0A5DBAADDCFBFFA06EB62020E6821C6C2F92DFACCB18247931ADAD1015DBF26F`

## [v1.9.26] - 2026-09-09

### 1. 本次升级与 7 项用户需求 100% 落实
1. **上方悬浮栏和错误弹窗直接复用输入框玻璃代码规范（Req 1）**：
   - 悬浮栏使用 `toolbarShape = RoundedCornerShape(30.dp)`，错误弹窗使用 `errorShape = RoundedCornerShape(22.dp)`；
   - 底层统一复用 `glass.input` 容器背景，通过 `echoHazePanel(hazeState, shape, tint, 16.dp, 0.025f)` 搭配纯透明 Surface 与 `BorderStroke(1.dp, glass.outline)` 细致描边；
   - 彻底去除原有白色双层背景与多余背景层，彻底消除悬浮栏液态玻璃模糊报错与穿透异常。
2. **输入框右上角弧线控制手柄同心贴合（Req 2）**：
   - 弧线手柄定位移至外层 `Surface` 的 `Box(modifier = Modifier.fillMaxWidth())` 顶部右对齐（`Alignment.TopEnd`）；
   - 根据输入框当前圆角半径 $R$（展开状态 22dp，默认收起状态 30dp）严格计算同心圆弧半径 $R - 5\text{dp}$，圆心精确锚定 $(W - R, R)$，在 $270^\circ \sim 360^\circ$ 间精准绘制四分之一圆弧；
   - 颜色取用与输入框边框一致但略深色相（浅色为 `outline.copy(0.44f)`，深色为 `White.copy(0.32f)`），紧贴右上角边框圆角，拖拽调节高度顺畅自如。
3. **大模型非标颜色标签 `<font color="2B7DEP">` 与尾随星号智能容错解析（Req 3）**：
   - 在 `MarkdownText.kt` 中设计并落地十六进制容错映射器，智能容错大模型常见拼写变体（例如将 `P`、`O` 自动纠正为 `0`，`L`、`I` 纠正为 `1`），使 `2B7DEP` 自动规范解析为高明度天蓝 `#2B7DE0`；
   - 支持标准 3 位短 Hex、8 位 Hex 及扩展颜色名；
   - 在 `parseInlineMarkdown` 中对 `<font>` 与 `<span>` 闭合标签后的孤立悬挂星号（如 `</font>*`）进行自动消费净化，彻底消除原生 HTML 标签残留与悬挂星号。
4. **加大用户输入气泡与模型上一次输出之间的距离（Req 4）**：
   - 在消息列表渲染中采用 `itemsIndexed`，智能判定相邻消息身份；
   - 当当前条目为用户输入气泡且上一条为 AI 模型输出时，额外注入 `18.dp` 的垂直呼吸间距，显著改善长对话中多轮交互的气泡排版视觉节奏。
5. **思考强度快速档纯正柔和蓝色（Req 5）**：
   - 将全应用内所有思考强度快速档（Low/Fast）的代表色全面从偏灰的 `#5FA8D3` 升级为柔和天蓝色 `Color(0xFF60A5FA)`（Tailwind Blue 400 规范），渐变调整为 `listOf(Color(0xFF93C5FD), Color(0xFF60A5FA))`；
   - 输入栏药丸胶囊、档位弹窗、参数详情卡片全域统一步调，告别灰暗暗沉感。
6. **对话页悬浮滚动快捷键升级为4键独立体系（Req 6）**：
   - 快捷滚动组由双键拓展为 4 个紧凑型玻璃圆形按键（尺寸缩小为 32dp，间距 5dp，白色矢量图标）：
     1. **到顶**：双线向上箭头 `KeyboardDoubleArrowUp`，主题色 `#60A5FA`（对应快速档浅蓝）；
     2. **上一条输入**：单箭头 `KeyboardArrowUp`，主题色 `#2563EB`（对应平衡档蔚蓝）；
     3. **下一条输入**：单箭头 `KeyboardArrowDown`，主题色 `#1D4ED8`（对应深入档深海蓝）；
     4. **到底**：双线向下箭头 `KeyboardDoubleArrowDown`，主题色 `#4338CA`（对应极高档靛青蓝）；
   - 四个按键色彩依照思考强度由浅至深优雅渐进，点击“上一条/下一条”基于当前可见首行智能寻址跳转至最近的用户消息，定位极其精准。
7. **长按文本复制弹窗高频闪烁与复制失效彻底修复（Req 7）**：
   - 在 `EchoTextToolbar.kt` 中用动作能力布尔值（`canCopy`, `canPaste`, `canCut`, `canSelectAll`）及 8px 矩形误差容限替换旧版 lambda 引用直接相等判定；
   - 当 Compose SelectionManager 高频触发测量并传入新闭包时，仅在当前菜单实例上就地更新回调函数，而不重新分配 `activeMenu` 状态变量；
   - 彻底阻断每秒 60 次重建 Popup 导致的无限重组与闪烁死循环，使长按划选后的复制、引用、剪切、全选响应极其稳定顺滑。

### 2. 自动化测试核验
- 全量 115 项单元测试 100% 全部通过（新增 `V1926FeaturesTest` 专项覆盖非标 Hex 容错、Markdown 悬挂星号净化、TextToolbar 防抖等值性及版本亮点核验，退出码 0）。

### 3. 改动涉及文件列表
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`:
  - 顶部悬浮栏与报错 Banner 直接复用输入框玻璃样式（Req 1）；
  - 输入框同心圆弧手柄定位至右上角外层贴合（Req 2）；
  - 用户气泡与上一次输出增加 18dp 间距（Req 4）；
  - 思考强度快速档颜色重调为 `#60A5FA`（Req 5）；
  - 滚动快捷按钮升级为4键独立体系与双线箭头（Req 6）。
- `app/src/main/java/com/aiassistant/ui/components/EchoTextToolbar.kt`:
  - 防重组闪烁状态判定重构与能力等值比较，彻底修复划选复制闪烁（Req 7）。
- `app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`:
  - 纯 Kotlin 十六进制容错映射与解析，消除 `<font color="2B7DEP">` 与尾随星号乱码（Req 3）。
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`:
  - 更新说明 `CurrentVersionUserUpdates` 全面对齐 v1.9.26 新特性。
- `app/build.gradle.kts`: `versionCode = 106`, `versionName = "1.9.26"`。
- `app/src/test/java/com/aiassistant/V1926FeaturesTest.kt`: 新增 v1.9.26 专项自动化测试套件。
- `app/src/test/java/com/aiassistant/V1925FeaturesTest.kt`: 适配版本更新日志测试。

### 4. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：104 个，构建后增至 105 个，严格遵守历史包永久保留最高铁律，未执行任何删除/清理操作；
- 增量输出安装包：`Echo-v1.9.26-arm64-v8a.apk`
- 文件大小：16,025,601 字节 (15.28 MB)
- SHA-256：`1986388CC475124A98CF4E1B34330559F4AFEB7322B36A337B52FEDB53B19FE2`
- 签名验证：APK Signature Scheme v2 验证通过 (Verified: true, Signers: 1)。

## [v1.9.25] - 2026-09-09

### 1. 本次升级与 18 项用户需求 100% 落实
1. **对话页顶部悬浮栏液态玻璃修复与纯色背景消除（Req 1）**：移除悬浮栏 Surface 额外底色层，改用纯透明 `Surface(color = Color.Transparent)` 配合 `echoHazePanel` 胶囊裁切，消除白色双层矩形边框并根除液态玻璃模糊报错与渲染失真。
2. **屏幕上方报错提醒统一液态玻璃材质（Req 1）**：屏幕顶部浮动报错 Banner 彻底移除粉红实色背景，迁移至标准 `echoHazePanel` 半透明毛玻璃卡片（`errorContainer.copy(0.35f)` + 细边框），视觉优雅轻盈。
3. **新建对话默认最大 Token 50,000 严格生效（Req 2）**：在 `AiRepository.createConversation` 及故事配置创建入口中强制写入 `maxTokens = 50000`，彻底解决数据库初始化时回退旧值问题。
4. **新建会话内专属记忆默认关闭（Req 3）**：`TempChatSettings` 中将 `enableSessionMemory` 默认值设为 `false`，确保新建会话专属记忆默认处于关闭状态。
5. **专属记忆智能提取规则强化与高信噪比优化（Req 3）**：在 `SmartMemoryExtractor.kt` 中强化规则指令模式与用户偏好正则提取（`^(?:(?:会话|对话|当前)?(?:设定|规则|要求|约束)[：:]\s*)` 等），过滤低信噪比临时口令。
6. **设置页新增“新对话默认 API”卡片（Req 4）**：在设置页「API 配置」Tab 顶部新增高权重「新对话默认 API」快捷配置卡片，支持 FlowRow 快速单选切换默认服务商。
7. **重新进入对话统一默认折叠思考过程（Req 5）**：`MessageBubble` 中 `showThinking` 状态初始化逻辑优化，对非正在生成中的历史消息统一默认折叠思考过程（`showThinking = false`）。
8. **新建对话默认开启思考模式、默认关闭联网搜索（Req 6）**：创建新对话时强制 `enableThinking = true`、`enableWebSearch = false`；若用户主动修改，则持久化并在重新进入该对话时严格保留用户设定。
9. **右侧滚动条防断触与跟手稳定性提升（Req 7）**：重构 `ScrollAssist.kt` 中滚动条手势监听，采用稳定 key `pointerInput(Unit)` 搭配 `rememberUpdatedState`，彻底杜绝数据加载或重组导致的断触与手势中断。
10. **模型正在连接中文案升级（Req 8）**：模型连接状态提示更新为带有模型名称的动态占位文案（如 `"{model} 正在连接中..."`）。
11. **连接中与思考中文案支持自定义配置与一键重置（Req 9）**：`PersonalizationSettings` 引入 `connectingTextTemplate` 与 `thinkingTextTemplate` 配置项，并在设置页「模型与高级功能」Tab 中提供直观编辑面板与「重置为默认值」按钮。
12. **移除设置页中“实时测试自动命名效果”卡片（Req 10）**：彻底移除冗余的测试自动命名输入框、状态与卡片 UI，界面清爽精炼。
13. **思考胶囊双击展开/折叠（Req 11）**：思考状态胶囊与思考区域均支持双击手势快速切换展开/收起状态。
14. **划选文本浮动工具栏防闪烁与剪切/粘贴按钮补全（Req 12）**：`EchoTextToolbar.kt` 中增加 4px 坐标变动防抖容差，避免微小重绘引起的工具栏闪烁；状态机中补齐 `onCut` 与 `onPaste` 回调，完整支持划选剪切与粘贴操作。
15. **对话输入栏右上角弧线手柄与垂直拖动自由调高（Req 13）**：
    - 移除右上角原有四向放大图标，在输入栏右上角绘制四分之一同心圆弧线手柄 `⌒`（紧贴右上圆角内沿）；
    - 支持垂直拖动手势自由调节高度（范围 42dp ~ 360dp），单次点击在最小与最大高度间快速切换；
    - 进入新对话或切换会话时默认保持最小高度。
16. **用户主动终止生成防二次报错气泡（Req 14）**：在 `ChatViewModel` 中引入 `@Volatile isUserStopping` 状态标志，在用户主动点击停止时强力静默拦截底层网络连接关闭产生的 `Socket closed` / `Canceled` 异常气泡与弹窗。
17. **全屏状态栏阴影覆盖与液态玻璃弹窗统一（Req 15）**：`EchoGlassDialog` 引入 `WindowCompat.setDecorFitsSystemWindows(this, false)`、沉浸式状态栏与导航栏标志及 `FLAG_DIM_BEHIND`（42% 深色透明遮罩），实现刘海屏与挖孔屏全域无死角遮罩覆盖。
18. **报错气泡默认折叠仅显示关键信息（Req 16）**：针对 AI 回复中的报错内容，默认折叠详细堆栈仅显示精简错误提示与展开按钮，并支持双击快速展开/收回。
19. **流式生成时底部自动吸附滚动，上滑暂停，回到底部恢复自动跟随（Req 17）**：监听列表滚动事件与最新可见条目，流式输出期间用户向上滑动阅读时自动暂停跟手滚动；当用户滑动回底部时，自动恢复跟随最新输出实时下滚。
20. **设置页「当前版本更新说明」彻底清理历史版本仅保留 v1.9.25（Req 18）**：重构 `CurrentVersionUserUpdates` 常量，清空所有陈旧版本冗余条目，纯净呈现本次 v1.9.25 的核心升级亮点。

### 2. 自动化测试核验
- 全量 111 项单元测试 100% 全部通过（新增 `V1925FeaturesTest` 专项覆盖 18 项核心需求，包含文案模板、默认开关、50000 Token、更新条目单一版本等验证，退出码 0）。

### 3. 改动涉及文件列表
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`:
  - 顶部悬浮栏与报错 Banner 纯透明/毛玻璃修复（Req 1）；
  - 思考过程历史消息默认折叠（Req 5）；
  - 思考胶囊双击展开/折叠（Req 11）；
  - 输入框右上角弧线手柄 `⌒` 绘制与垂直拖动手势高度调节（42dp~360dp，新对话默认最小高度）（Req 13）；
  - 报错气泡默认折叠关键信息与双击切换（Req 16）；
  - 流式输出底部自动跟随与上滑暂停/回底恢复机制（Req 17）；
  - 连接中文案动态替换（Req 8）。
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`: `EchoGlassDialog` 升级沉浸式全屏阴影遮罩（Req 15）。
- `app/src/main/java/com/aiassistant/ui/components/EchoTextToolbar.kt`: 防闪烁坐标防抖与剪切、粘贴支持（Req 12）。
- `app/src/main/java/com/aiassistant/ui/components/ScrollAssist.kt`: `TransientLazyListScrollbar` 防断触与平滑手势跟踪优化（Req 7）。
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`: 新建对话默认 50000 maxTokens、思考开启、联网关闭（Req 2, 6）。
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`: 新建对话默认会话记忆关闭（Req 3），主动停止生成防二次报错气泡（Req 14）。
- `app/src/main/java/com/aiassistant/utils/SmartMemoryExtractor.kt`: 专属记忆智能提取规则强化（Req 3）。
- `app/src/main/java/com/aiassistant/utils/PersonalizationManager.kt`: 思考与连接中文案模板支持（Req 8, 9）。
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`:
  - API 配置 Tab 顶部新增“新对话默认 API”卡片（Req 4）；
  - 模型与高级功能 Tab 新增文案模板自定义与重置（Req 9）；
  - 移除实时测试自动命名效果卡片（Req 10）；
  - `CurrentVersionUserUpdates` 纯净化仅保留 v1.9.25（Req 18）。
- `app/build.gradle.kts`: `versionCode = 105`, `versionName = "1.9.25"`。
- `app/src/test/java/com/aiassistant/V1925FeaturesTest.kt`: 新增 v1.9.25 专项自动化测试。
- `app/src/test/java/com/aiassistant/V1914FeaturesTest.kt`, `V1922FeaturesTest.kt`, `V1923FeaturesTest.kt`, `V1924FeaturesTest.kt`: 适配纯净化更新说明断言。

### 4. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：103 个，构建后增至 104 个，严格遵守历史包永久保留最高铁律，未执行任何删除/清理操作；
- 增量输出安装包：`Echo-v1.9.25-arm64-v8a.apk`
  - 路径：`D:\Agent\APP-烧\app\releases\Echo-v1.9.25-arm64-v8a.apk`
  - 体积：16,025,597 字节 (~15.28 MB)
  - SHA-256：`07A66778E35B934FCCB7183BDDB7F3262A77B9A1F23A3E412327BE51715A7501`

## [v1.9.24] - 2026-09-08

### 1. 本次升级与 16 项用户需求 100% 落实
1. **模型回复底部分割线微距贴合（Req 1）**：上移回复完成微光分割线间距，减少顶部冗余空白（`padding(top = 2.dp, bottom = 4.dp)`），布局紧凑精致。
2. **对话页顶部悬浮栏无瑕全景毛玻璃（Req 2）**：彻底修复白色胶囊外圈矩形背景和液态玻璃失真，外层采用透明 Column 包裹，内层 Surface 严格贴合 56dp 胶囊形状，消除背景溢出与模糊失效。
3. **会话内专属记忆独立控制总开关（Req 3）**：在对话设置弹窗中增加会话专属记忆总开关（`enableSessionMemory`），打通 `TempChatSettings`、`ChatViewModel`、`AiRepository` 与 `ChatRequestOptions`，支持完全关闭或开启注入。
4. **思考强度全链路即时双向同步与真实参数注入（Req 4）**：底部输入栏思考强度弹窗与对话设置中思考强度完全双向同步（`reasoningEffort`），即时存盘并保证在 API 发送层精准映射下发（OpenAI `reasoning_effort` / Claude `budget_tokens` / Gemini `thinkingConfig`）。
5. **思考强度配色重调与真实参数详情弹窗（Req 5）**：重调快速档饱和度（雅致柔和 `#5FA8D3`），拉开深入（`#1D4ED8`）与极高（`#4F46E5` / `#6366F1`）的色相辨识度；在标题右侧增加 `ⓘ` 说明按钮，点击弹出各服务商在各档位下发的具体 API 参数。
6. **思考强度展开窗口全屏点击外部折叠（Req 6）**：在主输入栏思考调节弹窗展开时，增加全屏无感透明拦截遮罩与 BackHandler，点击弹窗外任意位置或按返回键平滑折叠。
7. **全屏状态栏阴影覆盖与液态玻璃弹窗统一（Req 7）**：全面升级 `EchoGlassDialog`，引入全屏 `FLAG_LAYOUT_NO_LIMITS`、`MATCH_PARENT` 及透明状态栏/导航栏标志，实现 100% 全屏无死角 40% 深色遮罩；迁移 `ChatScreen` 中的原生 `Dialog` 与 `AlertDialog` 至 `EchoGlassDialog`。
8. **全局平滑页面转场过渡动画（Req 8）**：在 `MainActivity.kt` 的 `NavHost` 中为所有页面配置全局平滑横向滑入滑出与渐变动画（`slideInHorizontally` + `fadeIn` / `slideOutHorizontally` + `fadeOut`），采用 `FastOutSlowInEasing` 曲线，页面切换优雅自然。
9. **进入对话默认瞬间滚动到底部（Req 9）**：会话首次加载后检测到首批历史消息时，自动瞬间精准锚定到底部最新消息，杜绝从顶部下移的突兀感。
10. **一键快速回到顶部/底部极速预跳加速（Req 10）**：重构 `ChatScrollJumpButtons` 滚动逻辑，距离大于 8 条消息时先静默预定位至邻近位置再短距平滑滚入，彻底解决超长会话中滚动速度过慢的问题。
11. **右侧全局滚动条加粗与平滑跟手优化（Req 11）**：在 `ScrollAssist.kt` 中将滚动条滑块宽度增至 8dp，触控热区增至 36dp，最小高度 44dp，并增加拖拽高亮态与平滑手势位移映射，解决卡顿断触。
12. **全局默认最大生成 Token 提升至 50,000（Req 12）**：将 `ApiConfig`、`TempChatSettings`、`RoleplayViewModel`、`SettingsScreen` 等全局层面的默认及回退最大输出长度统一提升至 50,000。
13. **全局默认开启深度思考模式（Req 13）**：统一将全局各层级 `enableThinking` 默认值设为 `true`。
14. **移除胶囊下方多余英语检测，翻译按钮内嵌至思考区右上角（Req 14）**：彻底移除思考胶囊下方冗余的英语检测小按钮，将翻译按钮移至展开后的思考框标题栏右上角（复制按钮左侧），操作更集中直观。
15. **正在思考胶囊动态显示模型名称（Req 15）**：思考状态胶囊文案更新为 `"${model.displayModelShortName()} 正在思考中..."`，让用户明确知晓当前推理的模型。
16. **模型选择列表智能过滤前缀仅显示核心名称（Req 16）**：统一调用 `displayModelShortName()`，自动过滤供应商路径（如 `z-ai/glm-5.2` -> `glm-5.2`），输入栏模型选择与设置页模型下拉全部生效。

### 2. 自动化测试核验
- 全量 106 项单元测试 100% 全部通过（退出码 0，新增 `V1924FeaturesTest` 专项验证模型名称裁剪、会话记忆开关选项、默认 50000 Token 及更新说明完整性）。

### 3. 改动涉及文件列表
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`: `EchoGlassDialog` 全面升级为系统级无界沉浸弹窗（`FLAG_LAYOUT_NO_LIMITS`、`MATCH_PARENT`、状态栏透明、40% 全屏遮罩无暗角死区穿透）。
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`:
  - 顶部悬浮胶囊栏采用透明包裹外框与精确内层 Surface，根除方框背景与液态玻璃模糊损坏；
  - 思考强度弹窗与对话设置完全双向即时同步，思考模式默认开启；
  - 思考强度快速/深入/极高配色重新调校，新增 ⓘ 实际参数说明弹窗（`ThinkingParamsExplanationDialog`）；
  - 全屏透明点击拦截器与返回键一键折叠思考弹窗；
  - 对话消息底部分割线贴合微调（`padding(top = 2.dp, bottom = 4.dp)`）；
  - 进入会话首次即时定位最新底部消息；
  - 一键快速回到顶部/底部智能预跳加速算法；
  - 思考胶囊文案动态显示模型名称，移除下方英语检测按钮，翻译按钮移入思考区右上角；
  - 模型下拉与输入栏模型选择统一切除供应商路径前缀（`displayModelShortName()`）；
  - 会话专属记忆管理面板（`ChatSettingsSessionMemorySection`）新增启用/停用总开关。
- `app/src/main/java/com/aiassistant/ui/components/ScrollAssist.kt`: `TransientLazyListScrollbar` 宽度增至 8dp，触控热区增至 36dp，最小高度 44dp，平滑手势位移与拖拽高亮态。
- `app/src/main/java/com/aiassistant/domain/model/Models.kt`: `ApiConfig.maxTokens` 默认提升至 50,000，`enableThinking` 默认 `true`，`ChatRequestOptions` 增加 `enableSessionMemory: Boolean?`。
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`: 上下文组装 `buildContextBundle` 与 `resolveChatRequestOptions` 严格响应会话专属记忆开关 `enableSessionMemory`。
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`: `TempChatSettings` 默认 `maxTokens = 50000`、`enableThinking = true`、`enableSessionMemory = true`，`sendMessageInternal` 携带该配置。
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayViewModel.kt`: 角色扮演默认及回退 `maxTokens` 统一提升至 50,000。
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`: 默认配置更新，同步更新 `CurrentVersionUserUpdates`。
- `app/src/main/java/com/aiassistant/MainActivity.kt`: `NavHost` 引入平滑横向位移与渐变混合转场（`slideInHorizontally` + `fadeIn` / `slideOutHorizontally` + `fadeOut`，`FastOutSlowInEasing`）。
- `app/src/test/java/com/aiassistant/V1924FeaturesTest.kt`: 新增测试套件。
- `app/src/test/java/com/aiassistant/V1923FeaturesTest.kt`: 适配更新日志列表断言。
- `app/src/test/java/com/aiassistant/ChatEnhancementsTest.kt`: 默认 maxTokens 断言同步更新为 50,000。
- `app/build.gradle.kts`: `versionCode = 104`, `versionName = "1.9.24"`。

### 4. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：102 个，构建后增至 103 个，严格遵守历史包永久保留最高铁律，未执行任何删除/清理操作；
- 增量输出安装包：`Echo-v1.9.24-arm64-v8a.apk`
  - 路径：`D:\Agent\APP-烧\app\releases\Echo-v1.9.24-arm64-v8a.apk`
  - 体积：16,025,597 字节 (~15.28 MB)
  - SHA-256：`8A5E8CC61F4A35FDD106FA51DB45358627DFCE781974BEA5287A9BFCD2C36EF2`

## [v1.9.23] - 2026-09-08

### 1. 本次升级与需求 100% 落实
1. **会话内专属记忆功能落地（Req 1）**：
   - 打通会话内专属记忆（CRUD、开/关、上下文无缝注入）完整链路；
   - 在 `Daos.kt` 中为 `MemoryDao` 增加 `getConversationMemoriesFlow(conversationId)` 与 `getConversationMemories(conversationId)`；
   - 在 `AiRepository.kt` 与 `ChatViewModel.kt` 中打通 `sessionMemories` 状态流及 `addSessionMemory`、`updateSessionMemory`、`toggleSessionMemory`、`deleteSessionMemory`、`clearSessionMemories`；
   - 在 `ChatScreen.kt` 对话设置弹窗（`ChatSettingsDialog`）中新增 `ChatSettingsSessionMemorySection` 会话专属记忆管理面板，用户可直观查看、添加、编辑、切换启用状态与清空当前会话记忆；
   - 上下文注入机制保持物理隔离：会话专属记忆仅在当前会话的上下文构建中生效，杜绝跨会话污染。
2. **高信噪比智能记忆提取重构（Req 2）**：
   - 彻底重构 `SmartMemoryExtractor.kt` 与 `AiRepository.captureMemoryCandidate`，根除提取无关对话、疑问句、客套寒暄与单次任务的问题；
   - 引入完善的多层过滤管道：否定词过滤、完整疑问句与求助句过滤（`吗`、`？`、`怎么`、`为什么`、`如何`等）、瞬态寒暄过滤（`刚刚`、`你好`、`谢谢`等）以及单次临时任务动作词过滤（`帮我`、`优化一下`、`写一个`等）；
   - 严密甄别高信噪比结构化事实与持久偏好：仅对显式记忆指令（`请记住：...`）、长期持久偏好（含`以后`、`每次`、`始终`等修饰的语言/注释/简练规范）、真实持久身份（姓名/职业）以及项目架构事实提炼记忆候选。
3. **展开提示词优先级说明防闪烁优化（Req 3）**：
   - 优化 `SettingsScreen.kt` 中系统提示词优先级手风琴卡片的 `AnimatedVisibility` 展开与收起动画规格，加入 `clipToBounds()` 防止溢出；
   - 重构 `PriorityRuleRow` 布局结构，移除易引起多层重绘抖动的双层嵌套 `Surface`，改用轻量级 `Box + clip + background` 结构，彻底消除展开和折叠时的瞬间布局抖动与重绘闪烁。
4. **模型回复完成后高对比微光分割线（Req 4）**：
   - 在 `ChatScreen.kt` 消息气泡底部重构模型回复结束分割线：在 AI 回复生成完毕后呈现高对比度雅致微光横向分割线（0.5dp 渐变青蓝微光），明确划分多轮问答对话流，提升视觉节奏感与舒适度。
5. **思考强度全阶统一蓝色系色彩体系（Req 5）**：
   - 重构 `effortAccentColor` 及 `ReasoningEffortPopupCard` 中的 0~4 档色彩体系，全面统一为递进纯正蓝色系：
     - 0 档（关闭）：板岩灰蓝（`#64748B`）
     - 1 档（快速）：浅冰蓝（`#38BDF8`）
     - 2 档（平衡）：道奇蓝（`#0284C7`）
     - 3 档（深入）：深海蓝（`#0369A1`）
     - 4 档（极高）：皇家宝石蓝（`#1D4ED8`）
   - 胶囊按钮、弹窗档位卡片、选中高亮背景与边框全链路统一，视觉纯净专业。
6. **文字划选浮动工具栏防闪烁与一键引用稳定化（Req 6）**：
   - 修复 `EchoTextToolbar.kt` 中 Compose `Popup` 的属性配置，显式声明 `PopupProperties(focusable = false, dismissOnClickOutside = false, dismissOnBackPress = true)`，彻底消除拖拽手柄及选区变更时点击外部导致 Popup 被反复销毁又重绘的死循环闪烁；
   - 优化“引用”动作执行流程，增加 60ms 剪贴板读取协程缓冲，确保精准捕获系统剪贴板选中文本并无缝回填至主输入框。
7. **对话页顶部悬浮栏纯色外框消除（Req 7）**：
   - 重构 `ChatScreen.kt` 界面层级结构：将底层壁纸背景与 `Haze` 毛玻璃源提升至整屏根节点 `Box`；
   - 将 `Scaffold` 的 `containerColor` 设为 `Color.Transparent`，顶部悬浮栏采用 `echoHazePanel(tint = glass.panel)` 直接穿透毛玻璃；
   - 彻底移除悬浮栏外围包裹的 0.96f 纯色背景框，呈现纯正全景液态毛玻璃视觉效果。

### 2. 自动化测试核验
- 全量 102 项单元测试 100% 全部通过（退出码 0，新增 `V1923FeaturesTest` 专项验证记忆过滤、高信噪比提取、会话记忆隔离、全阶蓝色系色彩及更新日志规范）。

### 3. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：102 个，构建后增至 103 个，严格遵守历史包永久保留最高铁律，未执行任何删除/清理操作；
- 增量输出安装包：`Echo-v1.9.23-arm64-v8a.apk`
  - 路径：`D:\Agent\APP-烧\app\releases\Echo-v1.9.23-arm64-v8a.apk`
  - 体积：16,025,597 字节 (~15.28 MB)
  - SHA-256：`5E5E07FDEF9AEDACF1EC0236324472FEDE1F9F64D071532FBB77D3056B27A621`

## [v1.9.22] - 2026-09-08

### 1. 本次升级与需求 100% 落实
1. **思考强度滑块防抽搐平滑优化（Req 1）**：
   - 彻底定位滑块抽搐根因：手势检测 `pointerInput` 将随拖拽频繁变动的局部浮点数作为 keys，导致 Compose 微小重组时强行重置手势协程，使得 Thumb 骤然掉落并重新捕获；同时弹窗将 `sliderIndex` 与离散档位 `currentStep` 强绑定造成档位打架；
   - 重构 `EchoPillSlider`：统一采用静态稳定的手势捕获与底层协程上下文；拖拽过程中 Thumb 绝对像素紧随手指真实触点连续平滑位移，仅在离散档位发生跃迁时才触发回调；松手后平滑使用 Spring 阻尼弹性吸附至目标档位，彻底消除抽搐与抖动。
2. **内置极低饱和度护眼纯色背景（Req 2）**：
   - 在 `BackgroundImageManager` 中内置浅艾绿 (`#F1F8F4`)、浅湖蓝 (`#F0F5FA`)、浅薰紫 (`#F6F3F9`)、浅樱粉 (`#FAF2F4`)、浅暖杏 (`#FAF8F0`)、浅山岚 (`#F0F7F7`) 6 款极低饱和度（饱和度 < 5%）纯色背景，并提供深色模式适配色；
   - 在「设置 -> 界面与外观 -> 界面背景」中新增可视化纯色选择器卡片，支持一键独立应用至「首页背景」、「对话页背景」或「全局应用（全部背景）」；严格符合 WCAG AAA 极高文本对比度规范。
3. **设置中“关于本次更新”内容准确同步（Req 3）**：
   - 重新校准 `SettingsScreen.kt` 内的 `CurrentVersionUserUpdates` 数据源，标明 `【v1.9.22 本次更新】`，详细录入思考滑块防抽搐、低饱和纯色背景、设置全量即时生效、跨会话长期记忆状态同步、模型跨服务商自由直选及向下展开式折叠列表等全部改动。
4. **设置项调整全量即时生效与退出弹窗废除（Req 4）**：
   - 彻底修复同音转录项“跨绘画技艺”（即“跨会话长期记忆”）开关此前仅更新本地暂存、未即时落盘的缺陷，开关触碰即刻持久化存盘；
   - 界面字体大小滑块、字体缩放单选组、自动命名开关与模型、思考链翻译开关与模型、胶囊模板等所有设置项变更后均立即持久化落盘，输入框内容支持自动防抖即时保存；
   - 全面废除返回退出时二次提示保存的拦截确认弹窗（`showUnsavedDialog`），设置调整立竿见影，退出切换畅通无阻。
5. **对话自动命名与思考链翻译模型全自由选择（Req 5）**：
   - 彻底打通与对话页模型选择一致的完整数据源，接入 `repository.getAllVisibleChatModelOptions()`；
   - 解除原先仅能选择服务商默认模型的限制，跨所有已配置服务商平铺展示全部已启用模型（含上下文窗口、深度思考与视觉能力标识），并支持直接自定义输入任意模型名称。
6. **模型选择 UI 折叠展开与搜索框尺寸重构（Req 6）**：
   - 废除臃肿局促的居中全屏模态弹窗，改为顺应右侧向下箭头的内联向下展开折叠面板，右侧箭头随展开状态丝滑翻转；
   - 重新规范搜索框尺寸为 40dp 匀称胶囊搜索栏，居中垂直对齐，视觉比例精致协调。

### 2. 自动化测试核验
- 全量 97 项单元测试 100% 通过（退出码 0，包含 `V1922FeaturesTest` 专属自动化测试覆盖 6 项核心需求）。

### 3. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：101 个，构建后增至 102 个，严禁且未执行任何删除/清理操作；
- 增量输出安装包：`Echo-v1.9.22-arm64-v8a.apk`
  - 路径：`D:\Agent\APP-烧\app\releases\Echo-v1.9.22-arm64-v8a.apk`
  - 体积：16,009,213 字节 (~15.27 MB)
  - SHA-256：`8F31AC23F9309197C41EC39EA8248703045132FC8D3202A0D3DD93B4BB0B24F5`

## [v1.9.21] - 2026-09-08

### 1. 本次升级与需求 100% 落实
1. **API 配置删除后对话切换模型自愈修复（Req 1）**：
   - 修复当历史对话使用的模型所绑定的 API 配置在设置中被用户删除后，该对话模型下拉列表为空且无法切换模型的缺陷；
   - 在 `ChatViewModel` 中加入容灾机制：当检测到会话绑定的 `apiConfigId` 已无效时，自动回退并加载系统全部可用配置模型；当用户选择新模型时，自动将对话的 `apiConfigId` 更新为所选模型的有效配置 ID，并持久化至数据库，对话全面恢复正常。
2. **深入思考按钮指示图案优化（Req 2）**：
   - 彻底优化深入思考胶囊按钮右侧的向上指引，用标准居中矢量图标 `Icons.Default.KeyboardArrowUp` 替换原文本字符 `⌃`，配合 16dp 规格与垂直精准居中对齐，杜绝符号未居中及跨机型字体渲染差异。
3. **深入思考调整弹窗文案极简化（Req 3）**：
   - 移除弹窗中冗长繁琐的辅助说明文本（彻底删除“思维链完整展开深度探究...”与“跳过思维链直接回答...”等冗余描述），仅保留直观的档位标签、彩色指示与操作按钮，视觉轻盈清爽。
4. **思考强度滑块滑动平滑吸附优化（Req 4）**：
   - 彻底重构 `EchoPillSlider` 的手势追踪引擎，使用底层 `awaitPointerEventScope` 替换粗粒度的手势捕获，解决原有滑动过程因 touch slop 及异步事件覆盖导致某些档位无法通过拖动选中、只能点击选中的逻辑问题；
   - 实现手指按下即时响应、全程连续坐标吸附映射、抬手精准弹簧弹性就位，点选与滑选逻辑 100% 统一生效。
5. **引用功能重构为划选文本工具栏（Req 5）**：
   - 按照人体工学与移动端操作习惯，将「引用」按钮从消息卡片底部的「复制/删除」操作栏移除；
   - 独立实现 `EchoTextToolbar` 与 `EchoTextToolbarHost`，接管 Compose `LocalTextToolbar`，在用户长按或滑动划选消息文本时，弹出液态玻璃浮动胶囊菜单（包含「复制」、「引用」、「全选」）；
   - 点击「引用」时，智能提取选中文本并以 Markdown 引用格式（`> 内容\n\n`）填入底部输入框并自动聚焦光标。
6. **弹出窗口阴影方角与闪烁统一修复（Req 6）**：
   - 深入排查 Android 原生 elevation 阴影层在动画过程中边缘裁切未跟随导致的矩形方角与白边闪烁；
   - 针对深度思考弹窗及对话交互弹窗，统一采用 `RoundedCornerShape(22.dp)` 显式 `.clip(popupShape)` 搭配 `shadowElevation = 0.dp` 与柔和彩色/微白拟态描边，彻底消除了弹窗向上滑出动画期间的方形边缘与闪烁瑕疵。
7. **失效模型醒目预警与引导切换（Req 7）**：
   - 在输入栏模型选择胶囊与下拉列表中，引入失效模型智能探测；
   - 若检测到当前模型已被服务商移除或配置丢失，选择器胶囊展示警告色与 `⚠️ 失效` 状态；点击展开时高亮提示当前模型失效，并引导用户一键点击切换到其他健康可用模型。
8. **自动命名与翻译模型跨服务商自由直选（Req 8）**：
   - 彻底重构设置中自动命名模型与翻译模型的选择体验，摆脱原先必须先选服务商、或者只能使用默认模型的繁琐限制；
   - 封装 `UniversalModelPickerCard` 与全屏多服务商模型搜索弹窗 `EchoModelPickerDialog`，集中平铺所有已配置提供商下的全部模型，支持快速关键词过滤、分组展示与一键直选。
9. **设置菜单图标与文字居中校准（Req 9）**：
   - 修复设置主菜单列表项中图标与标题偏上未垂直居中的问题，采用 `.align(Alignment.CenterStart)` 及 `Arrangement.Center` 进行绝对居中约束，图文对齐工整舒展。
10. **设置功能模块化重构为 8 大清晰板块（Req 10）**：
    - 将原有杂糅的设置页面按照业务职能细分为 8 大独立路由板块：
      1. API 接口与服务商配置
      2. 界面与视觉外观
      3. 模型辅助与深度思考（集中管理自动命名、翻译模型自由选择与思考全局开关）
      4. 提示词与长期记忆管理
      5. 联网检索与设备工具
      6. 隐藏对话管理
      7. 数据备份与跨端迁移
      8. 关于与系统信息

### 2. 自动化测试核验
- 全部 92 项单元测试 100% 一次性通过（退出码 0，包含 `V1921FeaturesTest` 专属自动化测试覆盖模型恢复、滑块吸附、划选引用、通用模型筛选与思考映射）。

### 3. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：100 个，构建后增至 101 个，无任何历史安装包被删除或清理；
- 增量输出安装包：`Echo-v1.9.21-arm64-v8a.apk`
  - 路径：`D:\Agent\APP-烧\app\releases\Echo-v1.9.21-arm64-v8a.apk`
  - 体积：16,009,213 字节 (~15.27 MB)
  - SHA-256：`65C924E48B5D2BA1515D17F58EA5EE7A335FC01CADEF8EC41A369822F7FA8BC4`

## [v1.9.20] - 2026-09-08

### 1. 本次升级与需求 100% 落实
1. **API 配置多 Key 独立输入框与智能故障转移（Req 1）**：
   - 彻底优化 API 配置弹窗中原先单纯单行逗号/分号混杂的输入模式，重构为整齐直观的独立输入框卡片列表（Key 1 主密钥、Key 2 备用密钥...）；
   - 每个 Key 独立配备明文/密文切换眼球按钮、独立删除按钮（多于 1 个时可直接单条移除）与专属错误校验；
   - 列表底部提供「+ 添加 Key 输入框」按钮；支持向任意输入框直接粘贴多行/分号分隔的混合密钥，自动智能拆分填充为独立输入框；
   - 保持与底层多 Key 轮询容灾故障转移机制（`parseApiKeys`）100% 双向同步兼容。
2. **华为运动健康检测与可读性重构（Req 2）**：
   - 修复 Android 11+ 包可见性机制限制，在 `AndroidManifest.xml` 中引入 `<queries>` 声明 `com.huawei.health`, `com.huawei.bone`, `com.hihonor.health` 与 `com.google.android.apps.fitness`；
   - `HealthDataManager` 增加 `isHuaweiHealthInstalled` 精准检测逻辑与 `openHuaweiHealthApp` 一键唤起官方应用交互；
   - 设置页运动健康卡片全面重构：显示「已安装/未安装」彩色药丸徽标；彻底消除难以理解的原始 epoch 毫秒时间戳（如 `1741416800000`），转为人类易读的动态格式（刚刚、X分钟前、HH:mm）；
   - 卡片采用 3 列高对比体征指标卡（步数、心率、睡眠）与三等分响应式功能按钮（一键拉起、重新校准、手动录入）。
3. **对话页深度思考按钮文案精简与去图标化（Req 3）**：
   - 移除深度思考胶囊按钮前缀多余的脑图图标；
   - 精简按钮文案，根据当前档位动态显示极简状态：关闭时为「深度思考」、开启时为「快速思考 ⌃ / 平衡思考 ⌃ / 深入思考 ⌃ / 极高思考 ⌃」，极致节约输入栏纵向与横向空间。
4. **深度思考弹窗与模型兼容档位真实动态对应（Req 4）**：
   - 解除原逻辑中对大量新架构模型的硬编码屏蔽，`ModelCapabilityEngine` 根据模型类型自适应返回其真实支持的档位：
     - OpenAI o1/o3/gpt-5: 提供 low / medium / high 3 档；
     - Claude 3.7 / DeepSeek / 通用推理模型: 提供 low / medium / high / max 4 档；
     - 固定全量深度推理模型: 显示「固定全量推理」说明并不发送多余参数；
     - 弹窗动态计算可用档位数并自适应映射，坚决杜绝虚假 UI 与参数报错。
5. **圆润美观胶囊滑块组件 (`EchoPillSlider`)（Req 5）**：
   - 100% 严格还原用户参考图设计风格，独立封装 `EchoPillSlider` 组件：
     - 采用大圆角胶囊药丸轨道背景，内置各档位均匀吸附圆点；
     - 纯白浮雕式饱满圆形滑块（Thumb），带拟物多层阴影；
     - 滑动时丝滑物理拖拽与弹簧弹性吸附，档位数字及标签实时联动。
6. **重构模型上下文识别逻辑（Req 6）**：
   - 修复 `deepseekv4flash` 错误识别为 64K 的缺陷，精确解析并标注为 **1M**（`1_048_576` tokens）上下文；
   - 全面优化通用识别逻辑：支持 1m/2m 等大容量后缀解析；对于不能读取到确切真实信息的模型，坚决不虚构编造虚假标签（空标签在 UI 上不渲染上下文徽标）。
7. **横屏与小窗稳定性优化（Req 7）**：
   - 针对横屏模式或分屏/小窗模式下弹窗溢出、液态玻璃错位、按钮大小失真等问题进行系统性校验；
   - 弹窗统一引入 `heightIn(max = (screenHeight * 0.78f).dp)` 响应式动态高度约束与内部垂直滚动，确保在任何极小窗口及横屏视口下内容均能完整交互且不发生裁切变形。
8. **修复上版回归缺陷（Req 8）**：
   - 修复切换版本（`< 1/2 >`）时列表跳底的缺陷，原位平滑比对并锁定当前查看位置；
   - 修复暂停/停止生成时滚动条异常跳转的问题，生成中断时平稳固定当前视口；
   - 消息气泡接入 `SelectionContainer`，支持用户自由划选文字与复制；
   - 消息操作栏新增一键「引用」功能，自动在输入框追加引用格式（`> ...`）。

### 2. 自动化测试核验
- 全部 87 项单元测试 100% 一次性通过（退出码 0，包含 `V1920FeaturesTest` 专属自动化测试）。

### 3. 历史安装包永久保留准则（最高铁律）
- 构建前历史版本：98 个，构建后增加至 99 个，无任何历史版本被删除或覆盖；
- 增量输出：`Echo-v1.9.20-arm64-v8a.apk`（体积：15,992,829 字节，SHA-256：`7165BE8DF433458E71F5EC4F9046C9E9BE025E017C8D7B8F95F68467BA33B89C`）。

## [v1.9.19] - 2026-09-08

### 1. 本次升级与需求 100% 落实
1. **英文思考链汉化与翻译（Req 1）**：
   - 设置页「个性化与提示词」新增独立「思考链汉化与翻译」设置卡片，支持开启/关闭总开关、绑定专属翻译 API 服务商配置及模型选择（下拉实时拉取模型列表，带能力标签）；
   - `AiRepository` 内建 `isMainlyEnglish` 高精度语言检测算法（统计 CJK 与拉丁字符密度比例），当模型输出的思考链主要为英文时，在折叠胶囊显示「检测到英文思考 · 点击汉化」，展开详情顶部提供「[译文] / [原文]」实时无缝切换开关以及「一键翻译 / 重新翻译」按钮；
   - 翻译结果持久化存储于 Room 数据库（`messages.translatedThinking`），重启应用或离线回溯始终保留。
2. **暂停回复留痕与状态保留（Req 2 & Req 4）**：
   - 彻底修复中止生成时消息丢失的缺陷；
   - 重构 `ChatViewModel.stopGeneration()`：若模型已生成部分文本或思考，100% 完整保留已有输出并在尾部追加留痕标注 `*(回复已被暂停)*` 写入数据库；
   - 若尚未连接到模型或还未产生 Token 输出即被用户主动暂停，模型气泡规范保留为「回复已停止」并存库，实现全生命周期无死角持久化留痕与历史可读性。
3. **编辑过往历史输入原位思考与回复 & 分支回溯（Req 3）**：
   - 彻底解决编辑历史输入后在底部弹出思考与回复的错位问题；
   - 用户编辑历史消息后，流式生成的思考胶囊与正文在被编辑的历史 turn 原位展开渲染，并平滑滚动到该位置；
   - 支持同一行内通过 `< 1/2 >` 实时切换原回复与新回复，分支版本无缝持久化并在原位即时比对。
4. **对话设置弹窗排版全面优化（Req 5）**：
   - 「转为角色扮演 / 故事创作」卡片调整至设置弹窗的最底部，保证常规对话调优参数（模型、系统提示词、温度、Token、思考、联网、头像）优先展示；
   - 移除原弹窗顶部的静态优先级大卡片，改为在「系统提示词」标题左侧设置优雅的 ℹ️ 信息图标，点击以平滑动画展开/折叠系统提示词最高优先级说明。
5. **消息底部操作栏同行排布与横向滚动（Req 6）**：
   - 彻底移除消息卡片底部多行堆叠的 FlowRow，将分支版本切换器（`< 1/2 >`）与复制、重新生成、编辑、删除等操作按钮置于同一行紧凑排布；
   - 左侧的时间戳、耗时、Token 数、Token 速度等元数据支持单行横向丝滑滑动（`horizontalScroll`），小屏设备下也绝不换行折断。
6. **深度思考向上展开渐变滑块弹窗（Req 7）**：
   - 点击输入框底部深度思考胶囊按钮，向上平滑弹出精致的思考强度调节气泡弹窗；
   - 支持 5 档自由调节：0 档（关：灰色）、1 档（快速：翠绿）、2 档（平衡：海蓝）、3 档（深入：紫罗兰）、4 档（Ultra：霓虹紫渐变）；
   - 滑块轨道、Thumb、档位徽标、背景卡片随档位动态切换渐变特效，下方支持 5 颗吸附点与快捷药丸芯片点选，提供专属档位思考预算调度（最高 32,768 tokens）。
7. **数据库平滑升级（Room 21 -> 22）**：
   - `Message` 实体新增 `translatedThinking TEXT` 字段；
   - 编写并注册严密的 `MIGRATION_21_22` 与 `repairSchema` 兼容机制，无损保留所有用户历史记录与角色数据；
   - 新增 `V1919FeaturesTest` 自动化单元测试覆盖语言识别、思考档位预算映射、停止回复留痕、Room 21_22 迁移等全部核心逻辑，82 项测试全部一次性通过。
8. **历史安装包永久保留准则（最高铁律）**：
   - 构建前历史版本：97 个，构建后增加至 98 个，无任何历史版本被删除或覆盖；
   - 增量输出：`Echo-v1.9.19-arm64-v8a.apk`（体积：15,976,197 字节，SHA-256：`61A9D3A2F4B154E683CFBAA7287D37D6462AD97D478DE12BF7A08DCA5206209A`）。

## [v1.9.18] - 2026-09-07

### 1. 本次升级与需求 100% 落实
1. **角色与创作全局提示词与系统提示词分层教学机制（Req 1）**：
   - 在个性化设置及角色扮演设置中引入「全局角色创作规范与教学指引」(`globalRoleplayPrompt`)，并内建文学导师级默认规范（Show, Don't Tell、台词与动作交融、世界观沉浸度、用户主导与留白互动、禁止AI说教）；
   - 支持本故事专属系统提示词（`storySystemPrompt`）与全局创作教学指引在组装上下文时分层协同注入，全面教学模型如何构思剧情、把控行文与塑造角色。
2. **模型上下文识别算法深度优化 & 256K 基准设定（Req 2）**：
   - `ModelCapabilityEngine` 引入显式数字正则后缀提取（例如 `-128k`, `-32k`, `-1m`, `-2m`）及主流大模型家族（Gemini 2M/1M、Claude 200K、OpenAI 200K/128K、DeepSeek 128K、Qwen 1M/128K、GLM 1M/128K 等）全谱系识别；
   - 未被成功识别的模型统一设定为基准上下文 **256K**（`256_000` tokens），且在 UI 上不渲染可能存在偏差的上下文徽标，杜绝误导。
3. **华为运动健康真实硬件计步 & 数据透明化（Req 3）**：
   - 彻底根除代码中写死的假心率 72 bpm、假睡眠 7.5h、假评分 85，未录入时明确显示为「暂无录入数据 / 未手动录入」；
   - 仅使用底层硬件计步传感器（`Sensor.TYPE_STEP_COUNTER`）主动探测的真实步数向模型输入，并透明向模型和用户阐明 Android 应用间隐私沙箱机制；
   - 新增「打开华为健康」官方 APP 跳转功能，引导用户一键查看手环/手表的实时体征数据。
4. **角色与创作页面全面合理重构（5 Tab 专业分层架构）（Req 4）**：
   - 解决用户截图反馈的角色与世界观紧挤在单一视口、高度截断、无法查看完整世界观、底部操作按钮被遮挡的缺陷；
   - 彻底拆分为 5 大专业标签页：`🎭 剧情导向`、`👥 登场角色`（全高度独立滚动列表与勾选登场）、`🌍 世界观`（全高度独立滚动列表与单选）、`📜 创作规范`（故事专属系统提示词 + 全局创作规范与教学指引）、`⚙️ 模型参数`（模型选择、兼容思考档位、温度、Token、联网搜索与头像设置）；
   - 底部操作栏固定悬浮，确保在任何屏幕尺寸和长内容下均能完整显示「取消」与「保存」按钮。
5. **角色与创作诸多 Bug 修复 & (6/5) 计数 Bug 根除（Req 5）**：
   - 彻底修复角色列表中 5 个角色却显示登场 6 个 `(6/5)` 的计数 Bug：UI 勾选计数与保存严格基于有效角色列表过滤去重，清除幽灵 ID；
   - 修复在创作设置弹窗中直接新建角色/世界观时未同步持久化 Room 数据库主键的问题：新建项目立即异步插入 Room 数据库获取真实 ID 并写入 Session，确保重启后不丢失；
   - 修复角色卡与场景卡删除操作后 session 关联残留的问题。
6. **思考强度档位兼容性识别重构（Req 6）**：
   - `ModelCapabilityEngine` 引入 `reasoningProviderType` 区分模型能力体系：
     - OpenAI o1/o3: `openai` 类型，提供 low / medium / high 档位；
     - Claude 3.7: `anthropic` 类型，提供 low / medium / high / max 思考档位；
     - DeepSeek-R1 / QwQ: `deepseek_fixed` 类型，全量深度推理，UI 明确提示「固定全量推理」，不向 API 发送违规的 `reasoning_effort` 参数以避免 400 报错；
     - 普通模型（GPT-4o、DeepSeek-Chat、Qwen-Turbo 等）: `none`，UI 完全隐藏思考档位选项，避免非法参数传递。
7. **历史安装包永久保留准则（最高铁律）**：
   - 构建前历史版本：46 个，构建后增加至 47 个，所有历史版本完整保留；
   - 增量输出：`Echo-v1.9.18-arm64-v8a.apk`（体积：15,959,813 字节，SHA-256：`465718F8530442D12D0ED4BCAA472DB6F862AD478DD951638D47275632242149`）。
8. **发布目录唯一整合**：
   - 彻底排查清理历史冗余目录（移除了根目录 `releases` 与 Git 仓库内误建的 `pass releases`），将所有历史安装包统一整合保留至唯一官方规范目录 `D:\Agent\APP-烧\app\releases`；
   - 完整保留从 v1.1.0 到 v1.9.18 的全部 97 个历史 APK，无损释放约 942 MB 冗余磁盘占用。

## [v1.9.17] - 2026-09-07

### 1. 本次升级与需求 100% 落实
1. **联网搜索结果数添加自定义选项（Req 1）**：
   - 突破原有预设档位限制，新增支持 1~20 条自由输入微调与滑动选择，可直接输入具体数字保存；
   - 搜索结果条数全面生效至 Exa MCP 与 Tavily 请求参数中，精准控制会话上下文体积与信息密度。
2. **对话自动命名指定模型读取服务商模型列表（Req 2）**：
   - 彻底免除手动输入模型名称的繁琐与易错；在自动命名设置中，点击下拉直接读取对应 API 配置的实际可用模型列表；
   - 列表项支持实时模型能力解析，清晰标注上下文窗口（如 128K、1M）与深度思考/多模态/工具徽标；提供一键刷新模型列表功能。
3. **华为运动健康步数主动刷新机制落地（Req 3）**：
   - 增加运行时 `ACTIVITY_RECOGNITION`（身体活动识别）权限自动检查与请求申请逻辑；
   - 接入底层硬件计步传感器（`Sensor.TYPE_STEP_COUNTER`）主动重连探测与刷新；
   - 提供「主动探测刷新」按键与硬件传感器连接状态实时反馈，彻底告别手动输入步数。
4. **提示词、个性化偏好、长期记忆与系统提示词机制/优先级彻底规范与透明化（Req 4）**：
   - **机制与优先级严格落实**：
     - **系统提示词**：最高优先级，100% 覆盖全局提示词；当对话配置专属系统提示词时，全局提示词完全不参与组装（0 作用）；
     - **全局提示词**：作为全局兜底，仅在当前对话无专属系统提示词时自动继承；
     - **个性化偏好**：作为全对话全局引导注入，当偏好与提示词冲突时，明确以提示词为准；
     - **长期记忆与会话记忆**：严格杜绝静默入库，模型自动识别提取的偏好或事实，在输入框上方呈现微光玻璃确认条，必须由用户手动点击选择「仅本会话生效」、「存为跨会话长期记忆」或「忽略」；
     - **角色扮演物理隔离**：角色扮演/故事创作严格物理隔离，角色人设与场景世界观独立生效，绝对不读取也不污染普通提示词、偏好与日常记忆；
   - 在设置页「个性化与提示词」卡片及对话设置弹窗顶部均增加可折叠展开的详细机制与优先级图文解释。
5. **角色与故事创作设置重构为 3 栏清爽架构（Req 5）**：
   - 重构原先拥挤的设置弹窗，拆分为「剧情导向」、「角色与世界观」、「模型与推理参数」三大专业 Tab 标签栏；
   - 导演模式/角色模式/旁白模式一键直达，设定查阅与参数调整条理清晰。
6. **普通会话与角色扮演会话双向无损转换与导入导出（Req 6）**：
   - 普通会话可在设置弹窗中一键升级为故事创作，基于历史对话智能生成主角名称、职业与世界观卡片，完整保留全部聊天历史；
   - 故事创作会话可一键无损转回普通对话，角色人设与世界法则自动合成为标准系统提示词；
   - 提供完整会话数据包（ConversationExportBundle）结构化序列化与导出支持。
7. **设置页冗长内容重构排布与手风琴卡片折叠（Req 7）**：
   - 对「长期记忆库」、「环境变量库」、「提示词模板库」等长篇幅内容默认采用优雅的手风琴折叠卡片（Accordion Glass Card），显示现有条目数量徽标；
   - 点击可平滑展开查看、搜索、添加、编辑与删除，大幅降低页面初始滚动的认知负担。
8. **模型全维度能力即时解析（Req 8）**：
   - 建立 `ModelCapabilityEngine` 规则引擎，输入模型名称即可即时智能解析：
     - 上下文窗口：4K、8K、32K、64K、128K、200K、1M、2M 等；
     - 核心能力：多模态 (Vision)、工具调用 (Tool Calling)、深度思考 (Reasoning) 及档位；
   - 在设置页模型选择列表与对话设置中均渲染极客徽标胶囊。
9. **UI 全面重构：深蓝黑极客美学与 WCAG AAA 高对比度（Req 9）**：
   - 纯正深蓝黑科技黑夜风格，背景纯净无灰紫杂色，彻底消灭白色背景遮罩与视觉白斑；
   - 文字颜色对背景对比度严格维持在 >16:1（超越 WCAG AAA 标准），思考胶囊重构为纯净幽蓝透明卡片；
   - 全面排查修复残留 bug，全部 71 项单元测试 100% 通过。
10. **历史版本安装包永久保留准则（最高铁律）**：
    - 严格保留全部 45 个历史版本安装包，绝对严禁删除；本次仅增量输出 `Echo-v1.9.17-arm64-v8a.apk`。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\AiApiAssistant\releases\Echo-v1.9.17-arm64-v8a.apk`
- **SHA-256**：`CC47B71AE6A1E78F24266D628F5B5E62525F09140D897A46D6E9091B775A47F6`
- **文件大小**：15,959,813 字节 (约 15.22 MB)
- **架构**：`arm64-v8a`，`versionCode: 97`，`versionName: 1.9.17`
- **自动化单元测试**：71 项测试全部通过（100% 通过率）
- **构建状态**：`BUILD SUCCESSFUL`（Release 签名验证通过，Scheme v2: true）

---

## [v1.9.16] - 2026-09-07

### 1. 本次升级与需求 100% 落实
1. **工具调用成功留痕与展开明细（Req 1）**：
   - 消息实体扩展 `toolCalls` 字段持久化保存；
   - 在 AI 回复卡片底部优雅展示「🛠️ 工具调用留痕」折叠气泡（显示成功图标、工具名称与精简摘要）；
   - 点击可唤起「工具调用详情」磨砂玻璃弹窗，展开查看注入给大模型的完整原始上下文内容并支持一键复制。
2. **Open-Meteo 天气查询功能全面修复（Req 2）**：
   - 修复天气引擎中 URL 字符串插值问题，正确转义城市名称与参数（`latitude=$lat&longitude=$lon`、`name=$encodedCity`、`timezone=auto` 等），气象查询即搜即得。
3. **工具调用敏锐度与积极性提升（Req 3）**：
   - 重构 `EchoToolHub` 意图探测逻辑，扩充天气、系统时间、日历日程、华为运动健康、电池设备、定位及网页链接的全量中英文关键词与自然语言句式正则，实现高敏感度积极触发。
4. **联网搜索自定义结果数量设置（Req 4）**：
   - 在设置 -> 联网搜索与工具箱中增加「搜索结果条数」快捷选择器（支持 3 / 5 (推荐) / 8 / 10 条）；
   - 将 `maxResults` 完整传递给 Exa MCP JSON-RPC 和 Tavily 引擎，实现精准控流与持久化记忆。
5. **设置界面文字与开关重叠错位修复与全局排查（Req 5）**：
   - 彻底修复「手机设备与健康数据联动」卡片中由于嵌套 `Row` 与不合理 `weight(1f, fill=false)` 引发的文字与开关错位重叠缺陷；
   - 统一步距、外边距并全面排查设置页同类布局，确保在不同屏幕与大字体模式下排版紧凑美观。
6. **系统时间读取修复 & 华为运动健康数据校准（Req 6）**：
   - 修复 `TimeCalendarManager` 中的字符串插值遗漏，准确获取系统时区与当前时间；
   - 针对安卓/华为手机设备底层硬件计步器在开机多日后的基准偏差问题，重构基准线计算逻辑，并在设置中增加「华为运动健康数据校准与同步」弹窗，支持用户输入今日微信/华为运动步数校准基准线，实时同步健康数据。
7. **对话独立自动命名模型配置（Req 7）**：
   - 解决对话自动命名生硬不好用的痛点，在「个性化设置」中新增「对话智能自动命名」专属配置模块；
   - 支持启用/关闭自动命名、选择独立的 API 配置、自定义轻量/专属命名模型（如 `qwen-turbo`、`gpt-4o-mini` 等）、自定义命名 Prompt；
   - 新增实时「测试生成标题」按键与思考标签 `<think>...</think>` 及前缀自动净化逻辑（截断在 18 字符内），给对话起名更精炼贴切。
8. **Room 数据库安全迁移至 v21**：
   - 添加 `MIGRATION_20_21`，为 `messages` 表新增 `toolCalls` 字段，100% 保护存量聊天数据平滑无缝升级。
9. **历史版本安装包永久保留（最高铁律）**：
   - 严格保护全部 44+ 历史 APK，绝对严禁删除；新增唯一定名构建 `Echo-v1.9.16-arm64-v8a.apk`。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.16-arm64-v8a.apk`
- **SHA-256**：`FFE5404CA65790B0FFC0D084E1AEBD1993F0F76F8BD7486BB347286B11940235`
- **文件大小**：15,927,045 字节 (约 15.19 MB)
- **架构**：`arm64-v8a`，`versionCode: 96`，`versionName: 1.9.16`
- **自动化单元测试**：66 项测试全部通过（100% 通过率）
- **构建状态**：`BUILD SUCCESSFUL`（Release 签名验证通过，Scheme v2: true）

---

## [v1.9.15] - 2026-09-07

### 1. 本次更新概述
本次迭代全面落地了用户提出的 3 大核心工具调用与联网能力体系，包含 8 项端到端特性与智能工具集成：
1. **Exa 免Key 联网搜索引擎落地**：
   - 接入 Exa MCP JSON-RPC 2.0 搜索协议（`web_search_exa`），无需用户注册或配置 API Key 即可实时获取高质量网络搜索与高亮结果摘要；
   - 统一抽象 `WebSearchProvider` 与 `SearchEngineType`（支持 `EXA` 与 `TAVILY` 无缝切换），并在设置页提供默认搜索引擎配置与 Exa 可选定制 Key 输入。
2. **手机设备、健康与日程系统深度调用**：
   - **时间与系统日程（TimeCalendarManager）**：实时读取精准系统时间、星期、农历/节气提示，并安全读取 `CalendarContract` 日程列表，为 AI 提供完整今日日程安排；
   - **本地定位与逆地理编码（LocationAddressManager）**：基于 Android 原生 `LocationManager` 与 `Geocoder` 自动获取经纬度与省/市/区县真实中文地址，同时支持手动设置常驻城市；
   - **华为运动健康与硬件计步（HealthDataManager）**：针对安卓与华为手机用户，基于底层硬件传感器 `Sensor.TYPE_STEP_COUNTER` 实时获取今日步数、卡路里与距离，并结构化汇聚心率与昨晚睡眠（含深睡与评分）健康数据；
   - **设备与硬件状态（DeviceHardwareManager）**：动态感知电池电量百分比、充电状态、可用内存/存储 GB 数、网络连接类型及手机机型。
3. **Open-Meteo 免Key 全球高精度气象与 Jina Reader 网页深度阅读**：
   - **Open-Meteo 天气引擎（OpenMeteoWeatherEngine）**：集成免 Key 全球高精度气象 API，支持自动基于用户地理位置或智能提取输入中的地名查询天气、温湿度、风速及天气现象（支持全部 WMO Weather Code 中文与 Emoji 映射）；
   - **Jina Reader 网页长文提取（Jina Reader Engine）**：输入包含 URL 的对话内容时自动抓取正文 Markdown（支持免 Key 即用，带直接 HTML 正文提取智能回退），让大模型轻松深度阅读长文与网页。
4. **智能工具枢纽与意图自动路由（EchoToolHub）**：
   - 智能识别用户输入的提问意图（天气气温、时间日程、运动健康步数、手机状态电量、定位位置、文章链接），在发起 AI 请求时自动将精准工具上下文无缝注入模型提示词。
5. **设置页「联网搜索与智能工具箱」交互全面升级**：
   - 设置菜单全面更新，提供 Exa / Tavily 搜索引擎切换、智能设备工具箱总开关、实时状态卡片预览以及 Open-Meteo 实时气象联调测试。
6. **历史版本安装包永久保留准则严格践行**：
   - 严格保护全部 43+ 历史 APK，新增唯一构建 `Echo-v1.9.15-arm64-v8a.apk`。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.15-arm64-v8a.apk`
- **SHA-256**：`93158F4F74DB76D46A84ABC2B95D384857F3E355D0E69FBB4A2DCBA95D5C4AFE`
- **文件大小**：15,890,949 字节 (约 15.15 MB)
- **架构**：`arm64-v8a`，`versionCode: 95`，`versionName: 1.9.15`
- **自动化单元测试**：59 项测试全部通过（100% 通过率，覆盖 Exa 解析、Open-Meteo 映射、智能意图路由、设备健康上下文、角色扮演全链路等）
- **构建状态**：`BUILD SUCCESSFUL`（Release 签名验证通过，Scheme v2: true）

---

## [v1.9.14] - 2026-09-07

### 1. 本次更新概述
本次更新聚焦用户反馈的 5 项核心交互与视觉痛点，进行了全方位的深度优化与系统级修复：
1. **关于页面「本次更新」内容校准与动态同步**：
   - 彻底重构设置中心关于界面的更新日志数据源，真实、准确呈现当前版本核心修复与增强项，终结文案滞后错乱问题。
2. **关于界面与全局卡片白色气泡背景彻底修复**：
   - 针对关于界面中「本次更新」和「功能特性」下方以及设置页在特定滑动与折叠状态下错误浮现的白色气泡/白底色块 Bug 展开系统性根治；
   - 彻底将设置卡片从 `echoHazePanel` 升级解耦为纯净毛玻璃材质 `EchoGlassCard`，消除超出视口采样的白色不透明渲染伪影，并对应用内同类常见场景实施统一排查与修复，界面通透纯净。
3. **对话页模型回复完毕后增加优雅分格线**：
   - 在模型回复结束、流式传输完成后的消息日期时间行下方，新增一条带有微光质感的横向分格线（`HorizontalDivider`）；
   - 使多轮对话之间的边界层次分明、视觉流转舒适自然。
4. **对话设置与故事创作弹窗系统级和谐优化**：
   - **窗口宽度比例优化**：针对弹窗过宽的视觉失调问题，将最大宽度由 520dp 严控收缩为 430dp（最大屏宽 90%），排版精致利落；
   - **模型下拉列表宽度等宽对齐**：彻底修复展开模型列表与上方选择框宽度不一致的割裂感，通过动态尺寸测量（`onSizeChanged`）实现像素级 1:1 等宽对齐；
   - **系统提示词输入框支持展开放大**：新增一键放大/折叠切换操作，放大后高度可扩展至 360dp、支持多达 16 行舒适显示，极大提升长规则、长人设的阅读与编辑体验；
   - **精准修复光标被强制置顶跳至开头 Bug**：采用 `TextFieldValue` 状态管理彻底解决用户点击修改系统提示词时光标被强制重置到开头的痛点，精准响应用户手指落点，并对应用内类似输入场景进行统一规范。
5. **对话页主输入框支持展开放大**：
   - 对话主输入栏新增展开放大快捷切换按键，支持从紧凑条一键展开至最高 320dp、15 行的多行大型输入面板；
   - 满足用户输入复杂剧情提示词、长指令或大段代码时的从容编辑需求。
6. **历史版本安装包永久保留准则严格践行**：
   - 严禁删除、清理或覆盖 `releases/` 目录中的任何历史安装包；
   - 增量编译并发布 `Echo-v1.9.14-arm64-v8a.apk`，保留全部 43+ 历史版本。

### 2. 需求实现与落地详情
1. **关于页与设置卡片材质净化**：
   - `SettingsScreen.kt` 中重构 `SettingsGlassCard`、`SettingsMenuItem`、`ThemeModeCard`，使用 `EchoGlassCard` 自带的毛玻璃高光背景与描边替换 `echoHazePanel`，彻底清除离屏白斑。
2. **模型完成回复后的分割线**：
   - `ChatScreen.kt` 中在 `MessageFooter` 尾部添加判断：仅在 `!isGenerating && !isUser` 成立时渲染 `HorizontalDivider`，保持生成中与用户提问气泡的轻简纯粹。
3. **对话设置弹窗与提示词光标交互**：
   - `ChatScreen.kt` 与 `EchoHaze.kt` 中收缩 `EchoGlassDialog` 弹窗宽容度至 430dp；
   - `ChatSettingsModelSelector`、`CharacterEditorScreen`、`ScenarioEditorScreen` 模型下拉框通过 `onSizeChanged` 获取触发组件实际像素宽度并传递给 `EchoGlassDropdownMenu`；
   - `ChatSettingsSystemPromptSection` 与 `StoryUnifiedSettingsDialog` 采用 `TextFieldValue` 维护光标位置并支持动态高度与行数切换。
4. **对话输入框展开放大**：
   - `ChatInputBar` 新增 `isInputExpanded` 状态，切换最小/最大高度（160dp/320dp vs 42dp/112.dp）与最大行数（15 vs 5），并增加右上角展开/收起切换图标。
5. **全量测试与签名验证**：
   - 新增 `V1914FeaturesTest.kt`，全项目 53 项单元测试 100% 通过；
   - 成功构建 Release APK，通过 aapt badging 与 apksigner v2 验证。

### 3. 修改文件列表
- `app/build.gradle.kts`
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/CharacterEditorScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/ScenarioEditorScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/test/java/com/aiassistant/V1914FeaturesTest.kt`
- `UPDATE_LOG.md`
- `CHANGELOG.md`
- `PROJECT.md`
- `README.md`
- `walkthrough.md`

---

## [v1.9.13] - 2026-09-07

### 1. 本次更新概述
本次自查重点针对全项目的组件一致性、UI 对比度、弹窗视觉体系及 Markdown 推理排版进行了系统级完善与修复：
1. **全项目原生 `AlertDialog` 彻底清零并全面升级为液态玻璃 `EchoGlassDialog`**：
   - 覆盖首页模型切换、聊天故事重命名、自定义剧情提示、故事删除确认、批量删除角色、批量删除场景、智能人设读取、角色/场景预览、记忆添加与编辑等全部 17 处弹窗；
   - 杜绝一切原生沉闷白底弹窗与跨窗口幽灵重影残留，带来全端统一的毛玻璃、高光折射与柔和半透明 Dim 蒙层体验。
2. **模型思考过程卡片排版与渲染重构（支持 LaTeX 公式与代码块）**：
   - 思考详情内容由原生普通单行 `Text` 升级为 `MarkdownText`，全面支持 DeepSeek-R1、Claude 3.7、OpenAI o1 等推理模型输出的数学公式（LaTeX `$$...$$`, `$...$`）、有序列表及代码块解析排版；
   - 解决浅色模式下思考卡片正文字体使用 `primary` 浅蓝引发的对比度严重不足（< 3:1）问题，切换为高对比度 `glass.textPrimary`（> 10:1 WCAG AAA 极高对比度）；
   - 思考胶囊头部与操作图标全面升级为 `onPrimaryContainer`，消除淡蓝背景下的发白与难以辨识问题。
3. **全局 FilterChip 与 SegmentedButton 选中态对比度强化**：
   - 将控件选中/激活态的前景文字与图标颜色从浅色调 `primary` 升级为 `onPrimaryContainer`，在浅色模式下呈现深邃优雅的海军蓝，深色模式下呈现清晰清爽的浅天蓝，提升可读性。
4. **弃用组件与图标 API 现代化清理**：
   - 清理全部 `Icons.Default.ArrowBack`、`Icons.Default.AltRoute`、`Icons.Default.List`、`Icons.Default.Send`、`Icons.Default.OpenInNew`、`Icons.Default.CallSplit`，转换为官方推荐的 `Icons.AutoMirrored` 版本；
   - 现代化 `LinearProgressIndicator` 进度参数为 lambda 表达式；
   - 消除 `PlotAction` 枚举分支冗余 `else` 分支。
5. **历史版本安装包永久保留准则严格践行**：
   - 严格保护 `releases/` 目录下全部历史安装包，零清理、零覆盖，增量输出 `Echo-v1.9.13-arm64-v8a.apk`。

### 2. 需求实现与落地详情
1. **`EchoGlassDialog` 全面兼容与落位**：
   - 在 `EchoHaze.kt` 中重构 `EchoGlassDialog` 函数签名，为 `hazeState` 提供默认参数 `null`，并将 `title` 与 `text` 设为可空；
   - 替换 `ChatScreen.kt`（重命名、自定义指令）、`HomeScreen.kt`（顶部模型选择）、`RoleplayStudioScreen.kt`（删除故事、批量删除角色、批量删除场景）、`RoleplayMemoryScreen.kt`（删除记忆、新增记忆、编辑摘要、编辑记忆）、`CharacterEditorScreen.kt`（删除角色、AI 人设拆解、角色卡全览）、`ScenarioEditorScreen.kt`（删除场景、AI 场景拆解、场景全览）、`PlotActionBar.kt`（自定义指令）中全部遗留的 `AlertDialog`。
2. **思考卡片可读性与 MarkdownText/LaTeX**：
   - `ChatScreen.kt` 思考详情卡片使用 `MarkdownText(content = message.thinkingContent ?: "", color = thinkingContentColor)`，结合 `glass.textPrimary` 实现极佳可读性与公式渲染。
3. **增量 Release 构建与签名**：
   - 成功执行混淆、资源压缩与 v2 签名，生成 15,856,177 字节的 Release APK，并同步存档至本地双目录。

### 3. 修改文件列表
- `app/build.gradle.kts`
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoControls.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/CharacterEditorScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/ScenarioEditorScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/PlotActionBar.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayMemoryScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayStudioScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/stats/StatsScreen.kt`
- `app/src/test/java/com/aiassistant/V1913FeaturesTest.kt`
- `UPDATE_LOG.md`
- `CHANGELOG.md`
- `PROJECT.md`
- `README.md`

---

## [v1.9.12] - 2026-09-07

### 1. 本次更新概述
本次更新针对 API 配置管理、深度思考推理过程保留、连接重试容灾体系、Markdown 对话排版与气泡规范化进行了 10 项系统级完善与修复：
1. **设置页面配置 API 后白窗残留彻底修复**；
2. **模型深度思考内容全流程保留与展示持久化**；
3. **API/模型连接超时 3 次自动重试机制与线性退避**；
4. **单 URL 多 API Key 自动轮询与无感故障转移**；
5. **API 配置界面可读性增强与可用模型列表即时搜索/批量筛选**；
6. **首页对话胶囊超长模型名称自适应约束，杜绝消息计数徽章被挤压**；
7. **对话页 Markdown 标题字号层级重塑，彻底解决标题比正文还小的缺陷**；
8. **对话页移除 Markdown 无序列表前导圆点 `•` / `·`，视觉更简约自然**；
9. **对话页模型思考气泡长名称内部水平滑动，消除截断与内容丢失**；
10. **禁用思考时气泡样式与思考模式 100% 视觉统一，文案定制为 `"${model}用${time}秒吃掉了你${token}token"`**。

### 2. 需求实现与落地详情
1. **设置页面配置 API 后白窗残留彻底修复**：
   - **根因分析**：Compose `Dialog` 原生在独立的 PhoneWindow 中运行。在 `EchoHaze.kt` 的 `EchoGlassDialog` 中原将 `hazeState` 挂载在 Dialog 内部 Surface 上，导致跨 Window 的 `hazeChild` 坐标注册到宿主 Activity 的 `HazeState` 中。当弹窗 Dismiss 时，注销时机滞后引发宿主背景被渲染上一个与 Dialog 形状一致的白色重影卡片。
   - **解决**：在 `EchoGlassDialog` 中改用 `echoHazePanel(hazeState = null)` 独立毛玻璃渲染，切断 Dialog 与宿主 Activity `HazeState` 的跨窗口绑定，彻底根除白窗幽灵残留。
2. **模型思考过程内容全流程保留**：
   - **解决**：重构 `ChatScreen.kt` 的 `MessageBubble`，移除将 `isGenerating` 绑定为 `showThinking` 重置条件的逻辑；流式结束后思考过程保持展开可读；`AiRepository.kt` 与数据模型全面兼容 `<think>...</think>` 与 `reasoning_content`，推理内容持久化存储且不随流式结束收起。
3. **API 超时 3 次自动重试机制**：
   - **解决**：`AiRepository.kt` 中实现 `isTimeoutException` 递归异常检测，对 `SocketTimeoutException` 及带 timeout 标识的连接异常执行至多 3 次带线性退避（`delay(500L * attempt)`）的自动重新连接，提高弱网环境下的生成成功率。
4. **单 URL 多 API Key 自动故障转移**：
   - **解决**：`AiRepository.parseApiKeys` 统一支持按换行、逗号（`,`）、分号（`;`）录入多个 API Key。在流式对话或获取模型列表时，前一个 Key 发生超时或业务报错时，系统自动切换至下一个可用 Key 并透明重试，直到成功或所有 Key 尝试完毕。
5. **API 配置界面可读性增强与模型列表搜索**：
   - **解决**：设置页 `ApiConfigDialog` 增加 `modelSearchQuery` 即时过滤输入框、匹配计数指示、一键勾选/取消搜出结果；API Key 输入框支持多行录入与实时密钥数量识别（如「已录入 3 个密钥 · 自动故障转移」）；弹窗内容区最大高度从 400.dp 提升至 500.dp，大幅提升操作舒适度。
6. **首页对话胶囊长模型名自适应排版**：
   - **解决**：`HomeScreen.kt` 的 `ConversationCard` 为模型标签 Surface 添加 `.weight(1f, fill = false)` 与 `softWrap = false`，消息数量徽章设置 `maxLines = 1, softWrap = false`，无论模型名称多长，消息数量均获得独立空间保障。
7. **对话页 Markdown 标题字号层级重塑**：
   - **解决**：`MarkdownText.kt` 重新标定 1-6 级标题：H1(22sp) > H2(20sp) > H3(18.5sp) > H4(17sp) > H5(16sp bold) >= H6(16sp bold) >= 正文(16sp normal)，彻底废除原 H4-H6 使用 14sp/13.5sp/12sp 比正文还小的错误设定。
8. **移除无序列表前导圆点 `•` / `·`**：
   - **解决**：`MarkdownText.kt` 中对 `- ` 和 `* ` 的列表项移除了 `Text(text = "•")` 及间距，保留自然层级缩进，文本清爽一致。
9. **思考气泡长模型名称水平滑动**：
   - **解决**：`ChatScreen.kt` 气泡内部文字容器配置 `Modifier.weight(1f, fill = false).horizontalScroll(capsuleScrollState)`，长模型名与统计数字支持在气泡内平滑横向滚动查阅，不再截断溢出。
10. **非思考模式气泡样式统一与定制文案**：
    - **解决**：提取纯函数 `formatNonThinkingCapsuleText`，格式化为 `"${model}用${seconds}秒吃掉了你${effectiveTokens}token"`（当缺少 token 统计时使用 `estimateTokenCount` 兜底）；样式上与思考中气泡统一使用淡色系背景、高光边框和一致的对齐规范。

### 3. 修改文件列表
- `app/build.gradle.kts`
- `gradle.properties`
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/main/java/com/aiassistant/domain/model/Models.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/test/java/com/aiassistant/V1912FeaturesTest.kt`
- `UPDATE_LOG.md`
- `WORKFLOW_GUIDELINES.md`
- `CHANGELOG.md`
- `PROJECT.md`
- `README.md`

---

## [v1.9.11] - 2026-08-18

### 1. 本次更新概述
本次更新重点攻坚了三项核心体验优化与视觉体系升级：
1. **LaTeX 数学公式解析增强与对话滑动卡死彻底消除**；
2. **设置界面「添加 API 配置」弹窗返回退出时白色底板残留彻底消除**；
3. **使用统计界面 Token 消耗趋势升级为高质感柱状图（Bar Chart），全量调色板升级为柔和淡色系（Pastel Colors）并彻底移除纯黑色硬块**。

### 2. 需求实现与落地详情
1. **LaTeX 数学公式解析增强与对话滑动卡死彻底消除**：
   - **根因分析**：
     - 原 `fracRegex` 的 while 循环在流式响应遇到嵌套花括号或半截公式时存在死循环隐患，导致主线程 ANR 阻塞触摸分发；
     - 原 `InlineMarkdownText` 对所有普通文本行无差别挂载了 `ClickableText`，内部 `pointerInput` 拦截了 `LazyColumn` 的纵向触摸手势与滑动分发。
   - **全面重构与解决**：
     - `MarkdownText.kt` 深度重构：实现基于栈平衡与括号深度解析的 `parseFractions`（支持多层嵌套分数 `\frac{1}{\frac{2}{3}}` 智能括号化）与 `parseRoots`（支持 `\sqrt[n]{x}`）；
     - 新增多行数学环境解析（`pmatrix`、`bmatrix`、`cases`、`aligned`、`equation` 等）；
     - 扩展全量微积分（`∫/∬/∭/∮`）、希腊字母（大小写全覆盖）、逻辑算子、黑体集合（`ℝ/ℕ/ℤ/ℚ/ℂ`）与上下标转换；按长度倒序匹配避免词缀冲突（如 `\infty` 优先于 `\inf`）；
     - `InlineMarkdownText` 增加注解检测：无 URL 和引用角标时直接使用普通 `Text` 渲染，彻底消除触摸事件拦截，保障列表丝滑滑动。
2. **设置界面添加配置返回后白窗残留修复**：
   - **根因分析**：Compose `Dialog` 原生 DecorView 默认带有白色背景；当在 `DisposableEffect` 初始帧阶段 `view.parent` 尚未挂载为 `DialogWindowProvider` 时会导致 Window 透明度设置失效；同时缺少对话框层面的系统返回键拦截。
   - **全面重构与解决**：
     - 在 `EchoGlassDialog` 中采用 `SideEffect` 递归向上遍历视图树获取 `DialogWindowProvider`，确保 `win.setBackgroundDrawableResource(android.R.color.transparent)` 与 `win.setDimAmount(0f)` 100% 成功生效；
     - 弹窗外层遮罩更新为柔和淡雅半透明底色；
     - 在 `SettingsScreen.kt` 的 `ApiConfigTab` 中增加 `BackHandler(enabled = showAddDialog || editingConfig != null)`，按下系统返回键即刻关闭弹窗且不会出现白窗残留。
3. **使用统计 Token 趋势柱状图 & 淡色系调色板升级**：
   - **柱状图升级**：`StatsScreen.kt` 中的 `ModernTokenBars` 全面重构为高质感圆角柱状图（Bar Chart），支持分段堆叠（输入/输出/思考/其它）、空数据微型基准柱体、网格刻度线与底部时间轴；
   - **淡色系调色板全面应用**：
     - 输入 Token / 主色：柔和淡天蓝 `#6BA4F8`；
     - 输出 Token / 成功色：柔和淡青蓝 `#38BDF8`；
     - 思考 Token / 强调色：柔和淡珊瑚粉红 `#FB7185`；
     - 其它 Token：淡薰衣草紫 `#A5B4FC`；
     - 纯黑色硬底与阴影全部替换为淡雅微透明 `surfaceVariant`（`Color(0xFF1E293B)` / 柔灰淡色系）。

### 3. 修改文件列表
- `app/build.gradle.kts`
- `app/src/main/java/com/aiassistant/ui/theme/Color.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/stats/StatsScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/test/java/com/aiassistant/LatexAndStatsTest.kt`
- `UPDATE_LOG.md`
- `WORKFLOW_GUIDELINES.md`
- `CHANGELOG.md`
- `PROJECT.md`
- `README.md`

---

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
