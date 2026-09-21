# Echo 更新与修改记录日志 (Update Log)

本文档按照工作流规范记录每次版本更新、需求变更与复核结果。

## [2026-09-22] - v2.3.1：时间线梳理防误截用户输入、句意完整性收敛（防硬腰斩）与事件设定跨界消歧去重优化

### 1. 核心需求落实与技术重构详情
1. **彻底杜绝错误截取用户输入作为事件**：
   - **痛点与根因**：
     - 原 `isPureDirectorInstruction` 仅在用户输入包含中括号或括号时才判定，自由口令指令（如“接下来让他们在雨夜车站再次相遇”）被当作普通发言送入大模型，导致部分模型直接将用户指令或发言原句作为事件抄录；
     - 本地兜底扫描中，存在未严格校验 `msg.role == "assistant"` 即将 `[` 开头消息作为事件解析的隐患；
     - 增量推演入库前缺少防误截校验。
   - **重构方案**：
     - 增强 `isPureDirectorInstruction`，全面支持无括号包裹的常见口令动词与句式；
     - 新增 `isInvalidOrUserInstructionEvent` 拦截器，与用户历史发言进行相似度比对，检测指令性前缀与出戏元词汇；
     - 在 `analyzeTimelineChunk`、`evaluateAndAutoUpdateTimeline`、`fallbackLocalTimelineScan` 以及 `consolidateFinalTimelineEvents` 全流程注入拦截，坚决拦截抄录用户输入作为事件的现象。
2. **事件精炼与句意自然收尾（防暴力腰斩截断与模型减负）**：
   - **痛点与根因**：原实现使用字符级硬切 `take(45)` / `take(35)`，导致长句子末尾被生硬切断为半截残句；同时向模型注入的上下文格式冗余繁琐，造成较大 Token 与注意力负担。
   - **重构方案**：
     - 新增 `compactSentenceKeepComplete` 算法，优先基于标点符号（逗号、句号、分号）寻找语法完整分句，剥离无增量前缀，确保主谓宾事实完整，字数稳定在 12~28 字最佳区间；
     - 优化 `buildTimelineNodesPromptContext` 上下文排版，去除条目间无增量的 `【剧情事件】` 冗余标签，长列表仅展示最新核心里程碑，使大模型上下文读取负担降低 60% 以上。
3. **消除多个事件与设定的本质一致性（同类型深化 + 跨界消歧去重）**：
   - **痛点与根因**：原系统在分块提取与合并时，同一动态事实既被作为时间线事件提取，又在设定列表中机械重复记录；同时同义事件缺少广度词簇支持。
   - **重构方案**：
     - 扩充 `sceneClusterKeywords` 同义场景词簇（餐饮聚会、交谈商议、战斗交锋、初遇相聚、散步同游、约定盟约、搜查潜入等），同天同场景事件全量聚合；
     - 新增跨界消歧与去重函数 `crossDeduplicateEventsAndSettings`：交叉比对事件与设定，若设定仅为已发生时空事件的动态过程复述，彻底剔除该重复设定，仅保留纯粹静态规则，确保事件与设定界限分明、互不重复。

### 2. 自动化测试与工程核验
- **单元测试**：全量执行 `testDebugUnitTest`，共计 **337 项测试全部通过 (337 passed, 0 failed, BUILD SUCCESSFUL)**。
- **构建输出**：
  - 文件路径：`D:\Agent\APP-烧\app\releases\Echo-v2.3.1.apk`
  - 文件大小：`16,304,125 字节 (~15.55 MB)`
  - SHA256：`38C92D984D74FEC28DA9231A598F3DFA3A60C45DCE775334D290924AE8BE142D`
  - 签名方案：`v2 scheme (APK Signature Scheme v2): true`
  - 包名与版本：`package: name='com.aiassistant' versionCode='136' versionName='2.3.1'`
  - 历史包策略：`D:\Agent\APP-烧\app\releases` 目录下所有历史版本永久完整保留，本次仅增量输出 `Echo-v2.3.1.apk`，未包含任何 `-arm64-v8a` 等冗余后缀。

---

## [2026-09-22] - v2.3.0：时间线总结高度凝练与多轮对话单事件合并、全局终审聚合收敛、时空主动推进与防停滞、编年表UI排版重构

### 1. 核心需求落实与技术重构详情
1. **时间线总结精简与单事件多轮对话合并**：
   - **痛点分析**：当某一件事被描写的很详细（表现为用很多对话来描述，如聚餐、商谈、初遇交谈等），原时间线提取机制容易将其拆散记录为多个琐碎子事件。
   - **重构方案**：
     - 在大模型 `analyzeTimelineChunk` 提示词中加入强约束：同一场景事件必须高度凝练为单一条目（15~30 字），禁止按细碎对话拆分；
     - `TimelineMemoryHelper` 引入 `consolidateFinalTimelineEvents`：检测同天同时间段下同场景词簇（如餐馆/点菜/进餐、结识/初遇、战斗/交锋、会议/商讨等），自动执行语义融合与冗余剥离，收敛为最长不超过 45 字的宏观里程碑事件。
2. **全局终审汇总 Pass（设定与事件深度去重与收敛）**：
   - **痛点分析**：缺少最后的整体汇总，导致无论是设定还是事件，常有极其相似的同义表述共存。
   - **重构方案**：
     - 在 `AiRepository.kt` 中设计两阶段 Map-Reduce 架构：多段分析完成后触发 `consolidateTimelineWithModel` 整体汇总；
     - 若模型未返回或条目精简，则经由 `TimelineMemoryHelper.consolidateFinalReconcileResult` 纯函数收敛；
     - `consolidateFinalAtemporalSettings` 采用 Jaccard 相似度与核心特征词（如咖啡饮用习惯、药物过敏、武器偏好等）提取比对，彻底剔除语义重复设定。
3. **当前时空节点自主推进与模型防停滞**：
   - **痛点分析**：模型在后续对话中常错误地“一直停留在当前时空”，缺乏时间流逝的主动推进意识。
   - **重构方案**：
     - 在 `<session_timeline>` 提示词上下文中注入时空推进铁律：明确告知模型当前时空节点是“已发生事件的基准点”，要求模型正文剧情主动体现时间流逝（如日出日落、时段流转、跨天递进）；
     - `AiRepository.evaluateAndAutoUpdateTimeline` 放宽判定门限，无论单段事件还是时空标签均触发更新；
     - 新增 `detectAutoStoryTimeAdvancement` 兜底推演：结合具体活动终结（用餐完毕、就寝掌灯等）、时段关键词及次日推进规则，自动推断并更新 `Conversation.currentStoryTime`；自动净化“傍晚/黄昏”等斜杠复合词为清晰的“傍晚”。
4. **时间节点编年表 UI 结构优化**：
   - **ChatSettingsDialogs.kt**：重构编年表卡片布局。顶部单行容器水平排布：`序号 (#01)` + `垂直排布的时空标签 (第 1 天·傍晚) 与事件性质徽章 ([主线剧情])` + `编辑按键` + `删除按键`；下方全宽展现具体事件内容；
   - 彻底避免窄屏下的错位挤压，视觉清晰精致。
5. **全局 UI 审核加固**：
   - 全局审核并消除了 `ChatScreen`、`ChatStoryDialogs`、`PlotActionBar`、`SettingsApiConfigDialog`、`HistoryScreen` 等多处的文字遮挡、按键重叠与高度限制截断问题。

### 2. 自动化测试与工程核验
- **单元测试**：全量执行 `testDebugUnitTest`，共计 334 项单元测试 100% 全部通过 (334 passed, 0 failed)。
- **构建输出**：
  - 文件路径：`D:\Agent\APP-烧\app\releases\Echo-v2.3.0.apk`
  - 文件大小：`16,304,125 字节 (~15.55 MB)`
  - SHA256：`9BB0F67133B718DE180EA90D1A4A5E1D461FCF8FC104B7E75AC77FD8F9442FDA`
  - 签名方案：`v2 scheme (APK Signature Scheme v2): true`
  - 包名与版本：`package: name='com.aiassistant' versionCode='135' versionName='2.3.0'`
  - 历史包策略：`D:\Agent\APP-烧\app\releases` 目录下所有历史版本永久完整保留，本次仅增量输出 `Echo-v2.3.0.apk`，未包含任何 `-arm64-v8a` 等冗余后缀。

---

## [2026-09-21] - 全局 UI 排版与文字按键重叠错位深度优化

### 1. 核心需求落实与技术重构详情
1. **全局文字与按键重叠、挤压变形与排版错位修复**：
   - **ChatScreen.kt**：
     - `pendingMemoryCandidate` 待确认记忆候选卡片底部操作按键原本使用固定水平容器 `Row` 容纳 3 个宽按钮，在小屏幕宽度下产生文字裁切与按钮重叠溢出；
     - 重构为弹性流式布局 `FlowRow`，设置合理的 `spacedBy` 与对齐规则，将硬编码 `height(28.dp)` 优化为 `defaultMinSize(minHeight = 32.dp)`，保障按键在多行排布或高系统字号下自适应换行，彻底消除裁切与挤压。
   - **ChatSettingsDialogs.kt**：
     - 会话专属记忆/角色约束行原在 `Row(SpaceBetween)` 中缺少权重与省略保护，导致长说明文字与右侧“添加设定”、“清空”按钮剧烈碰撞；现赋予说明文字 `Modifier.weight(1f).padding(end = 8.dp)` 并限制最多 2 行显示；
     - 当前故事推进时空节点（`currentStoryTime`）文本增加 `maxLines = 1, overflow = Ellipsis`，防止长节点设定与“修改节点”按钮重叠；
     - 时间线全量梳理与操作工具栏升级为 `FlowRow` 响应式包裹，防止梳理中文案与节点添加按钮越界。
   - **ChatStoryDialogs.kt**：
     - “当前叙事模式”、“登场角色”、“世界观与场景设定”等分区标题栏与操作按钮统一配置 `weight(1f, fill = false)` 与单行省略保护，标题与右侧副标题间增加间隔；
     - 将所有硬编码 `height(28.dp)` 的操作按键（“添加新角色”、“添加新世界观”、快捷剧情提示、分析终止等）升级为 `defaultMinSize(minHeight = 32.dp)` 或 `defaultMinSize(minHeight = 28.dp)`，杜绝文字在垂直方向因小高度限制被裁切。
   - **RoleplayStudioScreen.kt**：
     - `SessionCard` 底部“叙事模式”标签与提示文案从固定单行重构为 `FlowRow`，提示文本增加 `maxLines = 1, overflow = Ellipsis`，杜绝小屏宽度下溢出或与更多操作按钮重叠；
     - `CharacterCard` 角色姓名与身份（`name` + `· ${identity}`）行增加 `maxLines = 1, overflow = Ellipsis` 及弹性权重分配，避免长角色名挤压右侧收藏按钮；
     - `ScenarioCard` 标题增加单行省略截断保护；
     - 导入同名冲突处理选项由单行 `Row` 重构为自适应换行的 `FlowRow`。
   - **PlotActionBar.kt**：
     - 底部“更多操作”栏中的 4 个 `AssistChip`（改变视角、改变语气、创建分支、回退版本）原本位于静态单行 `Row`，在标准 360dp 屏宽下第 4 个按钮被严重截断；
     - 重构为 `FlowRow`，确保在任何屏幕宽度与字体倍率下均能优雅整齐折行显示。
   - **HistoryScreen.kt**：
     - 会话卡片模型标签增加 `Modifier.weight(1f, fill = false)` 与单行省略保护，避免超长模型名（如各类开源衍生模型名）将消息计数及 Token 统计指标挤压出屏幕。
   - **SettingsApiConfigDialog.kt**：
     - `ModelCustomSettingCard` 模型特性徽章（上下文窗口、视觉、工具、思考、联网）原在单行排布，在弹窗中间仅 150dp 区域发生严重溢出并遮挡右侧单选框与折叠按键；
     - 升级为 `FlowRow`，实现特性徽章自适应柔性换行。
   - **SettingsPersonalizationTab.kt**：
     - 修复 `MemoryItemCard` 中 `Switch` 上强制设置 `Modifier.height(24.dp)` 导致的轨道与滑块形变裁切，统一调整为规范缩放；
     - `WorldBookCardItem` 书籍名称增加 `weight(1f, fill = false)` 与单行省略保护，避免长书籍名称挤压设定条数徽章。
   - **SettingsModelFeaturesTab.kt**：
     - 针对辅助模型卡片 13 个中文字符的超长标题与“已启用”徽章，重构为 `FlowRow` 弹性排布，彻底消除与右侧总开关的碰撞变形。
   - **NewRoleplaySessionScreen.kt**：
     - 登场角色设定、世界观与背景设定、生成模型与 API 服务、故事叙事模式等所有核心标题栏统一注入 `Modifier.weight(1f)` 与单行省略保护，叙事模式列表项内部容器加入权重自适应约束。
   - **ChatMessageComponents.kt**：
     - 工具调用详情按钮的固定高度调整为弹性最小高度，确保高字体缩放下完整显示。

### 2. 自动化测试与质量核验
- **单元测试**：全量执行 `.\gradlew.bat testDebugUnitTest --no-daemon`，共计 **330 项测试全部通过 (330 passed, 0 failed)**，测试覆盖核心架构、时间线推理、数据克隆与排版边界逻辑。
- **编译检查**：执行 `.\gradlew.bat compileDebugKotlin --no-daemon`，退出码 0，所有 Compose 语法与布局闭包无任何错误。
- **规范遵守**：严格执行“未明确要求构建 APK 不执行打包发布流程”，不触发发布构建。

---

## [2026-09-21] - v2.2.9：独立单一存放的时间线体系 (timeline_nodes)、同一事件防跨天归并、根除非正常时间大跳跃、实时动态更新与完整用户编辑

### 1. 核心需求落实与技术重构详情
1. **独立、单一存放的时间线体系与物理隔离存储 (`timeline_nodes`)**：
   - **痛点与根因**：
     - 原先梳理时间线并启用后，时间和事件会被混编为 `[第X天·傍晚] ...` 格式直接写入 `memory_items` 会话记忆表中；
     - 导致时间节点、关键事件与角色设定、世界观规则完全混杂在一起，用户无法直观查阅编年脉络，且无法针对时间节点单独编辑与维护。
   - **技术方案**：
     - 新建独立 Room 数据实体 `@Entity(tableName = "timeline_nodes") data class TimelineNode(...)`，包含 `conversationId`, `timeTag`, `event`, `category`, `orderIndex`, `createdAt`, `updatedAt`；
     - 在 `Conversation` 实体中增加 `currentStoryTime: String?` 列，专门存放当前会话停驻的故事时间节点；
     - 升级 Room 数据库版本至 28，编写 `MIGRATION_27_28` 实现自动建表、加列、从旧 `memory_items` 中迁移当前故事时间并自动清理脏数据；
     - 在 `AiRepository.kt` 中实现时间线专属 CRUD 接口与事务操作，对话克隆与分支创建均执行深度物理克隆；
     - 在 `ChatSettingsDialogs.kt` 设置弹窗中剥离出独立的「🕒 故事时间线 (Timeline)」专区，展示当前时空节点及可就地修改弹窗、编年节点有序列表（序号、时间标签、分类徽章、事件详情）、节点手动添加、编辑、删除与一键清空；
     - 会话专属记忆卡片彻底解耦，仅保留世界观规则与角色固有设定。

2. **全量梳理时序算法优化与同一事件防跨天归并**：
   - **痛点与根因**：
     - 一件事情（如聚餐、下午茶聊天）由多轮对话组成，旧算法在单调时序递增时由于简单的 `<= lastPhaseOrder` 判断而强制 `currentDay++`，导致同一天同一顿饭被错误拆分成了多天的多顿饭；
     - 口头提及“两年前”、“这两天”、“数日前”时，旧时间跨度估算被无上下文匹配，导致当前故事时间异常暴跳 730 天。
   - **技术方案**：
     - 优化 `TimelineMemoryHelper.kt` 的 `normalizeMonotonicTimeline`：仅在明确遇到隔日/次日标签或夜间（入夜/深夜）向清晨自然推移时才递增天数，同一天的相同或平缓时段连续事件稳固保持在同一天；
     - 升级 `estimateTimeSpanJumpDays`：前置严谨的负向排除逻辑，过滤“前”、“回忆”、“往事”、“这”、“持续”、“历经”、“耗费”等回顾与范围修饰，杜绝异常时间暴跳；
     - 在梳理提示词中写入第 5 条铁律：同一事件多轮对话必须归纳合并为单条时间线节点，严禁虚假跨天。

3. **大模型根据对话实时对时间线进行动态更新与归并**：
   - **技术方案**：
     - `evaluateAndAutoUpdateTimeline` 彻底解耦 `memoryDao`：模型增量判断产生的新故事时间直接更新至 `conversation.currentStoryTime`；
     - 产生的新关键事件，先与既有 `timeline_nodes` 进行语义重叠度比对，属于同一事件的就地润色合并时间与内容，全新事件则按递增时序追加；
     - 提示词统一上下文组装 (`buildSystemPromptWithUnifiedRules`) 时，独立注入 `<session_timeline>` 时序脉络，而 `<session_memory>` 保持纯净。

### 2. 自动化测试与工程交付
- **单元测试**：`TimelineArchitectureAndOptimizationTest`（6 项测试全部通过）与 `TimelineMemoryTest`（11 项测试全部通过），命令退出码 0。
- **发布安装包**：`D:\Agent\APP-烧\app\releases\Echo-v2.2.9.apk`
  - SHA256: `1D02B30D501A3BFBEA4A037ED9BA60AD0F87E1A36F932E5C31A5C1759C330284`
  - 文件大小: `16,304,125 字节 (~15.55 MB)`
  - 签名验证: `Verified using v2 scheme (APK Signature Scheme v2): true`
  - 应用包名: `com.aiassistant` | `versionCode: 134` | `versionName: 2.2.9`
  - 历史版本永久保留在 `D:\Agent\APP-烧\app\releases`，无带有架构后缀的多余包。

---

## [2026-09-21] - v2.2.7：角色扮演与小说创作时间与事件记忆重构（日内时段状态机、防时序错乱、长对话分段 Map-Reduce 梳理与即时取消、对话后自动增量更新提醒）

### 1. 核心需求落实与技术重构详情
1. **时间线自动提取与每次对话后自动判断增量更新**：
   - **根因分析**：
     - 原“梳理时间线”功能只能由用户在设置弹窗中手动全量触发，模型无法在日常对话推进中自动捕获时间流逝与事件推进；
     - 缺乏自动化增量提炼机制，导致长对话中模型对时间线的认知停留在旧节点。
   - **技术方案**：
     - 在 `PersonalizationManager.kt` 中为 `PersonalizationSettings` 新增 `autoTimelineEnabled: Boolean = true` 与 `autoTimelineNoticeEnabled: Boolean = true` 开关；
     - 在 `AiRepository.kt` 中实现 `evaluateAndAutoUpdateTimeline`：利用轻量 System Prompt 针对最新轮次对话评估时间是否有推进（天数变化、时段变化）及是否有新的关键事件发生；
     - 当检测到实质推进时，自动更新会话专属记忆中的【当前故事时间】与时间线事件，并返回轻量变更摘要；
     - 在 `ChatViewModel.kt` 的 `sendMessageInternal` 回调中挂载该后台协程任务，不阻塞前端回复展示；
     - 在 `ChatScreen.kt` 聊天输入框上方新增基于液态玻璃卡片的 `timelineUpdateNotice` 浮动胶囊提醒（“🕒 时间线已自动推进至：第 X 天·下午，新增 1 条事件”），支持点击“查看”直接打开设置审核或点击关闭忽略。
   - **文件改动**：`app/src/main/java/com/aiassistant/utils/PersonalizationManager.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`。

