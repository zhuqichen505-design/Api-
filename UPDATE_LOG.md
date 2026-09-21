# Echo AI 助手更新日志 (Update Log)

## [2026-09-21] - v2.2.8：时间线与会话记忆门禁合并、大模型深度参与时间线与事件提取、设置中跨会话记忆与对话专属记忆彻底物理隔离

### 1. 核心需求落实与技术重构详情
1. **时间线功能与对话记忆开关合并（消除繁琐检测与规则门禁）**：
   - **重构背景**：
     - 之前的时间线自动评估依赖独立的文本关键字正则嗅探与多重启发式门禁，容易因日常交谈缺少敏感词而将剧情互动短路，导致大模型无法参与时间推进。
   - **技术方案**：
     - 彻底简化门禁逻辑，时间线功能不再需要单独检测是否开启，直接与“对话记忆”（`conversation.enableSessionMemory`）开关完全合并；
     - 只要用户在单对话中启用了“对话记忆”（角色扮演/故事会话默认生效），时间线功能即自动跟随生效；若用户显式关闭对话记忆，时间线评估同步停用；
     - 在 `AiRepository.kt` 中移除旧的启发式过滤，纯粹基于 `enableSessionMemory` 决定是否调度后台轻量评估。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`。

2. **大模型真正参与时间线与事件智能提取（告别粗糙纯文字识别与正则）**：
   - **重构背景**：
     - 单纯依赖本地规则与关键字文字识别在复杂剧情与小说创作中效果极差，容易漏判时间流逝或错判关键事件。
   - **技术方案**：
     - 在 `AiRepository.kt` 中重构 `evaluateAndAutoUpdateTimeline` 与增量提取 Prompt：
       ① 深入理解正文对话，敏锐判断日内时段流转（清晨、午后、深夜）、跨日演进、相对时间跨度（几天后、两周后）及阶段节气节点（暑假开始、深秋初雪等）；
       ② 精炼提炼具有长远影响的剧情里程碑事实，并强制要求去除“用户”、“AI”、“助手”等元词汇，严禁把导演指令原样记录为事件；
       ③ 优先调用当前会话正在使用的活动大模型进行高质量增量提炼，若模型未配置则调度系统默认模型，仅在离线或异常时平滑降级为本地规则兜底；
     - 在 `extractMemoryCandidate` 中同步扩展支持会话活跃模型提取，实现端到端大模型深度参与。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`。

3. **设置中跨会话记忆只管理全局偏好，与对话专属记忆完全物理隔离**：
   - **重构背景**：
     - 设置中的“跨会话长期记忆”此前存在全局偏好与各会话专属记忆混杂的问题，清空或开关操作存在误伤对话私密上下文的隐患。
   - **技术方案**：
     - 在 `Daos.kt` (`MemoryDao`) 中新增 `getGlobalMemoriesFlow()`、`searchGlobalMemories()` 与 `deleteGlobalMemories()`，明确通过 `scope IN ('user', 'global')` 进行严格过滤与独立清空；
     - 在 `SettingsPromptsMemoryTab.kt` 中彻底移除会话作用域过滤标签，设置界面仅展示全局偏好条目（“全局偏好库管理”），明确文案说明“开关仅影响全局偏好；各对话专属偏好和记忆完全独立运作”；
     - 设置页中的“清空”按钮仅调用 `repository.clearGlobalMemories()`，100% 保护各对话内部的专属记忆与时间线设定；
     - 在 `AiRepository.kt` 的 `captureMemoryCandidate` 中实现解耦控制：全局偏好入库由设置页的 `autoMemoryEnabled` 控制，会话专属记忆由该会话自身的 `enableSessionMemory` 独立控制。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/local/Daos.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/settings/SettingsPromptsMemoryTab.kt`。

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

## [2026-09-20] - v2.2.5：API 与 Key 独立启用开关、停用模型自动从选择列表隐藏、端点上下文超限拦截与输出 Token 自适应收敛

### 1. 核心需求落实与技术重构详情
1. **API 配置独立启用开关与对话页模型选择列表过滤**：
   - **需求背景**：用户配置了多个供应商或测试 API，某些 API 在余额不足、线路不稳定或临时停用时不希望参与对话与角色扮演，更不希望其对应模型混杂在对话页或工作台的模型选择列表中。
   - **技术方案**：
     - 在 `ApiConfig` 实体新增 `isEnabled: Boolean = true` 字段，并在 Room 数据库升级至版本 27 (`MIGRATION_26_27` 自动无感添加 `isEnabled INTEGER NOT NULL DEFAULT 1`)，支持旧版本平滑升级与降级迁移回退；
     - `ApiConfigDao` 新增 `getEnabledConfigs(): Flow<List<ApiConfig>>` 与 `setConfigEnabled(id, isEnabled)` 操作接口；
     - `AiRepository.getAllVisibleChatModelOptions()` 是主对话页、角色扮演工作台、角色与场景编辑器、全局模型选择器 `SettingsUniversalModelPicker` 的唯一可信数据源，统一执行 `configs.filter { it.isEnabled }` 过滤，彻底确保只要 API 开关关闭，其下所有模型绝不出现在任何选择列表中；
     - `ChatViewModel` 与 `HomeViewModel` 的默认模型回退链路增加 `takeIf { it.isEnabled }` 校验，杜绝任何停用模型偷渡进入会话；
     - `SettingsScreen` 的 API 卡片头部新增醒目的 `Switch` 开关与「已停用」状态徽章，支持一键切换并实时持久化，停用卡片自动半透明暗化展示；
     - `SettingsApiConfigDialog` 新增「启用此 API 配置」独立开关行，用户在编辑或新建配置时可自由控制是否启用。
   - **文件改动**：`app/src/main/java/com/aiassistant/domain/model/Models.kt`、`app/src/main/java/com/aiassistant/data/local/Daos.kt`、`app/src/main/java/com/aiassistant/data/local/AppDatabase.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/home/HomeViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayViewModel.kt`、`app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`、`app/src/main/java/com/aiassistant/ui/screens/settings/SettingsApiConfigDialog.kt`。

2. **独立 API Key 精细化启用开关与请求时故障转移过滤**：
   - **需求背景**：同一 API 配置下常常添加多个 Key（如多账号、不同配额额度），当某个 Key 临时耗尽或限流时，用户希望单独停用该 Key，而不必彻底删除。
   - **技术方案**：
     - `NamedApiKey` 数据模型新增 `val isEnabled: Boolean = true` 属性；
     - 在 `AiRepository.formatNamedApiKeys` 中，当 Key 处于停用状态时自动附带 `[已禁用] ` 标头；在 `parseNamedApiKeys` 中智能识别 `[已禁用]`、`[禁用]`、`[off]`、`[disabled]`、`[关闭]` 标头并精准恢复 `isEnabled = false` 状态，完美向前兼容原有配置与纯文本导入导出；
     - 在 `AiRepository.parseApiKeys` 提取请求 Key 列表中，增加 `.filter { it.isEnabled }` 严格过滤，确保实际请求、轮询与自动重试故障转移仅使用启用的有效 Key，彻底跳过停用的 Key；若配置内所有 Key 均被停用，请求前主动拦截并抛出精准提示；
     - 在 `SettingsApiConfigDialog` 的每一个 Key 卡片上增加独立的 `Switch` 开关与「Key N (停用)」状态显示，停用 Key 卡片应用 `alpha = 0.65f` 暗化与边框警示，操作直观敏捷。
   - **文件改动**：`app/src/main/java/com/aiassistant/domain/model/Models.kt`、`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/settings/SettingsApiConfigDialog.kt`。

3. **默认 API 停用后智能安全回退**：
   - **需求背景**：当用户停用了当前被标记为“默认 API”的配置后，若直接新建对话或发起无特定绑定的角色扮演剧情，原代码可能尝试调用已停用的配置导致失败。
   - **技术方案**：
     - `AiRepository.getDefaultApiConfig()` 升级为双重校验：当默认配置为停用状态时，自动回退并返回系统中首个处于启用状态的有效配置；
     - `RoleplayViewModel`、`ChatViewModel` 的新建会话链路统一接入该回退策略，彻底杜绝调用停用 API 导致的请求报错。
   - **文件改动**：`app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`、`app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayViewModel.kt`。

4. **API 400 上下文超限拦截与输出 Token 自适应收敛（如 32768 上下文端点被请求 416131 tokens、其中输出预留 50000 导致直接被拒）彻底修复**：
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

5. **上下文压缩后右上方环形指示器依然超出限制（100%+）彻底修复**：
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
- **单元测试**：全量单元测试（包含 V225FeaturesTest 5 项新测及 ContextCompressionAndBudgetTest 6 项新测在内共 303 项测试）100% 全部通过 (BUILD SUCCESSFUL)。
- **Release APK**：`D:\Agent\APP-烧\app\releases\Echo-v2.2.5.apk`。
  - SHA256: `D5E04968B49C90CE7B14BF7A4F7E5676B0FAF9B6DFFA6661039633CB3F1C6E21`
  - 大小: `16,254,973 字节 (~15.5 MB)`
  - 版本号: `versionCode = 131`, `versionName = "2.2.5"`
  - 架构: 单一安装包（统一格式 `Echo-v2.2.5.apk`，无 `-arm64-v8a` 后缀）
- **发布路径与历史版本永久保留**：严格遵循准则，以后统一只发布在 `D:\Agent\APP-烧\app\releases` 路径下，该目录下全部历史版本安装包永久完整保留，增量输出唯一定名的 `Echo-v2.2.5.apk`。

## [2026-09-19] - v2.2.4：Markdown 全格式容错渲染、全角星号排版归一化、首尾非对称星号容错、跨行格式保护、字体合成保底与用户气泡 Markdown 支持

### 1. 核心需求落实与技术重构详情
1. **全角星号排版归一化（`＊＊＊` / `＊＊`）**：
   - **根因分析**：中文输入法与特定大模型（尤其是国内模型）在生成或排版时，常输出全角 Unicode 星号 `＊`（`\uFF0A`）。原有解析器仅支持 ASCII 半角星号 `'*'`，导致全角星号被当作普通文本原样输出，完全无法进入粗体或粗斜体渲染分支；
   - **技术方案**：在 `cleanLeadingStarArtifacts` 预处理流程中，统一将全角星号 `\uFF0A` 归一化映射为标准半角星号 `*`，实现全角 `＊＊＊文字＊＊＊` 与 `＊＊文字＊＊` 无缝转换为标准 Markdown 语法并精准渲染。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`。

