# Echo AI 助手更新日志 (Update Log)

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