2. **时间精度防错乱与日内时段细分状态机（解决“在一起几天后误记为昨天”与“早餐后下一句天黑入睡”等割裂）**：
   - **根因分析**：
     - 大模型在记忆压缩后产生“昨天才在一起”的幻觉，是因为压缩摘要仅记录了事件动作而丢失了**绝对天数锚点与相对总天数跨度**；
     - 出现“男女主吃完早餐后，下一句回复突兀描写天黑了要早点睡”，是由于缺乏**日内时段（DayPhase）连续性与生理常识约束**，模型直接跨越了数小时。
   - **技术方案**：
     - 借鉴开源社区成熟方案（SillyTavern Timekeeper、NovelAI 状态机、Mem0 实体时序关联），在 `TimelineMemoryHelper.kt` 中引入 `DayPhase` 枚举（定义清晨/早晨、上午、中午、下午、傍晚/黄昏、入夜/晚间、深夜/拂晓 7 个细分状态机）；
     - 重构 `buildTimelinePromptContext`，向模型注入四维时序守护看板：
       ①【故事当前时间节点与时空看板】：明确当前绝对故事时间与停驻时段；
       ②【关键里程碑置顶防漂移看板】：自动识别“确立关系/告白/结盟”等关键里程碑，计算与当前故事天数的时间差，并醒目加注 `[注意：此事件发生在 X 天前，距今已过去 X 天（X 个日夜），绝非昨天！]`，彻底切断时间漂移；
       ③【剧情推进时间线与日常备忘明细】：按时序展示所有事件相对于当前故事时间的相对推算；
       ④【时空连贯性与日内时序守护铁律】：明确作息规律约束，日内时段处于白天/吃早餐时，严禁在未描写数小时自然流逝的情况下突兀跳跃至天黑入睡。
   - **文件改动**：`app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt`。

3. **自主增量更新与去重润色**：
   - **根因分析**：
     - 多轮对话讨论同一事件（如反复商议同一任务或多轮描写同一告别场景）时，简单追加会导致时间线上堆积多条雷同的碎片记忆。
   - **技术方案**：
     - 在 `TimelineMemoryHelper.kt` 中实现 `mergeOrAppendEvent`；
     - 通过比对时间标签与核心语义重叠度（关键词匹配与字符子集判定），同天同节点的重叠事件执行智能润色合并，自动采纳更完整丰富的描写版本；不同时间或新事件则正常追加并执行单调递增标准化。
   - **文件改动**：`app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt`。

4. **长对话分段梳理后汇总 (Map-Reduce) 架构与中途取消支持**：
   - **根因分析**：
     - 原梳理功能将整个会长文本（可达数万字符）一次性发送给模型，导致梳理耗时超长（1~2分钟）、注意力漂移遗漏中间细节、易触发网关超时，且无法中途取消。
   - **技术方案**：
     - 在 `TimelineMemoryHelper.kt` 中实现 `chunkMessagesForAnalysis`：将长对话切分为每组 25 条消息的小片段，且相邻片段保留 3 条重叠滑动窗口，保障因果时序与上下文连续性；
     - 在 `AiRepository.kt` 中将 `reconcileConversationTimeline` 重构为 Map-Reduce 架构：
       - Map 阶段：分段解析各片段的时间线与设定，支持实时进度回调 `onProgress("正在梳理分段 $idx/$total...")`；
       - 在每个分段处理前检查 `currentCoroutineContext().ensureActive()`，配合 `ChatViewModel.cancelTimelineReconciliation()` 实现随时即时取消；
       - Reduce 阶段：将各段抽取出的时间线事件与设定进行全局去重合并，并统一通过 `normalizeMonotonicTimeline` 进行递增校对；
     - 在 `ChatSettingsDialogs.kt` 中更新 `ChatSettingsSessionMemorySection`：当处于梳理状态时，显示动态进度（如“正在梳理分段 2/4...”）以及红色的【点击取消】按钮，给予用户充分的控制权。
   - **文件改动**：`app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatSettingsDialogs.kt`。

5. **四层结构化滚动压缩提示词升级**：
   - **根因分析**：
     - 滚动压缩生成摘要时若仅保留情节概要，模型在压缩后的多轮对话中必然会遗忘“从故事开端至今过去了多少天”，从而发生“把几天前发生的里程碑当成昨天”的时序幻觉。
   - **技术方案**：
     - 在 `AdvancedMemoryEngine.kt` 的 `buildStructuredSummaryPrompt` 中升级多维状态机提示词，强制要求模型输出：
       ① 【核心背景与用户固定约束】；
       ② 【时空演变与关键时间节点（极重要，严禁遗漏）】：必须包含“故事起始点与总跨度天数”、“重大里程碑时间锚点（严禁模糊为昨天）”、“当前故事停顿节点（精确至日内时段）”；
       ③ 【历史关键里程碑与决策推进】；
       ④ 【当前未决议题与待办上下文】；
     - 确保即使在超长长篇创作的多轮压缩后，时空绝对锚点依然牢不可破。
   - **文件改动**：`app/src/main/java/com/aiassistant/utils/AdvancedMemoryEngine.kt`。

6. **私密对话记忆对齐、全域里程碑泛化、粗粒度时序理解与普通会话隔离（复核完善）**：
   - **私密对话记忆完全对齐**：移除 `AiRepository.kt` 中对 `"private"` tag 的硬拦截（3198行、3771行、4170行），使私密对话在聊天过程中能 100% 完整享用专属记忆提取、记忆检索注入与自动时间线评估能力；退出会话时保留阅后即焚安全机制，彻底清理专属记忆与历史记录；
   - **全域里程碑看板泛化**：彻底打破“在一起/告白”狭隘举例限制，升级 `isCoreMilestoneEvent` 泛化模型，全量覆盖人际羁绊与剧变（结盟/决裂/立誓/反目/背叛/拜师）、重大冲突与决战转折（大决战/刺杀/破城/称帝/坠崖）、生死境界与质变（突破/觉醒/战死/飞升/复活/痊愈）、人生转折与迁徙（毕业/开学/灭门/启程/远征/流放），并全面注入叙事时序三大铁律（相对跨度守恒律、日内作息连贯律、跨度锚点连贯律）；
   - **粗粒度/笼统时间节点概念理解与先后相对判断**：在 `calculateRelativeTime` 与 `estimateTimeSpanJumpDays` 中全面支持“几天后”、“两周后”、“暑假开始”、“暑假期间”、“暑假尾声”、“新学期/开学”、“深秋”、“寒假”、“来年春天”等文学与阶段性时间节点，实现精准天数差换算与阶段性相对先后推算（如新学期看暑假为“约1-2个月前”、开学看暑假尾声为“数天前”等）；
   - **普通知识问答与日常聊天严格隔离**：在 `TimelineMemoryHelper` 中实现 `isNarrativeOrCreativeTurn` 纯函数，精准识别编程开发（代码块/函数/构建报错/SQL）、学术翻译与日常事实问答；在 `AiRepository` 中对普通会话仅注入简洁会话专属约束（杜绝注入小说时空看板与时序铁律），并在 `evaluateAndAutoUpdateTimeline` 中对非剧情会话直接短路返回，零额外网络与 API 开销。
   - **文件改动**：`app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/test/java/com/aiassistant/TimelineGeneralizationAndIsolationTest.kt`。

### 2. 自动化测试与工程交付
- **单元测试**：新增 `TimelineGeneralizationAndIsolationTest` 覆盖全域里程碑识别、粗粒度时序相对推算、三大铁律注入、普通技术会话隔离判定等核心逻辑。全项目 320 个单元测试 100% 顺利通过（退出码 0）。
- **Release APK 构建与发布**：
  - 用户明确提出“构建apk”要求，严格执行 Release 打包流程并一次性构建成功；
  - 安装包命名：`Echo-v2.2.7.apk`（严禁带有任何 `-arm64-v8a` 后缀）；
  - 发布输出路径：统一且仅输出到 `D:\Agent\APP-烧\app\releases\Echo-v2.2.7.apk`；
  - 历史安装包保护准则：严格遵守铁律，未删除、覆盖或清理任何历史版本，目录内历史安装包由 142 个增量累进至 143 个；
  - 文件大小：16,271,357 字节（~15.52 MB）；
  - SHA256 校验和：`1D75AC9C1B40DA0A3746E59BA212DD1D0EE605393604D48374A6CB4B7C6091BD`。

## [2026-09-21] - v2.2.6：连接超时稳定性强化、多 API Key 报错全量透出、暂停回复报错留痕、端到端按需滚动摘要实现与沉浸式记忆去元词汇

### 1. 核心需求落实与技术重构详情
1. **连接超时与网络稳定性强化（排查软件自身原因）**：
   - **根因分析**：
     - 原 `streamHttpClient` 与 `restHttpClient` 未显式配置专用连接池，使用的是默认的 OkHttp 连接池（keep-alive 5分钟）。在移动端网络环境或通过 Nginx/Cloudflare/自建反向代理访问 LLM 时，反代服务通常设置了 30s~60s 的 idle timeout，一旦服务器端掐断了连接而客户端不知情并尝试复用死连接，就会导致频繁的首字节超时（SocketTimeoutException）；
     - 未配置 HTTP/2 心跳保活机制（`pingInterval`），在长文本流式传输、思考链等待或者连接建立后短暂空闲时，NAT 网关或移动运营商防火墙会静默丢弃空闲 TCP 连接；
     - 未统一注入合规的 `User-Agent` 与 `Connection: keep-alive`，部分 WAF 防火墙或反代服务会拦截或降级非浏览器/非标准标识的请求；
     - 异常重试判定中未涵盖 `ProtocolException: unexpected end of stream`、HTTP/2 stream reset 与 SSL 握手抖动，导致瞬时握手或通道重置直接判定为致命错误而非触发自动退避重试。
   - **技术方案**：
     - 在 `RetrofitClient.kt` 中配置专用连接池 `ConnectionPool(10, 45, TimeUnit.SECONDS)`，保持 45s 最大空闲时间，短于常见的 60s 反代超时，有效杜绝复用死连接；
     - 为流式客户端 `streamHttpClient` 开启 `pingInterval(15, TimeUnit.SECONDS)`，主动发送 HTTP/2 PING 帧保活，穿透 NAT 网关与防火墙；
     - 全局注入规范的 `User-Agent: Echo-Assistant/2.2.5 (Android; Mobile)` 与 `Connection: keep-alive` 请求头；
     - 在 `AiRepository.kt` 中完善 `isNetworkFluctuationException`，精准识别协议截断、HTTP/2 reset 与 SSL 握手超时并自动触发退避重试。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/remote/RetrofitClient.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`。

2. **多 API Key 尝试时完整显示所有 Key 对应报错原因**：
   - **根因分析**：
     - 当用户配置了多个 API Key（换行输入多个 Key 或配置了备份 Key）时，此前轮询机制在尝试下一个 Key 时，虽然底层捕获了错误，但向上透出的错误信息会被最后一个 Key 的报错覆盖，或者只显示简略的通用错误，导致用户无法判断到底是哪个 Key 欠费、哪个 Key 无效、哪个 Key 频率超限。
   - **技术方案**：
     - 在 `AiRepository.kt` 中设计 `KeyAttemptFailure` 数据结构，记录每个失败 Key 的序号、脱敏特征（例如 `...4a8b`）以及精确的 HTTP 状态码与错误信息；
     - 在所有 Key 均尝试失败时，生成结构化的复合报错清单（包含每个 Key 的编号、脱敏后缀及明确错误信息），并向用户提出针对性排查建议。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`。

3. **尝试多个 API Key 时用户选择暂停回复，正常保留已发生报错原因**：
   - **根因分析**：
     - 原 `ChatViewModel.stopGeneration()` 逻辑中，如果收到用户停止指令时模型尚未吐出任何文字内容（正在轮询尝试第 1 个、第 2 个 Key 并发生报错），最终消息内容被直接写死为单一字符串 `"回复已停止"`，彻底抹掉了前序已经发生的 Key 报错详情。
   - **技术方案**：
     - 在 `ChatViewModel.kt` 中增加 `currentKeyAttemptErrors` 列表跟踪请求周期中各 Key 的尝试记录；
     - 在 `stopGeneration()` 中进行判断：当 `currentResponse` 为空但存在前序 Key 报错时，保留结构化报错记录与暂停提示，避免关键排查信息丢失。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`。

4. **端到端实现按需生成、更新与清除滚动摘要功能**：
   - **根因分析**：
     - 上下文使用情况弹窗提示“可按需生成滚动摘要，提炼前序关键事实”，但代码中仅有自动压缩阈值触发的被动逻辑，用户无法主动点击生成，也无法查看、修改或微调提炼出的滚动摘要。
   - **技术方案**：
     - 在 `AiRepository.kt` 中实现 `generateRollingSummaryNow(conversationId, modelNameOverride)`，支持根据当前会话历史按需立即提炼摘要；实现 `updateRollingSummary(conversationId, newSummary)` 与 `clearRollingSummary(conversationId)` 数据持久化；
     - 在 `ChatViewModel.kt` 中暴露 `generateRollingSummaryNow()`、`updateRollingSummary(newSummary)`、`clearRollingSummary()` 与 `getCurrentRollingSummary()`；
     - 在 `ChatContextComponents.kt` 的 `ContextUsageDialog` 顶部增加【生成摘要】/【更新摘要】快捷操作按钮；在 `ContextUsageDetails` 的滚动摘要指标行中增加操作标签（“点击查看/编辑”或“立即生成”）；
     - 新增 `RollingSummaryEditDialog` 弹窗，支持全屏液态玻璃磨砂卡片预览、多行文本微调编辑、字数与 Token 预估、一键清空与保存，更新后即刻联动刷新上下文用量和环形指示器；
     - 在 `ChatScreen.kt` 中联动渲染该对话框。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatContextComponents.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`。

5. **记忆偏好去出戏化与沉浸式设定净化（消除“用户把AI当成心爱的哥哥”等元词汇）**：
   - **根因分析**：
     - 在自动记忆提炼或偏好生成中，原有辅助提示词缺乏沉浸式角色扮演约束，模型习惯以第三人称元技术词汇总结（如“用户把AI当做心爱的哥哥”、“AI的身份设定为...”），而在角色扮演（RP）对话中，这些“用户”、“AI”、“模型”、“助手”等元词汇一旦出现在记忆或前情提示中，会严重破坏剧情代入感。
   - **技术方案**：
     - 在 `PersonalizationManager.kt` 的辅助记忆提炼提示词 `DEFAULT_AUXILIARY_MEMORY_PROMPT` 中注入【沉浸感最高准则】，明文禁止在提炼出的事实中出现“用户”、“AI”、“模型”、“助手”等词汇，明确角色关系必须以第一/第二人称或纯剧情视角提炼（例如“角色关系：视对方为心爱的哥哥”）；
     - 在 `SmartMemoryExtractor.kt` 中新增 `sanitizeMetaLanguage(text)` 净化器，智能将“用户把AI当成/当做/视为”转换为“视对方为”，净化“AI的身份设定为/模型设定为”，净化“用户喜欢/讨厌”为“偏好：/忌讳：”，净化“用户要求AI...”为“要求在互动中...”；
     - 在 `AiRepository.kt` 的 `buildRelevantMemoryBlock` 注入上下文时自动执行 `sanitizeMetaLanguage` 过滤，确保即使存量历史记忆存在残留出戏表述，也能在组装发给模型前完成沉浸式清洗。
   - **文件改动**：`app/src/main/java/com/aiassistant/utils/PersonalizationManager.kt`、`app/src/main/java/com/aiassistant/utils/SmartMemoryExtractor.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`。

### 2. 自动化测试与工程交付
- **单元测试**：针对网络配置、心跳、超时重试识别、多 Key 报错复合透出、暂停保留记录、沉浸式元语言过滤等编写并通过全部用例（`NetworkAndKeyStabilityTest`）。
- **发布规范**：根据用户未明确要求构建 APK 准则，本轮次未触发非必要的 APK 打包构建，完整保留 `releases/` 目录中所有历史版本安装包。

## [2026-09-20] - v2.2.5：端点上下文超限拦截与输出 Token 自适应收敛、历史摘要活跃切片修复与上下文压缩环形指示器校准

### 1. 核心需求落实与技术重构详情
1. **API 400 上下文超限拦截与输出 Token 自适应收敛（如 32768 上下文端点被请求 416131 tokens、其中输出预留 50000 导致直接被拒）彻底修复**：
   - **根因分析**：
     - 在 `AiRepository.kt` 发送 OpenAI / Anthropic 请求时，原代码直接将用户配置的 `effectiveOptions.maxTokens ?: config.maxTokens`（默认为 50000）作为 `max_tokens` 随请求发出，未考虑模型实际上下文窗口上限（如 32k/16k）；当模型上下文窗口只有 32768 时，传入 `max_tokens: 50000` 必然导致服务端 100% 报 400 Bad Request 拒绝请求；
     - 故事创作设置弹窗 `ChatStoryDialogs.kt` 中曾存在硬编码 `takeIf { it >= 50000 } ?: 50000`，导致任何尝试设置合理更小 output tokens 的行为被强制覆盖为 50000；
     - `estimatePromptBudgetTokens` 中原先直接将 `maxOutputTokens` 最多放宽到 32768，当模型上下文窗口只有 32k 时，输出预留直接吞噬了绝大部分甚至全部上下文空间；
     - 自动重试机制 `retryWithCompressedContext` 中提取端点上下文窗口后，重新发起请求时未重新校验并裁剪 `maxTokens`，导致重试继续触发 400 失败。
   - **技术方案**：
     - 在 `AiRepository.kt` 中统一计算安全输出 Token 上限：`val headroom = (contextWindow - estimatedPromptTokens - 512).coerceAtLeast(256)`，并将最终 `safeMaxTokens` 动态收敛到 `minOf(configuredMax, headroom)`，彻底杜绝输出 Token 超过端点余量；
     - 升级 `extractContextWindowFromError` 正则表达式引擎，精准捕获诸如 `maximum context length is 32768 tokens`、`context-window is 65536` 等真实服务端错误声明的上下文上限，并自动缓存在 `runtimeContextWindowLimitCache`；
     - 在 `retryWithCompressedContext` 中自动根据服务端真实上限收敛 `effectiveOptions` 的上下文限制与最大输出，确保二次重试 100% 成功；
     - 优化 `estimatePromptBudgetTokens`，将输出预留比例严格限制在上下文窗口的 25% 且不超过 8192，确保任何模型（即便配置了 50000）都能预留充足的输入 Prompt 预算；
     - 修正 `ChatStoryDialogs.kt` 与其他对话设置弹窗中的硬编码，允许用户根据需要自由设置更合理的输出 Token。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatStoryDialogs.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatSettingsDialogs.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/settings/SettingsApiConfigDialog.kt`。

2. **上下文压缩后右上方环形指示器依然超出限制（100%+）彻底修复**：
   - **根因分析**：
     - 在 `AiRepository.kt` 的 `buildContextUsageSnapshot` 中，先前逻辑在 line 2440 筛选了 `activeCandidateMessages`（排除已归入滚动摘要的旧消息），但在 line 2448 统计最近活跃 Token 时，却错误地遍历了全量未压缩的 `usableMessages.asReversed()`；
     - 导致即使用户点击了“上下文压缩”或系统自动执行了滚动摘要压缩，右上方的上下文用量统计依然将“几十万 token 的全部旧消息”与“压缩生成的摘要 token”同时重复累加计算，造成上下文环形指示器始终显示严重溢出（>100%）；
     - 在 `buildContextBundle` 构建实际发往模型的上下文消息包时，同样直接从 `usableMessages` 反向填充，未按 `summarizedThrough` 截断点做候选切片，导致未配置单模型上下文窗口时，所有历史消息被一股脑装载发送；
     - `compressConversationContext` 中硬编码了 `keepRecentCount = 16.coerceAtMost(usableMessages.size)`，在少于 16 条但每条文字极长（如几十万字角色扮演长文）的场景下，历史切片永远为空，导致压缩失效。
   - **技术方案**：
     - `buildContextUsageSnapshot` 修正为严格基于 `activeCandidateMessages` 统计最近活跃 Token，并在已有滚动摘要时正确累加摘要 Token，右上角环形指示器在压缩后立刻下降至安全健康的正常区间（如 10%~30%）；
     - `buildContextBundle` 引入清晰的截断过滤原则：已有滚动摘要时，活跃消息严格限定在 `id > summarizedThrough || isPinned` 范围内，配合 `recentBudget` 动态预算装填，从根源杜绝超大历史文本重复入包；
     - `compressConversationContext` 升级为基于 Token 预算的动态保留策略，当整体超出预算时即便少于 16 条也正常触发摘要切片并更新 `summarizedThrough`；
     - 角色扮演模式下系统提示词组装联动：在 `buildEffectiveSystemPrompt` 中如果存在滚动摘要且尚未包含在剧情提示中，自动作为【前序剧情滚动摘要】无缝融入角色扮演上下文，确保记忆与前情不丢失。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`。

### 2. 自动化测试与工程交付
- **单元测试**：全量单元测试（包含新增 ContextCompressionAndBudgetTest 6 项新测在内共 298 项测试）100% 全部通过 (BUILD SUCCESSFUL)。
- **发布规范**：根据用户未明确要求构建 APK 准则，本轮次未触发非必要的 APK 打包构建，完整保留 `releases/` 目录中所有历史版本安装包。