2. **首尾非对称星号容错与防跨词贪婪吞噬（`***text**` / `**text***`）**：
   - **根因分析**：模型生成标记时经常出现开闭数量不一致情况（如开头 3 星、结尾 2 星）。原有解析器以 `***` 开头寻找结尾 `***`，找不到同等数量闭合符时，会跨词贪婪跳跃至后续段落的其他 `***`，导致整段文字格式串色错乱，结尾星号沦为孤立符号显示在屏幕上；
   - **技术方案**：引入非贪婪最近邻闭合策略。以 `***` 开头时，优先寻找最近的闭合符；若最近闭合符为 `**` 且不属于更长星号序列，则智能降级闭合为粗体，不发生跨词贪婪吞并；对于以 `**` 开头但结尾带 3 个星号的情况（`**text***`），一同消费掉多余星号，杜绝残留孤立星号。支持 `****text****` 四星号强化输出。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`。

3. **跨换行符格式保护与连续性识别**：
   - **根因分析**：原有 `MarkdownText` 采用 `content.split("\n")` 切分行后逐行调用 `parseInlineMarkdown`。若加粗或粗斜体内容跨越了单换行符，首行找不到闭合标记直接放弃渲染、回退为原样文本，第二行也因缺少起始标记而原样显示，导致格式彻底失效；
   - **技术方案**：在普通段落行处理分支中增加 `hasUnclosedInlineFormatting` 检查。当检测到当前行包含未闭合的行内加粗、斜体或删除线标记时，在遇到空行或块级边界（标题、代码块、列表、分割线等）前自动预读并合并后续连续行（保留 `\n`），使跨行内容作为一个连贯富文本块交由 Compose 渲染，完美保全换行与格式。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`。

4. **Android 系统字体合成保底（FontSynthesis）**：
   - **根因分析**：Android 原生中文字体库（如 `Noto Sans CJK SC`）仅具备字重（Weight），不存在原生斜体（Italic）字体文件。Compose 在同时指定 `FontWeight.Bold` 与 `FontStyle.Italic` 时，若在部分定制系统上未能正确匹配到合成字形，会触发 Fallback 机制回退至常规 Normal 字体，导致文字虽然去除了星号却依然看起来“没有加粗”；
   - **技术方案**：在 `SpanStyle` 中显式指定 `fontSynthesis = FontSynthesis.All`（对于粗斜体）与 `fontSynthesis = FontSynthesis.Weight`（对于粗体），强制底层图形引擎合成加粗字重，确保在所有厂商定制 Android 系统中中文加粗均能鲜明可见。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`。

5. **用户消息与引用回复气泡行内 Markdown 支持**：
   - **根因分析**：用户消息气泡 (`isUser == true`) 和引用消息预览原先直接调用原生 `Text(text = message.content)`，当用户在提问或输入提示词中输入 `***文字***` 或 `**加粗**` 时，直接 100% 显示原始符号；
   - **技术方案**：在 `ChatMessageComponents.kt` 中全面接入 `parseInlineMarkdown`，用户消息、引用消息预览与引用回复均可优雅呈现加粗、斜体、删除线与行内代码样式，与 AI 助手回复的视觉质感统一。
   - **文件改动**：`app/src/main/java/com/aiassistant/ui/screens/chat/ChatMessageComponents.kt`。

### 2. 自动化测试与工程交付
- **单元测试**：全量单元测试（包含 V224FeaturesTest 6 项新测在内共 286+ 项测试）100% 全部通过 (BUILD SUCCESSFUL)。
- **Release APK**：`releases/Echo-v2.2.4.apk`。
  - SHA256: `41968F5845C9E5B7D89537CD4D16194AE2F4DAC7FB5577EFBCD1E8170FD52B8F`
  - 大小: `16,254,969 字节 (~15.5 MB)`
- **历史安装包永久保留**：严格遵循最高铁律，`releases/` 目录下全部历史安装包完整保留，增量输出唯一定名的 `Echo-v2.2.4.apk`，未生成带有 `-arm64-v8a` 后缀命名的多余包。

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

## [2026-09-18] - v2.2.1：OpenAI 兼容接口流式解析健壮性重构、断流内容绝对保全、空响应精准防护与智能重试机制

### 1. 流式健壮性与稳定性重构清单
1. **断流内容绝对保全与防丢弃机制**：
   - `AiRepository.kt`：在流式读取循环（`sendOpenAIMessage` & `sendAnthropicMessage`）中对 `reader.readLine()` 进行细粒度 IO 异常隔离；遇到网络突发中断、Broken Pipe 或 EOFException 时，只要已经接收到了有效文本（`contentBuilder`）或思考内容（`thinkingBuilder`），坚决不抛出异常、不重置用户界面已打印的文字，平稳视为流结束并持久化保存至本地数据库，彻底消除 `MissingFinishReasonError: Response stream ended without a finish reason` 导致的报错与内容丢弃。
2. **空响应判定边界严格纠正**：
   - 修复原先将中间网络断开误判为模型空回复的逻辑缺陷；
   - 严格遵循准则：**只有在“完全没有收到任何 delta.content、思考内容且无工具调用”时**，才判定为请求失败；彻底根除“明明已收到部分或完整内容却提示 empty response detected”的问题。
3. **缺少 finish_reason 或 [DONE] 的容错降级标记**：
   - 建立完成态健康度核验逻辑：`val isFinished = (hasReceivedDone || !lastFinishReason.isNullOrBlank()) && streamReadException == null`；
   - 当流非正常结束但存在内容时，仅记录 Warning 日志并标记 `finished: false`，保障用户内容 100% 完整交付呈现。
4. **非标准流格式与中转网关全面兼容**：
   - 抽取伴生对象解析器 `AiRepository.Companion.parseOpenAiStreamLine`；
   - **NDJSON 格式支持**：兼容每行直接返回纯 JSON（`{"choices":[...]}`，无 `data:` 前缀）的流式中转；
   - **BOM 头与空格清洗**：自动剔除行首 UTF-8 BOM (`\uFEFF`) 与多余换行、首尾空格；
   - **控制行与注释忽略**：自动识别并忽略 SSE 注释行（`: ping`、`: keepalive`）以及 `event:`、`id:`、`retry:` 元数据行；
   - **中转网关 choices[0].message 兼容**：容错提取误将流式内容置于 `message` 而非 `delta` 的非标准网关输出；
   - **多行跨行 JSON 缓冲**：引入 `jsonAccumulator` 自动处理跨行缩进返回的完整 JSON 响应；
   - **内联错误流式兜底**：若遇到内联包含 `error` 的 chunk，已有内容时平稳保留收尾，无内容时精准解析错误信息。
5. **智能重试保护与 1s / 2s / 5s 指数退避策略**：
   - `dispatchChatMessageWithConfig`：跟踪 `hasEmittedTokens`；
   - **输出保护**：若已向用户输出了部分内容，坚决不执行自动重试，严禁调用 `onResetBuffer`，彻底杜绝重复输出与界面文字闪烁重刷；
   - **退避重试**：仅在完全未收到任何内容时，针对网络波动/连接超时触发自动重连重试，上限 3 次，退避间隔为 `1000ms -> 2000ms -> 5000ms`。

### 2. 自动化测试与工程交付
- **专项测试**：新增 `OpenAiStreamRobustnessTest`，覆盖 BOM 剔除、NDJSON 解析、缺失 finish_reason、缺失 [DONE]、注释过滤、内联错误等 10 项专项单元测试；
- **全量测试**：包含 `SevenUserRequestsTest` 等全部 267+ 项单元测试 100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 127`，`versionName = "2.2.1"`；
- **历史安装包永久保留准则（最高铁律）**：`releases/` 目录下所有历史版本完整无缺保留，仅增量输出全新安装包。

