# Echo v2.6.9 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.9 (`versionCode: 165`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.9.apk`
- **核心内容**：会话专属设定备份与智能导入原有对话（需求 1）、回复三点弹出菜单左右填铺满统一修复（需求 2）、输出中断思考与回复强制入库保全（需求 3）、思考胶囊文字垂直居中（需求 4）、思考胶囊模型名字物理固化与隔离（需求 5）。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| 会话专属设定备份与智能导入原有对话（需求 1） | ① 单对话备份导入 `restoreSingleConversationFromJson` 无条件执行 `conversation.copy(id = 0L)` 并 `insertConversation`，永远在本地新增"新对话"，原对话被架空无法导入；② 全量数据库合并引擎中 `memory_items` 被归入 `simpleTables` 盲插，未对 `conversationId` 重映射，ID 重分配后专属设定与会话失联；`conversation_branches` 同理；③ 备份前未执行 WAL checkpoint | ① `restoreSingleConversationFromJson` 引入原有会话智能匹配机制（ID/创建时间戳/自定义标题一致），命中原有会话时直接作为目标会话（`targetConvId = matchedConv.id`），智能更新会话设定并开启专属设定总开关；消息、时间线节点及专属设定（`memory_items`）执行内容去重增量合并入库，绝不强制新建新对话；② `mergeDatabaseFromBackup` 专有重映射 `memory_items` 和 `conversation_branches` 的会话 ID 并去重；③ `MemoryDao` 拓宽查询和删除范围至全部 `WHERE conversationId = :conversationId`；④ `createBackup` 前显式执行 `PRAGMA wal_checkpoint(FULL)` |
| 菜单左右填铺满（需求 2） | v2.6.8 重构 `EchoGlassDropdownMenu` 为自绘 Popup 后丢失了 M3 默认的宽度包裹约束，Popup 默认可用宽度为屏幕全宽，而 `DropdownMenuItem` 自带 `fillMaxWidth()`，撑满整屏 | Surface 与 Column 显式施加 `Modifier.width(IntrinsicSize.Max).widthIn(min = 160.dp, max = 280.dp)` 限制，确保无论在任何屏幕尺寸或子项内容下，菜单宽度自适应内容并收敛在 160dp ~ 280dp 之间，全仓所有液态玻璃菜单统一恢复紧凑优雅 |
| 输出中断思考与回复强制保全（需求 3） | OkHttp 流式断开或用户/系统打断抛出 `Socket closed` / `Canceled` 异常时，此前直接 `return@launch`，或用通用错误占位覆盖已接收内容，已收到的数百字思考或正文被清空丢失 | 在 `onError` 与 `catch` 异常块中增加抢救机制：只要 `partialResponse.isNotBlank() || partialThinking != null`，不论异常类型，第一时间通过 CAS 抢救入库，保存已有思考链与正文内容，正文末尾精准标注中断说明，严禁任何覆盖与清空 |
| 思考胶囊文字垂直居中（需求 4） | 思考胶囊内 Row 默认对齐不对齐，Text 组件受 Android 系统字体默认 paddingTop 与 leading 影响，视觉明显偏上 | 单行思考胶囊 Row 统一 `Alignment.CenterVertically`；文字 Box 显式 `Alignment.CenterStart`；`Text` 注入 `lineHeight = 16.sp` 与 `lineHeightStyle = LineHeightStyle(Alignment.Center, Trim.Both)`，消除字体系统内边距，实现文字精准物理居中 |
| 思考胶囊模型名字物理固化（需求 5） | `Message` 实体历史上未设计 `modelName` 字段，前端思考胶囊依赖动态反查，反查不到时回退到顶部选中的动态模型 `currentAssistantModelName`，用户在顶部切换模型会导致历史胶囊名字被错误覆盖替换 | ① `Message` 实体新增持久化字段 `val modelName: String? = null`，Room 数据库升至 `32` 并新增 `MIGRATION_31_32`；② `AiRepository` 与 `ChatViewModel` 保存消息时均写入发起调用的实际模型名字；③ `updateMessageModelMap` 反查到存量消息模型名时立即调用 `repository.updateMessageModelName` 回填入库持久化；④ `ChatScreen` 思考胶囊展示彻底移除回退到 `currentAssistantModelName`，永久物理固化 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**498 项全通、0 失败**，新增 MIGRATION_31_32、备份设定序列化与 modelName 固化断言）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.6.9.apk`，16,700,269 字节 (~15.93 MB)，SHA256 `5212A9D58FAEC29B491EBBCA18D66A5E600BF85781163153FC0212875DB001D5`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留，本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. **需求 1（会话专属设定备份与恢复）**：
   - 进入任一包含会话专属设定（角色特征、世界观规则、行为约束）的对话，长按或进入菜单执行「备份单对话」；
   - 在对话中追加几条消息或修改部分设定后，在设置页或首页重新导入刚才的单对话 JSON 备份：应用应提示导入成功，回到该会话检查——原对话**未被新建重复会话**，原有对话已融合更新，会话专属设定总开关处于开启状态，专属设定卡片 100% 完整展示，消息与时间线无缝增量合并。
2. **需求 2（三点菜单左右铺满修复）**：
   - 在对话界面中，点击任一助手回复右下角的三点图标「⋮」：弹出的菜单宽度应自适应文字内容（160dp ~ 280dp），精致居于气泡下方，**严禁横向撑满整屏**；
   - 检查输入框左侧「+」菜单及顶部模型切换菜单，同样保持精致小巧。
3. **需求 3（模型回复中断保全）**：
   - 选用开启思考链的模型发送复杂提示词，在模型输出思考或开始输出正文时，断开网络或点击停止按钮：对话界面应立即将已生成的思考链与部分正文保存为一条正式回复，气泡上方保留思考胶囊可展开查看全部已输出思考，正文末尾附带中断说明，绝不被丢弃或被错误提示清空覆盖。
4. **需求 4（思考胶囊文字垂直居中）**：
   - 观察助手回复上方单行思考胶囊（「已思考 (xs) · 模型名」）：胶囊内部文字应在圆角胶囊的高度中心线精确垂直居中，不再向上偏移。
5. **需求 5（思考胶囊模型名字物理固化）**：
   - 使用模型 A（如 DeepSeek-R1）发送一条消息并获得回复，回复上方胶囊显示「已思考 · deepseek-r1」；
   - 点击顶部模型选择器切换到模型 B（如 GPT-4o 或 Claude 3.5 Sonnet）：查看刚才由模型 A 生成的历史消息胶囊，模型名字**必须依然是 deepseek-r1**，绝不被替换为模型 B。

---

# Echo v2.6.8 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.8 (`versionCode: 164`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.8.apk`
- **核心内容**：连接等待提示不再顶动屏幕（需求 1）、使用统计模型名可左右滑动查看（需求 2）、请求健康时间线统一 14 × 6（需求 3）、返回首页/切换对话不影响正在进行的连接与回复（需求 4）、三点菜单颜色不均与边缘黑影修正（需求 5）、连接胶囊可正确收缩与图标顶对齐（需求 6）。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| 等待提示顶动屏幕（需求 1） | ① 提示由 `AnimatedVisibility` 增删节点，出现/收起改变气泡高度；② v2.6.7「生成中末项尺寸一变即钉底」使任何高度变化（含 120s 慢响应提示把胶囊从 1 行撑到 3 行）都重新钉底 → 视口整体位移；重连使 `connectElapsedSec` 归零时提示消失又反向回弹 | ① 提示改固定槽位 + 透明度渐变：`Text(minLines = maxLines = 2)` 恒定占两行并全程参与布局，可见性只驱动 `animateFloatAsState` 的 alpha（reduced motion 下 snap），出现/消失/计时归零/秒数增长均零布局抖动；② `ChatScreen` 钉底条件收窄为「仅当末项已被顶出视口下沿」才跟随，末项完整可见时不再滚动；流式正文增长仍由 70ms 节流跟随逻辑负责 |
| 胶囊无法收缩 + 图标居中（需求 6） | `maxLinesCount` 中 `isStatusError`/`isWaitingWithReason` 优先级高于 `isStatusExpanded`，等待期带原因的胶囊恒 3 行、报错胶囊恒 4 行 → 点「收起」无变化；行 `verticalAlignment` 仅展开态取 Top，多行等待胶囊实际 CenterVertically → 左侧图标被居中 | 展开态改「默认策略 + 显式覆盖」两级（`statusExpandOverride: Boolean?`），行数 `!expanded → 1`、报错 4、等待带原因 3、其余 16，**收起必然单行**；`enableSoftWrap = isStatusExpanded`；胶囊行恒 `Alignment.Top` + 图标恒定 top padding 1dp；新增 `showStatusToggle` 让短文案等待胶囊也有收起/展开键 |
| 菜单颜色不均 + 边缘黑影（需求 5） | Material3 `DropdownMenu` 内部 Surface 同时施加 3dp 色调高度（primary 着色叠在半透明 `glass.panelStrong`（alpha 0.92/0.94）之上，背后正文透出）与 3dp 阴影高度（18dp 圆角外缘黑边）；反编译 material3 1.2.1 确认 `MenuTokens.ContainerElevation` 同时用于二者 | 改为自绘 Popup：底色 `surfaceTint(5%) compositeOver surface`（不透明）、`tonalElevation = 0.dp`、`shadowElevation = 0.dp`，保留 1dp 描边 + 18dp 圆角；新增 `EchoMenuPositionProvider` 移植 M3 锚点避让规则；接口签名不变，13 处调用点统一受益 |
| 模型名显示不全（需求 2） | 统计页四处模型名用 `maxLines = 1 + Ellipsis` 截断且无横向滚动 | 新增 `ScrollableSingleLineText`（外部 weight 定宽 + 内部 horizontalScroll + softWrap=false + 按 align 对齐），替换模型下拉项、筛选胶囊值、模型占比图例、模型明细行（Token 总量/勾号位置不变） |
| 热力看板形状（需求 3） | `StatsPeriod.heatmapCells` 按周期递增（42/56/84/84/98/112/140/140），行数 3~10 行 | 全部统一为 84（14 × 6）；测试断言改为「恒为 14 × 6 = 84、行数恒 6」 |
| 后台连接/回复被打断（需求 4） | 子代理全链路只读审查结论：生成协程本身在 `applicationScope`（不随页面销毁取消、回复正常落库），但 ① 上下文回退确认提示（含续体）只挂在 ViewModel 上 → 用户先离开、请求后失败时无人应答且无超时 → **请求协程永久挂起**（重进显示"正在连接"、只能点停止）；② 收尾按 `conversationId` 清理，过期轮次会清空新一轮状态与锚点、并可能误删新会话；③ 同一会话可重复入栈产生两个 VM 互相取消；④ 重进会话不恢复实时 Key 报错明细（暂停正文丢失连接异常记录） | ① 提示提升到 `ChatGenerationManager.ActiveSession`（`answerContextFallbackPrompt`），重进会话转发该流并优先由会话应答，`markFinished()` 统一清理，另加 `withTimeoutOrNull(300s)` 兜底（超时按既有默认 FALLBACK 放行）；`stopGeneration` 两处都应答。② 新增 `isCurrentSession` 与 CAS 语义 `removeSession(id, session)`，三处收尾先判身份再回写 VM 状态。③ `MainActivity` 六处 `chat/` 导航补 `launchSingleTop = true`。④ 新增会话级 `keyAttemptErrors`，回调同步写入、挂载时恢复、停止时优先读会话 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**495 项全通、0 失败**，含更新后的热力看板 14 × 6 断言）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.6.8.apk`，16,700,269 字节 (~15.93 MB)，SHA256 `2A535BAD06B507FA8DCF7A6CE2907B2828AD37D4E0C8F6995F10AFEBCF884C9C`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与 v2.6.7/v2.6.6 完全一致，支持覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留，本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. **需求 1**：新会话发送消息后静置等待。连接满 30s 出现「连接时间较长，正在等待…（已等待 Ns）」时，屏幕**不得整体上移**；若触发重连（计时归零、提示淡出后再出现）同样不得移动；逐秒刷新期间视口稳定；等待超过 120s 时胶囊内出现「响应耗时较长…」多行状态，气泡只向下长高，**屏幕不应整体上滑**（末项仍在视口内时不跟随）。切系统「移除动画」后应为直接切换可见性，仍不移动。
2. **需求 6**：触发一次带重试原因的连接（多 Key 场景）或长状态文案，胶囊显示多行 → 点击胶囊应收成**单行**（可横向滑动看全），再点回到多行；全程胶囊左侧模型状态图标应始终贴胶囊**顶部**，不出现垂直居中。
3. **需求 5**：点击模型回复右下角「⋮」→ 弹出菜单背景应颜色均匀（背后聊天内容不透出）、圆角外缘无黑色阴影；在浅色与深色主题、深浅背景下各查一次。另可在首页卡片菜单/输入栏「+」菜单/模型选择器复看（同一组件）。
4. **需求 2**：进入使用统计 → ① 点「模型」下拉，任一长模型名可左右拖动查看全名，右侧勾号位置不变；② 单选一个长名模型，胶囊上的模型名可左右拖动，右对齐排版不变；③ 切到「模型占比」环形图，图例长模型名可左右拖动，右侧数值不动；④ 模型统计明细中的模型名可左右拖动，右侧 Token 总量贴右不动。
5. **需求 3**：使用统计页切换全部 8 个时间范围（1小时/4小时/8小时/1天/3天/7天/30天/90天），请求健康时间线始终为 14 列 × 6 行、整体高度不变；点选任一方格仍能弹出该时段明细。
6. **需求 4**：
   - 发送消息进入连接/回复中 → 返回首页停留 30s 以上 → 再进入该会话：回复应已继续/完成并落库，不应出现「正在连接」卡死；输入框不应一直是「停止」；
   - 连接中直接点「停止」后立刻重新发送：新一轮回复不应中途消失，发送按钮不应在生成中变回「发送」；
   - 若弹出过「上下文超限/空响应」确认框：在框出现时返回首页再进入该会话，弹窗应重新出现并可正常选择（不会永久卡住）；
   - 触发多 Key 连接失败（实时报错明细出现）后返回首页再进入该会话：明细应仍在；此时点停止，落库的「回复已暂停」正文应包含完整的 Key 报错记录；
   - 连续快速点两次同一会话卡片：只应进入一个对话页（不产生重复页面），生成不被互相取消。

---

# Echo v2.6.7 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.7 (`versionCode: 163`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.7.apk`
- **核心内容**：连接胶囊对齐与连接计时修复（含追加要求"换 Key 重试可重新计时但原因必须可视"）、连接过程防误滚动、上下文压缩对比预览修复与"完成=确认生效"、使用统计七项改进（悬浮栏/速度标注/切换纵向/模型多选/成功率口径/热力行数+2/去重复标题）。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| 头像偏移 | 助手气泡头行 CenterVertically，等待提示与胶囊同 Column，提示出现使其变高，头像随居中下移 | 头像与胶囊顶对齐同行（头像 offset -1dp 保持视觉居中），提示移至行下独立渲染（start=46dp 对齐胶囊左缘），向下延展不影响头像 |
| 计时消失重现/重置 | 计时器在 `if (generationState==Connecting)` 块内，状态闪断时整块离开组合 | 计时提升至气泡级 `LaunchedEffect(isGenerating, reconnectStatus)`：按"尝试阶段"计时，生成发起或重试状态变化（换 Key/重连）重新计时；提示改 AnimatedVisibility 含退场动画 |
| 重试原因可视（追加要求） | 携带原因的重连态胶囊单行横向滚动截断 | 连接/重连等待期胶囊允许换行至 3 行；重连等待期提示同样显示"已等待 Ns"并注明原因见上方状态；实时 Key 报错明细保持逐条展示 |
| 连接期屏幕误滑动 | LazyColumn 锚定（末项 index+偏移），连接期末项高度反复变化（胶囊 animateContentSize/实时报错明细/提示增减）把视口顶得上下位移 | 生成中且自动跟随态下，snapshotFlow 监听末项 index/size/总数变化，一变即重新钉底；手动上翻（autoFollowOutput=false）不干预 |
| 压缩预览不弹/秒缩 | `customPercent/confirmedPercent`（及 rounds 对）以 `usage` 字段作 remember key，滑条松手后异步重算 usage 回流改写 key，confirmed 基线被重置 → hasPendingChange 瞬变 false | 四个状态移除 remember key（仅首组取初值）；tier 保留 key 以便应用后自愈；卡片经 `onRegisterApplyPending` 向宿主注册"应用待确认变更"动作 |
| 完成键语义 | 右下角"完成"仅关闭对话框，未确认的变更被丢弃 | `ContextUsageDialog` "完成"键点击时先调用已注册动作（视同"确认应用并生效"）再关闭 |
| 成功率不准 | 报错后用户主动暂停走取消路径，完全不写 api_usage_stats，失败请求缺失、成功率虚高 | `stopGeneration()` 在 hasErrors（本轮出现过报错/重连）时补记 `ApiUsageStat(success=false)`，随 shouldSave CAS 只记一次 |
| 热力行数 | 各周期格数为 14 的 1~8 倍 | 各周期 +28（列数 14 不变，行数+2），`StatsDashboardTest` 补充逐周期行数断言 |
| 统计页 UI | 顶栏为普通 TopAppBar、时间/模型为滚动芯片、单选模型、环形图切换横向、表格外重复标题 | 设置页同款真悬浮玻璃栏 + 同行两个悬浮下拉（可展开选项列表、遮罩点击收起）；模型多选（deselected 集合，空=全选）；切换 chip 纵向；速度标注"平均"；删除表格外重复"模型统计明细"标题 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（495 项全通，含更新后的热力看板行数断言）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.6.7.apk`，16,700,269 字节，SHA256 `d9b35ca9fa6955939ebf2542357777d260b5007352a505acd134824e47b05467`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与 v2.6.6 完全一致，支持覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留，本次唯一定名增量输出

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. 安装 v2.6.7 覆盖升级，进入任一会话发送消息：
   - 连接等待 30s 后出现"连接时间较长…（已等待 Ns）"提示，头像位置不因提示出现而移动；提示出现后胶囊变高仅向下延展；
   - 若触发多 Key 重试：计时按新尝试重新开始，状态胶囊内重试原因完整换行可读，重连等待期 30s 后亦显示"已等待 Ns"；实时报错明细逐条可查；
   - 连接期间不操作屏幕：列表保持钉底，不出现无故上下滑动；手动上翻阅读时不被拉回。
2. 打开"上下文使用情况"→ 压缩档位选 LC 自定义比例并拖动百分比：前后对比预览稳定显示不回缩；点"确认应用并生效"或直接点右下角"完成"均应生效（完成后状态栏显示"已切换至…"）。
3. 使用统计页：顶部悬浮玻璃栏与两枚同行下拉（时间范围/模型）穿透滚动正常；模型下拉支持全选/反选/取消全选，概览与趋势图标题随筛选变化；"平均生成速度"标注到位；Token 类型/模型占比切换纵向、标题单行；热力看板行数较上版 +2；模型统计明细仅卡片内一个标题；报错后暂停一次生成，刷新统计后成功率应下降（失败次数 +1）。

---

# Echo v2.6.6 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.6 (`versionCode: 162`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.6.apk`
- **核心修复**：新建对话并发送消息后应用直接闪退（严重 bug），并全仓排查加固同类崩溃风险。