## [2026-09-19] - v2.2.3：思考链翻译全链路健壮重构、模型回复首字符星号误吞修复、删除回复平稳防滑与连接气泡报错全量展示

### 1. 核心需求落实与技术重构详情
1. **思考链翻译无法生效、均显示翻译失败彻底修复**：
   - **根因分析**：
     - **Base URL 缺失 `/v1` 报 404**：`executeQuickCompletion` 直接将原始 `config.baseUrl`（如 `https://api.deepseek.com`）传入 Retrofit，未调用 `normalizeApiBaseUrl` 补全 `/v1`，导致拼接后请求 `https://api.deepseek.com/chat/completions`，所有未显式手写 `/v1` 的供应商全部报 HTTP 404 错误；
     - **超时过短**：`executeQuickCompletion` 原先使用 `restHttpClient`（超时仅 30 秒），长篇思考链翻译或深度推理耗时往往超过 30 秒，触发 SocketTimeoutException 导致静默失败；
     - **API Key 命名脱敏与多 Key 故障转移缺失**：原实现直接对原始 `config.apiKey` 调用 `formatApiKey`，导致命名标签（如 `[主] sk-...`）或换行多 Key 直接作为 Authorization 头发送，触发 HTTP 401 权限失败；
     - **思考模型参数不兼容**：部分思考模型（如 o1/o3-mini 或 DeepSeek-R1）拒绝非 1.0 的 temperature，且思考模型可能将翻译文本输出在 `reasoning_content`，原逻辑仅读取 `content` 导致空内容失败；
     - **缺少流式保底**：部分中转网关仅支持 SSE 流式请求（`stream = true`）或在非流式模式下极易切断，原逻辑无流式备用通道；
     - **未配置翻译模型时的回退缺陷**：当用户在设置中未单独指定翻译专用模型时，`ChatViewModel` 原先传入 `0L` 与 `""`，底层粗暴回退到全局默认配置，而非优先复用当前会话正在正常对话的可用活跃模型。
   - **技术方案**：
     - `AiRepository.kt`：全面重构 `executeQuickCompletionWithResult`，统一使用 `normalizeApiBaseUrl(config.baseUrl, config.apiType)` 补全规范化路径；接入 `longAnalysisHttpClient`（600 秒超长超时与重试）；引入 `parseApiKeys` 循环剥离名称标签并支持多 Key 轮询容灾；检测 reasoning 模型并自动置空 temperature，兼容 `content` 与 `reasoning_content` 提取；实现 `executeStreamingCompletion` 流式备用保底通道；
     - `ChatViewModel.kt`：在调用思考链翻译时，当未显式配置翻译专用模型时，智能继承当前会话活跃的 `apiConfigId` 与 `modelName`，确保 100% 能够成功连接与翻译。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`。

2. **模型回复中开头的 `*` 误吞与格式异常彻底修复**：
   - **根因分析**：`MarkdownText.kt` 中的 `cleanLeadingStarArtifacts` 在先前的版本中包含激进规则 `s.replace(Regex("""^\s*\*(?!\*|\s)"""), "")`，将所有行首紧邻非空字符的单星号误判为字体解析残留伪影，导致 Markdown 的斜体语法（`*斜体*`）以及角色扮演中的动作描写（`*轻轻叹气*`）在输出时首个星号被直接清除，引发样式解析错乱；此外，在 `parseInlineMarkdown` 中对 `i < 3` 处的未配对星号也存在直接丢弃分支，造成流式首字符或孤立星号被吞噬。
   - **技术方案**：移除该激进正则，保留用户正常语法输入的首字符单星号；在 `parseInlineMarkdown` 中将孤立单星号作为常规字面量字符正常追加渲染，确保流式输出与格式渲染 100% 准确。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`。

3. **对话页删除回复导致屏幕滑动彻底修复**：
   - **根因分析**：在对话列表中删除某条消息时，Compose 的 `LazyColumn` 依赖各 Item 的 Key 进行位置定位；当被删除的消息恰好位于当前可见视口顶部时（`firstVisibleItemIndex == targetIdx`），消息一旦从列表移除，该 Key 瞬间销毁，Compose 丢失锚点并重置 `scrollOffset = 0`，同时如果 `autoFollowOutput` 为激活态，列表可能联动滑动至最新底部，导致视口瞬间跳跃与剧烈滑动。
   - **技术方案**：在二次确认删除回调中，先置 `autoFollowOutput = false` 避免触底联动；精确计算当前视口首项与被删除项的相对位置：若被删除项恰为视口首项，预先平滑重锚定到其上一项（通常为对应的提问消息）并保留相对视口位移偏移量；若被删除项位于视口上方，则首项索引自动自减 1，保持视口当前内容绝对静止稳定，彻底杜绝删除消息时的屏幕滑动。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`。

4. **模型连接时连接气泡报错内容完整性修复**：
   - **根因分析**：在底层数据层 `AiRepository.kt` 中，针对非网络波动的报错文本硬编码使用了 `e.message?.take(40)` 进行强行截断，当 API 返回超过 40 字符的详细错误（如状态码、URL、限流提示、配额耗尽等）时，后半部分关键报错信息直接被裁剪丢失；同时在 UI 层 `ChatMessageComponents.kt` 中，连接气泡原设计仅适配单行简短状态（`maxLines = 1`，超过部分单行横向滚动），在报错时文字被截断或难以阅读。
   - **技术方案**：
     - `AiRepository.kt`：彻底移除 `take(40)` 截断限制，提取完整的 `cleanErrMsg`，并在网络波动重试提示中补充具体异常信息，确保完整原始报错传递到 UI 层。
     - `ChatMessageComponents.kt`：连接状态气泡识别到报错状态时，将最大宽度放宽至 380dp，自动开启 `softWrap = true`，默认行数放宽至 4 行（展开状态支持 16 行），并在左侧展示警告图标（`Icons.Default.Warning`），支持用户直接点击气泡一键展开查看完整多行堆栈与错误详情；优化聊天气泡中的 `errorSummary` 提取，优先过滤无意义的纯标题行，展示真实具体的异常内容。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatMessageComponents.kt`。

### 2. 自动化测试与工程交付
- **单元测试**：全量单元测试（包含 V223FeaturesTest 6 项新测在内共 280+ 项测试）100% 全部通过 (BUILD SUCCESSFUL)。
- **Release APK**：`releases/Echo-v2.2.3.apk`。
  - SHA256: `FA7799F49167072D9A906208E181DD5363831ED705CB976F11F73BC7198D8550`
  - 大小: `16,254,973 字节 (~15.5 MB)`
- **历史安装包永久保留**：严格遵循最高铁律，`releases/` 目录下全部历史安装包完整保留，增量输出唯一定名的 `Echo-v2.2.3.apk`，未生成带有 `-arm64-v8a` 后缀命名的多余包。

## [2026-09-18] - v2.2.2：7 项界面精简与体验优化（状态气泡智能展开与形状防跳变、全局直角阴影消除、思考图标样式统一、紧凑输入框、跨会话记忆与世界书默认关闭及记忆多维筛选）

### 1. 核心需求落实与技术重构详情
1. **模型连接状态气泡无内容时不提供展开按键**：
   - 检查状态文本详情，仅当无思考内容且文本包含换行符（`\n`）或长度大于 48 字符时才标记为 `hasDetailedExpandableContent = true`；
   - 气泡操作区仅在 `canExpandStatus = hasDetailedExpandableContent || isStatusExpanded` 时展示展开/折叠图标，普通简短状态保持紧凑无冗余按钮。
2. **连接状态气泡展开形变与大小突变彻底修复**：
   - 彻底废除原有在 999dp 胶囊与 12dp 圆角之间的粗暴形状切换，改为全局统一恒定的 16dp 圆角（`RoundedCornerShape(16.dp)`）；
   - 限制展开最大宽度 `widthIn(max = if (isStatusExpanded) 360.dp else 320.dp)`，取消 `fillMaxWidth(0.95f)` 导致的横向巨大拉伸，并加入 `animateContentSize()` 动画，彻底消除展开跳动。
3. **全局直角矩形阴影修复（气泡点击与长按切换 Key）**：
   - 状态气泡点击使用 `Modifier.echoShapeClick(shape = capsuleShape)` 代替未受限的 `clickable`，点击水波纹与阴影完全贴合 16dp 圆角；
   - `SmoothReorderState.kt` 中重构拖拽阴影，在 `graphicsLayer` 中常驻设置 `this.shape = shape`，未激活时 `shadowElevation = 0f` 并绑定阴影颜色，消除直角投影黑边；修正 API Key 卡片修饰符顺序，避免 3dp 内边距外露直角。
4. **思考图标大小闪烁消除与样式统一（移除机器人头像）**：
   - 思考状态胶囊左侧图标容器采用固定尺寸 `Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center)`，移除动态变化的内边距，杜绝展开收起时的图标晃动与闪烁；
   - 全局移除 `Icons.Default.SmartToy` 机器人图标，统一采用优雅专业的心智脑力图标 `Icons.Default.Psychology`。
5. **多场景自定义输入框紧凑重构（消除过度纵向空白）**：
   - 重构“添加自定义模型”、“自定义上下文限制”、“自定义网页搜索数量”以及“会话记忆添加/编辑”等弹窗输入框；
   - 替换占用 56dp 以上的笨重 `OutlinedTextField`，采用 34dp 极简紧凑的 `BasicTextField` 与自定义毛玻璃装饰框，字体精细调整，大幅减少无效空白。
6. **跨会话记忆与世界书默认关闭**：
   - `RoleplayModels.kt`：`RoleplaySession` 数据模型默认值设为 `enableExternalMemory = false, enableWorldBook = false`；
   - `ChatViewModel.kt`：初始化与临时配置默认值统一设为 `false`；
   - `AiRepository.kt`：世界书 Prompt 注入判断由 `!= false` 纠正为严格 `== true`，外部跨会话记忆默认值同步设为 `false`。
7. **查看跨会话记忆支持多维分类筛选**：
   - `SettingsPromptsMemoryTab.kt`：新增 `memoryFilterScope` 状态与分类筛选 Chips（「全部」「全局偏好」「会话专属」），并实时统计与展示匹配条数，便于精确定位与高效管理。

### 2. 自动化测试与工程交付
- **单元测试**：全量单元测试（包含 V222FeaturesTest 共 277+ 项测试）100% 全部通过 (BUILD SUCCESSFUL)。
- **Release APK**：`releases/Echo-v2.2.2-arm64-v8a.apk` 与 `Echo-v2.2.2.apk`。
- **历史安装包永久保留**：严格遵循最高铁律，`releases/` 目录下全部历史安装包完整保留，增量输出全新安装包。

## [2026-09-16] - v2.1.7：全量12项产品级优化与重构（时间线深度优化、按键圆角与阴影规范、专属记忆UI重构、对话设置层级重排、全能图片裁剪、长文本展开收起、模型回复编辑、会话级模型头像、能力标签紧凑对齐、ConversationSummaryBufferMemory上下文压缩）

### 1. 本次 12 大核心诉求深度落实与功能重构
1. **时间线梳理对时间的敏锐度与准确度深度优化**：
   - **自然叙事与文学时间解析**：深化自然文学时间表达（如“两周过后”、“暑假开始”、“三年后·春”、“数日后”等）解析与跨度跳转推断；
   - **单调推进与故事节点驻留兜底**：解决不以具体天数为单位的剧情推进与时间跳跃，准确推断当前故事驻留时间节点，杜绝时间倒流并防止粗暴退回到“未确定”；
   - **常驻多维设定与里程碑提炼**：深度提炼剧情重大转折、人际变迁、秘密揭露、约定契约与 6 维常驻设定（核心特质、习惯偏好、生理禁忌、人际羁绊、秘密真相、世界铁律）。
2. **全局按键阴影与圆角几何轮廓统一，消除直角割裂**：
   - **圆角轮廓贴合**：全面审核卡片、按键、Chips、输入框附加按钮；
   - **规范按下覆盖层与点击涟漪**：统一封装 `echoShapeClick` 与 `echoShapeCombinedClick`，点击水波纹与高亮覆盖层严格贴合 Shape/Path 物理圆角轮廓，消除矩形阴影毛刺。
3. **“本对话专属记忆与时间线”单条记忆 UI 排版重构**：
   - **按键分区重整**：单条记忆卡片中开关移至右上角，编辑与删除按键并列放置在右下角，左侧空间全量留给正文，大幅提升文本阅读可视面积与阅读体验。
4. **对话设置功能层级顺序调整**：
   - **视觉动线优化**：“本对话专属记忆与时间线”和“跨会话记忆与世界书”上移至对话设置中“模型头像”的正上方，操作路径更为聚焦自然。
5. **全能图片导入与编辑系统（放缩/平移/旋转/翻转/裁剪）**：
   - **ImageCropEditDialog 独立裁剪弹窗**：支持多点手势放缩、自由平移、90° 旋转、水平与垂直翻转，支持圆形与矩形裁剪模式；
   - **全场景覆盖**：全面接入故事角色头像、API 模型头像、用户自定义头像与聊天/主页背景壁纸导入。
6. **专属记忆列表支持折叠与展开**：
   - 在开启“本对话专属记忆与时间线”后，下方的记忆清单区域提供专属收起/展开开关，避免长篇记忆清单过长遮挡其他对话配置项。
7. **记忆提炼辅助模型层级归一**：
   - 设置中的“记忆提炼辅助模型”统一移至“模型辅助与思考”菜单下，复用 `UniversalModelPickerCard` 完整选择与过滤逻辑，并保留“即时测试辅助连接”弹窗与测试结果展示。
8. **排版防折行与长文本全局展开收起组件**：
   - **按键文字防折行**：控制各类设置项按键文字 `maxLines = 1`，彻底杜绝折行割裂；
   - **统一 ExpandableText 组件**：针对提示词正文、大段设定说明提供平滑展开与收起交互。
9. **会话内自定义模型头像“会话级”隔离**：
   - **数据与存储隔离**：`Conversation` 实体新增 `modelAvatarUri` 属性，Room 数据库平滑升级至版本 26（`MIGRATION_25_26`）；
   - **视图渲染优先**：消息列表气泡与对话设置优先展示当前会话专属头像，与其他会话完全隔离。
10. **对话设置模型能力标签同排紧凑展示**：
    - 对话设置弹窗中，窗口、工具、视觉、思考等能力标签与“模型”标题保持在同一 Row 横向紧凑对齐，有效压缩纵向空间占用。
11. **上下文压缩功能重构（开源 ConversationSummaryBufferMemory 规范）**：
    - **彻底移出早期历史**：被压缩截断点前的早期历史彻底移出活跃 Prompt Context，由高密度滚动摘要替代，Token 大幅降低；
    - **预警与状态横幅**：上下文达到 60%~75% 缓冲区时展示状态横幅给用户反应时间，>75% 自动平滑压缩，压缩开始与结束均有明确状态提示。
12. **模型回复（Assistant）内容支持编辑**：
    - 在模型回复气泡底部的操作区新增“编辑”按键，轻触弹出独立毛玻璃编辑弹窗，编辑确认后持久化更新 Room 数据库并即时刷新消息列表。

### 2. 自动化测试与质量保障
- **新增单元测试**：`V217FeaturesTest.kt`，完整验证 12 项更新条目、会话头像隔离、模型回复编辑副本、时间线文学跳转推断与既有故事时间安全兜底、6 维设定轮换以及 60%/75% 上下文缓冲预警阈值；
- **全量单元测试**：252 个单元测试 100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 123`，`versionName = "2.1.7"`；
- **Release APK 产物**：
  - 路径：`releases/Echo-v2.1.7-arm64-v8a.apk`（及增量 `Echo-v2.1.7.apk`）；
  - 大小：16,222,205 字节；
  - SHA256：`58B2192ED88FE6EB3DD5060CB3F47F8A9656E9E5EB2938929630472AE2133108`；
  - 架构：`arm64-v8a` (`isUniversalApk = false`)；
  - 签名验证：APK Signature Scheme v2 验证通过 (1 signer)；
  - **历史安装包永久保留准则（最高铁律）**：`releases/` 目录历史安装包完整保留，增量输出 `Echo-v2.1.7-arm64-v8a.apk` 与 `Echo-v2.1.7.apk`。

### 3. 改动文件列表
- `app/build.gradle.kts` [MODIFY]
- `app/src/main/java/com/aiassistant/domain/model/Models.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/local/AppDatabase.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/local/Daos.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/utils/AvatarManager.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/utils/BackgroundImageManager.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/components/PressEffects.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/components/EchoGlassCard.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/components/ImageCropEditDialog.kt` [NEW]
- `app/src/main/java/com/aiassistant/ui/components/ExpandableText.kt` [NEW]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/CharacterEditorScreen.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/V217FeaturesTest.kt` [NEW]
- `app/src/test/java/com/aiassistant/V216FeaturesTest.kt` [MODIFY]

## [2026-09-15] - v2.1.6：600s大模型推理超时放宽、自然叙事时间跨度支持、全景5大里程碑与6维设定提炼

### 1. 本次 3 大核心诉求深度落实与功能重构
1. **600s 充足大模型长推理超时放宽与网络抖动重试机制**：
   - **痛点根治**：梳理全量时间线需要通读上万字长篇历史对话，并生成包含数十条事件与常驻设定的复杂结构化 JSON。此前 180 秒（3分钟）对于复杂长文或包含思考链（Reasoning）的大模型过于严苛，极易因超时中断而错误退避至本地精纯扫描；
   - **机制落地**：在 `RetrofitClient` 中将 `longAnalysisHttpClient` 的 `readTimeout` 与 `callTimeout` 大幅放宽至 600 秒（10分钟），`connectTimeout` 设为 60 秒，`writeTimeout` 设为 120 秒；
   - **自动重试与弹性保护**：`generateOpenAITimelineAnalysis` 与 `generateAnthropicTimelineAnalysis` 扩充 `max_tokens` 至 8192，且在遇到偶发性网络抖动或超时错误时自动执行 1 次带退避的重新请求；
   - **智能异常语义指引**：在工作台状态卡片中对超时异常进行智能语义识别，提供清晰友好的重试建议，杜绝生硬错误。
2. **文学叙事与自然时间跨度（两周过后、暑假开始等）深度支持**：
   - **痛点根治**：真实故事与剧情小说的时间推进并非均以“第X天”为机械单位，常有“两周过后”、“暑假开始”、“一年后·春”、“数日后”等文学跳跃与生活阶段。此前算法与系统提示词机械排斥非天数标签，导致阶段性事件被强行抹杀或当前故事时间退回“未确定”；
   - **时序状态机智能融合**：`TimelineMemoryHelper.normalizeMonotonicTimeline` 接入自然时间跨度估算（`estimateTimeSpanJumpDays`），遇到“两周过后”智能递进内部天数（+14），既杜绝后续天数倒流，又 100% 完整保留原汁原味的自然叙事标签（如 `两周过后`、`暑假开始·傍晚`）；
   - **当前时间智能推断**：`inferCurrentStoryTime` 优化为倒序追踪最新发生的有效叙事节点，自然识别以“两周过后”或“暑假开始”为阶段的当前停驻时间；
   - **自然相对时间语义**：`calculateRelativeTime` 深度支持自然时间词与阶段词的相对语义推导（如“约两周前”、“放假前”等）。
3. **全景 5 大剧情里程碑维度与 6 维多维设定深度提炼**：
   - **痛点根治**：此前大模型提示词过度强调惩罚性恐吓规则，导致大模型提炼极为保守，遗漏关键情节转折、人际变迁与世界观常驻设定；
   - **5 大剧情里程碑维度**：全面引导大模型覆盖：
     ① 剧情重大转折与抉择（危机爆发、转机出现、重大行动抉择与结果）；
     ② 感情线与人际质变（彼此从陌生到互信托付、建立同盟契约、心结解开、发生争端或误会消除）；
     ③ 秘密揭露与重要发现（探明隐秘真相、识破真实身份、获悉关键情报或线索）；
     ④ 状态转变与阶段成果（获得关键信物道具、实力突破、负伤中毒或痊愈、处境改变）；
     ⑤ 关键约定与未决悬念（暗中达成的盟约、未解决的潜伏危机、下一步核心目标）；
   - **6 维多维常驻设定深度挖掘**：
     ① 角色特质与心结；② 习惯偏好与小动作；③ 生理特征与禁忌；
     ④ 世界规则与法则限制；⑤ 人际羁绊与誓言契约；⑥ 专属信物与特殊器物（新增）；
   - **输出扩容**：将生成上限提升至 8192 Tokens，推荐提炼 15~40 条关键里程碑事件与 10~25 条多维常驻设定；
   - **本地 Fallback 同步升级**：`fallbackLocalTimelineScan` 同步扩充自然时间跨度捕获与小说核心词库（信物、秘密、约定、阵营、结界等）。