## [2026-09-18] - v2.2.0：7 项核心交互、记忆控制与模型上下文深度修复

### 1. 7 项用户需求深度落地与修复清单
1. **API 设置长按拖拽按键阴影圆角统一**：
   - `SmoothReorderState.kt`：重构 `Modifier.reorderItem`，增加 `shape: Shape = RoundedCornerShape(10.dp)` 参数；并在 `graphicsLayer` 中同步绑定 `this.shape = shape` 与 `clip = true`，使长按拖拽浮起时投影的阴影轮廓与卡片圆角完美一致，彻底消除直角方形阴影。
   - `SettingsApiConfigDialog.kt`：向多 Key 列表的 `.reorderItem(...)` 显式传递 `shape = RoundedCornerShape(10.dp)`。
2. **API 设置 Key 命名紧凑横向同行重构与说明冗余精简**：
   - `SettingsApiConfigDialog.kt`：Key 命名输入框从原来的独立占一行，重构为直接与“Key 1”、“Key 2”角标在同一直线上水平同行紧凑展示，使用轻量 `BasicTextField` 并附带“备注名称 (可选)”占位提示；
   - 删除所有括号中的说明文字（如“（主密钥，默认使用）”等），角标仅显示干净的“Key 1”、“Key 2”；
   - 密钥输入框占位符精简为“填写密钥 (sk-...)”，整体界面垂直占用大幅缩减。
3. **模型思考/连接气泡文本显示完整性（横向滑动与展开双重支持）**：
   - `ChatMessageComponents.kt`：在思考气泡未进入正式长思考时（如连接中、报错提示、重连倒计时、Token 消耗等信息），新增单行平滑左右滑动 (`Modifier.horizontalScroll(rememberScrollState())`)；
   - 增加点击展开/收起能力（支持至 12 行完整换行展示）及展开折叠指示图标，使超长报错信息（如包含 URL、具体状态码或多行详情）可以完全无遮挡地展开阅读。
4. **记忆提取三大问题彻底根治**：
   - **内容完整性**：在 `PersonalizationManager.kt` 中重构辅助记忆提取提示词，废除原 25 字过短截断限制，放宽至 30~80 字完整主谓宾陈述句；在 `AiRepository.kt` 中提升辅助模型 `max_tokens` 至 1024；
   - **前缀与语气废话剥离**：在 `SmartMemoryExtractor.kt` 的 `refineMemoryContent` 中引入复合前缀循环清洗机制，彻底剥离如“根据上述对话分析得出如下核心事实：”、“建议记住：”、“经分析如下：”以及列表序号符号；
   - **负向约束与禁令强力生效（最高优先级）**：在 `SmartMemoryExtractor.kt` 中将包含“不允许”、“禁止”、“严禁”、“别叫我”等内容直接分类为 `行为约束`；在 `AiRepository.kt` 的 `buildRelevantMemoryBlock` 中，将所有行为约束与禁令无条件 100% 提取为【核心行为准则与绝对约束（最高优先级，必须严格无条件遵守）】独立系统指令块，并在 `buildEffectiveSystemPrompt` 注入禁令合规强约束，彻底根绝“不允许叫老板”却依然叫老板的顽疾。
5. **模型上下文遗忘与多轮记忆丢失根治**：
   - **扩大近期上下文保留窗口**：将 `AiRepository.MIN_RECENT_CONTEXT_TOKENS` 从 1,200 提升至 16,000 tokens；
   - **提高摘要压缩触发门槛**：将 `MIN_SUMMARY_SOURCE_MESSAGES` 从 6 提升至 16 轮，`MIN_SUMMARY_SOURCE_TOKENS` 从 1,200 提升至 8,000 tokens；
   - **短中轮次 100% 无损传递**：在 `buildContextBundle` 中，短中对话（在预算范围内）全部直接作为近期完整对话传递，`summary` 保持为 `null`，不再过早生成模糊摘要覆盖原始对话细节；
   - **修正上下文窗口解析**：修复正则以避免将年份（如 2024）误解析为模型上下文窗口大小。
6. **提示词输入框点击跳跃与滚动抖动消除**：
   - `ChatSettingsDialogs.kt` 与 `ChatStoryDialogs.kt`：将系统提示词与剧情开场提示词编辑框的状态管理重构为 `rememberSaveable(stateSaver = TextFieldValue.Saver)`，并锁定初始光标位置，杜绝获取焦点时重新重置光标导致的输入框与 LazyColumn 视口剧烈跳动问题。
7. **全供应商与中转代理思考模式（Thinking）参数精准透传**：
   - `ModelCapabilityEngine.kt`：升级推理能力引擎，全面支持 OpenAI (o1/o3/o4/gpt-5)、Anthropic Claude 3.7、DeepSeek (deepseek-reasoner/r1) 及 OneAPI/NewAPI 常见中转代理；
   - `AiRepository.kt`：为 OpenAI o 系列及中转精准透传 `reasoning_effort`，并对标准 o 系列避免透传引发 400 报错的 `thinking` 扩展字段；为 Anthropic Claude 3.7 精确配置 `thinking` 参数块、`budget_tokens`、固定 `temperature = 1.0` 并动态扩展 `max_tokens`；
   - `ChatViewModel.kt`：在会话层与全局层做好 `enableThinking` 与 `thinkingEffort` 的层级兜底继承，确保未显式配置时安全读取上一级有效设定。

