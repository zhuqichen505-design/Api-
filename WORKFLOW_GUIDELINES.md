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

### [2026-10-02] v2.7.6 胶囊对齐修正、流式动画上移同行、输入框误展开与光标不可见修复、点击定位分级、编辑器跳顶根治（发版）
- **需求**：① 胶囊右侧对齐输入气泡右侧（此前被改为对齐右侧头像）；② 流式点状动画太小且连接时离胶囊太远，应上移与"连接时间长"提醒同行；③ 无要求情况下输入框被错误修改——点击默认展开、光标不可见；④ 点击输入框最右侧光标跳到下一行起始；⑤ 编辑回复/系统提示词点击中间仍跳开头（此前修复未生效）；⑥ 胶囊展开/收起时内部文字图案大小位置变化。构建 APK。
- **版本**：versionCode 172 / versionName 2.7.6 / Room v32（无 DB 变更）。
- **实现要点**：
  ① 胶囊 `weight(1f)` 恒定满宽 + 右缩 44dp（头像 36+间距 8）——右缘回到用户输入气泡右缘（v2.7.4 误按底部输入栏口径实现，实为对齐右侧头像）；
  ② 无正文阶段（连接/重连/思考）动画与连接提醒同行渲染、紧贴胶囊下方；槽位可见条件扩为 `isGenerating && content.isBlank()`；TypingIndicator 44×20dp / 点半径 4dp；流式期间仍随内容末尾；
  ③ 手柄不可见热区 36dp→24dp（点击输入框首行右端误触展开的根因）；自绘光标补 2dp 宽度（`getCursorRect` 恒返回零宽矩形，1.6.8 源码实锤——v2.7.4 起光标从未可见）；
  ④ 观察型 pointerInput 记录点击 x：软换行边界按点击位置分级绘制（右侧空白→下一行行首；文字上→行尾），显式换行行尾 400ms 窗口内校正光标至 `\n` 后；
  ⑤ 编辑器门禁改钳制型（1.6.8 源码实锤：`BringIntoViewResponderNode` 无条件向父级转发请求，no-op 门禁无效；FocusableNode 获焦请求"整个字段矩形"入视口→跳顶）——`calculateRectForParent` 把请求矩形钳入可视窗口，请求就地终结；
  ⑥ 胶囊内层对齐恒定 CenterVertically，展开/收起不再引起内部位移。
- **文件**：`ChatMessageComponents.kt`、`ChatInputComponents.kt`、`EchoScrollableTextEditor.kt`、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 503 项全通）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.7.6.apk`（16,716,653 字节，SHA256 `248192473B8CEF395132B87DD505ECB6D25D8CCAC0E61D75A4ACCA4E85826CFC`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留（共 182 个）。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.7.6 节）。
- **风险**：显式换行校正有 400ms 新鲜度窗口（极慢设备可能保持内置行尾落点）；手柄拖拽起点略收窄（24dp）；钳制门禁在视口未就绪时退化为不钳制。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-02] v2.7.5 流式动画持续至回复结束、底部距离稳定、编辑器光标跟随修复（发版）
- **需求**：① 流式输出的动画到回复结束才消失（v2.7.2 未按此语义实现）；② 流式动画与底部距离错误变化；③ 系统提示词/编辑回复文本框点击仍强制滑到内容顶端且内容无法跟随光标。构建 APK。
- **版本**：versionCode 171 / versionName 2.7.5 / Room v32（无 DB 变更）。
- **实现要点**：
  ① 确认"流式输出的动画"=呼吸光环点（v2.7.2 误按流式光标实现）；光环点改为持续整个生成过程——正文空白时位于内容起点、流式期间跟随内容末尾（末行下方 6dp）、回复结束消失；移除 v2.7.2 胶囊行下方独立节点；
  ② 动画随内容末尾 + 列表钉底跟随 → 流式期间与屏幕底部距离稳定；
  ③ `EchoScrollableTextEditor`：no-op `BringIntoViewResponder` 门禁拦截内建请求（javap 核实 foundation 1.6.8 签名 `bringChildIntoView(localRect: () -> Rect?)`），光标可见性由 `LaunchedEffect(选区/布局/内容高度)` 自行接管——光标行移出可视区才以最小距离滚动（4dp 余量），点击可视区内绝不滚动。
- **文件**：`ChatMessageComponents.kt`、`EchoScrollableTextEditor.kt`、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 503 项全通）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.7.5.apk`（16,716,653 字节，SHA256 `051817DF7BFFECEDB658E892C4C98A5C8F3CFAEE9D747EFB40B5877DB3E8951A`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留（共 182 个）。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.7.5 节）。
- **风险**：思考阶段动画位于思考面板下方（v2.7.2 的距头像统一契约由"随内容末尾"取代）；门禁隔离后跟随基于最近一次布局（误差≤一行）。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-02] v2.7.4 胶囊恒定满宽对齐输入气泡、输入框留白对称与行尾光标、发送收起键盘（发版）
- **需求**：① 思考/连接胶囊大小始终与输入气泡右侧对齐（此前依旧会变），展开/收缩键放胶囊最右侧，文字未填满时观感优化；② 输入框跨行时光标无法放在行尾最后一个字后边；③ 输入框文字区右侧空白太多、左右留白不对称，按左侧留空宽度修复；④ 发送后焦点消失（键盘收起）。构建 APK。
- **版本**：versionCode 170 / versionName 2.7.4 / Room v32（无 DB 变更）。
- **实现要点**：
  ① 胶囊无条件 `weight(1f)` 占满行宽（连接/思考/流式/完成恒定）；列表 contentPadding 水平 14→12dp 与输入栏统一、助手外层 Column `start=4/end=0`——右缘与输入气泡右缘精确对齐；
  ② 胶囊文本 Box `weight(1f, fill=false)`→`weight(1f)`，展开/收缩键贴最右；
  ③ 输入框 decorationBox 移除 28dp 右距，左右留白统一 4dp；
  ④ 输入框改 TextFieldValue（对外 String API）+ 自绘 `InputCursorOverlay`：cursorBrush 透明隐藏内置光标，软换行边界偏移绘制在前一行行尾、垂直居中，选区展开不绘制，reduced motion 恒亮；状态声明在 AnimatedContent 之外防丢失；
  ⑤ 两处发送按钮（直接/排队）发送前 `focusManager.clearFocus()` 收起键盘。