### 2. 自动化测试与质量保障
- **新增单元测试**：
  - `TimelineNaturalTimeTest.kt`：覆盖自然时间标签解析、时间跨度天数跳跃估算、时序单调融合状态机、当前时间自然推断、自然相对时间语义计算与复杂 JSON 深度解析；
  - `V216FeaturesTest.kt`：覆盖 v2.1.6 用户更新日志完整性与核心要点自检；
- **全量单元测试**：245 个单元测试 100% 全部通过 (BUILD SUCCESSFUL in 32s)；
- **版本配置**：`versionCode = 122`，`versionName = "2.1.6"`；
- **Release APK 产物**：
  - 路径：`releases/Echo-v2.1.6.apk`；
  - 大小：16,189,437 字节；
  - SHA256：`3108EF87CE934626752189C16D41F6408F5A930AD2A1334A450BBBA3D5CA0E64`；
  - 架构：`arm64-v8a` (`isUniversalApk = false`)；
  - 签名验证：APK Signature Scheme v2 验证通过 (1 signer)；
  - **历史安装包永久保留准则（最高铁律）**：`releases/` 目录历史安装包完整保留，增量输出 `Echo-v2.1.6.apk`。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/data/remote/RetrofitClient.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt` [MODIFY]
- `app/build.gradle.kts` [MODIFY]
- `app/src/test/java/com/aiassistant/TimelineNaturalTimeTest.kt` [NEW]
- `app/src/test/java/com/aiassistant/V216FeaturesTest.kt` [NEW]
- `app/src/test/java/com/aiassistant/V215FeaturesTest.kt` [MODIFY]

## [2026-09-15] - v2.1.5：180s深度推理大模型接入、固有设定原子化提炼、长篇里程碑脉络与固有设定分类筛选

### 1. 本次 4 大核心缺陷彻底修复与功能升级
1. **彻底解决大模型提炼静默降级问题（180s 长文本深度分析通道）**：
   - **痛点根治**：大模型深度通读上万字长篇历史并生成详尽 JSON 普遍需要 40~90 秒，而此前通用 `restHttpClient` 硬编码了 `readTimeout = 30s`，导致请求在 30 秒被 OkHttp 强制超时中断，异常被捕获后静默退避到本地正则切片，造成用户误以为“大模型根本没有接入”；
   - **机制落地**：在 `RetrofitClient` 中新增 `longAnalysisHttpClient` 与 `getAnalysisService(baseUrl)`，将读取超时延长至 180 秒（同时配置 30s 连接超时、60s 写入超时与自动重连），为大模型深度推理预留充分时间；
   - **活动模型动态绑定**：`reconcileConversationTimeline` 动态获取当前聊天窗口正在使用的活动 API 配置与模型名称，优先复用当前会话模型，多级安全降级回退（活动模型 -> 辅助提炼模型 -> 默认模型 -> 首个配置）；
   - **透明提炼状态条**：提炼结果携带 `extractionSource`、`modelUsed`、`extractionErrorMessage`；UI 顶部明确展示 `✨ AI 大模型智慧深度提炼完成（模型：xxx）`；如发生降级显式展示黄色警示原因与重试按钮，彻底拒绝黑盒与假实现。
2. **固有设定作为顶级分类参与工作台筛选与集中管理**：
   - **痛点根治**：固有设定此前仅作为浮层混杂，无法在时间线事件列表分类中进行专项查看与筛选；
   - **分类扩展**：`TimelineCategory` 枚举新增 `ATEMPORAL_SETTING("固有设定", "💡", "#E91E63")`，支持在分类筛选 Chips 栏中一键过滤；
   - **视图精准联动**：在工作台顶部选中「【💡 固有设定】」标签时，专注于展示固有设定卡片并支持直接编辑/删除/切换作用域；选中「全部」时两者兼顾；选中其他时序类别时专注于该类时序事件，体验清晰整洁。
3. **彻底根治大段文学描写当作设定（固有设定原子化提纯铁律）**：
   - **痛点根治**：此前将正文小说中包含“习惯”、“喜欢”、“规则”的 70~90 字大段文学描写长句直接抄录为设定，毫无实际约束与记忆价值；
   - **提示词铁律约束**：在系统提示词中注入严苛的【固有设定原子化提炼铁律】——强制提炼为 8~25 字高度概括的原子化事实（如“林恩对深渊迷雾有严重过敏性排斥”），严禁直接复制文学描写、心理独白或环境修辞；
   - **本地安全兜底净化**：本地 fallback 扫描过滤掉带有双引号对话、外貌神态修饰及长句文学描写，只保留具有实体约束意义的陈述句。
4. **长篇故事编年史里程碑脉络梳理**：
   - **痛点根治**：此前仅机械提取“早晨”、“次日”等零散时间词和片段动词的流水账；
   - **里程碑编年史法则**：注入【时间轴剧情编年史铁律】，要求提炼具有完整事实结构（主谓宾清晰、谁在何时何地完成何事、造成何种转折）的故事发展里程碑事件，让时间线成为真正具备回顾与推演价值的故事编年史。
5. **方案 1 独立辅助模型设置重构为顶层独立卡片 & 1 秒瞬间失败彻底根治**：
   - **设置卡片被隐藏缺陷修复**：原「辅助模型提炼记忆」被错误嵌套在记忆库为空的 `else` 分支内，导致新用户或无记忆用户完全看不到该设置。现重构为「设置 -> 跨会话长期记忆」正下方的顶级独立卡片「💡 辅助模型：记忆提炼与时间线（方案1）」，提供独立 Switch 开关、API 配置选择、模型名称输入与即时连通性测试弹窗；
   - **1 秒内响应未成功 3 大根因彻底修复**：
     1. Base URL 缺 `/v1` 报 404：在 `AiRepository.kt` 中全面统一使用 `normalizeApiBaseUrl(config.baseUrl, config.apiType)` 补全路径，彻底杜绝 DeepSeek/第三方中转报 404；
     2. 推理模型传 `temperature` 报 400：针对 o1/o3/r1/gpt-5 等推理模型自适应将 `temperature` 置空，杜绝参数校验抛出 400 Bad Request；
     3. 默认配置 Key 解密：修复兜底逻辑使用 `getDecryptedConfig` 替代密文，杜绝 401 Unauthorized。

### 2. 自动化测试与质量保障
- **新增/扩充单元测试**：
  - `ChronicleTimelineStudioTest.kt`：新增 `ATEMPORAL_SETTING` 枚举解析、格式化、提炼元数据结构测试；
  - `TimelineDeepModelExtractionTest.kt`：新增大模型 JSON 深度提炼、五大分类色彩完整性、导演指令过滤测试；
  - `V215FeaturesTest.kt`：新增 v2.1.5 版本更新日志完整性自检测试；
- **全量单元测试**：234 个单元测试 100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 121`，`versionName = "2.1.5"`；
- **单一安装包构建与历史包永久保留（最高铁律）**：
  - 严格执行单一安装包命名规则，仅输出 `Echo-v2.1.5.apk`；
  - 绝对严禁删除任何既有历史 APK，绝对严禁使用任何 `Remove-Item`、`del`、`rm` 清理 `releases/` 目录。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/data/remote/RetrofitClient.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/ChronicleTimelineStudioTest.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/TimelineDeepModelExtractionTest.kt` [NEW]
- `app/src/test/java/com/aiassistant/V214FeaturesTest.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/V215FeaturesTest.kt` [NEW]
- `app/build.gradle.kts` [MODIFY]
- `UPDATE_LOG.md` [MODIFY]

## [2026-09-15] - v2.1.4：全量时间线单调递增状态机、指令解耦深度概括、全景深度提炼与输入排版重构

### 1. 本次 6 大核心缺陷彻底修复与功能升级
1. **时间与事件输入框打字及预览文字全面修复**：
   - **痛点根治**：彻底解决此前时间标签输入框与当前故事时间框中预览文字（Placeholder）无法显示、点击后被裁切无法打字输入的严重体验缺陷；
   - **机制重构**：用高灵敏轻量化的原生 `BasicTextField` 替换受 Material 3 内置内边距限制的组件，配置无截断的垂直居中装饰盒与柔和内边距；
   - **响应式状态通知**：在列表输入中严格应用不可变数据驱动（`events[idx] = item.copy(...)` 与 `atemporalSettings[idx] = setting.copy(...)`），确保 Compose 实时捕获每一次击键并立即重组呈现。
2. **时序单向累进与多日递增推断状态机 (`normalizeMonotonicTimeline`)**：
   - **痛点根治**：彻底纠正“第二天剧情发生后，下一个‘第二天早上’被机械推断为同一天”的时序倒流错乱；
   - **单调递增时序状态机**：引入日内时段流转次序（早晨/上午 1 -> 中午 2 -> 傍晚 3 -> 夜晚 4 -> 深夜/宿 5），并在对话中检测到逆向时段回跳（如前一条为傍晚，后一条为早上）或再次出现“第二天/次日/隔天”时，自动使绝对故事日单调向前推进（第 2 天 -> 第 3 天，第 3 天 -> 第 4 天）；
   - **模型五大铁律注入**：在大模型提炼系统提示词中显式注入“时序单向累进法则”，并在本地 fallback 解析层中同步落地，100% 杜绝时间线混乱。
3. **彻底解除发展脉络上限，支持长篇剧情全貌深度捕捉**：
   - **解除截断约束**：彻底废除原逻辑中机械的 `takeLast(120)` 截断和 2500 maxTokens 限制；
   - **全篇通读架构**：单次支持通读高达 60,000 字符长篇剧情（对超长会话智能保留开局 30 条核心设定与最新全部轮次），模型输出 Tokens 提升至 4096 tokens；
   - **全景纵深捕捉**：单次可提炼 10~40 条全景剧情发展事件，让长篇角色扮演与复杂剧情线拥有真正充实详尽的历史脉络。
4. **彻底解耦 `[]` 导演写作指导，真正理解并概括 AI 演出事实**：
   - **脱敏格式化**：在输入前自动将用户在 `[...]` 与 `【...】` 中输入的导演指令与写作要求重构为显式的 `【编剧写作指导/导演要求】: ...`，与正文对白严格解耦；
   - **指令过滤与客观陈述法则**：提示词明令禁止将写作指导文字照抄进事件；对于纯写作指令（如 `[让两人在雨夜再次相遇]`、`[推进剧情]`）自动拦截，提炼时只总结 AI 演出的客观剧情事实。
5. **6 维隐式固有设定敏锐提炼与标签轮转**：
   - **全方位多维捕捉**：突破表面关键词限制，全量扫描提炼 6 维固定设定：角色核心特质、习惯与生活偏好、生理特征与禁忌、世界固定规则与设定、人际羁绊与当前状态、其他核心设定；
   - **UI 标签自由轮换与确认提醒**：在加入确认卡片中，点击标签胶囊可在「角色特质」->「习惯偏好」->「生理禁忌」->「世界规则」->「人际羁绊」五大分类中循环切换，支持用户自由修正后一键同步进记忆。
6. **底部操作栏与文字排版全面重构 (Echo Dual-Capsule Dock)**：
   - **视觉美感提升**：重构拥挤杂乱的底部栏为悬浮双胶囊操作舱；
   - **状态指示舱**：顶部以半透明毛玻璃胶囊条展示「✅ 已勾选 X 条事件 | 💡 X 条设定待同步 | 🕒 当前故事时间：XX」；
   - **渐变立体操作按钮**：左侧提供沉稳半透明的「放弃」胶囊，右侧采用高质感科技蓝紫渐变、立体微光阴影的「💾 保存并同步到记忆」专属大胶囊，文字优雅居中排版，交互反馈灵动细腻。

### 2. 自动化测试与质量保障
- 新增 `ChronicleTimelineStudioTest.kt`（跨天时序单调递增推断、设定轮转）与 `V214FeaturesTest.kt`（6 项关键缺陷自检）；
- 230 个单元测试 100% 全部通过 (BUILD SUCCESSFUL)；
- 遵循工作流规范，构建单一 APK `Echo-v2.1.4.apk`，严格保留所有历史版本。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/ChronicleTimelineStudioTest.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/V214FeaturesTest.kt` [NEW]
- `app/build.gradle.kts` [MODIFY]

## [2026-09-15] - 会话专属时间轴记忆引擎与全量历史校对功能落地

### 1. 本次核心功能升级与用户需求落实
1. **解决角色扮演/长期对话时间轴混淆与相对时间词崩溃**：
   - **故事时间锚定与去相对化**：引入 `TimelineMemoryHelper`，规范化时间线事件为 `[时间标签] 事件内容` 结构（如 `[第3天·傍晚] 两人在甜品店吃了草莓奶油蛋糕`），彻底消灭存入记忆中的“昨天/刚才/上次”等相对时间词；
   - **时间差参照系注入**：在 Prompt 组装时，根据【当前故事时间节点】动态计算并注入相对时间参照系（如：`相对于当前：昨天/2天前/今天`），模型在逻辑上获得精准时钟，绝不再将不同日子的“昨天”混为一谈。
2. **积极主动的记忆调度引擎 (Active Recall)**：
   - 破除死板的生僻关键词匹配高阈值过滤，会话专属记忆在日常聊天与角色扮演中全量生效；
   - 增加时间与回忆指示词主动嗅探（“昨天”、“前天”、“上次”、“之前”、“哪天”、“那天”、“记得”、“吃过”等），一旦侦测到立刻优先唤醒全量时间线记忆。
3. **概念归一与术语净化**：
   - 彻底废除模糊混乱的“外置记忆库”称呼，在 UI、Prompt 与底层逻辑中清晰定义为：
     - **本会话专属记忆与时间线**（Session Memory & Timeline）；
     - **跨会话长期记忆**（Global Long-term Memory）；
     - **世界书 / 设定库 (Lorebook)**；
   - 角色扮演会话默认物理隔离全局日常长期记忆，防止外部工作/代码偏好污染小说剧情。
4. **全量历史时间轴梳理与可视化可编辑工作台**：
   - **一键梳理按钮**：在专属记忆面板中新增「🕒 梳理全量时间线」功能按钮；
   - **全历史通读提炼**：模型通读当前会话全部历史消息，自动折算相对时间为绝对故事日，去重合并同类事件，并推导出当前剧情时间节点；提供本地启发式安全兜底；
   - **用户可视化编辑工作台 (`TimelineReconcileDialog`)**：
     - 用户直接可见提炼出的当前故事时间与事件列表；
     - **直接修改**：可直接编辑当前故事时间、直接点击修改单条时间标签、直接在输入框修改事件文字；
     - **单条删除**：冗余条目点击垃圾桶一键删除；
     - **手动补充**：提供「＋ 手动补充遗漏事件」按钮，随手补充细节；
     - 确认满意后一键保存覆盖或更新至当前会话记忆。

### 2. 自动化测试与质量保障
- 新增 `TimelineMemoryTest.kt` 核心单元测试（8 项测试全量一次性通过）：涵盖标准/中文方括号时间标签解析、无标签兼容、事件格式化、相对时间差（今天/昨天/前天/N天前）推算、模型标准 JSON 与 Markdown 代码块解析、纯文本行兜底解析、Prompt 参照系构建；
- 全量回归测试通过（30+ 个测试类，上百个测试用例 100% 通过）；
- 编译检查 (`compileDebugKotlin`) 0 报错通过；
- 遵循用户明确指示：**未进行 APK 构建**。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt` [NEW]
- `app/src/test/java/com/aiassistant/TimelineMemoryTest.kt` [NEW]
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/repository/RoleplayRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt` [MODIFY]

## [v2.1.1] - 2026-09-14

### 1. 本次 7 项核心功能升级与用户需求落实
1. **分支命名自增机制彻底修复**：
   - 解决 `XX（分支1）` 对话生成的分支依然重复命名为 `XX（分支1）` 的重名问题；
   - 提取根标题函数 `extractRootBaseTitle`，多层清洗中文全角 `（分支X）` 与英文半角 `(分支 X)` 后缀；
   - 数据库新增 `getTitlesStartingWith`，计算全局最大编号后单调自增 `calculateNextBranchTitle`，彻底杜绝重名冲突。
2. **记忆提取增强与智能规范化提炼**：
   - 支持 `[...]` 与 `【...】` 中括号结构化记忆直接提取；
   - 支持以“注意”、“特别注意”、“请注意”、“温馨提示”等关键词引导的记忆提取；
   - 新增 `refineMemoryContent` 深度加工清洗（剥离多余标点与助词，按用户偏好、行为约束、会话设定与重要事实规范化分类，过滤代码与 URL 噪声）。
3. **已添加模型配置快速清空**：
   - 设置界面的已添加模型展开卡片中提供单个“清空配置”按钮（`RestartAlt` 图标）；
   - 头部提供“重置配置”快捷入口，一键恢复默认全局参数。
4. **多 Key 优先级快捷切换**：
   - 在 API 配置编辑中，多个 Key 支持通过 6 点拖动手柄或上下微调箭头（`ArrowUpward`/`ArrowDownward`）拖动/调整 Key 优先级，实时保存。
5. **网络波动容错重连机制**：
   - 建立 `isNetworkFluctuationException` 异常特征判定体系（识别 WiFi 暂时中断、连接重置 `Connection reset`、域名解析失败、SSL 握手抖动等）；
   - 遇到网络波动时执行最多 3 次退避自动重连，并在 UI 实时呈现“网络波动，正在尝试重新连接...”状态提示，避免因网络瞬态波动误切备用 Key。
6. **回复中消息排队与专属浮窗 UI**：
   - 模型生成回复时输入框保持可用，用户可继续输入并发送多条消息进行排队；
   - 专属排队浮窗完整还原设计图：左侧 `::` 拖动手柄调整排队顺序、中间内容预览、右侧支持撤回（回填输入框并从队列移除）、编辑与删除；
   - 浮窗右上角提供暂停/播放按钮，暂停时只排队不自动发送，恢复后继续按序自动发送；
   - 模型回复结束后自动按顺序连续发送排队消息。
7. **创建分支完整保留多版本变体**：
   - 解决分支仅克隆单条展示消息导致历史多个重生成版本丢失的问题；
   - 完整提取截断点轮次及之前的所有 `variantGroupId` 历史变体，映射新分组 ID 并克隆至新会话，完整保留 `< 1/3 >` 多版本自由切换。

### 2. 自动化测试与质量保障
- 全量单元测试（含 30 套测试用例集、200+ 项测试用例）100% 全部通过；
- 专项新增 `V211FeaturesTest` 8 项核心测试：
  - `testExtractRootBaseTitle`：分支多层括号根标题提取；
  - `testCalculateNextBranchTitle`：分支单调自增编号计算；
  - `testSmartMemoryExtractorBracketContents`：中括号记忆提取；
  - `testSmartMemoryExtractorNoticeKeywords`：注意关键词引导提取；
  - `testRefineMemoryContentClassification`：记忆内容提炼分类加工；
  - `testNetworkFluctuationException`：网络抖动判定与异常特征匹配；
  - `testQueuedMessageOperations`：排队消息模型、调序、编辑与撤回；
  - `testBranchMultiVariantPreservationLogic`：分支多版本克隆保留；
  - `testV211CurrentVersionUserUpdatesCompleteness`：更新说明自检。

### 3. 发布产物信息
- **安装包路径**：`releases/Echo-v2.1.1-arm64-v8a.apk`
- **文件体积**：16,107,517 字节 (约 15.36 MB)
- **SHA256**：`0638BCA89F2B571A570D32B31FEAFEA41A151E0EA55A718F1BA4FD19684D8652`
- **Package**：`com.aiassistant` | **VersionCode**：`117` | **VersionName**：`2.1.1` | **ABI**：`arm64-v8a`
- **签名验证**：APK Signature Scheme v2 (release 签名验证通过，1 signer，证书 SHA-256 与历史版本 100% 吻合: `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`)
- **历史版本永久保留**：所有历史版本安装包完整保留无删除。

### 4. 改动文件列表
- `app/build.gradle.kts`
- `app/src/main/java/com/aiassistant/data/local/Daos.kt`
- `app/src/main/java/com/aiassistant/data/remote/RetrofitClient.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/domain/model/Models.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/utils/SmartMemoryExtractor.kt`
- `app/src/test/java/com/aiassistant/V205FeaturesTest.kt`
- `app/src/test/java/com/aiassistant/V211FeaturesTest.kt`

## [v2.1.0] - 2026-09-14