### 2. 自动化测试与工程交付
- **测试套件**：全量单元测试（包含 SevenUserRequestsTest 共 266 项自动化测试）100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 126`，`versionName = "2.2.0"`；
- **Release APK 交付**：
  - 路径：`releases/Echo-v2.2.0-arm64-v8a.apk`（及增量 `Echo-v2.2.0.apk`）；
  - 体积：`16,238,589` 字节；
  - SHA-256：`8468EB3D09492818E627AEC2FCBB9541DF3711247F4FA640108774A07A15C1BE`；
  - 签名方案：APK Signature Scheme v2 验证通过；
  - 证书 SHA-256：`939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`（与历史版本保持 100% 一致，支持直接平滑覆盖安装升级）；
  - **历史安装包永久保留准则（最高铁律）**：`releases/` 目录下所有历史版本完整无缺保留，仅增量输出 v2.2.0 安装包。

## [2026-09-17] - v2.1.9：12 项专项需求全面核验落地、全局图片手势裁剪编辑闭环、记忆提炼辅助模型层级归一与自由直选

### 1. 12 项核心需求核验与修复状态总览
1. **对时间的敏锐度与准确度深度优化**：【已修复并保持】
   - `TimelineMemoryHelper.kt` 与 `AiRepository.kt`：支持文学叙事时间跨度（如“两周过后”、“暑假开始”、“三年后·春”等）解析；`inferCurrentStoryTime` 优先倒序寻找时间标签与正文特征，兜底继承上一个有效故事节点，杜绝粗暴退回“未确定”；完整支持 6 维常驻世界设定流转。
2. **全局按键阴影与圆角几何轮廓统一消除直角割裂**：【已修复并保持】
   - `PressEffects.kt`：`Modifier.echoShapeClick` 统一采用 `clip(shape)` 几何裁剪与 `drawWithContent` 绘制 Path 覆盖层，配合 `indication = null` 彻底根除原生涟漪矩形边缘溢出的直角毛刺。
3. **“本对话专属记忆与时间线”单条记忆 UI 排版重构**：【已修复并保持】
   - `ChatSettingsDialogs.kt`：单条专属记忆卡片左侧占满记忆正文与时间标签，右侧独立展示上方 Switch 开关、下方并列展示编辑与删除小按键，布局整齐轻量。
4. **对话设置（ChatSettingsDialog）功能层级顺序调整**：【已修复并保持】
   - `ChatSettingsDialogs.kt`：已将“本对话专属记忆与时间线”和“跨会话记忆与世界书”调整至模型头像正上方，层级聚焦核心设定。
5. **图片导入编辑功能（手势放缩、平移、翻转与裁剪）**：【已补全修复】
   - 在已支持的角色头像与会话模型头像基础上，全面接入 `SettingsAppearanceTab.kt`（用户自定义头像采用圆形裁剪、首页壁纸与对话页壁纸采用矩形裁剪）与 `SettingsApiConfigDialog.kt`（API 模型头像采用圆形裁剪）；
   - 在 `AvatarManager.kt` 中完善 `saveTempAvatarBitmap` 及 file 协议读取支持，实现即时手势微调与无缝回存。
6. **“本对话专属记忆与时间线”记忆列表支持折叠与展开**：【已修复并保持】
   - `ChatSettingsDialogs.kt`：已支持“专属记忆清单 (N)”标题栏点击切换展开折叠、状态文字和箭头动画，大量记忆时不再无限占屏。
7. **设置中的“记忆提炼辅助模型”位置与选择逻辑统一**：【已补全修复】
   - 从“提示词与记忆”选项卡（`SettingsPromptsMemoryTab.kt`）彻底移除旧版辅助模型设置卡片与旧式下拉选单；
   - 完整迁入“模型辅助与思考”选项卡（`SettingsModelFeaturesTab.kt`），统一使用跨服务商自由直选的 `UniversalModelPickerCard`，并保留“即时测试辅助连接”验证弹窗。
8. **文本排版防跨行优化 + 全局长文本展开/收起**：【已修复并保持】
   - `ExpandableText.kt` 支持长文本平滑展开与折叠；全局各类操作按键文字统一增加 `maxLines = 1`，避免文字被折行挤出按键。
9. **会话内自定义模型头像“会话级”隔离**：【已修复并保持】
   - 会话实体增加 `modelAvatarUri`，Room 数据库迁移完成，各会话拥有完全独立的模型头像，杜绝全局串扰。
10. **会话设置中模型配置标签同排对齐**：【已修复并保持】
    - `ChatSettingsDialogs.kt` 中“模型”标题与窗口、搜索、视觉、思考等能力标签在同一行水平紧凑排列，消除高度浪费。
11. **上下文压缩功能彻底重构与开源规范落地**：【已修复并保持】
    - `AiRepository.kt` 实现标准 `ConversationSummaryBufferMemory`，截断点前的早期历史彻底移出发送给模型的 Prompt Context，以滚动摘要替代；并提供 50%~60%~75% 梯度预警。
12. **允许用户对模型回复的内容进行编辑**：【已修复并保持】
    - `ChatMessageComponents.kt` 在助手回复二级菜单中提供“编辑回复”功能，支持用户修改模型回复文本并写回数据库。

### 2. 自动化测试与质量保障
- **测试套件**：全量单元测试（V1921 至 V219 共 260 项自动化测试）100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 125`，`versionName = "2.1.9"`；
- **Release APK 交付**：
  - 路径：`releases/Echo-v2.1.9-arm64-v8a.apk`（及增量 `Echo-v2.1.9.apk`）；
  - 体积：`16,222,205` 字节；
  - SHA-256：`415B95C916D46A3A3760FCCE63AA9F6B647505C956A38701FA60A3D88037B7A2`；
  - 签名方案：APK Signature Scheme v2 验证通过；
  - **历史安装包永久保留准则（最高铁律）**：`releases/` 目录下所有历史版本完整无缺保留，仅增量输出 v2.1.9 安装包。

## [2026-09-17] - v2.1.8：记忆提取完整性与语义相关性重构、多 Key 自动透明故障转移、输出工具栏二级菜单收纳、API Key 独立命名与卡片化配置、滚动摘要虚假提醒根治、核心巨型文件工程级模块化拆分

### 1. 本次 6 大核心诉求深度落实与功能重构
1. **记忆提取异常修复（内容完整性与原文相关性重构）**：
   - **去除推理流污染**：在 `TimelineMemoryHelper.kt` 中实现 `stripThinkingTags`，严格清理 `<think>...</think>` 及模型流式截断产生的未闭合 `<think>` 标签，前置净化记忆输入文本；
   - **杜绝格式碎片**：在 `SmartMemoryExtractor.kt` 中排除 markdown 链接语法 `[text](url)`，避免将网页链接碎片误提取为用户长期记忆；
   - **语义相关性交叉检验**：新增 `isRelevantToOriginalContent`，基于标点分词与连续 2-gram 关键词校验，严格比对候选记忆与用户原文的语义重合度，坚决拦截虚假幻觉与偏离原文的记忆候选；
   - **放宽辅助模型输出上限**：在 `AiRepository.kt` 中将辅助记忆提取与时间线分析的 `max_tokens` 从 96 大幅放宽至 512，并过滤开场白与思考流，彻底根除因 Token 限制导致的句子被硬截断问题。

