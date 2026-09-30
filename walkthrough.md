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

# Echo v2.5.9 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.5.9 (`versionCode: 155`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.5.9.apk`
- **核心修复需求**（UI 细节统一审查与修复）：
  1. 连接超时提示：10s 过早 → **30s**；位置从连接胶囊右侧 → **胶囊正下方**；
  2. 设置页「核心 / 智能」等分区标题字号过小 → 提升至可读尺寸；
  3. API 配置长按拖动交换 Key 动画生硬 → 平滑抬升/让位/归位；
  4. API 配置支持长按拖动模型，调整模型列表顺序（持久化 sortOrder）。

---

## 二、改动与构建前复核清单 (Pre-Build Review Checklist)
- [x] 1. 用户提出的全部需求点 100% 落实（4/4 + 同类延伸）；
- [x] 2. 检查代码语法与 Compose 闭包作用域无异常（`compileDebugKotlin` Exit Code 0）；
- [x] 3. `app/build.gradle.kts` 中 `splits.abi` 仅 `arm64-v8a`、`isUniversalApk = false`；`versionCode = 155`、`versionName = "2.5.9"`；
- [x] 4. 全量单元测试 `testDebugUnitTest`：**456** 项通过 / 0 失败 / 0 错误（Exit Code 0）；
- [x] 5. Release APK 增量输出至 `D:\Agent\APP-Echo\app\releases\Echo-v2.5.9.apk`，**未包含 `-arm64-v8a` 后缀**，且所有历史版本安装包永久完整保留（目录内共 **165** 个 APK，本次仅新增 1 个）。

---

## 三、逐项修复走查

### 1. 连接等待提示（ChatMessageComponents.kt）
| 项 | 修改前 | 修改后 |
| :--- | :--- | :--- |
| 出现阈值 | ≥ 8s | **≥ 30s** |
| 错误色升级 | ≥ 20s | **≥ 60s** |
| 布局位置 | 连接胶囊右侧（Row 内） | **胶囊正下方**（Column 包裹） |
| 宽度 | 随文字伸展 | 约束为胶囊 maxBubbleWidth |

### 2. 分区 / 表单标题可读性
| 位置 | 修改前 | 修改后 |
| :--- | :--- | :--- |
| `EchoSectionHeader`（核心/智能/数据） | labelMedium 12sp Medium | **titleSmall SemiBold 15sp** |
| `SettingsInputField` 标题 | labelMedium 12sp SemiBold | **titleSmall SemiBold 14sp** |
| `PlotActionBar` 分区标题 ×5 | labelMedium 12sp Bold | **titleSmall SemiBold 14sp** |
| 角色扮演引导步骤标题 | labelMedium | **titleSmall SemiBold** |

### 3. 拖拽动画（SmoothReorderState.kt）
- 拖起/放下：lift 0→1，tween 160ms FastOutSlowIn，平滑驱动 scale(1+0.018·lift) 与阴影；
- 相邻换位：tween 220ms（取代 LowBouncy 弹簧），无过冲；
- 松手归位：tween 200ms（取代 MediumBouncy）；
- 高度按 key 记录，条目高度不等时阈值仍正确；
- 动画 Job 取消重入，连滑不打架。

### 4. 模型拖拽排序（SettingsApiConfigDialog.kt）
- 「已配置模型」卡片左侧新增 `DragIndicator` 手柄；
- 相邻交换写回 `availableModels`，保存时经 `replaceSelectedModels` 写入 `sortOrder`；
- `cleanModelNames` / `parseModelList` 保序去重（回归测试覆盖）；
- 搜索过滤时禁用拖拽并提示「清空搜索后可调整顺序」。

---

## 四、测试与构建结果

| 命令 | 结果 | 退出码 | 关键输出 |
| :--- | :--- | :--- | :--- |
| `gradlew compileDebugKotlin` | 成功 | 0 | 仅历史 deprecation 警告 |
| `gradlew testDebugUnitTest` | 成功 | 0 | **456 passed / 0 failed** |
| `gradlew lintDebug` | 成功 | 0 | 见下方说明 |
| `gradlew assembleRelease` | 成功 | 0 | `app-arm64-v8a-release.apk` |
| `git diff --check` | 成功 | 0 | 仅 CRLF 换行提示，无空白错误 |