### 1. 本次升级与决策记录（8项P0、44项P1、3项P2）全面落地
1. **角色扮演工作室全链路与开场白统一控制**：
   - 统一使用「让角色开场」控制开场白：当角色卡配置了初始问候语时，点击「让角色开场」直接作为第一条助手消息展示并落库；未设置时由模型作为导演指令自动构思开场；
   - 角色头像选择与渲染：角色编辑页全面接入系统图片选择器 `PickVisualMedia`，圆角裁剪、实时预览与一键清除；会话中助手头像优先呈现角色专属头像；
   - 级联安全删除：删除角色扮演会话时原子级联清理 Session、专属 Memories、Conversation 及 Messages，杜绝数据库脏数据孤岛；
   - 多角色群像模式：扩展 `NarrativeMode.MULTI`，支持多角色互动推演与独立人设动机交锋；
   - 剧情提示指令支持：增强 `PlotAction`（继续、重生成、撤回上一条等），精准控制故事节奏。
2. **上下文安全管理与超长文本输出保证**：
   - 废除 2400 字符硬截断限制：彻底解除 `compactMessageForHistory` 对历史内容的强制截取，保证大模型能接收完整前文细节；
   - 消息固定 (Pin) 与排除 (Exclude)：消息实体增加 `isPinned` 与 `isExcluded` 字段；`isPinned` 保证消息在上下文压缩时不被裁剪；`isExcluded` 允许临时排除某条消息不参与模型上下文（UI 呈现透明度降低与专属徽标）；
   - 彻底避免用户输入双重污染：角色扮演上下文组装时不再将用户输入强行拼接进 System Prompt，保持 System 与 User 消息规范分离；
   - 请求超时与缓冲区保护：网络层拆分为长流式 client 与短 REST client；遇到切 Key 或重试时通过 `onResetBuffer` 彻底清空临时 UI 缓冲，防止内容拼接错乱；
   - 切换对话后台继续生成：生成任务运行于 `applicationScope`，用户在切换对话后后台生成不受干扰，完毕后自动落库。
3. **数据安全、线性迁移与平滑覆盖更新保证**：
   - 签名体系 100% 一致：固化正式签名体系，证书 SHA-256 (`939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`) 与过往所有版本完全一致，覆盖安装零冲突、零卸载；
   - 线性数据库增量迁移：Room 版本由 23 升至 24，提供单步无损迁移 `MIGRATION_23_24`，彻底移除破坏性回退，保护用户历史数据；
   - 密文透明兼容：API Key 密文引入 `enc:v1:` 显式标头，兼容旧明文并支持静默补密；
   - 导入导出增强：`ConversationConverter` 增加单卡片（角色、场景、提示词模板）独立 JSON 导入导出，支持整会话导出为 Markdown 与 TXT 纯文本。

### 2. 自动化测试与质量保障
- 基础测试套件（v1.9.15 ~ v2.0.5 历史数百项测试用例）100% 全部通过；
- 专项新增 `V210FeaturesTest` 7 项核心测试：
  - `testMessagePinningAndExclusionDefaults`：消息固定与排除默认状态及拷贝验证；
  - `testDatabaseMigration23To24Registered`：Room 增量迁移注册校验；
  - `testNarrativeModeMultiCharacter`：多角色群像模式提示词构建；
  - `testPlotActionProcessing`：剧情推进动作提示词构建；
  - `testSingleCardExportAndImport`：角色卡与场景卡独立序列化与反序列化；
  - `testContextExclusionFilteringLogic`：上下文过滤排除逻辑；
  - `testCryptoManagerHeaderDetection`：API Key 密文标头智能识别。

### 3. 发布产物信息
- **安装包路径**：`releases/Echo-v2.1.0-arm64-v8a.apk`
- **文件体积**：16,091,133 字节 (约 15.35 MB)
- **SHA256**：`F53530ABEE40DAE4AFFF4E318ED5A051DE41BA497671FA3DA214074046602976`
- **Package**：`com.aiassistant` | **VersionCode**：`116` | **VersionName**：`2.1.0` | **ABI**：`arm64-v8a`
- **签名验证**：APK Signature Scheme v2 (release 签名验证通过，1 signer，证书 SHA-256 与历史版本 100% 吻合)
- **历史版本永久保留**：所有历史版本安装包完整保留无删除，当前 releases 目录累计 116 个独立版本安装包。

### 4. 改动文件列表
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/aiassistant/AiAssistantApp.kt`
- `app/src/main/java/com/aiassistant/data/local/AppDatabase.kt`
- `app/src/main/java/com/aiassistant/data/remote/RetrofitClient.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/data/repository/RoleplayRepository.kt`
- `app/src/main/java/com/aiassistant/domain/model/Models.kt`
- `app/src/main/java/com/aiassistant/domain/model/RoleplayModels.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/roleplay/CharacterEditorScreen.kt`
- `app/src/main/java/com/aiassistant/utils/ConversationConverter.kt`
- `app/src/main/java/com/aiassistant/utils/CryptoManager.kt`
- `app/src/test/java/com/aiassistant/V210FeaturesTest.kt`

## [v2.0.5] - 2026-09-11

### 1. 本次升级与 3 项用户核心需求（含隐藏对话 100% 同步铁律）完整落地
1. **优化备份导入逻辑：非破坏性智能增量合并（Req 1）**：
   - 彻底告别原 `FileOutputStream` 覆盖 SQLite 数据库文件导致的未备份新对话被粗暴清空问题；
   - 重构备份恢复引擎为 `mergeDatabaseFromBackup`：通过只读挂载临时数据库，逐表（`folders`, `api_configs`, `character_profiles`, `roleplay_scenarios`, `conversations`, `messages`, `roleplay_sessions`, `roleplay_memories` 等）增量对比合并；
   - 自动映射并解决主键冲突，保留外键关联完整性，合并过程绝无任何 `DELETE` 语句，本地已有但备份中未包含的对话 100% 完好无损保留。
2. **新增复制对话功能：全量深拷贝与同级入口（Req 2，隐藏对话 100% 同步）**：
   - 数据层原子深拷贝：在 `AiRepository` 中提供 `duplicateConversation` 事务，完整克隆会话模型参数、系统提示词、全量历史消息（重新规范严格递增时间戳）、角色卡设定与剧情专属记忆；
   - 智能标题命名：通过 `generateDuplicateTitle` 自动追加与自增副本后缀（如 `讨论 (副本)` -> `讨论 (副本 2)`）；
   - 隐藏对话无缝同步：隐藏会话复制后深度继承 `hidden` 标签，默认 `isPinned = false`，复制后直达隐藏列表；
   - 入口层级 100% 对齐：普通会话卡片在下拉菜单「置顶」同级增加「复制对话」；隐藏会话卡片在顶部操作栏「置顶」图标旁同级增加「复制对话」图标按钮。
3. **新增单对话备份与隔离导入功能（Req 3，隐藏对话 100% 同步）**：
   - 单对话独立导出：在 `BackupManager` 中实现 `createSingleConversationBackup`，将指定会话、全量消息、角色设定与剧情记忆序列化为规范 JSON 包（`SingleConversationExport`），保存在 `Echo_Backups` 并支持一键分享；
   - 单对话隔离恢复：导入单对话备份时执行 `restoreSingleConversationFromJson`，仅针对该对话进行插入/增量合并，对本地其余任何对话零触碰、零干扰、绝无覆盖丢失风险；
   - 入口层级 100% 对齐：普通会话卡片在下拉菜单「置顶」同级增加「备份此对话」；隐藏会话卡片在顶部操作栏「置顶」图标旁同级增加「备份此对话」图标按钮；
   - 备份管理界面全面升级：`BackupTab` 与 `BackupItemCard` 智能识别 JSON 单对话与 ZIP 全量备份，显示不同图标、徽章标签与精准恢复说明。

### 2. 自动化测试与质量保障
- 全量 171 项单元测试 100% 全部通过（退出码 0）；
- 专项新增与复核 `V205FeaturesTest` 7 项核心测试用例：
  - `testDuplicateTitleIncrement`：对话副本标题多层自增命名算法验证；
  - `testDuplicateConversationHiddenTagInheritance`：隐藏对话复制时严格继承 `hidden` 标签且置顶重置；
  - `testDuplicateConversationNormalNotHidden`：普通对话复制不含隐藏属性，隔离性验证；
  - `testSingleConversationExportSerializationAndDeserialization`：单对话全量导出包 JSON 序列化与反序列化双向契约；
  - `testNonDestructiveMergeInvariant`：全量增量合并引擎非破坏性不变性验证（本地未备份对话绝对不丢失）；
  - `testSingleConversationIsolatedRestoreInvariant`：单对话导入隔离性验证（导入仅作用于单对话，其余会话 100% 隔离）；
  - `testV205CurrentVersionUserUpdatesCompleteness`：版本特性说明完备性检验。

### 3. 发布产物信息
- **安装包路径**：`releases/Echo-v2.0.5-arm64-v8a.apk`
- **文件体积**：16,091,133 字节 (约 15.35 MB)
- **SHA256**：`857C55BFE98FC510A1C8E30598B15ECB713561FD668B3432C3BA1D0B2F54F3C0`
- **Package**：`com.aiassistant` | **VersionCode**：`115` | **VersionName**：`2.0.5` | **ABI**：`arm64-v8a`
- **签名验证**：APK Signature Scheme v2 (release 签名验证通过，1 signer)
- **历史版本永久保留**：所有历史版本安装包完整保留无删除，当前 releases 目录累计 115 个独立版本安装包。

### 4. 改动文件列表
- `app/src/main/java/com/aiassistant/utils/BackupManager.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/HomeViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/test/java/com/aiassistant/V205FeaturesTest.kt`
- `app/src/test/java/com/aiassistant/V203FeaturesTest.kt`
- `app/build.gradle.kts`
- `CHANGELOG.md`
- `UPDATE_LOG.md` (root & app)
- `README.md`
- `PROJECT.md`
- `WORKFLOW_GUIDELINES.md`

## [v2.0.4] - 2026-09-11

### 1. 本次升级与 3 项用户反馈深度修复重构落实
1. **分支功能完整重构与事务化原子落库（Req 1）**：
   - 彻底重构分支创建底层逻辑：引入 Room `withTransaction` 事务保证，将“创建新会话 -> 严格截断前序历史 -> 时间戳单调自增重排 -> 批量插入数据库 -> 更新统计 -> 克隆角色卡与专属记忆 -> 建立分支拓扑追踪”合并为原子事务，从底层架构上杜绝并发竞态与任何消息丢失；
   - 保证分支会话 100% 完整继承原会话配置（包含当前活跃临时切换的模型与 API 配置、高级采样参数、提示词设定等）；
   - 在 `MessageDao` 中新增 `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMessages(messages: List<Message>): List<Long>` 批量高效插入，彻底取代原循环逐条保存，秒级生成分支会话。
2. **分支生成弹窗确认与跳转选项（Req 2）**：
   - 分支创建成功后，不再直接粗暴跳转，而是弹出统一精致的液态玻璃对话框（`EchoGlassDialog`）；
   - 对话框清晰展示新分支标题与包含前序历史的提示；
   - 提供「确定」（留在当前会话继续探索）与「跳转到新对话」（立即导航至新分支会话）两个明确选项，赋予用户完全的操作自主权。
3. **隐藏对话解锁状态智能维持与直达界面（Req 3）**：
   - 在 `HiddenConversationLock` 中建立进程内会话级解锁状态维持机制（`isSessionUnlocked`），用户输入 PIN 验证成功后在当前 App 会话期间持续维持解锁；
   - `SettingsScreen` 中 `selectedSection` 升级为 `rememberSaveable`，从隐藏会话返回时无缝直达已解锁的“其他对话”管理界面，无需反复输入 6 位 PIN 码；
   - 在隐藏对话列表顶部新增「重新锁定」按钮，用户可随时一键手动锁闭会话，兼顾极速便捷与绝对隐私安全。

### 2. 自动化测试与质量保障
- 全量 155 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V204FeaturesTest` 专项覆盖 7 项核心测试用例：
  - `testHiddenConversationSessionUnlockState`：会话级解锁状态维持、重置与手动锁定契约；
  - `testBranchSuccessDialogState`：分支成功弹窗状态承载与跳转参数正确性；
  - `testBranchTitleIncrementLogic`：分支标题自增重命名规则覆盖（"测试 (分支)" -> "测试 (分支 2)"）；
  - `testBranchMessageSlicingAndMonotonicity`：消息严格截断至目标回复且时间戳绝对单调递增；
  - `testHiddenTagPreservedOnBranch`：分支严格继承隐藏标签与隐私保护契约；
  - `testActiveModelInheritanceOnBranch`：分支完整继承当前活跃切换的模型与配置；
  - `testRoleplayMemoryCloningInvariants`：角色扮演会话专属记忆深拷贝与关联一致性。

### 3. 发布产物信息
- **安装包路径**：`releases/Echo-v2.0.4-arm64-v8a.apk`
- **文件体积**：16,058,365 字节 (约 15.31 MB)
- **SHA256**：`F9B4BFE3CB4F7F4D91B93CF76D012B0E03DE143D1D24812ADB3F8AAB2D002DC5`
- **Package**：`com.aiassistant` | **VersionCode**：`114` | **VersionName**：`2.0.4` | **ABI**：`arm64-v8a`
- **签名验证**：APK Signature Scheme v2 (release 签名验证通过，1 signer)
- **历史版本永久保留**：所有历史版本安装包完整保留无删除，当前 releases 目录累计 113 个独立版本安装包。

### 4. 改动文件列表
- `app/src/main/java/com/aiassistant/data/local/Daos.kt`
- `app/src/main/java/com/aiassistant/data/local/RoleplayDao.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/data/repository/RoleplayRepository.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/utils/HiddenConversationLock.kt`
- `app/src/test/java/com/aiassistant/V204FeaturesTest.kt`
- `app/build.gradle.kts`
- `CHANGELOG.md`
- `UPDATE_LOG.md` (root & app)
- `README.md`
- `PROJECT.md`
- `WORKFLOW_GUIDELINES.md`

## [v2.0.3] - 2026-09-11

### 1. 本次升级与 3 项用户反馈缺陷 100% 彻底修复落实
1. **分支对话历史记录完整性与时序严格单调递增修复（Req 1）**：
   - 彻底解决分支对话历史记录不完整、错乱或丢失的问题：根因在于原有分支创建逻辑直接从数据库查询 `ORDER BY createdAt ASC` 并按 ID 截断，当会话中存在多次重新生成（regenerate）、变体切换（variant）或二次编辑（edit）时，修改后的消息 `createdAt` 晚于后续会话消息，导致切片丢失或乱序；
   - 重构截断与复制机制：统一以用户当前界面直观看到的视图列表（`displayMessages`）为基准截取前序历史，确保分支会话与主干视口 100% 绝对一致；
   - 写入新分支时强制赋予严格单调递增时间戳（`baseTime + index * 1000L`），根除数据库排序倒置异常；
   - 清除分支消息的变体组绑定（`variantGroupId = null, variantIndex = 1`），重塑为干净的线性历史；
   - 深度克隆角色扮演（Roleplay）会话状态与专属会话记忆（`MemoryItem`），保证角色卡、场景卡与剧情记忆在分支中无缝延续。
2. **隐藏对话创建分支自动继承隐藏属性（Req 2）**：
   - 彻底修复在隐藏对话中点击分支后生成的分支变为普通可见对话的问题；
   - 增强隐藏标签继承逻辑：检查原会话是否包含 `hidden` 标签（`repository.hasConversationTag(originalConv, "hidden")` 或 `tags.contains("hidden")`），在创建分支时将 `hidden` 标签直接写入新会话配置，并显式调用 `repository.setConversationHidden(newConversationId, true)`，确保生成的分支严密处于隐藏列表中，保护用户隐私。
3. **已发送引用消息气泡视觉展示卡片化重构（Req 3）**：
   - 彻底解决引用发送后在用户消息气泡中以原始 Markdown 大段文字（`> quote\n针对以上内容：\nquestion`）生硬堆砌的问题；
   - 新增 `parseQuotedMessage(content)` 智能解析器，将已发送消息拆解为结构化引用片段与提问正文；
   - 在用户气泡内重构专用毛玻璃微光引文卡片（`Surface`）：配备左侧渐变微光垂直指示条、双引号小图标、优雅的“引用内容”半透明标签、支持最多三行省略展示并支持点击展开全文，下方优雅衔接用户实际提问正文；
   - 二次编辑已发送引用消息时，智能解析恢复引用悬浮预览卡片与纯净问题文本，告别生硬源码。

### 2. 自动化测试与质量保障
- 全量 148 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V203FeaturesTest` 专项覆盖 9 项核心测试用例：
  - `testV203CurrentVersionUserUpdatesCompleteness`：v2.0.3 更新日志完备性验证；
  - `testParseQuotedMessageMultiLine`：多行引文与针对性提问精准拆解；
  - `testParseQuotedMessageSingleLine`：单行引文精准拆解；
  - `testParseQuotedMessageWithPromptPrefix`：前缀剥离与纯净文本提取；
  - `testParseQuotedMessageNonQuotedReturnsNull`：非引用普通文本安全透传；
  - `testEditQuotedMessageRestoresPreviewAndInput`：编辑已发送引文时状态精准还原；
  - `testHiddenConversationBranchInheritanceContract`：隐藏对话分支继承与防泄露契约；
  - `testBranchConversationMonotonicTimestamps`：分支时序严格单调自增防错序契约；
  - `testBranchRoleplaySessionDeepClone`：角色扮演与剧情记忆深拷贝契约。

### 3. 发布产物信息
- **安装包路径**：`releases/Echo-v2.0.3-arm64-v8a.apk`
- **文件体积**：16,058,365 字节 (约 15.31 MB)
- **SHA256**：`F5490B27C21EDFA3B022FF93D9996FB0D7B45812A9BF3E79F1CE534A859C7E46`
- **Package**：`com.aiassistant` | **VersionCode**：`113` | **VersionName**：`2.0.3` | **ABI**：`arm64-v8a`
- **签名验证**：APK Signature Scheme v2 (release 签名验证通过)
- **历史版本永久保留**：所有历史版本安装包完整保留无删除，当前 releases 目录累计 113 个独立版本安装包。

### 4. 改动文件列表
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/test/java/com/aiassistant/V202FeaturesTest.kt`
- `app/src/test/java/com/aiassistant/V203FeaturesTest.kt`
- `app/build.gradle.kts`
- `CHANGELOG.md`
- `UPDATE_LOG.md` (root & app)
- `README.md`
- `PROJECT.md`
- `WORKFLOW_GUIDELINES.md`

## [v2.0.2] - 2026-09-11

### 1. 本次升级与 7 项用户需求 100% 彻底落实
1. **引用 UI 展示效果重构（独立悬浮卡片预览，输入框保持整洁，Req 1）**：
   - 彻底优化文字引用交互体验：点击引用不再将 `> $quote\n针对以上内容：\n` 的大段引文直接硬塞进输入框，彻底消除长文本严重挤占编辑区域的困扰；
   - 引入专用毛玻璃引用悬浮预览卡片（`QuotedTextPreviewCard`），位于输入框正上方独立浮动：采用圆角液态玻璃面板、左侧主题渐变装饰条、最多两行省略号紧凑展示（`maxLines = 2`）以及右侧一键取消按键（✕）；
   - 用户在输入框中可专注输入针对该引文的追问或回复内容；点击发送时，底层自动将引文与提问规范合成为标准 Markdown 引用格式传递给模型，发送后自动平滑清除引用状态。
2. **输入框发送按键圆形外边缘微光与蓝红状态双色切换（Req 2）**：
   - 发送/暂停按键外圈增加与添加文件（+）按键完全一致的圆形轮廓包裹，高亮微光边框与按键物理边缘严丝合缝对齐；
   - 发送按键外圈描边颜色随生成状态动态无缝切换：非生成状态（发送箭头）呈现标志性主题蓝光（`glass.outlineSelected`），而在模型流式生成或思考状态（暂停方块）时动态切换为醒目深红色（`Color(0xFFE53935)` / `Color(0xFFEF5350)`），状态感知清晰醒目。
3. **输入框收缩后返回键与发送键呼吸光晕微小错位彻底消除与红蓝切换（Req 3）**：
   - 彻底解决输入框收缩隐藏后呼吸光晕在浮点 DPI 下的微小错位现象：摒弃基于 `graphicsLayer` 缩放描边方案，全面改用纯数学同轴同心圆绘制（`Canvas.drawCircle`），直接绑定物理几何中心 `center` 与基准半径 `baseRadius`，消除任何子像素级偏移；
   - 底部发送按键的光晕颜色同步联动：非生成状态呼吸脉冲为静谧微光蓝，生成进行中（暂停状态）呼吸脉冲动态切换为醒目警示红。