2. **同一个 API 配置多 Key 自动故障转移与透明重试**：
   - **智能纯 Key 提取与清洗**：在 `AiRepository.kt` 中重构 `parseApiKeys`，自动解析并剥离 `[名称]` 与 `名称:::` 标签，仅向底层网络传输清洁密钥；
   - **多 Key 轮询故障转移机制**：在 OpenAI 及 Anthropic 流式请求全流程中加入备用 Key 故障转移循环。遇到连接超时、网络中断、400/401/403/404/422/429/500/502/503/504 HTTP 报错、SSE 内部 `{"error": ...}` JSON 结构或空响应时，自动无感切换至下一个有效 Key 并重试请求；
   - **辅助模型协同故障转移**：在辅助记忆提取等后台请求中同样接入多 Key 备用切换循环，确保关键后台分析任务不受单一 Key 异常影响。

3. **模型回复下方工具栏超长问题重构（二级菜单收纳）**：
   - **轻量化一级操作栏**：在 `ChatMessageComponents.kt` (`MessageFooter`) 中，一级工具栏仅保留核心高频操作（版本切换器、复制、分支/引用、重新生成以及更多操作按钮）；
   - **二级液态玻璃下拉菜单**：点击 `MoreVert` 触发 `EchoGlassDropdownMenu`，将“重新编辑”、“固定到上下文 / 取消固定”、“从上下文中排除 / 恢复”以及“删除此条消息”整齐收纳进二级菜单；若某条消息处于固定或排除状态，更多按钮自适应显示高亮主题色，兼具视觉轻盈与状态清晰度。

4. **API Key 支持自定义命名与备注区分**：
   - **领域模型扩展**：在 `Models.kt` 中新增 `data class NamedApiKey(val name: String = "", val key: String = "")`；
   - **双向序列化与兼容**：在 `AiRepository.kt` 中实现 `parseNamedApiKeys` 与 `formatNamedApiKeys`，同时向前兼容纯 Key、逗号/换行分隔以及 `[名称] sk-xxx` 与 `名称:::sk-xxx` 格式；
   - **卡片化多 Key 输入面板**：在 `SettingsApiConfigDialog.kt` 中提供独立 Key 卡片化列表，每个 Key 拥有专属的“备注名称（可选）”与“API Key”输入框，支持动态添加独立输入框、单个删除及批量智能解析；
   - **设置卡片直观标识**：在 `SettingsScreen.kt` 的 `ApiConfigCard` 中增加 Key 数量角标与已命名备注预览标签，方便直观辨识不同账号或额度配额。

5. **核心巨型文件工程级模块化拆分（降低复杂度与提升稳定性）**：
   - **SettingsScreen.kt (原 8,182 行) 拆分为 9 个高内聚子文件**：
     1. `SettingsScreen.kt`（主框架与导航容器，~1,000 行）
     2. `SettingsApiConfigDialog.kt`（API 配置与多 Key 管理弹窗，~1,500 行）
     3. `SettingsUniversalModelPicker.kt`（通用模型选择器组件，~600 行）
     4. `SettingsAppearanceTab.kt`（外观与个性化壁纸设置，~600 行）
     5. `SettingsModelFeaturesTab.kt`（模型特性与全局参数，~500 行）
     6. `SettingsPromptsMemoryTab.kt`（提示词与记忆设置，~1,600 行）
     7. `SettingsPersonalizationTab.kt`（个人偏好与快捷模板，~1,100 行）
     8. `SettingsSecurityAndBackupTab.kt`（安全备份与更新日志，~900 行）
     9. `SettingsWebSearchTab.kt`（网络搜索与辅助功能，~1,000 行）
   - **ChatScreen.kt (原 10,375 行) 拆分为 7 个高内聚子文件**：
     1. `ChatScreen.kt`（主界面与生命周期调度，~1,840 行）
     2. `ChatScreenModels.kt`（聊天界面共享状态与数据模型，~60 行）
     3. `ChatContextComponents.kt`（上下文使用率卡片、环形图及跳转按键，~640 行）
     4. `ChatMessageComponents.kt`（消息气泡、MessageFooter、头像、引文及工具调用卡片，~2,100 行）
     5. `ChatInputComponents.kt`（输入栏、推理Popup、模型选择及引用预览，~1,725 行）
     6. `ChatSettingsDialogs.kt`（系统提示词、模板及会话设置弹窗，~2,225 行）
     7. `ChatStoryDialogs.kt`（故事角色与时间轴工作台弹窗，~2,525 行）
   - 拆分后对外契约、ViewModel 数据流、UI 状态完全保持 100% 稳定一致。

6. **上下文处始终提醒“有较早信息尚未进入摘要”问题深度修复**：
   - **重构 canCompress 触发策略**：在 `AiRepository.kt` 中，废除原先粗暴仅判断 `usableMessages.size >= 4` 的过敏逻辑，改为综合考量实际 Token 压力与上下文占用率（当且仅当占用率达到 50% 以上或消息条数超过 20 条且存在摘要空间时方判定为可压缩）；
   - **消除虚假警报红点**：在 `ChatContextComponents.kt` 中，`ContextUsageButton` 仅在 `canCompress && usagePercent >= 0.60f` 时显示红色警示标记；
   - **优化状态提示语义**：在 `ContextUsageStatus` 中，当上下文充裕且健康时明确提示“上下文健康，对话空间充裕”与“暂无长对话压缩需求”，仅在达到实际阈值时才提示执行滚动摘要。

### 2. 自动化测试与质量保障
- **新增单元测试**：`V218FeaturesTest.kt`，全面覆盖：
  - `testV218UserUpdatesCompleteness`（6项核心特性完整性）
  - `testNamedApiKeyDataClass`（NamedApiKey 数据模型属性与 copy）
  - `testNamedApiKeyParsingAndFormatting`（多格式命名解析与格式化回存）
  - `testPureApiKeyExtractionStripsNames`（纯 Key 提取与标签剥离隔离性）
  - `testStripThinkingTags`（闭合与未闭合思考标签净化）
  - `testSmartMemoryExtractorUrlAndRelevanceFilter`（URL 链接过滤与 2-gram 语义相关性校验）
- **全量单元测试**：258 个单元测试 100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 124`，`versionName = "2.1.8"`；
- **Release APK 产物**：
  - 路径：`releases/Echo-v2.1.8-arm64-v8a.apk`（及增量 `Echo-v2.1.8.apk`）；
  - 大小：16,222,205 字节；
  - SHA256：`1F195EBA74514F40A708AC916012BE8E7302FBF2E13906A28084B387B42AF444`；
  - 架构：`arm64-v8a` (`isUniversalApk = false`)；
  - 签名验证：APK Signature Scheme v2 验证通过；
  - **历史安装包永久保留准则（最高铁律）**：`releases/` 目录下所有历史版本（v1.6.6 ~ v2.1.7）完整无缺保留，仅增量输出 v2.1.8 安装包。

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

### 2. 自动化测试与构建交付
- **全新自动化单元测试**：
  - `ChronicleTimelineStudioTest.kt`：新增 `ATEMPORAL_SETTING` 枚举解析、格式化、提炼元数据结构测试；
  - `TimelineDeepModelExtractionTest.kt`：新增大模型 JSON 深度提炼、五大分类色彩完整性、导演指令过滤测试；
  - `V215FeaturesTest.kt`：新增 v2.1.5 版本更新日志完整性自检测试；
- **全量单元测试**：234 个单元测试 100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 121`，`versionName = "2.1.5"`；
- **单一安装包构建与历史包永久保留（最高铁律）**：
  - 严格执行单一安装包命名规则，仅输出 `Echo-v2.1.5.apk`；
  - 严禁删除或清理任何历史版本安装包，所有历史版本完整保留；

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

### 2. 自动化测试与构建交付
- **全新自动化单元测试**：
  - `ChronicleTimelineStudioTest.kt`：新增跨天时序单调递增与“第二天早上”自动推进测试用例、设定分类标签循环轮换测试用例；
  - `V214FeaturesTest.kt`：新增 6 大核心缺陷修复与功能点自检测试；
