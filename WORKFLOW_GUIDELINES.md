# 每任务执行清单与留痕规范 (Workflow Guidelines)

> 常驻准则见根目录 `D:\Agent\APP-Echo\AGENTS.md`（最高准则、事实卡、发布铁律、Git 规则）。
> 本文件是**每次任务的执行清单模板 + 最近一次执行记录**，随代码一并提交留痕；更早的执行历史见 `UPDATE_LOG.md`。

## 一、每任务执行清单（交付前逐项勾选，未执行项必须如实标注"未执行 + 原因"）

### A. 基线确认
- [ ] `git status` / `git log --oneline -3`：分支正确、已有未提交改动已向用户说明，未混入本次任务
- [ ] 确认当前 versionName / versionCode（`app/build.gradle.kts`）与 AppDatabase version（`AppDatabase.kt`）

### B. 实现
- [ ] 只实现用户明确要求的内容；同类问题已全仓排查
- [ ] 未做无关重构；未修改与需求无关的文件
- [ ] 已输出临时实施方案供用户参考（根因/需求理解、改动思路、涉及文件、影响面与验证方式；见 AGENTS.md §2.3）

### C. 验证（记录每条命令的退出码）
- [ ] `./gradlew.bat compileDebugKotlin --no-daemon`
- [ ] `./gradlew.bat testDebugUnitTest --no-daemon`
- [ ] `./gradlew.bat lintDebug --no-daemon`
- [ ] `git diff --check`

### D. 留痕（均在仓库内更新，根目录同名文件为指针不改）
- [ ] `UPDATE_LOG.md` 新条目置顶
- [ ] `PROJECT.md` 版本 / DB / 功能 / 构建命令（有变化时）
- [ ] `README.md` 版本与功能清单（发版时）
- [ ] `CHANGELOG.md`（发版时）
- [ ] `walkthrough.md`（发版时）+ 本文件「最近一次执行记录」

### E. 发布（仅当用户要求 APK）
- [ ] versionName / versionCode 已递增；`splits.abi` 仅 `arm64-v8a`、`isUniversalApk = false`
- [ ] `./gradlew.bat assembleRelease --no-daemon`
- [ ] 复制为 `D:\Agent\APP-Echo\app\releases\Echo-v<version>.apk`（增量输出，严禁删除/覆盖历史包，严禁架构后缀命名）
- [ ] SHA256 + `apksigner verify --print-certs` 已记录；已如实标注签名性质（echo-release.jks 为 Android Debug DN，非正式生产签名）
- [ ] 未声称设备安装/启动验证（除非确已执行），已给人工验收步骤

### F. Git
- [ ] 提交前确认无密钥、local.properties、APK、构建产物、用户数据
- [ ] 只提交本次真实修改；发版时打 `v<version>` 标签（不要遗漏：v2.5.8/v2.5.9 曾漏打）
- [ ] push 并 `git ls-remote origin main` 核验；失败时如实报告本地领先数与原因

## 二、最近一次执行记录