## 二、临时实施方案与根因走查
| 项 | 详情 |
| :--- | :--- |
| 根因 | v2.6.4 LC 档引入的窗口计算 `coerceIn(2, usableMessages.size)`：新建对话首发消息落库后 `usableMessages.size == 1`，coerceIn 下界(2) > 上界(1) 构成空区间抛 `IllegalArgumentException`；触发链 = 消息落库 → Room 流发射 → `loadConversation` 收集器调 `refreshContextUsage()` → 档位预览遍历含 LC → 崩溃。与 v2.6.5 锚点改动无关（v2.6.4 起即存在） |
| 修复 | 窗口改 `minOf(size, maxOf(2, window))`：任意 size ≥ 1 安全，语义不变（至少保留最近 1 轮、不超过总数） |
| 同类排查 | 全仓 121 处 `coerceIn` 逐一核对：`ScrollAssist` 滚动拇指（轨道 < 44dp 潜在崩溃）、`ReadableColors` 背景取样（verticalStart > 1f 潜在崩溃）两处加固；ReadableColors 第 92 行与其余 118 处确认边界恒有效 |
| 回归测试 | 新增 2 项：LC 档单条消息不崩且内容不变；低百分比（10%）保留最近一轮 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（73 文件，495 项全通，新增 2 项）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.6.6.apk`，16,683,885 字节，SHA256 `77A4A284D877A26D76E10B3DC2058DF6739FEAD3F95E4B82BCA62E3AEA3B069E`，签名校验通过（CN=Android Debug，非正式生产签名）

## 四、人工验收步骤
1. 安装 v2.6.6 覆盖升级；
2. 新建对话 → 发送第一条消息：不再闪退，正常流式输出与落库；
3. 新对话中切换压缩档位到「LC 自定义比例」再发送：正常；
4. 长对话滚动辅助条、自定义首页/对话背景（含极端小图）回归观察无异常。

---

---

# Echo v2.6.5 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.5 (`versionCode: 161`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.5.apk`
- **核心修正**：v2.6.4 对「连接时删除回复出现多窗口/合并」的修复方向错误（删除导致生成中的回复直接消失），本版回滚误改并按用户澄清的真实场景重做——删除同一位置的过去回复时，正在连接/输出的回复位置必须保持不变。