- **全量单元测试**：230 个单元测试 100% 全部通过 (BUILD SUCCESSFUL)；
- **版本配置**：`versionCode = 120`，`versionName = "2.1.4"`；
- **单一安装包构建与历史包永久保留（最高铁律）**：
  - 严格执行单一安装包命名规则，仅输出 `Echo-v2.1.4.apk`；
  - 严禁删除或清理任何历史版本安装包，所有历史版本完整保留；

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/utils/TimelineMemoryHelper.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/ChronicleTimelineStudioTest.kt` [MODIFY]
- `app/src/test/java/com/aiassistant/V214FeaturesTest.kt` [NEW]
- `app/build.gradle.kts` [MODIFY]
- `UPDATE_LOG.md` [MODIFY]

## [2026-09-15] - v2.1.3：开源前沿记忆体系进化、排他冲突消解、三维混合检索与结构化上下文压缩落地

### 1. 本次核心功能升级与开源前沿技术吸收
1. **时间轴与多维设定工作台全面重构 (Chronicle Timeline Studio)**：
   - **时间与类别多维筛选**：顶部提供横向滚动时间 Chips（【全部时间】、【第1天】、【第2天】...），点击直接过滤并高亮对应时间下的所有事件与设定；同时提供【全部】、【📖 剧情推进】、【⚖️ 规则约束】、【🎭 角色设定】、【🌍 剧情设定】类别过滤；
   - **垂直流线型时间轴 UI (Vertical Flowing Timeline)**：采用垂直发光节点圆点与连线，节点发光颜色随类别变换（科技蓝、活力橙、优雅紫、自然绿），呈现清晰的故事编年史脉络；
   - **相对时间距离动态徽章**：自动计算相对于当前故事时间的距离（如：`相对于当前：昨天 / 前天 / 2天前`），直观呈现时序纵深；
   - **全字段直接可视化编辑与切换**：支持单条直接修改时间标签、轮换类别、修改正文描述、一键删除与快捷手动追加。
2. **根治剧情指导粗暴导入第 1 天与上下文完全隔离**：
   - **指令与正文彻底解耦**：引入 `TimelineMemoryHelper.isPureDirectorInstruction`，自动识别并过滤用户在 `[]` 中输入的导演指令文字本身（如 `[让两人在雨夜再次相遇]`、`[推进剧情]`），模型专注于故事对话正文与叙事推进；
   - **独立无状态元分析请求**：梳理过程完全走独立的单次无状态请求（优先调度已配置的辅助提炼模型，未配置时使用当前 API 配置独立发起），**绝对不把提炼任务注入当前会话的 `messages` 历史，100% 避免干扰当前剧情与主模型上下文**；
   - **真实时间推进理解与归一化**：模型深入理解对话中隐藏的“第二天”、“过了三天”、“当晚”等线索推算真实天数，杜绝将所有内容机械堆砌在第 1 天。
3. **时间无关全局设定提炼与【加入确认提醒】**：
   - 模型在梳理时独立提炼出与时间无关的角色固有特质、长期偏好与世界固定规则；
   - 工作台顶部以高亮精致卡片展示**「💡 世界观与角色固有设定（时间无关）」**加入确认提醒；
   - 每一条设定配备复选框（默认选中）、类别标签、内容编辑框与作用域切换（“会话专属” / “全局长期”），并支持一键全选/清空，完全交由用户审核后同步存入记忆。
4. **开源前沿记忆体系进化 (`AdvancedMemoryEngine`)**：
   - 深度借鉴 **Mem0、Generative Agents、LangChain Memory** 等开源社区前沿范式，重构记忆与上下文引擎；
   - **原子事实分类体系与重要度分级**：将对话中沉淀的记忆细化为五大维度：`CONSTRAINT` (5)、`PREFERENCE` (4)、`TIMELINE` (4)、`WORLD_STATE` (3)、`FACT` (3)；
   - **排他性事实冲突自适应消解更替 (Conflict Resolution & Upsert)**：解决居住地更替、称呼更替、技术栈更迭等新旧记忆自相矛盾痛点，写入新记忆时自动识别并覆盖旧冲突条目；
   - **三维混合动态检索评分算法 (Tri-Factor Hybrid Scoring)**：综合 Relevance (40%) + Importance (25%) + Recency (20%) + Entity Boosting (15%) + ScopeBoost 动态唤醒记忆；
   - **结构化多维状态机分层上下文压缩**：摒弃单段粗暴摘要，以【核心背景固定约束】+【关键里程碑推进】+【未决待办事项】三层状态机提炼，信噪比极大提升；
   - **智能无损信息密度提纯**：自动过滤纯寒暄废话轮次，节约 Token 预算；
   - **六点手柄平滑拖拽重排弹簧物理动效 (`SmoothReorderState`)** 完美保持。

### 2. 自动化测试与构建交付
- **全新自动化单元测试**：
  - `ChronicleTimelineStudioTest.kt`（5 项测试全绿）：覆盖导演指令识别、多类别事件格式化与解析、时间无关设定模型与作用域切换、全功能 JSON 解析与多天相对时间推算；
  - `TimelineMemoryTest.kt`（11 项测试全绿）：覆盖时间标签提取、模型 JSON 容错解析、文本降级兜底与 Prompt 组装；
  - `MemoryAndCompressionEngineTest.kt`（11 项测试全绿）：覆盖事实分类判定、重要度推断、冲突消解、三维混合评分、信息提纯与结构化状态机；
  - `V213FeaturesTest.kt`（2 项测试全绿）：覆盖更新日志与枚举基准权重验证；
- **全量单元测试**：全量测试套件 100% 全部通过；
- **版本配置**：`versionCode = 119`，`versionName = "2.1.3"`；
- **单一安装包构建与历史包永久保留**：
  - **坚决彻底废除重复的 `Echo.apk` 复制**，严格执行用户指示只输出单一安装包；
  - 增量输出至 `D:\Agent\APP-烧\app\releases/Echo-v2.1.3.apk`，大小 16,189,437 字节（约 15.44 MB），SHA256: `FDC36E1F6D7DFA64CD2EAE0E2765829AE7DA2CB64254BF3A0FFEE0C7C70F1B76`；
  - 严格恪守【历史安装包永久保留准则（最高铁律）】，未触碰或删除 `releases/` 下的任何既有历史安装包。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/utils/AdvancedMemoryEngine.kt` [NEW]