**新增测试** `UiPolishRegressionTest`（5 项）：
- `cleanModelNames_preservesCustomOrder` / `_dedupesWithoutReorder`
- `parseModelList_preservesJsonArrayOrder`
- `smoothReorder_liftAndKeyHeight` / `_swapAnimatesWithoutThrowing`

**Lint 说明**：Kotlin 2.2.21 产出 Metadata 2.2.0，超出当前 Compose Lint 内嵌 kotlinx-metadata-jvm（≤2.0.0）支持范围，`UnrememberedAnimatable`、`StateFlowValueCalledInComposition`、`CoroutineCreationDuringComposition` 等探测器在 `isComposable()` 解析时直接崩溃（工具链缺陷，旧代码同样触发）。已在 `app/lint.xml` 仅隔离该批崩溃探测器并注明根因；**其余全部 Lint 规则保持启用**。

---

## 五、APK 产物技术元数据

| 项目 | 参数 / 校验值 |
| :--- | :--- |
| **文件名称** | `Echo-v2.5.9.apk` |
| **绝对路径** | `D:\Agent\APP-Echo\app\releases\Echo-v2.5.9.apk` |
| **文件大小** | 16,651,117 字节 (约 15.88 MB) |
| **Package ID** | `com.aiassistant` |
| **Version Name** | `2.5.9` |
| **Version Code** | `155` |
| **Application Label** | `Echo` |
| **Target ABI** | `arm64-v8a` |
| **Min SDK / Target SDK** | `26` / `34` |
| **SHA-256 校验和** | `18C12B503F0F91603312230A876046C8983EB1379193367CDF2A02ED946B8908` |
| **签名机制** | APK Signature Scheme v2（`apksigner verify` 通过） |
| **签名证书 DN** | `C=US, O=Android, CN=Android Debug` |
| **签名证书指纹 (SHA-256)** | `93:96:38:f6:d3:e9:af:7f:8a:98:0e:62:af:52:d2:75:fe:e7:33:81:f2:13:0c:c4:e2:0a:0d:34:9f:98:e2:1f` |

> **签名说明**：使用项目内 `keystore/echo-release.jks`（alias `androiddebugkey`）签名。该证书 DN 为 Android Debug（与此前 v2.5.x 系列发布包指纹一致），**并非正式生产上传密钥**。`apksigner verify --print-certs` 已确认签名有效。

---

## 六、Git 状态
本次改动 7 个源文件 + 2 个新增文件 + 版本/文档同步，**尚未提交**（待用户确认后提交）。

### 修改文件
1. `app/build.gradle.kts`（versionCode 155 / versionName 2.5.9）
2. `app/src/main/java/com/aiassistant/ui/screens/chat/ChatMessageComponents.kt`
3. `app/src/main/java/com/aiassistant/ui/components/EchoSectionHeader.kt`
4. `app/src/main/java/com/aiassistant/ui/components/SmoothReorderState.kt`
5. `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsApiConfigDialog.kt`
6. `app/src/main/java/com/aiassistant/ui/screens/settings/SettingsScreen.kt`
7. `app/src/main/java/com/aiassistant/ui/screens/roleplay/PlotActionBar.kt`
8. `app/src/main/java/com/aiassistant/ui/screens/roleplay/RoleplayStudioScreen.kt`
9. `README.md` / `PROJECT.md` / `CHANGELOG.md` / `UPDATE_LOG.md` / `WORKFLOW_GUIDELINES.md`

### 新增文件
1. `app/lint.xml`
2. `app/src/test/java/com/aiassistant/UiPolishRegressionTest.kt`

---

## 七、剩余风险
1. **真机手感**：拖拽抬升/换位节奏（160/220/200ms）为代码层设计值，未在真机做触感走查，如有偏好可再调 `SmoothReorderState` 常量。
2. **Lint 工具链**：Compose Lint 元数据崩溃需升级 AGP / Compose Lint 依赖才能根治；当前以隔离探测器方式保证 lintDebug 可运行。
3. **安装验证**：当前环境无连接的 Android 真机/模拟器，**未执行安装与启动验证**。人工验收步骤：
   - 将 `D:\Agent\APP-Echo\app\releases\Echo-v2.5.9.apk` 传至设备安装；
   - 打开设置 → 确认「核心 / 智能 / 数据」标题字号清晰；
   - 进入 API 配置 → 拖动 Key 与模型手柄，确认动画顺滑、顺序可持久化；
   - 发起一次对话，确认连接 30 秒后才出现「连接时间较长」提示且位于胶囊下方。