## 二、根因与修复走查
| 项 | 详情 |
| :--- | :--- |
| v2.6.4 误改回滚 | 移除 `cancelGenerationIfDeletingActiveTurn`，删除恢复为纯数据库操作，生成绝不被删除动作取消 |
| 跳位根因 | 流式气泡挂载点依赖 variant 组锚位（组内首条已落库消息）与配对宿主；同一位置存在多条回复（如未被错误占位识别的「回复已停止」消息 + 重新生成的流式回复）时，删除过去的回复使组锚位移动或组消失 → 挂载在「内联组位 ↔ 底部兜底」间切换 → 流式回复跳到上方/下方变成额外回复，结束后落库又合并回原位 |
| 生成锚点 | 新增 `GeneratingAnchor(userMessageId, userGroupId)`：触发本轮的用户消息（普通发送/编辑重发 = 刚落库用户消息；重新生成 = 目标轮用户消息）；存入 `ChatGenerationManager.ActiveSession` 并在重进会话时恢复；生成结束时清空 |
| 挂载规则 | 流式气泡优先内联挂载于锚点用户消息之后（`isGeneratingAnchorHostItem`：id 命中未分组用户消息；编辑重发按 user 分组命中）；variant 组仍有已落库回复时维持组内挂载（带切换器）；仅锚点不存在（用户消息被删）时回退底部兜底；兜底条件收紧为 `!isBranchStreamingMounted` 防双份 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（73 文件，493 项全通，新增 3 项锚点宿主判定回归）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.6.5.apk`，16,683,885 字节，SHA256 `0011741AFF84D1B40CE586A93EBF28FAB0583C4203A88D4F530C7E2C9A581B80`，签名校验通过（CN=Android Debug，非正式生产签名）

## 四、人工验收步骤
1. 安装 v2.6.5 覆盖升级；
2. 复现原场景：制造一条失败/停止的回复（错误占位），对其点「重新生成」；在连接/输出过程中长按删除那条过去的错误回复——正在连接/输出的回复应**原地不动**（保持在触发该轮的用户消息之后），不跳到上方/下方，也不消失；
3. 生成结束后回复正常落库显示，无重复窗口；
4. 普通发送、编辑重发、多 variant 会话各回归一次流式输出位置与版本切换器显示。

---

---

# Echo v2.6.4 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.4 (`versionCode: 160`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.4.apk`
- **核心需求**：
  1. 模型连接时删除回复仍出现多个回复窗口（连接中的回复完成后与上方回复合并）；
  2. 统计时间范围新增 4小时/8小时/3天；
  3. 请求健康时间线任意范围均铺满；
  4. 缓存命中率/token 计算真实性优化 + 新增 TPS 维度（移除峰值单段）。