- **文件**：`ChatMessageComponents.kt`、`ChatScreen.kt`、`ChatInputComponents.kt`、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 503 项全通）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.7.4.apk`（16,716,653 字节，SHA256 `5632B80F8B32F0EDC2A12247E5D5B44FD5272BEA68BE34EEB5B27232215AAC84`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留（共 181 个）。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.7.4 节）。
- **风险**：硬换行（回车）后光标仍在新行行首（常规习惯）；首行行尾与右上角手柄触控区重叠为历史既有行为。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-02] v2.7.3 胶囊对齐口径修正 + 全项目流畅度与稳定性专项治理（发版）
- **需求**：① 修正前版需求 5 口径——胶囊右侧与用户输入气泡右侧对齐；② 遍历整个项目，在不影响功能的前提下优化流畅度与稳定性；③ 构建 APK。用户特别提醒：审计结论仅供参考，以逐条核查与项目实际为准。
- **版本**：versionCode 169 / versionName 2.7.3 / Room v32（无 DB 变更）。
- **核查原则落地**：三份并行审计仅作线索，逐条打开文件核实调用链后实施；否决 1 条误报（Int 溢出实算不成立）；跳过 3 项中风险（气泡 BoxWithConstraints 重构、恢复回滚原子性、冷启动全异步恢复）并留痕；纯函数优化配套等价性单测，测试先后捕获实现层两处边界缺陷后修复。
- **实现要点**：
  ① 胶囊生成期 `weight(1f)+padding(end=40dp)` 与用户气泡右缘精确对齐（44−4=40dp）；
  ② `ChatViewModel` 消息订阅单例 Job 去重（16 处调用叠加订阅是长会话越用越卡残留根因）+ 草稿/检查点读取 IO 异步化；`RoleplayViewModel` 四处订阅叠加取消旧 Job；
  ③ 备份链路（单对话备份 runBlocking/设置页 5 处/隐藏对话页）与角色卡 TXT 导入全部迁 IO 调度；
  ④ `BackgroundImageManager`/`AvatarManager`/`ChatAvatar` 自定义头像三级位图缓存（lastModified 或时间戳文件名保证失效正确）；
  ⑤ `ChatScreen` 流式跟随改 `LaunchedEffect(isGenerating)+snapshotFlow`（原 key 每 token 全页重组）；跳转按钮 visible 改 `derivedStateOf`；
  ⑥ Markdown 热路径：`MarkdownSegmentationCache` 增量分段（等价性单测）、13 个正则常量化、LaTeX 符号表常量预排序、引用角标有界前瞻、`isErrorMessage` 记忆化、删除 `isThinkingEnglish` 死计算；
  ⑦ `BackupManager` Zip-Slip 防护（canonicalPath 校验）；`AiAssistantApp` 自动备份延后至 DB 初始化成功后。
- **文件**：16 个 Kotlin 源文件 + `MotionRoundTests.kt`（新增等价性用例）+ `app/build.gradle.kts` + 文档。
- **验证**：compile/test（74 文件 503 项全通、0 失败）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.7.3.apk`（16,716,653 字节，SHA256 `F3C29DB6AD16AB02DAA02E35D62ACEA81BA45E69731B1AF673F86815595942F9`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留（共 180 个）。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.7.3 节）。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-02] v2.7.2 编辑框跳转末尾按钮与滚动滑块、输入跳顶修复、胶囊圆环全程化与尺寸统一、流式光标持续与对齐修正（发版）
- **需求**：① 编辑模型回复左下角加向下按钮，点击光标锁定文字末尾；② 编辑模型回复与输入系统提示词等大文本内容框右侧统一加滑块；③ 修复此类内容框中部输入时页面跳回文字顶端；④ 胶囊左侧从连接到回复结束显示圆环动效，回复完毕显示静态圆环；⑤ 连接/思考（结束）胶囊尺寸统一，右侧与用户输入气泡左侧对齐；⑥ 流式输出动画持续到回复完毕；⑦ 连接/思考时流式输出动画与头像距离统一为较近者；⑧ 流式光标远离文字一点并垂直对齐。构建 APK。
- **版本**：versionCode 168 / versionName 2.7.2 / Room v32（无 DB 变更）。
- **实现要点**：
  ① 新建 `EchoScrollableTextEditor.kt`（`BasicTextField` + 自管 `verticalScroll` + 自绘 `EchoVerticalScrollSlider` 滑块，溢出淡入/拖动点按定位/隐藏占位不跳变）；编辑回复/仅修改内容对话框（`ChatScreen.kt`）与系统提示词对话框（`ChatPromptDialogs.kt`）统一迁移，编辑状态 `String` → `TextFieldValue`；
  ② 编辑模型回复对话框按钮行最左新增 `ArrowDownward`：selection 置 `TextRange(text.length)` + `animateScrollTo(maxValue)`；"仅修改消息内容"同步；
  ③ 跳顶根因 = `OutlinedTextField` 高度截断后内部滚动重置；新组件文本无高度约束整体布局、外层滚动经 bringIntoView 跟随光标，最小距离滚动；
  ④ 胶囊图标槽扩至 Streaming 均显示 `EchoPulseRing` 动效；`EchoPulseRing` 新增 `animated` 参数，Idle 静态圆环（不建动画实例），颜色与生成期同源；
  ⑤ 生成期胶囊 `weight(1f)` 占满头像行剩余宽度（右侧对齐用户气泡列内容边界），落库后恢复自适应；
  ⑥ 光标穿透标题/列表/引用/参考资料末块（`isLastLine`），表格/数学/围栏/分割线/空尾补独立行光标 `StreamingTailCursor`（`tailNeedsStandaloneCursor`），围栏刚开启同样补；
  ⑦ 呼吸光环点移至胶囊行正下方（start 4 / top 10 / bottom 4dp），复现连接态原始间距；
  ⑧ 光标几何常量顶层化 + `echoCursorLineTransform`：`lineRight + 2dp` 外移、末行行盒内垂直居中，正文与围栏光标共用。
