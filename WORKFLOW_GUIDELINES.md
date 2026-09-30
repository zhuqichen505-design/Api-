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