## 二、逐项实现走查
| 需求 | 根因/实现 |
| :--- | :--- |
| 1 删除回复 | 根因：`deleteMessage` 只删库不取消生成 → 请求继续执行并照常落库（幽灵窗口），重新生成场景新回复并入被删回复 variant 组（"合并"）。修复：`cancelGenerationIfDeletingActiveTurn`——被删消息属于当前生成轮次（assistant / 触发本轮 user / 本轮开始后）时静默取消：置 `isUserStopping` 阻断占位落库、取消请求与协程、移除会话、清空流式状态，不落任何占位消息；删除无关历史不影响生成 |
| 2 时间范围 | `StatsPeriod` 新增 4小时(16桶)/8小时(24桶)/3天(36桶)，共 8 档；环比/趋势/热力全适配 |
| 3 铺满 | 各周期热力格数全部取列数 14 的整数倍（14/28/56/56/70/84/112/112），任意范围网格无末行空缺，窗口精确等于所选周期 |
| 4 真实性+TPS | 修复三处失真：① Anthropic `input_tokens` 不含缓存读/写却直接当输入（命中率可超 100%、总量偏低）→ 对齐 OpenAI 口径（输入含 cache_read+cache_creation，命中仅计 cache_read）；② Anthropic `output_tokens` 含 thinking 又叠加估算思考量 → 从输出扣除，total 保持 API 真实值；③ OpenAI 兼容端点 `<think>` 文本被 completion_tokens 包含又叠加估算 → 同样扣除。核验：`stream_options.include_usage=true` 已开启（真实 usage 优先）、DeepSeek `prompt_cache_hit_tokens` 已在提取链。TPS：概览「生成速度」（输出 Tokens÷有耗时请求总秒数）替换「峰值单段」；模型表新增 TPS 标签与「速度」排序（第 5 维），高亮同步 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（73 文件，490 项全通，新增 4 项）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.6.4.apk`，16,683,885 字节，SHA256 `4080DC76731F8B590AD8730C1F7D4F56DEFC7A1ADDF390DACED398FD02BD92B0`，签名校验通过（CN=Android Debug，非正式生产签名）

## 四、人工验收步骤
1. 安装 v2.6.4 覆盖升级；
2. 删除回复验收：发送消息后在连接/生成过程中长按删除当前回复（或上一次失败占位回复）——应立即取消生成、无任何新占位气泡出现；随后正常发送不再出现合并/多窗口；
3. 统计范围验收：进入使用统计，确认周期栏出现 4小时/8小时/3天 且各档图表与环比正常；
4. 热力看板验收：逐个切换 8 个时间范围，确认健康时间线网格均被完整铺满（无末行空缺）；
5. TPS 验收：概览第三行显示「生成速度 x.x t/s」；模型明细表出现 TPS 标签，切换「速度」排序时 TPS 高亮且排序生效；
6. 真实性抽查：Anthropic 供应商长对话（含缓存）下，缓存命中率应回到 0~100% 合理区间，总 Token 与供应商后台量级一致。

---

---

# Echo v2.6.3 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.3 (`versionCode: 159`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.3.apk`
- **核心修复**：用户截图反馈——流式生成中连接失败时红色胶囊只显示「模型连接失败」标题、看不到具体原因。

