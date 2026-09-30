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