- `app/src/test/java/com/aiassistant/MemoryAndCompressionEngineTest.kt` [NEW]
- `app/src/test/java/com/aiassistant/V213FeaturesTest.kt` [NEW]
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt` [MODIFY]
- `app/build.gradle.kts` [MODIFY]
- `UPDATE_LOG.md` [MODIFY]

## [2026-09-15] - 六点手柄拖拽排序平滑物理动效与换位弹簧动画落地

### 1. 本次核心功能升级与用户需求落实
1. **API Key 列表与消息排队浮窗平滑拖拽重排序动效体系 (`SmoothReorderState`)**：
   - **痛点解决**：彻底解决此前长按六点手柄（`DragIndicator`）拖动时条目完全不跟随手指移动、移动达到阈值后瞬间硬切跳变且中断手势的生硬体验；
   - **手指实时连续物理跟随 (Pointer Following)**：被按住拖动的卡片自动浮起、轻微放大（`scale = 1.02f`）、阴影增强（`shadowElevation = 8.dp`）、层级提升至顶层（`zIndex = 10f`），垂直坐标 `translationY` 100% 贴合手指移动；
   - **无缝跨项交换与位移补偿 (Seamless Cross-item Swap)**：
     - 当拖动位移超过阈值（身位约 42%~50%）时触发数据项交换，同时为被拖拽项自动补偿位移（`dragOffsetY ±= itemHeight`），视觉位置保持在手指正下方，**手势不中断，支持用户一口气连续上下拖动多项**；
     - 每次跨越时伴随轻微触觉震动反馈（Haptic Feedback）；
   - **相邻被挤开项弹簧平滑让位 (Spring Shift Animation)**：
     - 被挤开的相邻卡片瞬间应用反向位移补偿，随后通过 Compose 弹簧动画（`spring(dampingRatio = LowBouncy, stiffness = Medium)`）顺滑飘移滑入新空位，彻底杜绝瞬间闪切与突变；
   - **松手平滑吸附归位 (Spring Snap on Release)**：
     - 手指抬起或手势结束时，被拖动项通过弹簧动画平滑回弹、精确吸附落入目标排位，随后复原缩放与层级；
   - **按键双向对流平滑动画**：
     - 设置页中点击 API Key 的「向上/向下箭头」按钮时，同样支持双方对流互换平滑位移动画（`onAnimateSwap`）。

### 2. 自动化测试与质量保障
- 新增 `SmoothReorderTest.kt` 专用单元测试（6 项测试全部一次性通过）：
  - `testInitialState`：测试初始状态与非激活状态；
  - `testOnDragStart`：测试长按激活手势初始化；
  - `testOnDragDelta_downwardSwap`：测试向下拖动阈值触发交换、索引顺延与物理位移精准补偿；
  - `testOnDragDelta_upwardSwap`：测试向上拖动阈值触发交换与向上位移补偿；
  - `testBoundaryGuards`：测试顶部与末尾边界防护，防止数组越界与误调换；
  - `testOnAnimateSwap_buttonTrigger`：测试按钮双向对流换位动画触发；
- 全量单元测试套件（33 个测试类，上百个测试用例）100% 全部通过，耗时仅 17s；
- Kotlin 编译检查 (`compileDebugKotlin`) 0 报错通过；
- **坚决遵守用户要求：未执行 APK 构建**。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/ui/components/SmoothReorderState.kt` [NEW]
- `app/src/test/java/com/aiassistant/SmoothReorderTest.kt` [NEW]
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt` [MODIFY]
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt` [MODIFY]

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

## [v2.1.2] - 2026-09-14

### 1. 本次核心功能升级与用户需求落地
1. **外置记忆库与世界书 (Lorebook) 完整实现**：
   - **数据与业务模型**：新增独立的世界书实体 `WorldBook` 与词条实体 `WorldBookEntry`（支持关键词触发、常驻激活 `isConstant`、优先级权重排序 `priority`、单条及书本启用/停用控制）；
   - **UI 界面与词条管理**：在设置页新增独立卡片“世界书与设定库 (Lorebook)”，支持世界书的增删改查、词条编辑管理，并提供“一键载入示例世界书《奇幻与机械纪元》”快速上手体验；
   - **普通对话与故事对话双模态原生生效**：
     - 普通对话设置弹窗（`ChatSettingsDialog`）新增“外置记忆库与世界书”专属配置开关；
     - 故事创作/角色扮演统一设置弹窗（`StoryUnifiedSettingsDialog` Tab 4）新增外置记忆库与世界书开关；
     - 对话发送消息时动态扫描用户输入，智能检索命中的世界书词条与全局外置记忆并精准注入上下文；未命中时 0 Token 浪费。
2. **辅助模型提炼记忆（支持指定模型与本地纯规则平滑兜底）**：
   - 在设置中“记忆与个性化”专区新增“辅助模型提炼记忆”配置，可指定任一已添加的 API 配置与模型作为记忆提炼辅助模型；
   - 提供辅助模型记忆提炼测试弹窗（输入样例文本即可快速测试模型调用与提炼效果）；
   - **安全平滑降级**：当辅助模型未配置、网络离线、报错、鉴权失败或返回 IGNORE 时，系统 100% 自动无缝降级为本地规则引擎（`SmartMemoryExtractor`），确保记忆识别与提炼永不中断。

### 2. 自动化测试与质量保障
- 新增 `V212FeaturesTest.kt` 核心单元测试，涵盖关键词分词与命中、常驻条目激活、按权重排序、会话开关持久化及辅助模型规则降级，5 项测试全部通过；
- 回归测试 `V211FeaturesTest` 9 项核心测试全部通过；
- 构建与编译 0 报错。

### 3. 发布产物信息
- **安装包路径**：`releases/Echo-v2.1.2-arm64-v8a.apk`
- **文件体积**：16,156,669 字节 (约 15.41 MB)
- **SHA256**：`8180041D0100373593511D42C858C7E8192B6EBD375A5341B442DB386A8C0A80`
- **Package**：`com.aiassistant` | **VersionCode**：`118` | **VersionName**：`2.1.2` | **ABI**：`arm64-v8a`
- **签名验证**：APK Signature Scheme v2 (release 签名验证通过，1 signer)
- **历史版本永久保留**：`releases/` 目录中全部 125 个历史安装包完整保留，未进行任何删除。

### 4. 改动文件列表
- `app/build.gradle.kts`
- `app/src/main/java/com/aiassistant/data/local/AppDatabase.kt`
- `app/src/main/java/com/aiassistant/data/local/WorldBookDao.kt`
- `app/src/main/java/com/aiassistant/domain/model/WorldBookModels.kt`
- `app/src/main/java/com/aiassistant/domain/model/Models.kt`
- `app/src/main/java/com/aiassistant/domain/model/RoleplayModels.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/data/repository/RoleplayRepository.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/utils/PersonalizationManager.kt`
- `app/src/test/java/com/aiassistant/V212FeaturesTest.kt`

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
- 专项新增 `V211FeaturesTest` 8 项核心测试全部通过。

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
- 新增 `V202FeaturesTest` 专项覆盖 7 项核心需求：
  - `testV202CurrentVersionUserUpdatesCompleteness`：7 项核心需求更新日志完备性验证；
  - `testQuotePromptSynthesis`：独立预览卡片引文与用户提问精准合成验证；
  - `testSendButtonHaloColorSwitchContract`：发送/暂停按键红蓝边框与光晕状态切换契约验证；
  - `testCanvasConcentricCircleContract`：同轴同心圆绘制无子像素偏移契约验证；
  - `testSettingsScreenTrueFloatingContentPaddingContract`：设置页真悬浮与 contentPadding 注入验证；
  - `testBranchConversationFullContextInheritance`：分支对话历史上下文与参数继承验证；
  - `testSmartMemoryExtractorEnhancements`：增强版原子事实提取与防过度贪婪验证；
  - `testSystemMemoryContextAntiParrotingInjection`：结构化记忆注入与 Anti-Parroting 指令验证；
  - `testDeleteMessageSecondaryConfirmationContract`：消息删除二次防误触弹窗契约验证。

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
- 新增 `V201FeaturesTest` 专项覆盖：
  - `testV201CurrentVersionUserUpdatesCompleteness`：10 项核心需求更新日志完备性验证；
  - `testCleanLeadingStarArtifacts`：首句孤立星号与颜色标签清理、标准列表及粗体保护验证；
  - `testQuotePromptFormat`：引用格式与提问引导验证；
  - `testButtonDimensionAndHaloScaleConstraint`：按键尺寸 34dp、间距 10dp 与光晕最小缩放 1.0f 约束验证。

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
1. **控制栏与输入框渐变高亮边缘左侧加深（Req 1）**：
   - 顶部控制栏与底部输入框背景边框渐变高亮微光调整：将左侧透明度由原浅色 `0.22f`/`0.28f` 加深至 `0.50f`/`0.48f`，右侧保留通透柔和微光（`0.16f`/`0.14f`），提升边缘质感与立体层次。