## 二、根因与修复走查
| 项 | 详情 |
| :--- | :--- |
| 根因 | 胶囊文案逻辑 `isConnectionFailed \|\| isMessageContentError` 分支硬编码返回「模型连接失败」，把生成中 Key 报错/重试状态（具体原因）整体替换为无原因标题，点击展开看到的也只是该标题 |
| 主修复 | 拆分分支：已落库错误消息保持简洁标题（红框报告承载详情）；**生成中连接失败直接显示 reconnectStatus 具体原因**（错误态 4 行换行 + 点击展开 16 行） |
| 连带修复 | `isConnectionFailed` 增加 `message.content.isBlank()` 守卫：Key 重试失败后恢复成功时，残留 reconnectStatus 不再让已流式输出的气泡显示失败态 |
| 明细清理 | 首个正文/思考 token 到达即清空 `keyAttemptErrors`，成功流式后旧明细卡不残留 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（73 文件，486 项全通）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.6.3.apk`，16,683,885 字节，SHA256 `18C5EB74A0BB2B4BEDB811AC840EF688C436B7D337F8E3488334D7F4B5E21AB9`，签名校验通过（CN=Android Debug，非正式生产签名）

## 四、人工验收步骤
1. 安装 v2.6.3 覆盖升级；
2. 填入错误 API 地址/停用 Key 后发送消息：连接失败时红色胶囊应直接显示具体原因文本（而非仅「模型连接失败」），点击胶囊可展开完整信息；多 Key 配置下同时出现「连接异常 · 实时明细」卡片逐条列出各 Key 报错；
3. 修好配置后重新发送：失败明细卡在开始输出后自动消失，胶囊恢复正常思考/Token 统计显示。

---

---

# Echo v2.6.2 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.2 (`versionCode: 158`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.2.apk`
- **核心需求与修复**：
  1. 压缩对长对话效果差（L2-L4 均压缩 97%+），新增自定义百分比条数压缩选项（保留原有最近轮数自定义）；
  2. 压缩前后对比预览 UI 完善；
  3. 模型连接报错无需手动暂停即可直接看到错误信息；
  4. 数据看板修复与增强：a. 环形图灰色间隔；b. Token 构成增加模型占比；c. Token 趋势改平滑折线图；d. 模型明细排序选项可见区别；e. 新增热力矩形看板（点选查看局部时段请求数/成功率/Token）。

---

## 二、逐项实现走查

### 1. LC 自定义比例压缩档（需求 1）
| 层 | 实现详情 |
| :--- | :--- |
| 领域 | `CompressionTier.LC`（level 5「自定义比例」）；`CompressionTierPolicy` 百分比常量（默认 30%、范围 10~90%、步长 5）、`getRetainedRoundsDesc` LC 分支、`fallbackOnContextOverflow` LC→L4 |
| 装配 | `assembleTieredContextMessages` 新增 `customRetainPercent`：LC 窗口 = ceil(消息总数×百分比)、至少保留最近 1 轮；窗口内"尽量保留"（预算裁剪），置顶/最近 1 轮无条件保留；摘要注入沿用滚动摘要优先 |
| 数据 | Room **v30→v31**：conversations 新增 `compressionCustomPercent`（DEFAULT 30），`MIGRATION_30_31`（addColumnIfMissing，非破坏性）并接入迁移链；DAO `updateCompressionTier` 扩展 |
| 仓库 | 快照与请求组装全链路传参；`getConversationContextUsage` 新增轮数/百分比覆盖（预览实时重算不落库） |
| UI | 档位卡新增 LC 单选项 + 百分比滑杆；`ChatViewModel.setCompressionTier(tier, rounds, percent)` 三参落库 |

### 2. 前后对比预览完善（需求 2）
- 双条形对比行（当前档位灰条 vs 新档位主色条，长度按基线 Token 归一化）+ 保留策略说明 + 释放徽标；
- 修复滑杆调整后预览不刷新（原为静态快照）：`onValueChangeFinished → viewModel.previewCompressionSettings(rounds, percent)` 即时重算 tierPreviews。

### 3. 连接报错实时可见（需求 3）
- 根因：Key 尝试失败明细（`currentKeyAttemptErrors`）仅在手写暂停路径写入消息，生成过程不可见；
- 修复：`ChatViewModel.keyAttemptErrors: StateFlow<List<String>>` 实时入流；三处流式气泡传入 `liveKeyErrors`；`MessageBubble` 渲染「连接异常 · 实时明细」红色可折叠卡片（默认展开，逐条 Key #N 掩码 + 报错，附自动重试提示）。