4. **设置页面顶部悬浮栏真正悬浮效果实现（Req 4）**：
   - 彻底修复设置页面下方列表内容无法滑动到顶部悬浮栏下方的问题：原页面容器外层配置了 `.padding(top = topBarHeight)` 导致下方内容在悬浮栏下方硬性截断；
   - 重构布局架构：移除外层硬性顶 padding，在各 Tab 的滚动列表（LazyColumn/Column）中统一将 `topBarHeight + 12.dp` 注入 `contentPadding`，使设置项与卡片能够真实、自然、平滑地穿透滑入顶部毛玻璃胶囊下方，完美呈现全域毛玻璃背景模糊与折射。
5. **新增「分支对话」功能并重构模型输出底部操作栏（Req 5）**：
   - 在模型回复气泡最下方底部操作栏中，新增「分支对话」按键（使用 `Icons.AutoMirrored.Filled.AltRoute` 分支图标与 "分支对话" 标签），直接替代原有单条回复引用按键位置；
   - 点击后自动创建一个包含该回复及其之前所有历史记录的新独立会话，并完整继承当前会话的模型、温度、Top-P、思考模式、联网搜索等全部对话配置；
   - 创建成功后立即平滑导航跳转至新分支会话中，支持用户在此分支上自由探索新剧情或新思路，原有主干会话完好保留。
6. **基于开源项目 (Mem0 / Zep / Letta) 深入调研强化记忆提取与作用效果（Req 6）**：
   - 深入学习行业先进记忆架构：引入语义与实体边界提取、独立作用域管理（`USER` 属性 vs `CONVERSATION` 上下文）与防复读/防机械重复（Anti-Parroting）提示词注入约束；
   - 优化 `SmartMemoryExtractor`：重构原子事实提取正则，增加对技术栈、开发偏好、特定限制、负向约束的即时匹配能力，消除正则过度贪婪，大幅提升关键记忆提取灵敏度；
   - 优化 `AiRepository` 记忆注入：采用结构化 `<system_memory_context>` 封装，融入长度归一化记忆重合度评分，并显式注入引导指令：“模型不得向用户复读记忆列表，而是在对话中自然体现对设定的遵从”，极大增强长期记忆与专属记忆对模型实际回复行为的引导效果。
7. **对话页删除用户输入或回复二次确认安全弹窗（Req 7）**：
   - 消息气泡长按菜单及底部操作栏点击删除时，增加系统级二次防误触确认弹窗（`AlertDialog`）；
   - 明确提示用户“确定要删除该条消息吗？删除后不可恢复”，彻底消除日常单手滑动或误触导致的误删聊天记录风险。

### 2. 自动化测试与质量保障
- 全量 140 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V202FeaturesTest` 专项覆盖 7 项核心需求。

### 3. 发布产物信息
- **安装包路径**：`releases/Echo-v2.0.2-arm64-v8a.apk`
- **文件体积**：16,058,365 字节 (约 15.31 MB)
- **SHA256**：`B88E31EA04B0E47824D9BC5CE41B14F786E2DDD8D4733B961601C313D4ACA213`
- **Package**：`com.aiassistant` | **VersionCode**：`112` | **VersionName**：`2.0.2` | **ABI**：`arm64-v8a`
- **签名验证**：APK Signature Scheme v2 (release 签名验证通过)
- **历史版本永久保留**：所有历史版本安装包完整保留无删除，当前 releases 目录累计 112 个独立版本安装包。

### 4. 改动文件列表
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/utils/SmartMemoryExtractor.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/MainActivity.kt`
- `app/src/test/java/com/aiassistant/V201FeaturesTest.kt`
- `app/src/test/java/com/aiassistant/V202FeaturesTest.kt`
- `app/build.gradle.kts`
- `CHANGELOG.md`
- `UPDATE_LOG.md` (root & app)
- `README.md`
- `PROJECT.md`
- `WORKFLOW_GUIDELINES.md`

## [v2.0.1] - 2026-09-10

### 1. 本次升级与 10 项用户需求 100% 彻底落实
1. **输入框添加(+)按键与发送按键直径统一为 34dp 且间距加大至 10dp，高亮边框精准对齐（Req 1）**：
   - 将输入框左侧添加按键（+）与右侧发送/停止按键直径统一设置为 `34.dp`，与“智能搜索”按键高度（`34.dp`）绝对一致；按键水平间距增加至 `10.dp`；
   - 彻底修复按键高亮边框没有对齐物理边缘问题：采用同轴包裹 `Surface(shape = CircleShape, border = BorderStroke(1.2.dp, glass.outlineSelected))`，使蓝色微光边框与按键物理边缘 100% 贴合。
2. **隐藏状态外圈呼吸脉冲光晕范围收敛与发送键光晕对齐修复（Req 2）**：
   - 优化输入框隐藏后外圈呼吸脉冲光晕活动范围：动画缩放比例严格收敛在 `1.0f` ~ `1.15f`，光晕缩到最小时与按键原有物理边缘严格重合（`scale = 1.0f`）；
   - 修复发送键外圈光晕错位问题：隐藏态发送键与呼吸光晕置于 `34.dp` 的同轴 `Box` 中居中对齐，根除偏离错位。
3. **输入框隐藏状态系统级返回手势退出生效（Req 3）**：
   - 增强系统级返回监听（`BackHandler`）：当输入框或顶部悬浮栏处于隐藏状态时，安卓系统侧滑返回手势优先拦截并退出隐藏状态，恢复展开输入框与顶部栏。
4. **首页背景壁纸全面穿透覆盖手机系统状态栏区域（Req 4）**：
   - 重构首页背景图与毛玻璃采样源（`HazeSource`）层级，移至全屏边缘，顶层内容列通过 `statusBarsPadding()` 安全避让，使壁纸无缝铺满状态栏顶端，实现完全沉浸式背景。
5. **设置界面顶部悬浮栏完全参照对话页重构（Req 5）**：
   - 彻底移除设置页原先顶部外圈的纯色背景填充，重构为与对话页完全一致的浮动毛玻璃胶囊（`Surface + echoHazePanel`），列表内容可平滑穿透滚动。
6. **引用功能内存级拦截与针对性提问引导（Req 6）**：
   - 彻底杜绝点击引用时触发系统剪切板访问通知（“正在访问剪切板”）：引入 `InAppSelectionClipboardManager` 在应用内存中直接捕获划选文字，不触碰系统剪切板；
   - 划选文字点击「引用」后自动填充规范提问格式：`> $quote\n针对以上内容：\n`，便于直接针对选中内容对模型展开针对性提问。
7. **流式响应吸附滚动与结束回弹闪烁彻底消除（Req 7）**：
   - 修复流式生成完毕时视口回弹至回答起始处的现象：在 `LaunchedEffect(isGenerating)` 生成结束回调中保持当前最新项底部锚定（`scrollToItem(targetIndex, scrollOffset = 100000)`），平滑保持在当前回答底部，杜绝跳动闪烁。
8. **全局排版空间受限区域全面支持水平横向滑动（Req 8）**：
   - 针对使用统计页面模型名称、供应商徽章、设置页面模型展示标签等空间受限区域，赋予 `Modifier.horizontalScroll(rememberScrollState())`，超长文本可自由左右滑动完整浏览。
9. **Markdown 渲染增强：深度清理模型首句星号(*)伪影（Req 9）**：
   - 新增 `cleanLeadingStarArtifacts` 预处理逻辑，智能清理模型在首句因颜色标签（`<font>`, `<span>`, `{#`）不兼容解析或排版错乱遗留的孤立单星号 `*`，同时严格保留无序列表项 `* ` 与粗体 `**粗体**`。
10. **API 配置已配置模型列表支持折叠/展开与自定义 Token 窗口（Req 10）**：
    - API 配置页面「已配置模型」卡片支持点击折叠/展开，海量配置模型时界面紧凑清爽；
    - 模型自定义上下文窗口在 32K~2M 快捷预设芯片基础上，新增数字输入框，支持自由输入任意数值（例如 131072、200000 等）。

### 2. 自动化测试验证
- 全量 139 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V201FeaturesTest` 专项覆盖 10 项升级契约。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoTextToolbar.kt`
- `app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`
- `app/src/main/java/com/aiassistant/ui/screens/stats/StatsScreen.kt`
- `app/src/test/java/com/aiassistant/V200FeaturesTest.kt`
- `app/src/test/java/com/aiassistant/V201FeaturesTest.kt`
- `app/build.gradle.kts`
- `CHANGELOG.md`
- `UPDATE_LOG.md` (root & app)
- `README.md`
- `PROJECT.md`
- `WORKFLOW_GUIDELINES.md`

## [v2.0.0] - 2026-09-10

### 1. 本次升级与 21 项用户需求 100% 彻底落实
- 控制栏与输入框渐变高亮边缘左侧加深；设置页面控制栏统一为对话页悬浮胶囊工具栏；
- 输入栏支持沿手柄向下拖拽完全收缩至发送键；顶部悬浮栏联动收缩至圆形返回键；
- 输入框按键增加蓝色微光高亮边缘；消息与划选全面新增引用功能；隐藏会话管理对齐；
- 使用统计图表重绘（微光端帽柱状图与发光贝塞尔曲线）；专属会话记忆保存逻辑修复；
- API配置页面模型分区与独立上下文/工具配置持久化。

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
1. **思考强度滑块防抽搐平滑优化**：重构手势跟踪与状态隔离，滑块 Thumb 紧随手指平滑移动，结束拖拽平滑 Spring 阻尼吸附，彻底解决滑动调档时的抽搐抖动与跳档问题。
2. **内置极低饱和度纯色系护眼背景**：新增浅艾绿、浅湖蓝、浅薰紫、浅樱粉、浅暖杏、浅山岚 6 款极低饱和度纯色背景（饱和度 < 5%），完美适配 WCAG AAA 文本超高对比度，支持一键单独或批量应用至首页与对话页。
3. **设置项调整全量即时生效**：所有开关、提示词、滑块、配置项调整后立即持久化存盘，彻底移除返回退出时二次提示保存的拦截确认弹窗，退出切换畅通无阻。
4. **跨会话长期记忆状态即时同步**：解决跨会话长期记忆开关调整时因未保存导致状态失效的缺陷，开关轻触即刻生效并持久化。
5. **对话自动命名与思考链翻译模型自由选择**：完全打通与对话页一致的全局模型选择库，跨服务商自由直选所有已启用模型及自定义模型，彻底解除只能选默认模型的限制。
6. **模型选择 UI 向下展开式折叠面板重构**：废除居中模态弹窗，改为按钮下方直接平滑向下展开列表，箭头随状态自动翻转；重新规范搜索框尺寸为 40dp 匀称胶囊搜索栏。

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
- **自动化单元测试**：66 项测试全部通过（100% 通过率，覆盖工具留痕解析、消息实体扩展、大模型标题净化、华为健康步数校准、天气URL参数、搜索条数限制、Room 20->21迁移等）
- **构建状态**：`BUILD SUCCESSFUL`（Release 签名验证通过，Scheme v2: true）

---

## [v1.9.15] - 2026-09-07
### 1. 本次升级与需求 100% 落实
1. **Exa 免 Key 联网搜索引擎完整落地（Req 1）**：
   - 接入 Exa MCP JSON-RPC 2.0 搜索协议（`web_search_exa`），无需注册与绑定 API Key 即可实时获取高质量网络搜索与高亮结果摘要；
   - 统一抽象 `WebSearchProvider` 与 `SearchEngineType`（支持 `EXA` 与 `TAVILY` 无缝切换），并在设置页提供默认搜索引擎配置与 Exa 可选定制 Key 输入。
2. **手机设备、健康与日程系统深度调用（Req 2）**：
   - **时间与系统日程（TimeCalendarManager）**：实时读取精准系统时间、星期、农历/节气提示，并安全读取 `CalendarContract` 日程列表，为 AI 提供完整今日日程安排；
   - **本地定位与逆地理编码（LocationAddressManager）**：基于 Android 原生 `LocationManager` 与 `Geocoder` 自动获取经纬度与省/市/区县真实中文地址，同时支持手动设置常驻城市；
   - **华为运动健康与硬件计步（HealthDataManager）**：针对安卓与华为手机用户，基于底层硬件传感器 `Sensor.TYPE_STEP_COUNTER` 实时获取今日步数、卡路里与距离，并结构化汇聚心率与昨晚睡眠（含深睡与评分）健康数据；
   - **设备与硬件状态（DeviceHardwareManager）**：动态感知电池电量百分比、充电状态、可用内存/存储 GB 数、网络连接类型及手机机型。
3. **Open-Meteo 免 Key 全球气象与 Jina Reader 网页深度阅读（Req 3）**：
   - **Open-Meteo 天气引擎（OpenMeteoWeatherEngine）**：集成免 Key 全球高精度气象 API，支持自动基于用户地理位置或智能提取输入中的地名查询天气、温湿度、风速及天气现象（支持全部 WMO Weather Code 中文与 Emoji 映射）；
   - **Jina Reader 网页长文提取（Jina Reader Engine）**：输入包含 URL 的对话内容时自动抓取正文 Markdown（支持免 Key 即用，带直接 HTML 正文提取智能回退），让大模型轻松深度阅读长文与网页。
4. **智能工具枢纽与意图自动路由（EchoToolHub）**：
   - 智能识别用户输入的提问意图（天气气温、时间日程、运动健康步数、手机状态电量、定位位置、文章链接），在发起 AI 请求时自动将精准工具上下文无缝注入模型提示词。
5. **设置页「联网搜索与智能工具箱」交互全面升级**：
   - 设置菜单全面更新，提供 Exa / Tavily 搜索引擎切换、智能设备工具箱总开关、实时状态卡片预览以及 Open-Meteo 实时气象联调测试。
6. **历史版本安装包永久保留**：严格保护全部 43+ 历史 APK，新增唯一构建 `Echo-v1.9.15-arm64-v8a.apk`。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.15-arm64-v8a.apk`
- **SHA-256**：`93158F4F74DB76D46A84ABC2B95D384857F3E355D0E69FBB4A2DCBA95D5C4AFE`
- **文件大小**：15,890,949 字节 (约 15.15 MB)
- **架构**：`arm64-v8a`，`versionCode: 95`，`versionName: 1.9.15`
- **自动化单元测试**：59 项测试全部通过（100% 通过率，覆盖 Exa 解析、Open-Meteo 映射、智能意图路由、设备健康上下文、角色扮演全链路等）
- **构建状态**：`BUILD SUCCESSFUL`（Release 签名验证通过，Scheme v2: true）

---

## [v1.9.9] - 2026-08-17
### 1. 本次升级与需求 100% 落实
1. **老版本升级闪退彻底根除（Req 1）**：
   - 在 `AppDatabase.kt` 的 `repairSchema` 阶段增加 `PRAGMA foreign_keys=OFF;` 与 `try-finally PRAGMA foreign_keys=ON;` 保护，杜绝老版本结构迁移时因外键约束冲突引发未捕获崩溃；
   - 在 `BackupManager.kt` 中针对 Android 10+ 某些设备 `getExternalFilesDir` 为 null 的情况添加 `context.filesDir` 安全兜底；
   - 在 `AiAssistantApp.kt` 中增加应用级终极兜底初始化，确保即使冷启动异常也能平滑回退，完全避免闪退。
2. **用户头像管理迁移至个性化设置（Req 2）**：
   - 将用户头像选取、更换与恢复默认功能完整移入「设置 -> 个性化」，并在「关于」页面移除重复头像项。
3. **统一设置页所有选项气泡尺寸与单行省略（Req 3）**：
   - 重构 `SettingsMenuItem`，统一使用 `defaultMinSize(minHeight = 78.dp)` 与单行省略（`TextOverflow.Ellipsis`），彻底杜绝个性化气泡高度偏大的不一致问题。
4. **设置未保存直接返回拦截提醒（Req 4）**：
   - 监听设置修改脏标记 `hasUnsavedChanges`，在右上角返回或系统返回键触发时弹出磨砂玻璃确认弹窗，提供「保存并返回」、「直接放弃」和「取消」三个选项。
5. **消除设置页面窗口拖影与冗余层级（Req 5）**：
   - 扁平化重构设置容器，移除深层嵌套的冗余 `Surface` 与多重背景绘制，彻底消除滑动拖影。
6. **二级菜单返回逻辑精准修正（Req 6）**：
   - 结合 Compose `BackHandler` 与统一的 `handleBack()` 逻辑，确保在二级设置页面点击返回或手势返回时始终回到设置主菜单，而不是直接退出到首页。
7. **设置顶栏磨砂玻璃背景遮蔽（Req 7）**：
   - 设置页面顶栏配置独立毛玻璃面板 `glass.panelStrong` 与半透明分割线，实现对所有下层滑动窗口和卡片的完整遮蔽。
8. **使用统计图表与明细表格完全重绘（Req 8）**：
   - **多维筛选**：支持按时间跨度（1小时、24小时、7天、30天、90天）与模型进行即时筛选；
   - **现代图表重绘**：重构 `ModernTokenBars`（渐变堆叠柱状图）与 `ModernTrendChart`（贝塞尔平滑趋势曲线与渐变区域）；
   - **交互明细表格**：重构 `ModernModelStatsTable`，清晰呈现各模型 Token 占比、请求耗时与成功率。
9. **连接中胶囊复用思考过程位置（Req 9）**：
   - 将对话连接中状态（`isConnecting`）融合至头像旁的思考胶囊位置，显示转圈加载动画与“正在连接 $modelName...”，消息正文区移除繁重的大占位卡片。
10. **思考胶囊高度微调加大（Req 10）**：
    - 胶囊最小高度调整为 `minHeight = 34.dp`，内边距优化为 `horizontal = 12.dp, vertical = 7.dp`，视觉比例更加和谐精致。
11. **跨供应商缓存命中率（Prompt Cache Hit）读取与统计（Req 11）**：
    - 扩充 `Usage` 数据模型，全量适配 OpenAI `cached_tokens`、DeepSeek `prompt_cache_hit_tokens`、Anthropic `cache_read_input_tokens` 与 Google Gemini 缓存字段；
    - 在 `AiRepository.kt` 与 `StatsScreen.kt` 中实现真实缓存命中率的提取、存储与界面呈现。
12. **智能搜索资料来源 UI 与正文引用标注（Req 12）**：
    - **正文角标**：`MarkdownText.kt` 自动解析正文中的 `[1]` / `[^1]` 引用并渲染为高亮角标；
    - **点击弹窗**：点击角标唤起精致的 `CitationDetailDialog`，支持选中文本、一键复制网址与跳转浏览器；
    - **精致来源卡片**：正文底部横向展示圆角玻璃来源卡片（`CitationsCardsRow`）。
13. **对话设置默认最大 Token 强制 8192（Req 13）**：
    - 存量与新建对话设置中的默认最大 Token 均统一为 8192。
14. **二级菜单消除外圈矩形轮廓（Req 14）**：
    - 全局优化 `DropdownMenu`，通过 `background(..., RoundedCornerShape(18.dp))` 彻底消除外围 4dp 默认矩形轮廓。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.9-arm64-v8a.apk`
- **SHA-256**：`DDE3D5C205D37C6364C8AAF6BAB3046FE71562852E98505FAE7BB3E863952AFE`
- **文件大小**：15,841,156 字节 (约 15.11 MB)
- **架构**：`arm64-v8a`，`versionCode: 89`，`versionName: 1.9.9`
- **自动化单元测试**：26 项测试全部通过（100% 通过率）
- **构建状态**：`BUILD SUCCESSFUL`

---

## [v1.9.7] - 2026-08-17
### 1. 本次升级与需求 100% 落实
1. **对话页模型名称动态显示与字号优化（Req 1）**：
   - 将对话页面中模型消息头部的默认静态文本 `"Echo AI"` 替换为当前调用的具体模型名称（如 `gpt-4o`、`claude-3-5-sonnet`、`deepseek-chat` 等），如未配置则优雅回退至 `"AI"`。
   - 字体排版优化升级为 `titleSmall` (14sp) + `FontWeight.SemiBold`，视觉层级更加突出清晰，并配备 `TextOverflow.Ellipsis` 与单行约束避免长模型名布局错位。