- **文件**：新增 `EchoScrollableTextEditor.kt`；修改 `EchoConnectionIndicator.kt`、`MarkdownText.kt`、`ChatMessageComponents.kt`、`ChatScreen.kt`、`ChatPromptDialogs.kt`、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 502 项全通、0 失败）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.7.2.apk`（16,700,269 字节，SHA256 `3EBE68CE1D57B5E78E6E1014C4954319C970FFA223D85CA7F9DD48E14EF0311C`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留（构建前共 178 个）。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.7.2 节）。
- **风险与口径**：需求⑤"与用户输入气泡左侧对齐"按"消息列内容边界（用户气泡列右基准）"恒定宽度实现，若需按触发轮用户气泡左缘动态对齐可再迭代；详见 walkthrough v2.7.2 剩余风险。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-02] v2.7.1 时间线提示自动消失、流式动画防抖、消息仅编辑、时间线时间理解与过度推进治理、旧报错残留修复（发版）
- **需求**：① 时间线变动提示弹窗显示几秒后自动消失；② 流式输出动画与思考胶囊有时突然变大/变小造成屏幕错误滑动一小段；③ 用户消息"仅编辑"（只改显示内容不重新提问）；④ 时间线优化（a 有完整时间线仍时间记忆错误；b 模型过度执着推进时间）；⑤（过程中追加）报错后重新生成/重发直接弹出过往报错。构建 APK。
- **版本**：versionCode 167 / versionName 2.7.1 / Room v32（无 DB 变更）。
- **实现要点**：
  ① `ChatScreen.kt` 对 `timelineUpdateNotice` 加 `LaunchedEffect` 5 秒自动消失；
  ② 胶囊 `AnimatedContent` 显式 `SizeTransform { _,_ -> snap() }` 禁用内层尺寸动画（只留外层 `animateContentSize`）；连接提示槽改 `AnimatedVisibility(fade+expand/shrink)` 平滑出入场；滚动钉底逻辑未动；
  ③ `MessageBubble`/`MessageFooter` 新增 `onEditInPlace` + 用户菜单"仅修改内容"项 + `ChatScreen` 编辑对话框；`editAssistantMessage` 更名 `updateMessageContent`（仅改 content，与角色无关）；
  ④ `RoleplayRepository` 修复把 `currentPlotSummary` 当故事时间注入的真 bug（改 `conversation.currentStoryTime` → 会话记忆回退）；两处注入 prompt 新增【时间记忆权威声明】并把"主动推进"收敛为"默认守时 + 明确描写才顺延 + 单轮至多一个相邻时段"；评估 prompt 新增【默认守时与单步推进铁律】；`TimelineMemoryHelper` 新增 `hasExplicitTimePassageDescription`/`isUnreasonableStoryTimeJump`，`AiRepository` 增设第二道守卫，本地兜底 `detectAutoStoryTimeAdvancement` 多步顺延需描写依据；
  ⑤ `ChatViewModel` 五个流式回调补 `isCurrentSession` 守卫、启动块显式清 `_reconnectStatus` 并 `cancelActiveRequest` 取消残留调用、四处 `saveErrorReply` 调用点补 `isStillCurrentRound` 守卫。
- **文件**：`ChatScreen.kt`、`ChatMessageComponents.kt`、`ChatViewModel.kt`、`TimelineMemoryHelper.kt`、`AiRepository.kt`、`RoleplayRepository.kt`、`TimelineNaturalTimeTest.kt`（+3 项）、`TimelineRefinementAndCompressionTest.kt`（防停滞断言随指令收敛同步更新）、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 502 项全通、0 失败）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.7.1.apk`（16,700,269 字节，SHA256 `89ACABC79B0483312FFE8ADFA63FD512CFBFB4D7FEC6581AC76C9FA66862FA9A`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留（共 178 个）。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.7.1 节）。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] v2.7.0 紧急修复：消除卡顿与闪退、彻底根除消息流自激死循环、菜单测量与冷启动深度性能优化（发版）
- **需求**：更新新版本（v2.6.9）后软件非常卡顿，还会出现闪退现象。修复并构建 APK。
- **版本**：versionCode 166 / versionName 2.7.0 / Room v32（无 DB 变更）。
- **实现要点**：
  ① 彻底移除 `updateMessageModelMap` 遍历过程中的所有写库操作，其唯一职责严格限制为更新内存中的 `_messageModelMap` 状态供 UI 渲染；
  ② 增加 Fast-path 检查：若所有 assistant 消息已解析或已在内存 Map 中，直接快速返回，0 协程开销，0 数据库查询开销，打字与流式生成彻底恢复丝滑；
  ③ 引入 `hasBackfilledHistoricalModelNames`（`AtomicBoolean`）单例保护机制：仅在进入会话时由独立后台协程静默执行**至多一次**历史旧消息的持久化回填，执行完毕后标志恒为 true，彻底切断 `Flow 监听 -> 写库 -> InvalidationTracker -> 重新发射` 的死循环链条；
  ④ `EchoHaze.kt` 外层 `Surface` 恢复只接收调用方传入的 `modifier`；仅在内层 `Column` 遵循 Material 3 官方推荐规范施加单层 `.widthIn(min = 160.dp, max = 280.dp).width(IntrinsicSize.Max)`，既维持回复三点菜单美观自适应不撑满全屏，又杜绝多次遍历卡顿与测量崩溃；
  ⑤ `AiAssistantApp.kt` 冷启动自动备份移入 `applicationScope.launch(Dispatchers.IO)` 异步执行，主线程零阻塞，冷启动秒开；`BackupManager.kt` 中的 WAL Checkpoint 补充 `use { it.moveToFirst() }` 确保游标安全执行与关闭。