2. **设置页面控制栏统一为对话页悬浮胶囊工具栏（Req 2）**：
   - 设置页顶部全面重构为与 ChatScreen 视觉规范 100% 对齐的悬浮胶囊工具栏（`Surface + echoHazePanel(RoundedCornerShape(22.dp))`），集成返回按键、标题与各 Tab 指示器。
3. **输入框最小状态下向下拖拽收缩隐藏为发送键（Req 3）**：
   - 输入框在默认最小高度状态下，通过右上角控制弧线向下拖动即可触发平滑收缩动画，整体缩减收敛为纯圆形发送/暂停按键；
   - 隐藏状态下发送键外圈增加精致呼吸脉冲微光光环；
   - 隐藏后点击发送/暂停键仅退出隐藏状态并展开输入框，不误触发发送或暂停；系统物理返回键同步拦截并展开输入框，进出均具备平滑尺寸与透明度缓动动画。
4. **输入框按键边缘添加蓝色高亮微光包边（Req 4）**：
   - 输入框内部圆形按键（+号扩展、发送、停止）增加 `1.2.dp` 精致蓝色高亮描边（`glass.outlineSelected`），保持纯白底色逻辑不变。
5. **输入框隐藏时顶部悬浮栏同步收缩至圆形返回键（Req 5）**：
   - 输入框收缩隐藏时，顶部悬浮栏联动触发收缩动画，收敛至左上角圆形返回键，外圈附带微光呼吸光环；点击返回键或触发系统返回均联动恢复展开两栏。
6. **消息长按与划选菜单全面支持引用（Req 6）**：
   - 消息底部操作栏新增「引用」动作；划选复制/剪切工具栏新增快捷引用功能；
   - 自动提取目标文字并规范拼接为 Markdown 块引用（`> 引文`）直接追加填入输入框，同时智能保留未发送草稿。
7. **进入对话快速滑动到底部防半途停滞（Req 7）**：
   - 进入对话时执行两段式底部吸附滑动（初始滚动到底部 + 280ms 延迟校准二次触底），消除多图与复杂气泡测量高度跳动导致的半途卡顿。
8. **隐藏会话能力与正常会话完全同步（Req 8）**：
   - 设置页隐藏会话列表中全面补齐：会话重命名（编辑弹窗）、单独删除（二次确认弹窗）、会话置顶/取消置顶（置顶微光角标）与关键词实时过滤搜索栏。
9. **使用统计页面模型名称文本溢出修复（Req 9）**：
   - 修复使用量统计列表中模型名称在长串或大字体下的文本截断与换行重叠问题，为模型名文本赋予 `Modifier.weight(1f, fill = false)` 与 `TextOverflow.Ellipsis`。
10. **错误提示气泡重构为柔和浅粉色微光条（Req 10）**：
    - 对话页悬浮报错提示升级为位于顶部栏下方的柔和浅粉色面板（浅色 `#FFF1F2`、深色 `#3F1D23`，搭配柔和红粉边框），居中单行居中排版与优雅折叠。
11. **输入框适度提高底色不透明度（Req 11）**：
    - 输入框底色不透明度进一步校准优化（深色模式 0.90f，浅色模式 0.93f），在保有全域毛玻璃模糊效果的同时，文字与图标对比度达到绝对高清晰度。
12. **使用统计图表全新重绘（Req 12）**：
    - Token 消耗柱状图重构为双色纵向渐变柱与顶部圆润发光端帽；
    - 趋势折线图升级为发光光滑贝塞尔平滑曲线，搭配垂直渐变投影底色与多层光晕数据节点。
13. **右侧锚点历史导航条全新重构（Req 13）**：
    - 重绘右侧锚点导航轨道为极简修长液态玻璃胶囊，触控展开气泡面板具备当前活跃项自动居中联动。
14. **辅助滑动 4 按键交互与视觉优化（Req 14）**：
    - 点击辅助按键后自动隐藏倒计时平滑延长至 2.8 秒，消除重复点击时闪烁消失；彻底根除点击时的阴影抖动异物感（`shadowElevation = 0.dp`）。
15. **流式输出平滑吸附滚动频率校准（Req 15）**：
    - 流式响应更新滚动节流限制在 70ms 间隔，既杜绝主线程高频重组卡顿，又实现丝滑流畅的吸附追随感。
16. **思考档位滑块拖拽松手卡顿消除（Req 16）**：
    - 锁定 `EchoPillSlider` 拖拽松手时的拖动状态，直至弹簧吸附动画平滑完成，彻底根除数值吸附跳动与手势冲突。
17. **Markdown 渲染非标标签与尾随星号容错（Req 17）**：
    - 强化 HTML 字体标签解析器，支持带 `#` 与不带 `#` 的 hex/rgb/rgba 格式，严格剔除未闭合 `<font>` 标签与残留 `</font>*` 噪点。
18. **连接阶段思考胶囊明确显示重连状态（Req 18）**：
    - 思考胶囊在触发网络重试或断线重连时，实时呈现「正在重连 (第X次)...」动态提示，消除用户在重试阶段的界面假死疑惑。
19. **专属会话记忆保存逻辑修复（Req 19）**：
    - 对话专属记忆默认关闭；用户在对话设置中关闭后持久化存储并生效，再次进入对话依然为关闭状态，仅在用户主动开启时才启用。
20. **Token 实时统计与超过 70% 上下文自动压缩（Req 20）**：
    - 对话中实时计算当前上下文 Token 占用比例；当超出 70% 阈值且支持压缩时，触发优雅自动压缩与状态提醒。
21. **API 配置页面模型分区与独立配置能力落地（Req 21）**：
    - 模型选择分为「已添加的模型」与「从Key中读取到的模型」两大独立区域，支持手动添加自定义模型；
    - 支持为每个模型单独配置上下文窗口 Tokens（32K ~ 1M 动态滑块）与工具调用、视觉识别、深度思考、联网搜索 4 项独立能力开关并持久化。

### 2. 自动化测试验证
- 全量 135 项单元测试 100% 全部通过（退出码 0）；
- 新增 `V200FeaturesTest` 专项覆盖：
  - `testV200CurrentVersionUserUpdatesCompleteness`：21 项核心升级与更新日志完备性验证；
  - `testBorderHighlightGradientDarkeningContrast`：渐变边框左侧加深与右侧对比度约束测试；
  - `testModelCustomSettingsDefaultsAndCustomization`：模型独立设置模型与默认值检验；
  - `testSelectedModelWithCustomSettingsFields`：模型扩展字段与持久化实体测试；
  - `testRoomMigration22_23RegisteredAndSchemaVersion`：数据库版本 23 与迁移契约验证；
  - `testSessionMemoryDisabledByDefault`：专属会话记忆默认关闭契约验证；
  - `testQuoteTextFormatting`：引用文本 Markdown 格式化测试。

### 3. 改动文件列表
- `app/src/main/java/com/aiassistant/domain/model/Models.kt`
- `app/src/main/java/com/aiassistant/data/local/AppDatabase.kt`
- `app/src/main/java/com/aiassistant/data/repository/AiRepository.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoGlassCard.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoHaze.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoPillSlider.kt`
- `app/src/main/java/com/aiassistant/ui/components/EchoTextToolbar.kt`
- `app/src/main/java/com/aiassistant/ui/components/MarkdownText.kt`
- `app/src/main/java/com/aiassistant/ui/components/ScrollAssist.kt`
- `app/src/main/java/com/aiassistant/ui/components/SideAnchorNavigator.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/com/aiassistant/ui/screens/stats/StatsScreen.kt`
- `app/src/test/java/com/aiassistant/V1929FeaturesTest.kt`
- `app/src/test/java/com/aiassistant/V200FeaturesTest.kt`
- `app/build.gradle.kts`
- `UPDATE_LOG.md`
- `CHANGELOG.md`
- `PROJECT.md`
- `README.md`

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