### 4. 数据看板修复与增强（需求 4）
| 项 | 实现详情 |
| :--- | :--- |
| a 灰色间隔 | `donutSweepDegrees` 去除切片间隙，连续铺满 360°；底环仅无数据时绘制 |
| b 模型占比 | Token 构成卡「Token 类型 / 模型占比」双视图；`toModelDonutSlices` Top4 + 其他；图例含数值/占比 |
| c 折线趋势 | `buildModelTokenSeries`（Top4+其他，与分桶等长）+ `ModelTokenTrendChart`：中点贝塞尔平滑曲线、虚线网格、顶部横滑图例、5 刻度 X 轴；替换堆叠柱状图 |
| d 排序区别 | 排序模式对应行内指标高亮（主色底+描边+加粗）：Tokens 高亮总量文本，请求数/成功率/耗时高亮对应标签 |
| e 热力矩形 | 「请求健康时间线」卡：`buildHealthCells`（周期定制格数 12/48/84/60/90），Canvas 14 列网格 + 点击选中；「健康状态」（绿/黄绿/橙/红/灰）与「Token 热度」双视图；选中展开局部时段明细（范围/请求数/成功率/Token/失败） |

---

## 三、构建与验证复核清单
- [x] 1. `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] 2. `testDebugUnitTest --no-daemon`：Exit Code 0（73 测试文件，486 项全通 / 0 失败 / 0 错误，较 v2.6.1 新增 10 项）
- [x] 3. `lintDebug --no-daemon`：Exit Code 0
- [x] 4. `git diff --check`：Exit Code 0（仅 CRLF 提示）
- [x] 5. `assembleRelease --no-daemon`：Exit Code 0
- [x] 6. 发布 APK 输出至 `D:\Agent\APP-Echo\app\releases\Echo-v2.6.2.apk`（历史包 100% 保留，增量输出）

---

## 四、APK 产物技术元数据

| 项目 | 参数 / 校验值 |
| :--- | :--- |
| **文件名称** | `Echo-v2.6.2.apk` |
| **绝对路径** | `D:\Agent\APP-Echo\app\releases\Echo-v2.6.2.apk` |
| **文件大小** | 16,683,885 字节 (约 15.91 MB) |
| **Package ID** | `com.aiassistant` |
| **Version Name** | `2.6.2` |
| **Version Code** | `158` |
| **Room 数据库版本** | `31`（MIGRATION_30_31，非破坏性新增列） |
| **Target ABI** | `arm64-v8a` |
| **Min SDK / Target SDK** | `26` / `34` |
| **SHA-256 校验和** | `4CEAC42E7981586AC4866C770DC8BB2F29BA01DB3B85E450D913CC102FE6C5E2` |
| **签名机制** | APK Signature Scheme v2（`apksigner verify` 通过） |
| **签名证书 DN** | `C=US, O=Android, CN=Android Debug` |
| **签名证书指纹 (SHA-256)** | `93:96:38:f6:d3:e9:af:7f:8a:98:0e:62:af:52:d2:75:fe:e7:33:81:f2:13:0c:c4:e2:0a:0d:34:9f:98:e2:1f` |

> **签名说明**：使用项目内 `keystore/echo-release.jks`（alias `androiddebugkey`）签名，证书 DN 为 Android Debug（与历史版本指纹一致），**非正式生产上传密钥**。

---

## 五、Git 状态
本次改动 14 个源文件（含 2 个测试文件）+ 版本/文档同步，详见提交记录。

### 修改文件
1. `app/build.gradle.kts`（versionCode 158 / versionName 2.6.2）
2. `domain/model/CompressionTier.kt`（LC 档 + 策略常量/描述/降档）
3. `domain/model/Models.kt`（Conversation/ConversationContextUsage 新增 compressionCustomPercent）
4. `data/local/AppDatabase.kt`（v31）+ `data/local/migrations/AppDatabaseMigrations.kt`（MIGRATION_30_31）+ `data/local/Daos.kt`
5. `data/repository/helpers/ChatContextAssemblyHelper.kt`（LC 装配）
6. `data/repository/AiRepository.kt`（快照/组装/覆盖参数/档位更新）
7. `ui/screens/chat/ChatViewModel.kt`（三参档位设置、实时预览重算、keyAttemptErrors 流）
8. `ui/screens/chat/ChatContextComponents.kt`（LC 滑杆 + 预览改版）
9. `ui/screens/chat/ChatScreen.kt`（回调接线 + liveKeyErrors）
10. `ui/screens/chat/ChatMessageComponents.kt`（实时报错明细块）
11. `ui/screens/stats/StatsScreen.kt`（看板 a-e）
12. `test/.../CompressionTierPolicyTest.kt`（+5 项）与 `test/.../StatsDashboardTest.kt`（+5 项、改 2 项）

---

## 六、剩余风险与人工验收
1. **安装验证**：本机无真机/模拟器，**未执行安装与启动验证**。人工验收步骤：
   - 安装 `Echo-v2.6.2.apk` 覆盖升级（Room v30→v31 自动迁移，数据无损）；
   - 压缩档位：打开长对话 → 上下文管理 → 选「LC 自定义比例」拖动百分比滑杆，确认预估 Token 实时变化、双条形对比正常；确认应用后长对话不再被压缩 97%+；
   - 连接报错：故意填错 API 地址后发送，确认流式回复下方直接出现「连接异常 · 实时明细」卡片并逐条列出报错，无需手动暂停；
   - 数据看板：进入使用统计逐项核对——环形图无灰色间隔且可切换模型占比；Token 趋势为平滑折线并带模型图例；健康时间线可点选方格查看时段明细；模型明细切换排序时对应指标高亮。
2. **LC 档预算行为**：预算极紧张时 LC 窗口会被裁剪（置顶与最近 1 轮仍保留），实际保留比例可能低于设定值；此为防溢出的既定设计。
3. **Lint 工具链**：沿用 v2.5.9 起的已知隔离方案（Compose Lint 元数据崩溃探测器），其余规则全部有效。

---

---

# Echo v2.6.1 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.1 (`versionCode: 157`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.1.apk`
- **核心需求与修复**：
  1. 修复流式输出偶现「两个相同回复同时进行流式输出」或「回复位置错误」的现象（此前回复结束后才恢复正常）；
  2. 全面更新使用统计界面：优化美观度与数据可视化，提供更丰富的数据看板。

---

## 二、逐项实现走查