### [2026-10-01] v2.6.8 等待提示不再顶屏 + 统计模型名可滑动 + 健康时间线 14×6 + 后台生成稳定性 + 菜单配色/胶囊收缩（发版）
- **需求**：① 连接等待文字出现/消失不再导致屏幕滑动；② 统计页多处模型名可左右滑动看全；③ 请求健康时间线所有时间范围保持 14 × 6；④ 返回首页/点击其他对话不影响模型连接与回复；⑤ 回复右下角三点弹窗颜色不均、边缘黑影；⑥ 连接胶囊多行时无法收缩、收缩/展开使左侧图标错误居中。构建 APK。
- **版本**：versionCode 164 / versionName 2.6.8 / Room v31（无 DB 变更）。
- **实现要点**：提示改固定槽位（`minLines = maxLines = 2`）+ alpha 渐变（`animateFloatAsState`，reduced motion 用 snap），连接/重连期布局零抖动；胶囊展开态改 `statusExpandOverride: Boolean?` 两级（收起必然单行），胶囊行恒 `Alignment.Top` 且图标恒定 top padding，新增 `showStatusToggle`；`EchoGlassDropdownMenu` 改自绘 Popup（不透明底色 + tonal/shadow 均为 0，新增 `EchoMenuPositionProvider` 移植 M3 锚点避让）；新增 `ScrollableSingleLineText` 替换四处模型名截断；`StatsPeriod.heatmapCells` 全量 84（14 × 6）；需求 4 按子代理根因审查逐条修复——上下文回退提示提升到 `ActiveSession`（`answerContextFallbackPrompt`，重进会话转发并可应答）+ `withTimeoutOrNull(300s)` 兜底、`isCurrentSession`/CAS `removeSession(id, session)` 身份守卫三处收尾、`MainActivity` 六处 `chat/` 导航补 `launchSingleTop`、会话级 `keyAttemptErrors` 恢复与停止时优先读取。
- **文件**：`ChatMessageComponents.kt`、`EchoHaze.kt`、`StatsScreen.kt`、`ChatGenerationManager.kt`、`ChatViewModel.kt`、`MainActivity.kt`、`StatsDashboardTest.kt`、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 495 项全通、0 失败）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.6.8.apk`（16,700,269 字节，SHA256 `2A535BAD06B507FA8DCF7A6CE2907B2828AD37D4E0C8F6995F10AFEBCF884C9C`，CN=Android Debug **非正式生产签名**，证书与 v2.6.7 一致）；历史包完整保留。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.6.8 节）。
- **已知剩余项**：生成中的排队消息仍存在于 ViewModel 本地（`_messageQueue`），用户离开会话页会随 VM 丢失；本轮未迁移到应用级（涉及发送链路与队列 UI 多处，避免无真机情况下的高风险重构），已在交付报告如实说明。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] v2.6.7 连接胶囊/计时/防误滚动/压缩预览修复 + 使用统计七项改进（发版）
- **需求**：头像与连接胶囊对齐不漂移、胶囊只向下延展；连接计时提示不消失重现（追加：换 Key 重试可重新计时但原因必须可视）；连接过程防误滚动；压缩对比预览稳定弹出且"完成=确认生效"；使用统计七项（悬浮栏+同行下拉、速度标平均、切换纵向、模型多选、成功率口径修正、热力行数+2、去重复标题）。构建 APK。
- **版本**：versionCode 163 / versionName 2.6.7 / Room v31（无 DB 变更）。
- **实现要点**：头像+胶囊改顶对齐同行（Avatar 增可选 modifier 参数，offset -1dp），等待提示移至行下；计时提升至气泡级按尝试阶段计时（LaunchedEffect(isGenerating, reconnectStatus)），等待期胶囊 3 行换行、重连期提示带已等待；ChatScreen snapshotFlow 末项尺寸变化即钉底（autoFollowOutput 时）；压缩卡片四状态去 remember key + onRegisterApplyPending + 完成键应用待确认变更；统计页 Box 覆盖式悬浮栏（StatsFilterDropdown×2 同行）、deselectedModels 多选、heatmapCells 各周期 +28、成功率补记 ApiUsageStat(success=false)。
- **文件**：ChatMessageComponents.kt、ChatScreen.kt、ChatContextComponents.kt、StatsScreen.kt、ChatViewModel.kt、StatsDashboardTest.kt、app/build.gradle.kts 及文档。
- **验证**：compile/test（495 全通）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，Echo-v2.6.7.apk（16,700,269 字节，SHA256 d9b35ca9…b05467，CN=Android Debug 非正式生产签名，与 v2.6.6 证书一致）；历史包完整保留。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.6.7 节）。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] 流式等待加载动画重新设计：呼吸光环点（非发版）
- **需求**：流式输出的加载动画不够好看，重新设计。
- **版本**：versionCode 162 / versionName 2.6.6 / Room v31（均无变更，未发版）。
- **实现要点**：`EchoWaveDots`（三点波浪、文本色）→ `EchoThinkingDots`（呼吸光环点）：相位错开呼吸脉冲（放大+38%、提亮、身后光环 1.4r→3.0r 扩散渐隐），脉冲占周期前 55%、余下静息；配色改吃 `generationAccentColor` 与光标/脉冲环同源；周期入 `EchoMotion.Cycle.thinkingDots=1500`；单 InfiniteTransition + Canvas 仅 draw 阶段读取、reduced motion 静态降级（R-3/R-7 不变）。
- **文件**：`EchoConnectionIndicator.kt`、`EchoMotion.kt`、`ChatMessageComponents.kt` 及文档。
- **验证**：compile/test/lint/diff --check 全部 Exit Code 0；无真机，人工验收：新对话首 token 前观察气泡内呼吸光环动画（思考档位色/primary），系统「移除动画」下为静态三点。
- **详情**：见 `UPDATE_LOG.md`。

### [2026-10-01] v2.6.6 修复新建对话发送消息闪退（LC 窗口空区间 coerceIn）（发版）
- **需求**：新建对话发送消息后应用直接闪退（严重 bug），立即修复并全仓排查同类问题。
- **版本**：versionCode 162 / versionName 2.6.6 / Room v31（无变更）。
- **根因**：v2.6.4 LC 档窗口计算 `coerceIn(2, usableMessages.size)` 在消息数 < 2 时构成空区间抛 `IllegalArgumentException`；首发消息落库后 Room 流触发 `refreshContextUsage` 档位预览即崩。
- **修复与加固**：LC 窗口改 `minOf(size, maxOf(2, window))`；`ScrollAssist` 滚动拇指与 `ReadableColors` 背景取样两处潜在同类崩溃加固（`minOf/maxOf` 或先钳制参数）；全仓 121 处 `coerceIn` 逐一核对，其余边界恒有效。
- **文件**：`ChatContextAssemblyHelper.kt`、`ScrollAssist.kt`、`ReadableColors.kt`、`CompressionTierPolicyTest.kt`（+2 项）、`build.gradle.kts` 及文档。
- **验证**：compile/test(495)/lint/diff --check/assembleRelease 全部 Exit Code 0；APK `Echo-v2.6.6.apk` (SHA256 `77A4A284D877A26D76E10B3DC2058DF6739FEAD3F95E4B82BCA62E3AEA3B069E`)。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] v2.6.5 删除回复不再影响正在生成的回复（回滚误改 + 生成锚点）（发版）
- **需求**：修正 v2.6.4 误改（删除导致生成中回复消失）；删除同一位置的过去回复时，正在连接/输出的回复位置必须不变（此前会跳到上方/下方变成额外回复，结束后又合并回原位）。
- **版本**：versionCode 161 / versionName 2.6.5 / Room v31（无变更）。
- **实现要点**：回滚 `cancelGenerationIfDeletingActiveTurn`；新增 `GeneratingAnchor`（触发本轮的用户消息 id + user 分组）存入生成会话并随重进恢复；流式气泡优先内联挂载于锚点之后（`isGeneratingAnchorHostItem`），组内有已落库回复时维持组内挂载，仅锚点缺失时回退底部兜底；兜底条件收紧为 `!isBranchStreamingMounted`。
- **文件**：`ChatViewModel.kt`、`ChatGenerationManager.kt`、`ChatScreen.kt`、`ChatMessageComponents.kt`、`RegenerateVariantSwitcherTest.kt`（+3 项）、`build.gradle.kts` 及文档。
- **验证**：compile/test(493)/lint/diff --check/assembleRelease 全部 Exit Code 0；APK `Echo-v2.6.5.apk` (SHA256 `0011741AFF84D1B40CE586A93EBF28FAB0583C4203A88D4F530C7E2C9A581B80`)。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] v2.6.4 生成中删除回复静默取消、统计新时间范围、热力看板铺满、token 真实性与 TPS（发版）
- **需求**：①连接中删除回复的多窗口/合并问题；②新增 4小时/8小时/3天 范围；③健康时间线任意范围铺满；④token/缓存真实性优化 + TPS 维度（移除峰值）。
- **版本**：versionCode 160 / versionName 2.6.4 / Room v31（无变更）。
- **实现要点**：`cancelGenerationIfDeletingActiveTurn` 静默取消（不落占位）；`StatsPeriod` 8 档 + 热力格数全取 14 倍数；修复 Anthropic 缓存口径（input 含 cache_read+creation、命中仅计 cache_read）与 thinking 双计（Anthropic/OpenAI `<think>` 扣除估算）；`avgTps` 概览指标 + 模型表 TPS 标签与「速度」排序。
- **文件**：`ChatViewModel.kt`、`AiRepository.kt`、`StatsScreen.kt`、`StatsDashboardTest.kt`、`build.gradle.kts` 及文档。
- **验证**：compile/test(490)/lint/diff --check/assembleRelease 全部 Exit Code 0；APK `Echo-v2.6.4.apk` (SHA256 `4080DC76731F8B590AD8730C1F7D4F56DEFC7A1ADDF390DACED398FD02BD92B0`)。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] v2.6.3 连接失败胶囊显示具体原因（发版）
- **需求**：用户截图反馈生成中连接失败只显示「模型连接失败」标题、看不到原因。
- **根因**：胶囊文案 `isConnectionFailed` 分支硬编码标题，替换掉了含具体原因的重连状态文本。
- **修复**：生成中失败改为直接显示 reconnectStatus 原因文本（可展开）；`isConnectionFailed` 加 `content.isBlank()` 守卫防恢复后残留失败态；首个 token 到达清空实时失败明细。
- **版本**：versionCode 159 / versionName 2.6.3 / Room v31（无变更）。
- **文件**：`ChatMessageComponents.kt`、`ChatViewModel.kt`、`build.gradle.kts` 及文档。
- **验证**：compile/test(486)/lint/diff --check/assembleRelease 全部 Exit Code 0；APK `Echo-v2.6.3.apk` (SHA256 `18C5EB74A0BB2B4BEDB811AC840EF688C436B7D337F8E3488334D7F4B5E21AB9`)。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] v2.6.2 自定义比例压缩档、对比预览完善、连接报错实时可见与数据看板修复增强（发版）
- **需求**：
  1. 压缩支持自定义百分比条数（L2-L4 对长对话压缩 97%+ 过狠）；
  2. 前后对比预览 UI 完善；
  3. 连接报错无需手动暂停即可见；
  4. 看板 a-e：环形图灰色间隔 / 模型占比视图 / 平滑折线趋势 / 排序高亮 / 热力矩形看板。
- **版本**：versionCode 158 / versionName 2.6.2 / Room **v31**（MIGRATION_30_31 新增 conversations.compressionCustomPercent，非破坏性）。
- **实现要点**：新增 `CompressionTier.LC`（默认 30%，10~90 步长 5）；LC 窗口=ceil(总数×百分比)、预算内尽量保留、置顶与最近 1 轮强制保留；预览滑杆经 `getConversationContextUsage` 覆盖参数实时重算；`keyAttemptErrors` StateFlow + MessageBubble 实时明细卡；`donutSweepDegrees` 无缝化、`toModelDonutSlices`、`buildModelTokenSeries`+`ModelTokenTrendChart`、排序高亮、`buildHealthCells`+`HealthTimelineCard`。
- **验证**：
  - `compileDebugKotlin`: Exit Code 0
  - `testDebugUnitTest`: Exit Code 0 (73 测试文件，486 项全通，新增 10 项)
  - `lintDebug`: Exit Code 0
  - `git diff --check`: Exit Code 0
  - `assembleRelease`: Exit Code 0
  - Release APK 输出至 `D:\Agent\APP-Echo\app\releases\Echo-v2.6.2.apk` (SHA256: `4CEAC42E7981586AC4866C770DC8BB2F29BA01DB3B85E450D913CC102FE6C5E2`)
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-09-30] v2.6.1 流式输出重复/错位修复与使用统计看板全面改版（发版）
- **需求**：
  1. 修复流式输出偶现「两个相同回复同时进行流式输出」或「回复位置错误」（回复结束后恢复正常）；
  2. 全面更新使用统计界面，优化美观度与数据可视化，提供更丰富的数据看板。
- **版本**：versionCode 157 / versionName 2.6.1 / Room v30（无 DB 实体变更，仅查询补次级排序键）。
- **实现要点**：
  1. 根因：`ChatScreen.kt` 分支流式挂载判定中 `pairedVariantGroupId` 对 `reply_*` 命名返回 `null`，与未分组消息项的 `null` groupId 构成 `null == null` 判等，导致每个未分组消息后多渲染一份相同流式气泡；`isStreamingBranchHostItem` 显式空值防护后统一两处判定；`MessageDao` 补 `id ASC` 次级排序；
  2. 统计看板改版：环比对比芯片、3×3 指标网格（新增失败次数/缓存命中率/平均响应/峰值单段）、Token 构成环形图、24 小时调用分布、供应商消耗占比、失败原因归纳、模型明细表增强、图例按卡片配置、空态收敛；纯逻辑 internal 化。
- **文件**：`ChatScreen.kt`、`ChatMessageComponents.kt`、`Daos.kt`、`StatsScreen.kt`、`RegenerateVariantSwitcherTest.kt`、`StatsDashboardTest.kt`（新增）、`build.gradle.kts` 及相关文档。
- **验证**：
  - `compileDebugKotlin`: Exit Code 0
  - `testDebugUnitTest`: Exit Code 0 (73 测试文件，476 项全通，新增 18 项)
  - `lintDebug`: Exit Code 0
  - `git diff --check`: Exit Code 0
  - `assembleRelease`: Exit Code 0
  - Release APK 输出至 `D:\Agent\APP-Echo\app\releases\Echo-v2.6.1.apk` (SHA256: `E59449093D1FB039F9D8489E89A1F338DD7BE8D7A2F468863BC331D01A2AB495`)
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-09-30] v2.6.0 上下文回退选择、设定时间线编辑、连接失败胶囊优化与供应商拖拽排序（发版）
- **需求**：
  1. 弱网连接不畅或空响应导致上下文回退时弹窗提供「回退 / 忽略 / （当前对话）永久忽略」选择；
  2. 时间线与设定提取/变更待确认卡片支持长按文字直接进入编辑模式，编辑后提供「取消」与「应用」；
  3. 模型连接失败时胶囊不显示消耗 Token，直接显示「模型连接失败」，点击胶囊可自由折叠/展开红框错误报告；
  4. 拖动模型或 API Key 排序时关闭交换边界处的震动反馈；
  5. 设置里的模型供应商支持长按手柄拖动排序并持久化记住。
- **版本**：versionCode 156 / versionName 2.6.0 / Room v30。
- **文件**：`ChatScreen.kt`、`ChatMessageComponents.kt`、`ChatViewModel.kt`、`AiRepository.kt`、`Models.kt`、`PersonalizationManager.kt`、`SmoothReorderState.kt`、`SettingsScreen.kt`、`SettingsApiConfigDialog.kt`、`UiPolishRegressionTest.kt`、`build.gradle.kts` 及相关文档。
- **验证**：
  - `compileDebugKotlin`: Exit Code 0
  - `testDebugUnitTest`: Exit Code 0 (73 测试文件，458 项全通)
  - `lintDebug`: Exit Code 0
  - `git diff --check`: Exit Code 0
  - `assembleRelease`: Exit Code 0
  - Release APK 输出至 `D:\Agent\APP-Echo\app\releases\Echo-v2.6.0.apk` (SHA256: `CF38591EC6083249588ED24A713E77FBDB46F95F38E963E33A23A8C3FF0BCBC7`)
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。
