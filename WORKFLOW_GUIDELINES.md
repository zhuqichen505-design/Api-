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