### 1. 流式输出重复/错位根因修复（需求 1）
| 模块 | 实现详情 |
| :--- | :--- |
| 根因 | `ChatScreen.kt` 分支流式气泡内联挂载判定中，`displayItem.groupId == pairedVariantGroupId(streamingBranchGroupId)` 在 `streamingBranchGroupId = "reply_<id>"`（重新生成无分组历史消息时的兜底命名）时 `pairedVariantGroupId` 返回 `null`，与所有未分组消息项的 `null` groupId 构成 `null == null` 判等命中——每个未分组消息后都渲染一份相同的流式气泡（N 份同回复同时流式输出 + 错位）；回复结束后流式气泡统一消失、落库消息接管，与用户观察的"恢复正常"完全吻合。`isBranchStreamingMounted` 挂载检查存在同一缺陷（导致底部兜底气泡被误判已挂载而消失） |
| `ChatMessageComponents.kt` | 新增纯函数 `isStreamingBranchHostItem(itemGroupId, streamingBranchGroupId, messageId)`：显式要求 `pairedId != null` 才参与相等判定，保留 `turn_<id>_` 前缀匹配 |
| `ChatScreen.kt` | 内联挂载判定与 `isBranchStreamingMounted` 两处统一改用上述函数 |
| `Daos.kt` | `getMessagesByConversation` / `getMessagesList` 补 `id ASC` 次级排序键，同毫秒入库消息顺序稳定（次要加固） |

**修复后行为矩阵**：
| 生成方式 | 修复前 | 修复后 |
| :--- | :--- | :--- |
| 普通发送 | 底部兜底气泡（正常） | 底部兜底气泡（仅一份，行为不变） |
| 重新生成无分组历史消息 | **N 份相同错位流式气泡** | 底部兜底气泡（仅一份） |
| 编辑重发 / 重生成带分组消息 | 配对 user 项后原位内联气泡（正常） | 配对 user 项后原位内联气泡（仅一份，行为不变） |

### 2. 使用统计看板全面改版（需求 2）
| 模块/板块 | 实现详情 |
| :--- | :--- |
| 核心概览 | 总消耗大数字 + 周期/筛选标题；环比上一周期对比芯片（Token 消耗、调用量，▲/▼ 走向与正负着色；读取窗口扩大一倍一次取回双周期数据）；3×3 指标网格：总请求数、调用成功率、失败次数、输入/输出/思考 Token、缓存命中率、平均响应（ms/s/min 自适应）、峰值单段（附时段标注） |
| Token 构成环形图（新增） | 输入/输出/思考/其他四切片 Canvas 环形图，切片间 3° 间隙，中心显示总量，图例逐项数值 + 占比 |
| 24 小时调用分布（新增） | 按本地时区 0-23 时聚合调用次数直方图，柱体亮度随频次增强，副标题给出最活跃时段与次数 |
| 供应商消耗占比（新增） | 按 provider 聚合 Token 占比横条（Top 6），含调用量与百分比 |
| 失败原因归纳（新增） | 失败记录按错误首行归组，Top 4 高频原因 + 计数徽标；无失败时整卡隐藏 |
| 既有图表 | Token 堆叠柱状图与成功率走势曲线保留原视觉；修复成功率卡片底部误挂 Token 图例问题（图例改为按卡片配置） |
| 模型明细表 | 每行新增「失败 N」「缓存 N%」标签（有数据时显示）；耗时统一 `formatMillis` 自适应格式 |
| 空态 | 筛选后无数据仅展示概览 + 空态卡，不再渲染全部空图表 |
| 可测试性 | 统计纯逻辑（聚合/分桶/占比/失败归纳/环比/环形角度/格式化）收敛为 `internal` 纯函数，供 `StatsDashboardTest` 覆盖 |

---

## 三、构建与验证复核清单
- [x] 1. `compileDebugKotlin --no-daemon`：Exit Code 0（仅存量 deprecation 警告）
- [x] 2. `testDebugUnitTest --no-daemon`：Exit Code 0（73 测试文件，476 项全通 / 0 失败 / 0 错误，较 v2.6.0 新增 18 项）
- [x] 3. `lintDebug --no-daemon`：Exit Code 0
- [x] 4. `git diff --check`：Exit Code 0（仅 CRLF 换行提示，无空白错误）
- [x] 5. `assembleRelease --no-daemon`：Exit Code 0
- [x] 6. 发布 APK 输出至 `D:\Agent\APP-Echo\app\releases\Echo-v2.6.1.apk`：
  - 文件大小：16,667,501 字节 (~15.89 MB)
  - SHA256：`E59449093D1FB039F9D8489E89A1F338DD7BE8D7A2F468863BC331D01A2AB495`
  - 签名验证：`apksigner verify --print-certs` Exit Code 0（证书 CN=Android Debug，**非正式生产签名**）
  - 历史版本完整性：历史安装包 100% 完整保留，本次唯一定名增量输出

---

## 四、APK 产物技术元数据

| 项目 | 参数 / 校验值 |
| :--- | :--- |
| **文件名称** | `Echo-v2.6.1.apk` |
| **绝对路径** | `D:\Agent\APP-Echo\app\releases\Echo-v2.6.1.apk` |
| **文件大小** | 16,667,501 字节 (约 15.89 MB) |
| **Package ID** | `com.aiassistant` |
| **Version Name** | `2.6.1` |
| **Version Code** | `157` |
| **Application Label** | `Echo` |
| **Target ABI** | `arm64-v8a` |
| **Min SDK / Target SDK** | `26` / `34` |
| **SHA-256 校验和** | `E59449093D1FB039F9D8489E89A1F338DD7BE8D7A2F468863BC331D01A2AB495` |
| **签名机制** | APK Signature Scheme v2（`apksigner verify` 通过） |
| **签名证书 DN** | `C=US, O=Android, CN=Android Debug` |
| **签名证书指纹 (SHA-256)** | `93:96:38:f6:d3:e9:af:7f:8a:98:0e:62:af:52:d2:75:fe:e7:33:81:f2:13:0c:c4:e2:0a:0d:34:9f:98:e2:1f` |

> **签名说明**：使用项目内 `keystore/echo-release.jks`（alias `androiddebugkey`）签名。该证书 DN 为 Android Debug（与此前 v2.5.x / v2.6.0 系列发布包指纹一致），**并非正式生产上传密钥**。`apksigner verify --print-certs` 已确认签名有效。

---

## 五、Git 状态
本次改动 5 个源文件 + 1 个新增测试文件 + 版本/文档同步，详见提交记录。

### 修改文件
1. `app/build.gradle.kts`（versionCode 157 / versionName 2.6.1）
2. `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`（流式挂载判定修复）
3. `app/src/main/java/com/aiassistant/ui/screens/chat/ChatMessageComponents.kt`（新增 `isStreamingBranchHostItem`）
4. `app/src/main/java/com/aiassistant/data/local/Daos.kt`（消息查询补 `id ASC` 次级排序）
5. `app/src/main/java/com/aiassistant/ui/screens/stats/StatsScreen.kt`（使用统计看板全面改版）
6. `app/src/test/java/com/aiassistant/RegenerateVariantSwitcherTest.kt`（新增 4 项回归测试）
7. `README.md` / `PROJECT.md` / `CHANGELOG.md` / `UPDATE_LOG.md` / `WORKFLOW_GUIDELINES.md`