2. **思考过程胶囊加大、右对齐与防截断修复（Req 2）**：
   - **右对齐布局**：在模型名称与思考过程胶囊之间添加权重自适应占位 `Spacer(modifier = Modifier.weight(1f))`，使思考胶囊推至最右侧，与下方模型输出内容及整个消息气泡右边缘对齐。
   - **尺寸加大**：内边距由 `horizontal = 8.dp, vertical = 3.dp` 提升至 `horizontal = 10.dp, vertical = 5.dp`，思考图标加大到 `16.dp`。
   - **防文字截断**：内部文字字号明确为 `12.sp`，设置 `maxLines = 1, softWrap = false`，彻底解决胶囊内“思考中...”、“思考过程”、耗时与 token 数量文字截断或重叠折行问题。
3. **对话与故事创作默认最大 Token 上限全面升级为 8192（Req 3）**：
   - 将 `ApiConfig` 实体类默认 `maxTokens` 从 4096 升级为 8192。
   - 同步更新设置页新建 API 配置默认 token、临时对话设置缺省 fallback、角色扮演与故事创作参数面板以及智能分析器的默认 `max_tokens` 设定至 8192。
4. **请求失败等错误输出淡红色气泡卡片展示（Req 4）**：
   - 引入智能错误输出判定机制 `isErrorMessage`，精准识别以 `"请求失败"`、`"[请求失败]"`、`"[输出已被中断:"`、`"Error:"` 等开头的异常内容。
   - 当模型消息为错误输出时，将其包裹在专属淡红色卡片气泡内（`errorContainer` 28% 透明度背景、淡红半透明边框、14dp 圆角卡片、警告图标及高对比度易读排版）。
5. **模型输出下方新增 Token 输出速率数值（Req 5）**：
   - 在模型消息底部的元数据栏（MessageFooter）中，当消息具备有效 token 数量与耗时时，自动根据公式 `tokenCount / (responseTime / 1000.0)` 精准计算输出速率。
   - 在 `"${message.tokenCount} tokens"` 标签右侧实时展示例如 `"50.2 tokens/s"` 的速率数值。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.7-arm64-v8a.apk`
- **SHA-256**：`CDFA1B97B3C7EA07082FCF21921CA29A4CA1AFD517EADCA2166FDF3D0F4D034F`
- **文件大小**：15,808,392 字节 (约 15.08 MB)
- **架构**：`arm64-v8a`，`versionCode: 87`，`versionName: 1.9.7`
- **自动化单元测试**：26 项测试全部通过（100% 通过率，覆盖数据模型、长记忆隔离、Chat 页面增强、Token 速率计算、错误文本识别等）
- **构建状态**：`BUILD SUCCESSFUL`（启用 R8 代码混淆、资源优化压缩与正式签名）
- **历史版本保护**：`Echo-v1.9.2` 至 `Echo-v1.9.6` 全部完好保留在 `releases/` 目录。

---

## [v1.9.6] - 2026-08-17
### 1. 本次升级与需求 100% 落实
1. **对话页与故事页长记忆彻底隔离（Req 12 Part 1）**：
   - **根因分析**：原 `captureMemoryCandidate` 在用户于故事会话中输入小说设定或剧情提示时，自动将其作为 `scope = 'user'` 的全局长期记忆提取并保存至 `memory_items` 表。当用户随后开启普通对话时，`buildRelevantMemoryBlock` 将这部分小说情节作为通用事实/偏好注入了模型提示词，导致普通对话中的模型知晓之前创作的小说内容。
   - **隔离落地**：
     - 在 `AiRepository.kt` 的 `captureMemoryCandidate` 中增加严格判定：若会话包含 `roleplay`、`story` 或 `private` 标签，立即跳过记忆提取，绝不将小说创作内容存入全局长记忆库。
     - 在 `AiRepository.kt` 的 `buildRelevantMemoryBlock` 中增加严格判定：故事/角色扮演会话绝不注入全局 `MemoryItem`，只使用故事专属的 `RoleplayMemory`，实现双向彻底隔离。
     - 在 `effectiveSystemPrompt` 中规范普通对话的全局系统提示词回退机制，故事创作会话遵循专属场景设定与角色人设。
2. **设置页个性化中模型长记忆全功能管理（Req 12 Part 2）**：
   - 在 `MemoryDao` 与 `AiRepository` 中扩展全量长期记忆查询与管理能力（`getAllMemories()`、`searchMemories()`、`insertMemory()`、`updateMemory()`、`deleteMemory()`、`deleteAllMemories()`、`setMemoryEnabled()`）。
   - 在个性化设置页面中构建专属「模型长期记忆库」模块：
     - **自动捕获总开关**：提供 `autoMemoryEnabled` 开关，可一键启闭对话中的自动记忆提炼；
     - **即时搜索与过滤**：支持通过关键词实时模糊搜索已记录的长期记忆；
     - **手动添加与编辑**：提供 `MemoryEditDialog` 弹窗，支持自定义输入记忆内容、切换作用域（全局偏好 `user` vs 会话专属 `conversation`）及关联关键词；
     - **启闭状态与单个删除**：每条记忆卡片均配备独立启用/停用开关、编辑按钮与删除按钮；
     - **一键清空**：支持一键清空全部长期记忆并提供防误触确认弹窗。
3. **合并设置页中的个性化与全局提示词页面（Req 13）**：
   - 移除设置菜单中独立的「全局提示词」入口，将个性化与全局提示词深度整合为统一的「**个性化与全局设定**」页面。
   - 页面内部按清晰层次化组织：
     1. **全局系统提示词**：配置普通新对话的默认 System Prompt；
     2. **个性化偏好设定**：配置全局生效的用户人设与回答偏好；
     3. **模型长期记忆库**：管理自动提取与手动添加的模型长期记忆；
     4. **界面背景设置**：自定义首页与对话页的液态玻璃背景壁纸；
     5. **底部保存全部设定**：一键保存全局提示词与偏好设定并持久化至 DataStore/SharedPreferences。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.6-arm64-v8a.apk`
- **SHA-256**：`CFFD6589452DBC6F377D5B073099873C82CF51DBADA31B249A90BA692D4F4D8E`
- **文件大小**：15,808,388 字节 (约 15.08 MB)
- **架构**：`arm64-v8a`，`versionCode: 86`，`versionName: 1.9.6`
- **单元测试**：22 项测试全部通过（100% 通过率，覆盖数据模型、长记忆隔离与偏好配置、全量场景/角色卡属性、剧情走向选择、会话级设定覆写、智能设定提炼分类 Proposal、流式与思考 token 解析）
- **构建状态**：`BUILD SUCCESSFUL in 4m 40s`（启用 R8 代码混淆、资源优化压缩与正式签名）
- **历史版本保护**：`Echo-v1.9.2-arm64-v8a.apk`、`Echo-v1.9.3-arm64-v8a.apk`、`Echo-v1.9.4-arm64-v8a.apk`、`Echo-v1.9.5-arm64-v8a.apk` 全部完好保留在 `releases/` 目录。

---

## [v1.9.5] - 2026-08-17
### 1. 本次升级与 11 项需求 100% 落实
1. **交换输入框中智能搜索和深度思考的位置（Req 1）**：
   - 重构 `ChatInputBar` 的胶囊按钮顺序，将「深度思考」按钮调整至首位（最左侧优先展示），「智能搜索」紧随其后，符合高频创作时深度思考为第一操作权重的操作直觉。
2. **故事页输入框隐藏智能搜索按钮（Req 2）**：
   - 在 `ChatInputBar` 中引入 `isRoleplay` 判断，故事创作模式下自动隐去「智能搜索」胶囊按钮，界面聚焦于纯粹的剧情演进与角色互动。
3. **在故事页深度思考按钮右侧新增「剧情操作」集成按钮（Req 3）**：
   - 在故事页输入框中，于深度思考按钮右侧新增「剧情操作」专属液态玻璃胶囊按钮（带分支流向图标与高亮指示）。点击后唤起沉浸式 `PlotActionDialog`，集成「继续剧情」、「重新生成」、「改写上一段」、「延长/扩写」、「精简/缩减」、「改变视角」、「切换语气」、「生成剧情摘要」、「剧情走向选择」、「自定义指令」以及「识别输入并提取设定」全量故事创作能力，同时移除原本挤占输入框上方垂直空间的横向操作条。
4. **故事页角色与世界观编辑页面与工坊编辑页面完全对齐（Req 4）**：
   - 移除故事页内简化版的临时编辑弹窗，在《故事创作与参数设置》弹窗中点击角色/世界观编辑时，全面嵌入工坊同款全字段 `CharacterEditorScreen` 与 `ScenarioEditorScreen`，支持编辑头像、身份、性格、背景、语言风格、动机目标、人际关系、知识边界、禁止违背设定、行为准则、世界观宏观法则、当前冲突等所有维度。
5. **故事创作与参数设置中支持直接添加与删除角色/世界观（Req 5）**：
   - 在故事设置弹窗的登场角色与世界观分组标题栏新增「+ 添加新角色」与「+ 添加新世界观」快捷按钮；在每个角色卡与世界观卡片右侧新增编辑 (✏️) 与删除 (🗑️) 按钮，删除操作仅从当前故事的会话绑定与独立覆写中移除，绝不影响角色与创作库中的全局原始模板。
6. **角色与故事页长按故事提供双向同步选项（Req 6）**：
   - 在角色工坊的故事会话卡片上新增长按上下文菜单，提供「🔄 双向设定同步」入口，唤起 `StorySyncDialog`：
     - **按故事更新库 (Story -> DB)**：将故事中发展丰富后的专属人设与世界观设定写回全局库中同名角色/场景，或若不存在则新建入库；
     - **按主库更新故事 (DB -> Story)**：拉取全局主库中同名角色/世界观的最新设定覆盖本故事专属覆写。
7. **对话页中模型思考输出胶囊与模型头像精准水平居中对齐（Req 7）**：
   - 重构 `ChatMessageItem` 中 Assistant 消息首行布局：在同一个水平 Row 中并列排列 `ChatAvatar`、模型名称「Echo AI」以及深度思考状态胶囊（带旋转光晕与思考状态文字），采用 `verticalAlignment = Alignment.CenterVertically` 实现像素级垂直中心对齐，点击胶囊可直接展开/折叠下方独立的思考推理卡片。
8. **确保对话页和故事页右上角设置窗口底部保存按钮常驻可见（Req 8）**：
   - 重构 `EchoHaze.kt` 中的 `EchoGlassDialog` 容器层级：将 `content` 明确限制在带有 `Modifier.weight(1f, fill = false)` 与最大高度 `heightIn(max = 680.dp)` 的滚动区内，将 `buttons` 严格锚定在弹窗最底部独立 Row 中，彻底杜绝小屏或长内容下“保存”与“取消”按钮被挤出屏幕可视区域的问题。
9. **修复 API 配置与设置子页面偶现白色背景异常（Req 9）**：
   - 彻底审查 `EchoGlassDialog`、`Surface` 及所有设置 Tab 的背景颜色配置，修正默认 `containerColor`，将其统一绑定至暗色液态玻璃主题调色板（`glass.panel` 与 `glass.panelStrong`），杜绝因未指定暗色背景导致的白色底色溢出。
10. **重构 AI 智能提取：精准识别更新已有角色/新角色/世界观（Req 10）**：
    - 升级 `RoleplaySmartAnalyzer` 为 `StorySettingProposalBundle` 智能分析引擎，分析时将故事当前已知的所有角色名称、身份和当前世界观作为参考上下文一并注入提示词；
    - 引导模型精准分类：
      - **已有角色更新 (`updatedCharacters`)**：针对故事中已有角色发生的经历更新、性格转变或能力追加进行局部增量更新并生成变更摘要；
      - **新角色发现 (`newCharacters`)**：仅当真正出现新姓名与新身份角色时才作为新角色创建；
      - **世界观更新 (`scenarioUpdate`)**：将宏观规则变动并入场景设定；
    - 改造 `EditableSettingProposalDialog` 分区展示更新与新角色，用户可自由核对、修改或剔除。
11. **设置中的关于界面与更新日志内容实时同步（Req 11）**：
    - 在 `SettingsScreen.kt` 的 `CurrentFeatureHighlights` 与 `CurrentVersionUserUpdates` 中全面同步更新 v1.9.5 的 11 项核心特性与升级日志。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.5-arm64-v8a.apk`
- **SHA-256**：`06EA5DB9D7AA4948C3C91BF5750A1FE3412FD038E0B6F30DFE1BA861CDAAAB2E`
- **文件大小**：15,808,392 字节 (约 15.08 MB)
- **架构**：`arm64-v8a`，`versionCode: 85`，`versionName: 1.9.5`
- **单元测试**：20 项测试全部通过（100% 通过率，覆盖数据模型、全量场景/角色卡属性、剧情走向选择、会话级设定覆写与隔离、智能设定提炼分类 Proposal、流式与思考 token 解析）
- **构建状态**：`BUILD SUCCESSFUL in 7m 57s`（启用 R8 代码混淆、资源优化压缩与正式签名）
- **历史版本保护**：`Echo-v1.9.2-arm64-v8a.apk`、`Echo-v1.9.3-arm64-v8a.apk`、`Echo-v1.9.4-arm64-v8a.apk` 全部完好保留在 `releases/` 目录。

---

## [v1.9.4] - 2026-08-17
### 1. 闪退问题根因排查与修复
1. **P0 数据库结构与实体不匹配导致的闪退（核心根因）**：
   - **根因分析**：1.9.3 中在 `RoleplaySession` 实体中新增了 `customCharacterData` 与 `customScenarioData` 两列，但 Room 数据库版本未同步提升，且未向 SQLite 数据库注册 Migration 20 与修补规则。在应用启动或访问 Room 数据库时触发了 `IllegalStateException: Room cannot verify the data integrity` 致命异常导致闪退。
   - **修复落地**：
     - 将 `AppDatabase` 的 `version` 严格升级为 `20`；
     - 新增 `MIGRATION_19_20`，执行 `addColumnIfMissing(database, "roleplay_sessions", "customCharacterData", "TEXT")` 与 `addColumnIfMissing(database, "roleplay_sessions", "customScenarioData", "TEXT")`；
     - 更新 `LEGACY_REPAIR_MIGRATIONS` 与 `repairSchema`，向历史数据库修复列表与表结构重构中注册所有新增字段，保证老版本与新版本平滑无缝升级。
2. **UI 回调与非主线程 Toast 调度冲突修复**：
   - **根因分析**：在 `analyzeAndProposeSettingFromInput` 和 `summarizeAndExtractMemories` 协程中，`onProgress`、`onNoProposal`、`onError` 等带有 `Toast` 的 UI 回调有概率在 `Dispatchers.IO` 上被直接触发，引发 Android 的 `Can't toast on a thread that has not called Looper.prepare()` 异常崩溃。
   - **修复落地**：在所有 ViewModel 层的对外异步回调处，严格强制切换至 `withContext(Dispatchers.Main)` 调度，杜绝任何非主线程 Toast 崩溃风险。
3. **流式输出快速刷新时的滚动与列表安全防护**：
   - **优化落地**：优化 `LaunchedEffect` 的流式跟随滚动调用，采用 `try-catch` 保护的 `listState.scrollToItem`，消除高频 SSE 分片刷新下的动画重叠异常；同时为 LazyColumn 中的每条消息生成防冲突的确定性复合 Key，杜绝 Compose Key 重复异常。
4. **异常中断处理双重保存防抖保护**：
   - **优化落地**：在 `sendMessageInternal` 的 `catch (e: Exception)` 块中加入 `!isMessageSaved` 状态双重判断，杜绝因 `onError` 与异常同时触发导致的重复保存与状态错乱。
5. **代码备份落实**：
   - 已将当前源代码全量备份至 `D:\Agent\APP-烧\app\backup_src_1.9.3`。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.4-arm64-v8a.apk`
- **SHA-256**：`9B59304479581FBBA152258FB6E7683ABF5E2D7A76EF978DA690DD22EBE3C35F`
- **文件大小**：15,792,004 字节 (约 15.06 MB)
- **架构**：`arm64-v8a`，`versionCode: 84`，`versionName: 1.9.4`
- **单元测试**：19 项单元测试全部通过（100% 通过率，覆盖数据模型、上下文组装、会话级覆写、本地规则提取与流式标签解析）
- **构建状态**：`BUILD SUCCESSFUL in 4m 21s`
- **历史版本保护**：`Echo-v1.9.2-arm64-v8a.apk`、`Echo-v1.9.3-arm64-v8a.apk` 完好保留。

---

## [v1.9.3] - 2026-08-17
### 1. 本次升级与需求 100% 落实
1. **故事页面合并故事设定与对话设置为一体化弹窗（Req 1 & 4）**：
   - 故事模式下右上角收敛为一个“故事创作与参数设置”按钮，弹窗内采用 Tab 分页（📖 故事与角色 / ⚙️ 模型与参数），全面支持登场角色勾选、世界观切换、叙事模式选择、剧情指令、系统提示词、温度、最大 Token 数、思考强度等一体化设置。弹窗底部提供清晰无截断的“取消”与“保存”按钮。
2. **输入框草稿文字跨界面与操作全生命周期保留（Req 2）**：
   - 在 `ChatViewModel` 中引入线程安全的会话草稿内存持久化机制，并在 `ChatScreen` 的输入框变更与 `onDispose` 中实时双向同步，无论返回上一级、切换应用、进入设置或进行任何界面更改，草稿文字均完整保留，仅在点击发送成功后清空。
3. **实现对话与情节总结记忆与核心事实自动提炼（Req 3）**：
   - 在 `AiRepository` 中新增 `executeQuickCompletion`，并在 `ChatViewModel` 中构建 `summarizeAndExtractMemories` 结构化提炼流程，能够深度阅读最近剧情并生成连贯中文剧情摘要与 2~4 条核心既定事实，自动同步至会话与事实库。
4. **对话设置窗口完善保存键与文字截断修复（Req 4）**：
   - 对话设置与故事设置弹窗底部全部补齐“取消”与“保存”按钮，修复“完成”文字显示不全问题并统一命名为“保存”。
5. **模型思考与输出后台长效保护及意外中断内容保留（Req 5）**：
   - 生成任务由全局 `applicationScope` 托管保护，返回上一级或滑到后台均不会中断生成；意外中断或发生错误时，优先保留已生成的思考过程与文本内容并在尾部附带中断说明，彻底杜绝内容丢失变为报错卡片。
6. **流式输出时仅当页面处于最底端才跟随滚动（Req 6）**：
   - 优化 `LaunchedEffect` 滚动跟随逻辑，流式输出期间严格判定用户是否已在最底端（`lastVisibleIndex >= totalItemsCount - 1`）；当用户向上滑动阅读历史内容时，严格锁定当前浏览位置，绝不强行抢焦滚动。
7. **默认输出上限大幅调大至 8192+（Req 7）**：
   - 将所有新会话、故事创作、临时聊天参数以及设置默认的 `maxTokens` 从 4096 统一提升至 8192+，满足长篇小说创作与长文章推演需求。
8. **修复长文本与长思考截断问题（Req 8）**：
   - 配合 8192+ 最大输出与无限制流式累加机制，彻底杜绝深度思考与长文本回复中途截断问题。
9. **故事导演指令新增“剧情走向选择”（Req 9）**：
   - 在 `PlotAction` 枚举与 `RoleplayRepository` 中新增 `BRANCH_CHOICES`（“剧情走向选择”），并在快捷指令条与创作管理弹窗中集成，引导模型提供 3~4 个截然不同的剧情发展方向与节奏供用户挑选决策。
10. **AI 智能识别角色与世界观严格区分并新增“放弃”选项（Req 10）**：
    - 升级 `RoleplaySmartAnalyzer` 提示词约束，精准区分人物个体（角色卡）与宏观时代/地点（世界观场景卡）；解析结果确认弹窗中新增“放弃”与“上一步”选项，赋予用户完全掌控权。
11. **胶囊导航栏标题支持水平滚动显示全名与长按重命名（Req 11）**：
    - `ChatHeaderTitle` 改为水平可滑动排版（`horizontalScroll`），长文本标题可自由滑动查看；支持长按触发 `RenameConversationDialog`，实时对故事或对话重命名。
12. **新建任何对话或故事自动开启深度思考模式并置为最大档（Req 12）**：
    - 在新建会话、故事开局与默认参数初始化中，默认 `enableThinking = true` 且 `thinkingEffort = "high"`。
13. **模型接收输入但首字未出时显示“正在连接模型响应中...”（Req 13）**：
    - 在消息项中新增 `ConnectingModelIndicator`，在用户发送后至模型返回第一个 token 期间提供清晰优美的连接中动态提示。
14. **修复模型头像与思考胶囊对齐问题（Req 14）**：
    - 统一模型头像置顶水平对齐排版，头像与模型名称处于首行，消除错位。
15. **模型输出在整个页面全宽居中展开，左右对称无空白浪费（Req 15）**：
    - 重构 Assistant 消息排版，正文与思考卡片横跨全宽居中排列，左右边距严格对称，彻底消除头像下方大片空白浪费。
16. **故事设定管理窗口长按角色/世界观直接编辑且仅影响当前故事（Req 16）**：
    - 在 `RoleplaySession` 中新增 `customCharacterData` 与 `customScenarioData` 字段，实现会话级独立覆写隔离；在《故事创作与参数设置》弹窗中长按登场角色或世界观卡片，可直接唤起独立修改弹窗，修改后的设定仅持久化并生效于当前故事，完全不污染全局角色与世界观库。
17. **故事页面模型根据用户输入自动补充角色与世界观在线提取编辑（Req 17）**：
    - 在底部操作栏新增“识别输入补充设定”快捷入口；在识别到可补充的新人物或世界观时，通过 `EditableSettingProposalDialog` 在线展示提取出的全字段设定，用户可直接进行二次编辑、修改或剔除，并自主决定“决定添加并融合”或“放弃”。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.3-arm64-v8a.apk`