- **文件**：`ChatViewModel.kt`、`EchoHaze.kt`、`AiAssistantApp.kt`、`BackupManager.kt`、`MemoryAndCompressionEngineTest.kt`、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 499 项全通、0 失败）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.7.0.apk`（16,700,269 字节，SHA256 `EA7E5035EB3A6C6C3832E110C51E9FD08E5757A30337AE9750B83F2D43BA9034`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留（共 177 个）。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.7.0 节）。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

### [2026-10-01] v2.6.9 会话专属设定备份与智能导入合并 + 菜单左右填铺满统一修复 + 输出中断思考与回复强制入库保全 + 思考胶囊文字垂直居中 + 思考胶囊模型名字物理固化（发版）
- **需求**：① 备份只备份时间线未备份会话专属设定，恢复后缺少专属设定且被视为新对话无法导入原有对话；② 点击回复下方三点弹出菜单左右填铺满统一修复；③ 模型已输出思考或部分回复时中断保全入库严禁覆盖；④ 思考胶囊文字垂直居中；⑤ 思考胶囊模型名字物理固化与隔离。构建 APK。
- **版本**：versionCode 165 / versionName 2.6.9 / Room v32（新增 MIGRATION_31_32，`messages` 表增加 `modelName TEXT`）。
- **实现要点**：
  ① 单对话恢复 `restoreSingleConversationFromJson` 引入原有会话智能匹配机制（ID/创建时间戳/自定义标题一致），命中原有会话时直接作为目标会话（`targetConvId = matchedConv.id`），智能更新会话设定并开启专属设定总开关；消息、时间线节点及专属设定（`memory_items`）执行内容去重增量合并入库，绝不强制新建新对话；`mergeDatabaseFromBackup` 专有重映射 `memory_items` 和 `conversation_branches` 的会话 ID 并去重；`MemoryDao` 拓宽查询和删除范围至全部 `WHERE conversationId = :conversationId`；`createBackup` 前显式执行 `PRAGMA wal_checkpoint(FULL)`；
  ② `EchoHaze.kt` 的 `EchoGlassDropdownMenu` 显式施加 `Modifier.width(IntrinsicSize.Max).widthIn(min = 160.dp, max = 280.dp)`，全仓所有液态玻璃菜单恢复自适应精致尺寸；
  ③ `ChatViewModel.kt` 的 `onError` 与 `catch` 异常块增加 CAS 抢救入库保全机制：检测只要 `partialResponse.isNotBlank() || partialThinking != null`，不论异常类型，第一时间抢救保存思考链与正文内容入库，附带中断说明，严禁任何覆盖与清空；
  ④ `ChatMessageComponents.kt` 思考胶囊 Row 单行改 `Alignment.CenterVertically`，Box 显式 `Alignment.CenterStart`，`Text` 注入 `lineHeight = 16.sp` 与 `LineHeightStyle(Alignment.Center, Trim.Both)`，精准物理垂直居中；
  ⑤ `Message` 实体永久持久化 `modelName` 字段，存库与保存错误回复/停止生成时均锁定模型名，`updateMessageModelMap` 存量反查自愈回填数据库，`ChatScreen.kt` 思考胶囊展示彻底移除回退到 `currentAssistantModelName`。
- **文件**：`AppDatabase.kt`、`Daos.kt`、`AppDatabaseMigrations.kt`、`AiRepository.kt`、`Models.kt`、`EchoHaze.kt`、`ChatMessageComponents.kt`、`ChatScreen.kt`、`ChatViewModel.kt`、`BackupManager.kt`、`CompressionTierPolicyTest.kt`、`MemoryAndCompressionEngineTest.kt`、`app/build.gradle.kts` 及文档。
- **验证**：compile/test（74 文件 498 项全通、0 失败）/lint/diff --check 全部 Exit Code 0；assembleRelease Exit Code 0，`Echo-v2.6.9.apk`（16,700,269 字节，SHA256 `5212A9D58FAEC29B491EBBCA18D66A5E600BF85781163153FC0212875DB001D5`，CN=Android Debug **非正式生产签名**，证书与历史版本一致）；历史包 100% 完整保留。
- **未执行**：真机安装/启动验证（无设备），已给人工验收步骤（见 walkthrough.md v2.6.9 节）。
- **详情**：见 `UPDATE_LOG.md` 与 `walkthrough.md`。

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