### 新增文件
1. `app/src/test/java/com/aiassistant/StatsDashboardTest.kt`（14 项统计看板领域测试）

---

## 六、剩余风险与人工验收
1. **安装验证**：当前环境无连接的 Android 真机/模拟器，**未执行安装与启动验证**。人工验收步骤：
   - 将 `D:\Agent\APP-Echo\app\releases\Echo-v2.6.1.apk` 传至设备安装（可覆盖升级 v2.6.0）；
   - 流式修复验收：打开任一历史会话（消息未经过"编辑重发"改造的普通会话），对最后一条回复点「重新生成」——修复前会偶现多条相同流式回复错位，修复后应始终只有一条流式气泡且位于列表底部；普通发送与编辑重发场景亦各验证一次；
   - 统计看板验收：进入「使用统计」，切换 1小时/1天/7天/30天/90天 与模型筛选，逐项确认：核心概览九项指标与环比芯片、Token 构成环形图、24 小时调用分布、供应商消耗占比、失败原因归纳（如有失败记录）、模型明细表新标签均正常渲染；空数据会话/筛选下仅显示概览与空态卡。
2. **UI 视觉走查**：环形图与直方图为 Canvas 自绘，已在代码层保证深浅主题使用 `rememberEchoChartColors` 深色变体；真机深色模式下的观感如需微调可再调透明度参数。
3. **Lint 工具链**：沿用 v2.5.9 起的已知隔离方案（Compose Lint 内嵌 kotlinx-metadata 不支持 Kotlin 2.2 元数据，仅隔离崩溃探测器，其余规则全部有效），根治需升级 AGP/Compose Lint。

---

---

# Echo v2.6.0 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.6.0 (`versionCode: 156`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.6.0.apk`
- **核心需求与修复**：
  1. 弱网连接不畅或空响应导致上下文回退时弹出选项窗口（回退、忽略、当前对话永久忽略）；
  2. 时间线与设定提取/变更待确认卡片，支持长按文字就地编辑，编辑后提供「取消」与「应用」；
  3. 模型连接失败时胶囊不显示 Token 消耗，直接显示「模型连接失败」，点击胶囊可折叠/展开下方红色错误报告；
  4. 拖动模型或 API Key 排序时，关闭交换成功交界处的手机马达震动反馈；
  5. 设置中的模型供应商列表支持长按拖动改变顺序并持久化。

---

## 二、逐项实现走查

### 1. 弱网上下文回退弹窗选择（需求 1）
| 模块 | 实现详情 |
| :--- | :--- |
| `Models.kt` | 定义 `ContextFallbackChoice { FALLBACK, IGNORE, PERMANENTLY_IGNORE }` 与 `ContextFallbackPromptState` |
| `AiRepository.kt` | `isContextFallbackPermanentlyIgnored(conversationId)` 检查会话是否免回退；触发回退前挂起询问 `onContextFallbackPrompt`，依据用户选择分流 |
| `ChatViewModel.kt` | `_pendingContextFallbackPrompt` 挂起协程状态机，`handleContextFallbackDecision` 恢复挂起并传递决策 |
| `ChatScreen.kt` | 监测到 `pendingContextFallbackPrompt` 时弹出 `AlertDialog`，三路按钮分别响应「回退」、「忽略」及「（当前对话）永久忽略」 |

### 2. 时间线与设定提取长按编辑（需求 2）
| 待确认卡片 | 正常展示状态 | 长按编辑状态 |
| :--- | :--- | :--- |
| `pendingMemoryCandidate` | 展示提取记忆文本与「忽略 / 仅本会话生效 / 存为长期记忆」按钮 | 切换为 `OutlinedTextField`，展示「取消」（恢复原样）与「应用」（入库本会话专属记忆） |
| `pendingTimelineProposal` | 展示时空推进与事件节点，附带「忽略 / 确认应用」按钮 | 切换为 `OutlinedTextField`（支持修改时空标签、事件时间、事件内容），展示「取消」与「应用」 |

### 3. 连接失败胶囊视觉与错误报告折叠（需求 3）
| 胶囊状态 | 修改前 | 修改后 |
| :--- | :--- | :--- |
| 连接失败文案 | 误显消耗 Token 模板 | **模型连接失败**（彻底屏蔽 Token 消耗） |
| 错误报告折叠 | 红色卡片始终展开，无法控制 | 胶囊右侧显示 `ExpandLess` / `ExpandMore` 箭头，点击胶囊可自由折叠/展开红框错误报告 |

### 4. 拖动排序震动反馈消除（需求 4）
- `SmoothReorderState.kt`：彻底移除 `onDragDelta` 中向上与向下越过 0.42f 阈值交换判定成功时的 `haptic?.performHapticFeedback(...)`，杜绝交界重合判定处手机马达反复响动。

### 5. 设置页模型供应商长按拖动排序（需求 5）
- `SettingsScreen.kt` `ApiConfigTab`：引入 `providerReorderState = rememberSmoothReorderState()`，为 `ApiConfigCard` 左侧增加六点手柄，拖动交换即时更新本地列表并写入 `AiRepository.saveApiConfigOrder`；
- `AiRepository.kt`：`getAllApiConfigs()` 结合 `apiConfigOrderTrigger` 响应式流，供应商列表在拖拽后即刻在全应用生效。

---

## 三、构建与验证复核清单
- [x] 1. `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] 2. `testDebugUnitTest --no-daemon`：Exit Code 0（73 测试文件，458 项全通）
- [x] 3. `lintDebug --no-daemon`：Exit Code 0
- [x] 4. `git diff --check`：Exit Code 0
- [x] 5. `assembleRelease --no-daemon`：Exit Code 0
- [x] 6. 发布 APK 输出至 `D:\Agent\APP-Echo\app\releases\Echo-v2.6.0.apk`：
  - 文件大小：16,667,501 字节 (~15.89 MB)
  - SHA256：`CF38591EC6083249588ED24A713E77FBDB46F95F38E963E33A23A8C3FF0BCBC7`
  - 签名验证：`apksigner verify --print-certs` Exit Code 0（证书 CN=Android Debug，非正式生产签名）
  - 历史版本完整性：历史安装包 100% 完整保留，本次唯一定名增量输出。

---