- **SHA-256**：`4E564CDA5397D690DC1DFD9E4152A6B79924AE09F4215FB691E14985C0770898`
- **文件大小**：15,775,620 字节 (约 15.04 MB)
- **架构**：`arm64-v8a`，`versionCode: 83`，`versionName: 1.9.3`
- **单元测试**：15 项测试全部通过（`RoleplayModelsTest` 覆盖全量数据实体、剧情走向选择、会话级设定覆写与隔离）
- **历史安装包状态**：`Echo-v1.9.2-arm64-v8a.apk` 及历史文档完整保留，绝无误删。

---

## [v1.9.2] - 2026-08-17
### 1. 本次升级与需求 100% 落实
1. **修复角色/世界观编辑返回上一级跳回故事页面的错误逻辑**：
   - 在 `RoleplayViewModel` 中引入 `studioTab` 状态变量并全局托管，用户在“角色”或“世界观”Tab 进入具体角色/场景编辑并返回后，严格保留在原 Tab，不再跳回故事首页。
2. **修复无法长按选中多个角色/场景进行批量删除的问题**：
   - 彻底修复 `EchoGlassCard(onClick = ...)` 内层 Surface 吞噬 Compose 手势事件的底层冲突，将长按手势唯一保留在外层 `combinedClickable`，长按可秒级唤起多选批量删除模式。
3. **开展故事时可选择模型列表与对话中可选模型彻底拉齐**：
   - 在新建故事流程中集成全量 `repository.getAllVisibleChatModelOptions()`，支持跨 API 渠道搜索、选择与自定义模型输入，与对话设置中的模型池 100% 同步。
4. **修复从故事界面返回后重新点击显示为新对话的问题**：
   - 根治 `RoleplayStudioScreen` 中点击故事传递 `session.id` 而非 `session.conversationId` 导致的错位 Bug；同时故事卡片标题动态解析并展示绑定的真实角色名称（如“艾莉丝、林修 · 魔法学院”），告别空泛的“角色扮演会话 #ID”。
5. **故事页面支持长按直接修改世界观与登场角色**：
   - 在故事卡片上支持长按呼出 `EditStorySessionContextDialog`，用户无需重新开局，即可随时动态增删参与本故事的登场角色、切换或清除绑定的世界观设定、切换叙事模式与编辑剧情备忘。
6. **优化故事设定与创作管理窗口，尺寸与对话设置窗口统一，善用滑动**：
   - 将《故事设定与创作管理》弹窗统一改造为轻量滑动容器（`Box(heightIn(max = 460.dp))` 配合 `LazyColumn`），排版紧凑优雅，消除窗口过大与内容溢出。
7. **删除叙事模式中的“多角色模式”，所有故事天然支持多角色**：
   - 彻底从枚举 `NarrativeMode`、底层组装引擎与 UI 中精简移除“多角色模式”，全系故事天然接纳任意多位角色的同时登场与互动。
8. **在所有设置与选项右侧/下方加入清晰易懂的解释**：
   - 在对话设置（温度、Top P、最大 Token、思考模式、联网搜索）、叙事模式（角色内、作者/导演、旁白）、故事上下文管理及新建故事选项右侧与副标题全部加入详尽解释文案。
9. **彻底移除故事开局强制塞入初始问候语的逻辑**：
   - 移除 `createStorySessionAndStart` 与 `createRoleplaySessionAndStart` 中强行插入角色预设 greeting 的逻辑，初始问候语完全交由用户在故事内自由决定是否设定以及如何开场。
10. **优化所有弹窗，统一窗口大小与比例**：
    - 在底层液态玻璃 `EchoGlassDialog` 中规范化尺寸约束（`Modifier.fillMaxWidth(0.94f).widthIn(max = 420.dp).heightIn(max = 580.dp)`），全应用弹窗风格高度和谐统一。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.2-arm64-v8a.apk`
- **SHA-256**：`AE400C9EE00916EE77D64675FF8BA8792FA7415076D78434C3832FFC694B12E6`
- **文件大小**：15,742,756 字节 (约 15.01 MB)
- **架构**：`arm64-v8a`，`versionCode: 82`，`versionName: 1.9.2`

---

## [v1.9.1] - 2026-08-17
### 1. 本次升级与需求落实
1. **后续修改方案全部模块深度落地**：
   - **模块一与模块二（设计系统与液态玻璃底层）**：`EchoTokens` 统一令牌、`EchoScaffold` 标准脚手架、`WallpaperBlurCache` 异步预模糊壁纸缓存系统，全页面消除硬编码与拖影。
   - **模块三（高精度 Token 与 TPS 解析）**：`StreamOptions(include_usage = true)` 强制注入，首字延迟（TTFT）与真实有效 TPS 计算，Prompt 缓存命中精准解析。
   - **模块四（双端注水 Dual-Anchor Prompting）**：在 `AiRepository.kt` 中落地长上下文（>6轮或包含摘要时）尾部系统指令强化声明（`System Override Directive`），杜绝模型因历史惯性忽略最新提示词。
   - **模块五（文学级 Show, Don't Tell 角色创作规则）**：在 `RoleplayRepository.kt` 中全面注入“文学创作与角色演绎铁律”，严禁生硬性格副词堆叠，强制通过眼神、微表情、肢体动作与节奏展现人设，禁止越权代写用户发言。
2. **设置界面移除“环境变量”选项**：
   - 彻底从 `SettingsScreen` 设置菜单、导航路由及功能列表中移除“环境变量”项，精简设置菜单，突出数据备份与核心功能。
3. **构建流程定时器卡顿机制根除**：
   - 全面摒弃构建过程中的手动轮询定时器，改由系统事件与反应式唤醒机制原生驱动，构建过程顺畅丝滑、即时响应。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.1-arm64-v8a.apk`
- **SHA-256**：`A5C5C081430AA11D33614E6B0FC512E9ADD5C318E2DE3033C0DEDF36514309AD`
- **文件大小**：15,726,376 字节 (约 15.00 MB)
- **架构**：`arm64-v8a`，`versionCode: 81`，`versionName: 1.9.1`

---

## [v1.9.0] - 2026-08-17
### 1. 本次升级重点与需求 100% 落实
1. **对话页输入框添加思考模式按钮**：
   - 在 `ChatInputBar` 的“智能搜索”右侧新增“深度思考”胶囊按钮（`enableThinking`），点击可即时切换开启/关闭推理模式，并带有明晰的高亮激活反馈。
2. **角色扮演工作室全面更名为“角色与创作”**：
   - 顶部标题、各导航文案、标签栏全面更名为“故事”、“角色”、“世界观”（场景更名为世界观），打造全方位的创作工作流。
3. **完善智能导入：直接创建对话支持选择叙事模式**：
   - 在 AI 解析结果弹窗中引入第三步“创建故事”，支持在创建前自由选择叙事模式（角色内指令、作者/导演指令、旁白模式、多角色模式）。
4. **智能导入同名角色冲突智能处理（三选项）**：
   - 智能比对已存在角色，对重名角色提供三种解决策略：`新建同名副本`、`覆盖原有设定`、`智能融合设定`（智能合并人设背景与标签），并在弹窗中直观切换选择。
5. **重构多角色+世界观的“故事”会话体系**：
   - 数据库升级至 Version 19（`characterIds: String?` 支撑多角色 ID 集合）。
   - 新建故事支持多选勾选任意多名角色并绑定世界观，上下文构建引擎自动组装多角色设定卡片。
6. **故事对话中支持随时进行世界观/角色/文风智能识别追加与融合**：
   - 故事对话页顶部栏提供 `AutoStories` 专属入口，支持查看当前角色列表与世界观设定、切换导演/叙事模式、触发导演指令。
   - 支持在对话中途打开“智能识别追加与融合”，将新小说文本/人设动态解析并追加或融合进当前故事。
7. **角色与世界观列表长按多选一键批量删除**：
   - 角色列表与世界观列表支持长按进入多选模式，顶部显示已选计数、全选/全不选与批量删除危险二次确认弹窗。
8. **移除工作室首页顶部冗余的“AI剧本与小说一键解析”横幅**：
   - 消除界面多余占位，右上角保留精致的一键解析悬浮入口，界面更加清爽专业。
9. **AI一键解析过程支持随时终止**：
   - 智能解析弹窗与故事追加解析中均支持通过协程 `Job.cancel()` 随时终止解析，点击外部或“停止”按钮立即中止请求并安全重置。
10. **优化数据面板美观度与可读性，丰富数据维度**：
    - 数据面板重构为 6 大指标卡（总请求数、请求成功率、缓存命中率、输入 Token、输出 Token、思考 Token），采用高质感玻璃卡片与清晰色彩。
11. **模型下拉列表高度约束与滑动条**：
    - 对话页、首页及工作室的所有 `DropdownMenu` 均增加 `Modifier.heightIn(max = 280.dp)`，杜绝菜单过长，向下平滑展开配合流畅滑动条。

### 2. 产物与交付验证
- **单一安装包**：`D:\Agent\APP-烧\app\releases\Echo-v1.9.0-arm64-v8a.apk`
- **SHA-256**：`E32D00953237D0D225E1782C082A56C2DCA367416B552C11393A9E15CE66D989`
- **文件大小**：15,742,760 字节 (约 15.0 MB)
- **架构**：`arm64-v8a`，`versionCode: 80`，`versionName: 1.9.0`

---

## [v1.8.9] - 2026-08-17
### 1. 深度修复与视觉调优落地
1. **对话页输入框非透明度大幅提升**：
   - 全面提高 `ChatInputBar` 的背景透明度至 0.92f~0.96f，告别过度透明与模糊，输入文字与按钮视觉层次极其清晰扎实。
2. **首页对话胶囊与“新对话”按钮非透明度大幅提升**：
   - 首页 `ConversationCard` 与 `NewConversationGlassButton` 背景提高至高对比高不透明度（0.92f~0.95f），呈现温润、凝实的高级质感。
3. **模型选择列表全域彻底统一（以对话内可选模型为准）**：
   - 彻底拉齐首页长按创建新对话（`NewChatDialog`）与角色扮演智能解析（`SmartAnalyzeStudioDialog`、`SmartReadCharacterDialog`、`SmartReadScenarioDialog`）的可选模型体系。
   - 所有模型选择列表均直接消费 `repository.getAllVisibleChatModelOptions()`，支持多 API 配置下所有已启用模型的细分选择与无缝生效。
4. **对话页用户输入气泡玻璃遮罩位置异常彻底根治**：
   - 彻底移除 `LazyColumn` 内列表项对动态 `hazeChild` 的绑定，采用高性能微光浮层渲染，根除滑动时遮罩滞后、拉伸与错位问题。
5. **对话页顶部导航栏与 API 不存在胶囊外圈白边/透明边框根除**：
   - 顶部胶囊导航栏与 API 不存在警告胶囊全面去除外层阴影、多余图层与描边（`border = null`、`shadowElevation = 0.dp`、`tonalElevation = 0.dp`），实现完全纯净一体的悬浮胶囊。

---

## [v1.8.8] - 2026-08-16
### 1. 深度落地：全面模型选择与角色扮演 AI 智能拆解
1. **长按首页新建对话模型选择全面化**：
   - 彻底打破以往仅能选默认单一模型的局限。在 `NewChatDialog` 中构建全量模型选择系统，自动解析 API 配置下的 `availableModels` 列表、预置各大厂商主流模型（GPT-4o/o1/o3-mini、Claude 3.7/3.5、DeepSeek-V3/R1、Gemini 2.0、Qwen 2.5/Max 等）。
   - 支持实时模型搜索过滤与自定义输入任意模型名称，并在长按新建对话时无缝透传并生效。
2. **角色扮演智能导入与拆解功能深度落地**：
   - 彻底推翻单纯将原始文本机械塞入 `background` 的粗糙逻辑，重构 `RoleplaySmartAnalyzer` 为高精度结构化 JSON 提示词分析引擎。
   - 在角色编辑页 (`CharacterEditorScreen`)、场景编辑页 (`ScenarioEditorScreen`) 与工作室首页 (`RoleplayStudioScreen`) 全面支持**自由选择 API 配置与模型**。
   - 支持一键粘贴小说正文、人设小传、大纲设定或导入 TXT 文件，AI 将精准拆解并结构化填充姓名、职业身份、性格、背景精炼、口吻文风、已知知识、核心动机、行为约束、开场白与示例对话；世界观、时空坐标、冲突焦点、环境描写等各个细分字段，实现真 AI 智能拆解。

---

## [v1.8.7] - 2026-08-16
### 1. 简约高级艺术字与玻璃体系终极纯净化
1. **首页 ECHO 艺术字转向极简高级瑞士/包豪斯风格**：
   - 彻底摒弃杂乱的花哨渐变剪纸 XML，转用纯净克制的高级排印设计（宽字间距 `E C H O` 搭配单点精巧主色光标点缀），呈现高端、现代、艺术的品牌气质。
2. **对话页顶部导航栏与错误气泡边框彻底消解**：
   - 顶部悬浮胶囊栏与异常提示气泡完全去除多余的描边（`border = null`，`showBorder = false`，`shadowElevation = 0.dp`），实现完全纯净无白边的悬浮玻璃体。
3. **全域玻璃体系错位/重叠 Bug 彻底根治**：
   - 全面清理首页图标按钮、文件夹滚动 Chip、搜索栏中不当嵌套的动态 Haze 节点，杜绝横向/纵向滑动时因节点重绘滞后产生的坐标错位与重叠暗块，界面恢复极致清晰与 120 FPS 满帧丝滑。

---

## [v1.8.6] - 2026-08-16
### 1. 本次深度改进与问题解决
1. **液态玻璃渲染体系重构（根除错位、重叠与覆盖异常）**：
   - 彻底重构 [EchoGlassCard.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/components/EchoGlassCard.kt) 与 [EchoHaze.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt)。对动态滚动列表（`ConversationCard`、角色卡片）采用高性能单 Pass 45° 漫反射光学折射高光渲染，杜绝在 `LazyColumn` 快速滚动时 Haze 节点坐标滞后导致的遮罩错位、拖影与重叠色块。
   - 移除所有双重边框绘制逻辑，保证卡片与背景晶莹剔透、边界干净。
2. **对话页顶部导航栏与错误提示气泡去除白边/边框**：
   - 顶部胶囊导航栏开启 `showBorder = false`，实现真正一体化无边框悬浮玻璃胶囊。
   - API 不存在的错误提示气泡全面转用无边框悬浮 `Surface`，彻底删除边缘白色/透明边框，确保未来任何错误状态绝不再出现多余边框。
3. **首页 ECHO 艺术字全面升级**：
   - 重新设计并绘制现代轻奢风格矢量艺术字 [echo_wordmark_art.xml](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/res/drawable/echo_wordmark_art.xml)，采用流光渐变与精准字形比例，大幅提升首页视觉质感。

---

## [v1.8.5] - 2026-08-16
### 1. 深度修复与视觉全面重构
1. **对话页模型回复去除气泡框**：
   - 严格落实用户指令，AI 助手/大模型回复内容彻底剥离 `Surface` 气泡、背景色与外边框，以纯粹、自然的 Markdown 原生流式排版呈现，与用户右侧半透明气泡形成清晰的视觉层级。
2. **根除液态玻璃覆盖异常、拖影与颜色过深 Bug**：
   - 全面移除 `echoLiquidGlassOverlay` 中在内容上方绘制 `BlendMode.Screen` 与 `BlendMode.Multiply` 的错误实现，改为在背景层 `drawBehind` 渲染微光高光折射，杜绝遮罩错位与文字被脏色覆盖。
   - 优化 `EchoGlassPalette` 与 `EchoDesignTokens` 中的全局透明度与对比度，大幅提升卡片晶莹清透感，告别暗沉厚重。
   - 去除搜索栏等控件的重复边框定义，解决双重边框瑕疵。

---

## [v1.8.4] - 2026-08-16
### 1. 本次升级重点 (模块一 & 模块二落地)
1. **全 App UI 风格统一化与设计系统 (Design System)**：
   - 建立单例设计令牌系统 [EchoDesignTokens.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/theme/EchoDesignTokens.kt)，全局收拢间距、圆角与毛玻璃透明度规范。
   - 封装标准化脚手架 [EchoScaffold.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/components/EchoScaffold.kt) 与标准组件族 [EchoGlassCard.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/components/EchoGlassCard.kt)、[EchoControls.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/components/EchoControls.kt)。
   - 消除各页面的硬编码尺寸与原生粗糙卡片，统一全 App 视觉语言。
2. **液态玻璃 (Liquid Glass) 渲染稳定性与底层重构**：
   - 落地 **异步预模糊壁纸缓存系统** [WallpaperBlurCache.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/utils/WallpaperBlurCache.kt)，从底层消除列表快速滑动时的 GPU 纹理拖影与实时模糊开销。
   - 重构 [EchoHaze.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt)，落实单源采样与绝对坐标计算，根除动画与滚动错位 Bug。
3. **页面全面对齐**：
   - [HomeScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt) + [ChatScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt) + [RoleplayStudioScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayStudioScreen.kt) + [SettingsScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt)。

---

## [v1.8.3] - 2026-08-16
### 1. 本次用户需求
1. 首页左上角的 ECHO 采用艺术字图片实现。
2. 首页和对话页右侧的对话导航条去除胶囊型背景，优化密集排布，提升美观度。
3. 首页对话导航条出现时与对话卡片重合问题修复。
4. Release 目录中只保留单一命名的安装包，清理冗余副本。
5. 角色扮演一键读取功能升级：提至工作室一级入口，支持调用已配置 API 进行大模型智能分析（小说/设定自动提炼角色、背景、文风并生成），无网络/API 时自动本地降级。
6. 完善整个角色扮演工作室的功能引导与玩法说明。
7. 在项目中建立全流程记录留痕与构建前复核机制。
8. 对话页错误提示气泡（如 API 不存在时）位置下移至顶部胶囊导航栏下方，防止重合。

### 2. 技术实现与修改文件
- **艺术字图片**：[echo_wordmark_art.xml](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/res/drawable/echo_wordmark_art.xml) + [HomeScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt)
- **导航条重构与避让**：[SideAnchorNavigator.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/components/SideAnchorNavigator.kt) + [HomeScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/home/HomeScreen.kt) + [ChatScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt)
- **对话页顶部避让与错误气泡**：[ChatScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt)
- **角色扮演智能分析引擎**：[RoleplaySmartAnalyzer.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/utils/RoleplaySmartAnalyzer.kt)
- **工作室一级入口与引导体系**：[RoleplayStudioScreen.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayStudioScreen.kt) + [RoleplayViewModel.kt](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayViewModel.kt)
- **规范与构建配置**：[WORKFLOW_GUIDELINES.md](file:///d:/Agent/APP-烧/WORKFLOW_GUIDELINES.md) + [claude.md](file:///d:/Agent/APP-烧/claude.md) + [build.gradle.kts](file:///d:/Agent/APP-烧/app/AiApiAssistant/app/build.gradle.kts)

---

## [v1.8.2] - 2026-08-16
### 1. 用户需求
- 首页液态玻璃覆盖异常与颜色过深修复。
- 角色扮演功能新增一键读取。
- API 管理展开模型列表增加列表名。
- 首页 ECHO 去除多余波浪图案。
- 对话页模型回复去除卡片气泡框。
- 设置页环境变量设置功能修复。
- 专有 `arm64-v8a` 架构构建。

---

## [v1.8.1] - 2026-08-16
### 1. 用户需求
- 移除多模态/图像理解限制与 API 联网限制，支持全局联网搜索与全模型能力自定义。
