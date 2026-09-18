@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.aiassistant.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import com.aiassistant.domain.model.ChatModelOption
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiassistant.AiAssistantApp
import com.aiassistant.BuildConfig
import com.aiassistant.R
import com.aiassistant.data.repository.AiRepository
import com.aiassistant.domain.model.ApiConfig
import com.aiassistant.domain.model.NamedApiKey
import com.aiassistant.domain.model.Conversation
import com.aiassistant.domain.model.EnvironmentVariable
import com.aiassistant.domain.model.MemoryItem
import com.aiassistant.domain.model.WorldBook
import com.aiassistant.domain.model.WorldBookEntry
import com.aiassistant.domain.model.ModelCapabilityEngine
import com.aiassistant.domain.model.ModelCustomSettings
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.ui.components.EchoGlassCard
import com.aiassistant.ui.components.EchoGlassDialog
import com.aiassistant.ui.components.EchoGlassDropdownMenu
import com.aiassistant.ui.components.readableTextColorFor
import com.aiassistant.ui.components.rememberReadableBackdropColors
import com.aiassistant.ui.components.echoFilterChipBorder
import com.aiassistant.ui.components.echoFilterChipColors
import com.aiassistant.ui.components.echoFilterChipElevation
import com.aiassistant.ui.components.echoGlassPalette
import com.aiassistant.ui.components.echoSegmentedButtonBorder
import com.aiassistant.ui.components.echoSegmentedButtonColors
import com.aiassistant.ui.components.echoHazePanel
import com.aiassistant.ui.components.echoHazeSource
import com.aiassistant.ui.components.echoShapeClick
import com.aiassistant.ui.components.rememberEchoHazeState
import com.aiassistant.ui.components.rememberSmoothReorderState
import com.aiassistant.ui.components.reorderItem
import com.aiassistant.ui.components.reorderDragHandle
import com.aiassistant.utils.AvatarManager
import com.aiassistant.utils.BackgroundImageManager
import com.aiassistant.utils.BackupManager
import com.aiassistant.utils.HiddenConversationLock
import com.aiassistant.utils.TavilySearchSettings
import com.aiassistant.utils.AppThemeMode
import com.aiassistant.tools.search.SearchEngineType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aiassistant.tools.cloud.OpenMeteoWeatherEngine
import java.text.SimpleDateFormat
import java.util.*

internal val CurrentFeatureHighlights = listOf(
    "沉浸式角色扮演工作室与剧情自由创作",
    "对话与故事长记忆严格隔离与全功能管理",
    "合并一体化个性化与全局设定管理面板",
    "多角色设定与世界观一站式管理及双向同步",
    "AI 智能识别、提炼追加与已有设定精准融入",
    "剧情动作指令库（推进、改写、分支、摘要等）",
    "多模型 API 与流式对话及深度思考推理",
    "Token 预算上下文、滚动摘要与长记忆库",
    "上下文使用情况查看与主动压缩",
    "全量数据备份与安全加密恢复",
    "全界面 Echo 液态玻璃设计与暗色主题适配"
)

internal val V205UserUpdates = listOf(
    "优化备份导入逻辑：重构为非破坏性增量合并引擎，导入备份时绝不删除本地已有但备份中未包含的对话",
    "新增复制对话功能：支持深拷贝整个会话生成全新独立对话，全量复制消息、高级参数、角色卡设定与会话专属记忆",
    "复制对话入口对齐：普通对话与隐藏对话均在「置顶」同一层级提供「复制对话」入口，体验 100% 同步对齐",
    "复制隐藏对话特性同步：复制隐藏对话自动继承隐藏标签与隐私安全属性，即刻出现在隐藏会话列表中",
    "新增单对话备份功能：支持将单个会话及其完整消息、角色扮演与专属记忆导出为独立备份，入口在「置顶」同一层级",
    "单对话备份导入隔离：导入单对话备份时仅新增或更新该对话，对其余所有会话 100% 隔离，绝无覆盖消失风险",
    "备份管理卡片增强：自动区分展示全量备份（ZIP）与单对话备份（JSON），提供针对性增量恢复安全提示",
    "分支功能原子事务、生成成功确认弹窗与隐藏会话密码维持特性完美保持"
)

internal val V220UserUpdates = listOf(
    "API 设置拖拽阴影圆角统一：长按切换 Key 优先级时的投影阴影完全贴合 10dp 卡片圆角，彻底消除直角矩形割裂感",
    "API Key 命名同行紧凑排版：Key 命名直接在 Key 标识右侧同行展示，删除括号内冗余说明，界面垂直空间大幅精简",
    "思考与连接状态气泡完整查看：支持左右手势平滑滑动与点击多行展开/折叠，模型连接与复杂报错信息一览无余",
    "记忆提取完整性与废话剥离：解除字数过短截断限制，支持 30~80 字完整主谓宾陈述句，循环清洗分析型前缀与语气废话",
    "绝对行为约束与负向禁令生效：识别不允许/严禁/禁忌称谓，无条件置顶注入最高优先级系统指令，彻底根绝禁令失效顽疾",
    "多轮上下文记忆保留大幅提升：近期完整对话保留预算扩展至 16000 tokens，杜绝 2~3 轮对话后过早摘要与细节遗忘",
    "提示词输入框视口防抖与全平台思考参数透传：输入框获取焦点平稳无跳跃；OpenAI/Claude 3.7/DeepSeek/中转思考参数精准透传"
)

internal val V219UserUpdates = listOf(
    "12 项全项目专项需求深度核验与精细落地：全面复核时间跨度敏锐度、按键圆角防割裂、专属记忆排版、对话设置层级、长文本防折行、会话头像隔离、标签同排对齐、标准滚动摘要与模型回复可编辑",
    "全局图片手势裁剪与编辑统一闭环：全面接入 ImageCropEditDialog，用户头像与 API 模型头像支持圆形裁剪，首页壁纸与对话壁纸支持矩形裁剪",
    "手势微调自由操控：支持双指平滑放缩、单指平移拖拽、90° 顺时针旋转与水平/垂直镜像翻转，实时生成高保真预览",
    "记忆提炼辅助模型层级归一与自由直选：彻底剥离旧式下拉选单，统一移入「模型辅助与思考」选项卡，消除设置层级混乱",
    "跨服务商自由直选：采用 UniversalModelPickerCard 跨服务商自由直选所有模型，免去二级下拉切换",
    "即时测试连接与平滑降级验证：保留即时测试连接验证弹窗与自定义提炼提示词延迟防抖自动保存"
)

internal val V218UserUpdates = listOf(
    "记忆提取完整性与相关性重构：彻底剥离思考流（<think>标签及未闭合思维链），排除URL/Markdown语法碎片，严格校验记忆内容与原文语义相关性，并放宽提取Token上限至512，杜绝残缺断句",
    "多 API Key 自动故障转移：同一配置下填写多个 Key 时，遇到网络波动、连接超时或 4xx/5xx/内联 error 报错时透明无感尝试备用 Key，并自动剔除命名标签",
    "模型输出工具栏二级菜单收纳：新增更多操作（MoreVert）液态玻璃菜单，优雅收纳重新编辑、固定/取消固定到上下文、排除/恢复及删除，外层工具栏极致轻盈",
    "API Key 自定义命名与备注区分：新增独立 Key 卡片化录入，支持对不同供应商或额度的 Key 自定义备注命名（支持 [名称] Key 与 名称:::Key 格式），设置页一目了然",
    "滚动摘要提醒逻辑深度修复：彻底根除无上下文压力时的“有较早信息尚未进入摘要”虚假红点与提示，仅在高上下文压力或溢出阈值时提示滚动摘要",
    "核心巨型文件工程级模块化拆分：将 SettingsScreen 与 ChatScreen 近 20,000 行巨型单体文件按业务职能高内聚拆分为聚焦子模块，架构清晰稳健，零功能回归"
)

internal val V217UserUpdates = listOf(
    "时间线敏锐度与时空跳转优化：全面增强时空跳转与自然叙事敏锐度",
    "全局按键阴影与圆角几何轮廓统一：统一按键阴影与圆角几何轮廓",
    "专属记忆与时间线排版重构：优化记忆与时间线排版",
    "对话设置功能层级重整：重组对话设置功能层级",
    "全能图片裁剪与编辑系统：引入自由裁剪与旋转",
    "专属记忆列表支持折叠展开：支持长列表优雅折叠展开",
    "记忆提炼辅助模型层级归一：模型辅助与思考设置体验整合",
    "排版防折行与长文本全局展开收起：支持防折行与 ExpandableText",
    "会话级自定义模型头像隔离：支持会话级自定义模型头像隔离",
    "对话设置模型能力标签同排展示：支持能力标签同排展示",
    "标准上下文压缩体系：基于 ConversationSummaryBufferMemory 与缓冲区预警",
    "模型回复（Assistant）支持编辑：模型回复支持手动重新编辑"
)

internal val V216UserUpdates = listOf(
    "600s 充足模型响应超时放宽：针对长篇小说全文通读与深度分析耗时较长的客观规律，大幅放宽长推理 OkHttp 超时至 600 秒（10分钟），增加连接重试，彻底消除严苛过早超时导致的降级",
    "文学叙事与自然时间跨度深度支持：告别死板硬套‘具体天数’，全面拥抱‘两周过后’‘暑假开始’‘三年后·春’等文学自然时间跨度与阶段节点，保持叙事原貌",
    "自然叙事与单调推进智能融合：跨度跳跃（如两周后、一个月后）智能估算推进底层时序步进，有效防止时序倒流的同时完整保留真实叙事时间标签",
    "5 大核心剧情里程碑维度提炼：全面覆盖剧情重大转折与抉择、感情线与人际质变、秘密揭露与重要发现、状态转变与阶段成果、未决悬念与核心伏笔，脉络充实饱满",
    "6 维常驻与多维设定深度挖掘：敏锐捕捉角色特质与心结、习惯偏好与小动作、生理特征与禁忌、世界规则与法则限制、人际羁绊与誓言契约、专属信物与特殊器物",
    "大模型输出扩充至 8192 Tokens：深度分析支持高达 8192 Tokens 超长结构化输出，长篇大作 30+ 关键事件与丰富多维设定完整呈现，绝不截断",
    "超时异常精准语义反馈与重试支持：异常卡片智能识别超时错误并提供清晰指引，固有设定专属分类与工作台筛选体验持续稳定保持"
)

internal val V215UserUpdates = listOf(
    "180s 深度大模型长文推理接入：重构长推理 OkHttp 客户端，超时上限提升至 180 秒，彻底解决 30 秒超时导致大模型提炼静默降级为本地切片的严重问题",
    "固有设定独立分类与专属筛选：时间线工作台新增「💡 固有设定」顶级分类，点击可专项集中查看、独立管理与一键同步所有固有设定",
    "固有设定原子化提纯铁律：提示词严令提炼 8~25 字高度概括的原子化约束事实，严禁直接抓取小说中包含‘习惯’‘规则’的大段文学描写长句",
    "长篇编年史里程碑事件法则：拒绝零碎时间词与动词切片流水账，以完整事实（主谓宾清晰、何时何地发生何转折）梳理故事发展编年史脉络",
    "大模型提炼透明状态指示条：提炼完成后顶部显示 AI 深度提炼专属状态与所用模型，如发生降级显式展示原因与重试入口",
    "聊天窗口活动模型动态智能绑定：时间线梳理优先复用当前聊天窗口选中的活跃大模型，支持自动多级降级安全保护",
    "时间输入框、时序单调状态机与 Echo 胶囊 Dock 特性稳定保持"
)

internal val V214UserUpdates = listOf(
    "时间输入框全面可用：重构文本输入组件与 Compose 响应状态机，彻底消除预览文字被吞与无法打字故障",
    "时序单向递增状态机：解决在‘第2天’剧情后后续‘第二天早上/次日’被错误倒流识别为第2天的逻辑缺陷，单调累加至第3天、第4天",
    "放开时间轴捕捉上限：取消历史消息截断限制，全量通读百轮对话历史并扩充模型输出上限至 4096 Tokens，脉络完整连贯",
    "编剧写作指导深度脱敏：严格解耦并脱敏 [] 与 【】 中括号导演指令，深度结合正文事实生成客观陈述句，严禁照抄指令原词",
    "固有设定 6 维敏锐挖掘：深入提炼生理禁忌、习惯嗜好、身份过往、世界规则、人际羁绊与言语风格，大幅提升敏感度与精准度",
    "底栏重构为 Echo 胶囊 Dock：重构保存同步按键为立体液态玻璃渐变光泽胶囊，优化统计指示微徽章与文字排版",
    "开源前沿记忆体系与平滑拖拽重排动画特性稳定保持"
)

internal val V213UserUpdates = listOf(
    "开源前沿记忆体系升级：借鉴 Mem0 原子事实分类体系与重要度分级（不可违背约束、偏好习惯、时空经历、世界状态、客观事实）",
    "排他性属性冲突智能消解：居住地更替、称呼更替、偏好技术栈更迭等自动识别并覆盖替换，杜绝新旧矛盾冲突记忆共存",
    "三维混合动态检索：融合语义相关度 (40%)、静态重要度 (25%)、时间半衰期衰减 (20%) 与实体精准命中加成 (15%)，实现高保真记忆召回",
    "结构化多维上下文压缩：摒弃单段流式摘要，采用【核心背景固定约束】+【关键里程碑推进】+【未决待办事项】三层状态机，极大提高信息密度与信噪比",
    "智能无损信息密度提纯：自动识别并过滤纯寒暄废话轮次，杜绝无意义 Token 消耗",
    "长按拖拽平滑动画：多 Key 列表与消息排队浮窗长按移动时支持平滑重排位移动画，拖拽手感更自然直观",
    "剧情记忆与时间线提取核对：对话菜单支持一键读取完整历史，智能提炼故事时间线与重要记忆，支持用户可视化核对与二次编辑"
)

internal val V211UserUpdates = listOf(
    "分支命名单调自增：彻底修复 XX(分支1) 再次生成分支仍为 XX(分支1) 的重名问题，支持中文半角全角括号与自动编号递增",
    "记忆提取与提炼增强：支持 [] 与 【】 结构化中括号记忆提取，支持「注意」「特别注意」等关键词引导，并自动进行语义规范化加工提炼",
    "模型配置快速清空：设置中已添加的模型自定义参数支持单个一键清空与头部批量重置，快速恢复默认参数",
    "多 Key 优先级快捷切换：设置中多个 API Key 支持通过拖动手柄或上下微调箭头灵活快捷调整优先级",
    "网络波动容错重连机制：遇到 WiFi 抖动、断网重连或连接重置时自动执行退避重连（最多 3 次），并实时展示友好提示",
    "生成中消息排队与专属浮窗：模型回复过程中输入框保持可用，发送内容进入专属排队浮窗，支持拖动手柄调序、撤回回填输入框、编辑、删除与暂停控制",
    "分支创建完整保留多版本：创建分支截断历史时，完整克隆所选轮次的所有生成变体（版本 1、2、3...），保留新会话内的版本自由切换"
)

internal val CurrentVersionUserUpdates = V220UserUpdates

internal val V204UserUpdates = listOf(
    "分支功能完整重构：基于数据库事务与严格切片，规范严格递增时序，全链路杜绝历史记录颠倒或截断缺失",
    "分支生成弹窗确认与跳转：创建分支后弹出精致液态玻璃对话框，支持「确定」留在当前会话与「跳转到新对话」灵活选择",
    "隐藏对话解锁会话维持：解锁密码后持久维持会话解锁态，从隐藏对话返回直达已解锁会话列表，支持一键「重新锁定」",
    "隐藏属性与分支深度继承：在隐藏对话中分支严格继承隐藏标签与安全锁定，并完整继承活跃模型配置与高级参数",
    "角色扮演与会话记忆克隆：分支对话无缝克隆角色卡、场景世界观设定及会话专属长期记忆",
    "引用气泡内嵌卡片化：引用发出后在消息气泡中以精致内嵌毛玻璃卡片优雅展示，消除原始 Markdown 字符堆叠",
    "引用折叠与重新编辑联动：气泡内引文支持轻触展开/折叠，点击重新编辑时自动还原至输入框悬浮预览卡片",
    "输入框呼吸光晕与悬浮栏体验保持"
)

internal val V203UserUpdates = listOf(
    "分支功能深度修复：基于视口所见即所得切片，规范严格递增时间戳，杜绝历史颠倒与缺失，完整继承全量上下文",
    "隐藏对话分支属性继承：在隐藏对话中创建分支时，新分支自动继承隐藏标签与锁定状态",
    "角色扮演与会话记忆克隆：分支对话无缝克隆 RoleplaySession 角色、场景与会话专属记忆",
    "引用气泡内嵌卡片化：引用发出后在消息气泡中以精致内嵌毛玻璃卡片优雅展示，消除原始 Markdown 字符堆叠",
    "引用折叠与重新编辑联动：气泡内引文支持轻触展开/折叠，点击重新编辑时自动还原至输入框悬浮预览卡片",
    "输入框发送键与呼吸光晕红蓝切换与同心校准完美保持",
    "设置页真悬浮栏穿透滚动体验保持",
    "删除消息二次确认防误触机制保持"
)

internal val V202UserUpdates = listOf(
    "引用UI重构：引用文字由独立毛玻璃预览卡片呈现，不再强塞入输入框，发送时自动拼接提示词",
    "发送键边缘圆环校准：与添加文件(+)键保持完全一致的精致圆形边缘，随按键状态在蓝色与红色之间平滑切换",
    "收缩状态同心光晕重构：采用绝对同心数学绘制，彻底根除亚像素错位微小偏差",
    "收缩发送光晕红蓝切换：生成中状态光晕与边框同步变红，停止/发送状态保持纯正蔚蓝",
    "设置页真悬浮栏实现：列表支持从透明毛玻璃悬浮栏下方穿透滚动，视觉与交互完美统一",
    "模型回复新增分支对话功能：一键创建包含该回复及所有前序上下文的全新独立会话分支",
    "长期记忆与专属记忆架构升级：吸收业界前沿范式，新增技术栈/职业身份/负向约束提取，引入防鹦鹉学舌系统引导",
    "删除二次确认机制：对话页内删除输入消息或 AI 回复全面增加二次确认弹窗，防止误删"
)

internal val V201UserUpdates = listOf(
    "输入框(+)添加与发送按键直径统一为34dp（与智能搜索高度一致），间距加大至10dp，高亮边框精准贴合物理边缘",
    "输入框隐藏状态外圈呼吸脉冲光晕优化：缩至最小时与按键边缘严密重合，修复发送键脉冲错位问题",
    "输入框隐藏后支持系统级返回手势（侧滑返回）无缝退出隐藏状态并恢复面板",
    "首页壁纸全面穿透覆盖手机系统状态栏区域，带来真正全沉浸式视觉体验",
    "设置界面顶部悬浮栏完全参照对话页重构：透明毛玻璃质感，移除外圈背景填充，列表可从后方平滑穿透滚动",
    "引用功能内存级拦截：彻底杜绝系统剪切板访问弹窗提示，自动拼接'针对以上内容：'提问前缀",
    "流式输出自动滚动体验重构：生成结束平滑锚定于消息底部，根除滚动跳回回答起点的闪烁回弹问题",
    "全局排版优化：统计图表与模型配置等空间受限区域全面支持水平横向滑动浏览完整文本",
    "Markdown 渲染增强：深度清理模型首句前因颜色标签不兼容产生的孤立星号(*)伪影",
    "API模型配置支持列表展开/折叠，上下文窗口支持自定义数字输入与预设快速选择双轨模式"
)

internal val V200UserUpdates = listOf(
    "顶部控制栏与底部输入框边缘渐变高亮左侧加深，质感层次显著增强",
    "设置页面控制栏完全统一为对话页悬浮胶囊工具栏，风格一致性达到 100%",
    "输入栏支持沿手柄向下拖拽完全收缩至发送键，搭配呼吸脉冲光环与系统返回键同步退出",
    "顶部悬浮栏与输入框同步收缩至圆形返回键，支持点击恢复与系统返回拦截",
    "输入框内部按键边缘新增精致蓝色高亮微光包边，底色逻辑稳定保持",
    "会话长按与划选菜单全面新增'引用'功能，自动拼接 Markdown 引用块填入输入框",
    "修复隐藏会话重命名、删除、置顶与检索能力，与正常会话功能 100% 同步对齐",
    "使用统计图表全面重构：修复模型名称溢出截断，柱状图支持微光端帽，折线图升级为发光贝塞尔曲线",
    "对话设置页专属会话记忆保存逻辑修复，关闭后持久生效，不再被动强制开启",
    "API配置页面模型列表按'已添加'与'从Key读取'清晰分区，支持每模型独立定制上下文窗口与工具能力"
)

internal val V1929UserUpdates = listOf(
    "输入框与顶部悬浮栏透明度精确优化：略微降低透明度，提升文字清晰度与对比度，兼顾通透毛玻璃质感与极佳可读性",
    "重构组件背景着色渲染层级，解决因背景未绘制导致的文字与底层内容冲突问题",
    "保持思考胶囊文本稳定显示与异常滚动保护",
    "保持流式分支生命周期重置与幽灵气泡过滤机制",
    "输入框同心圆弧手柄尺寸与辅助滑动 4 键半透明质感持续保持"
)

internal val V1928UserUpdates = listOf(
    "输入框半透明液态毛玻璃效果完美还原，根除全黑/纯色覆盖，恢复通透背景模糊",
    "顶部悬浮工具栏与报错弹窗实现真实毛玻璃半透明透字效果，消除双层底色覆盖",
    "彻底修复模型思考胶囊文字无法显示问题，移除异常滚动裁剪，增加多级文案安全兜底",
    "根除对话页流式生成后幽灵气泡残留，完善分支生成生命周期自动回收与异常空消息过滤",
    "输入框放大同心圆弧手柄尺寸永久统一，划选复制工具栏解耦防抖彻底保持稳定",
    "辅助滑动 4 个按键保持柔和浅天蓝半透明体系与微光质感"
)

internal val V1927UserUpdates = listOf(
    "顶部悬浮工具栏与错误提示无边缘包裹且全屏穿透，文字半透明透出并实时液态毛玻璃模糊",
    "输入框放大弧线手柄大小彻底统一，拖拽前后永久固定为精致 22dp/17dp 贴角同心弧",
    "对话页辅助滑动 4 个按键重构为柔和浅天蓝半透明体系，搭配微光白边与透光质感",
    "文本划选弹窗与快照状态深度解耦，彻底修复高频闪烁死循环，复制与引用 100% 稳定响应",
    "大模型非标字体颜色与尾随星号容错解析，输入气泡呼吸感间距保留",
    "延续思考快速档纯正天蓝配色与新建会话记忆隔离规范"
)

internal val V1926UserUpdates = listOf(
    "顶部悬浮栏与错误弹窗直接复用输入框玻璃背景规范，消除悬浮栏与弹窗状态栏阴影异常穿透",
    "输入框右上角弧线控制手柄与边框精确同心贴合，拖拽微调更优雅",
    "全面兼容大模型非标颜色标签（如 <font color=\"2B7DEP\"> 与尾随星号）的精准渲染",
    "加宽用户输入气泡与上一条模型回复的纵向间距，提升长对话视觉呼吸感",
    "思考强度快速档重调为柔和纯正天蓝色，告别偏灰暗沉感",
    "对话页悬浮滚动快捷键升级为4键独立体系（到顶/上一条/下一条/到底），阶梯色彩与双线箭头",
    "文本长按选中弹窗防抖优化",
    "延续新建对话专属记忆默认关闭与长列表滚动条防断触优化"
)

internal val V1925UserUpdates = listOf(
    "对话流式响应时增加跟手跟随自动滚动",
    "修复对话顶部悬浮栏在部分机型上背景异常与玻璃穿透问题",
    "修复弹窗状态栏阴影未完整覆盖状态栏顶部边缘",
    "新建对话默认思考开启、联网关闭，专属记忆默认关闭",
    "新对话默认 API 选择入口移至「设置 - API 配置」顶部",
    "思考中与连接中状态文案支持在设置中自定义并一键重置",
    "长按文本工具栏增加剪切与粘贴，修复高频闪烁与空框问题",
    "修复点击中断后错误生成两条回复的问题",
    "对话中错误提示气泡支持折叠与双击展开/收起",
    "优化长列表滚动条滑动稳定性，避免快速拖动断触",
    "输入框展开按钮重构为优雅弧形控制手柄，支持拖拽随手调整高度"
)

internal val V1924UserUpdates = listOf(
    "修复弹窗状态栏阴影未完整覆盖状态栏顶部边缘问题",
    "修复顶部悬浮栏在部分机型上背景异常与玻璃穿透问题",
    "优化长列表滚动条滑动稳定性，避免快速拖动断触",
    "新建对话默认开启深度思考、关闭联网搜索、关闭会话专属记忆",
    "模型选择器支持按供应商分组和名称搜索"
)

internal val V1923UserUpdates = listOf(
    "修复弹窗状态栏阴影未完整覆盖状态栏顶部边缘",
    "会话专属记忆支持独立开关与范围隔离",
    "思考强度快速档位颜色调整",
    "液态玻璃组件优化"
)

val SettingsPanelShape = com.aiassistant.ui.theme.EchoTokens.Radius.shapeXl
val SettingsInnerShape = com.aiassistant.ui.theme.EchoTokens.Radius.shapeLg

@Composable
fun SettingsGlassCard(
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = echoGlassPalette()
    EchoGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsPanelShape,
        containerColor = glass.panel,
        borderColor = glass.outline,
        showBorder = true
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun SettingsInputField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder.isNotBlank()) {
                { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) }
            } else null,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            shape = SettingsInnerShape,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
            )
        )
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Long) -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val glass = echoGlassPalette()
    val settingsBackgroundBitmap = remember(context) {
        BackgroundImageManager.getHomeBackgroundBitmap(context)
    }
    val hazeState = rememberEchoHazeState()
    var selectedSection by rememberSaveable { mutableStateOf<String?>(null) }

    fun executeBack() {
        if (selectedSection != null) {
            selectedSection = null
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = selectedSection != null) {
        executeBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.background)
                .echoHazeSource(hazeState)
        ) {
            settingsBackgroundBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        val readableBackdrops = rememberReadableBackdropColors(settingsBackgroundBitmap)
        val toolbarShape = RoundedCornerShape(22.dp)
        val toolbarTint = glass.input
        val toolbarContentColor = readableTextColorFor(
            background = toolbarTint,
            fallbackSurface = readableBackdrops.top
        )

        val topBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topBarHeight = topBarPadding + 56.dp + 12.dp
        val tabContentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topBarHeight + 8.dp, bottom = 28.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when (selectedSection) {
                    null -> SettingsMenu(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = tabContentPadding,
                        onSectionSelected = { selectedSection = it }
                    )
                    "api_config" -> ApiConfigTab(hazeState = hazeState, modifier = Modifier.fillMaxSize(), contentPadding = tabContentPadding)
                    "appearance" -> AppearanceTab(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        contentPadding = tabContentPadding
                    )
                    "model_features" -> ModelFeaturesTab(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = tabContentPadding
                    )
                    "prompts_memory" -> PromptsMemoryTab(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = tabContentPadding
                    )
                    "personalization" -> AppearanceTab(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        contentPadding = tabContentPadding
                    )
                    "web_search" -> WebSearchTab(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = tabContentPadding
                    )
                    "hidden_conversations" -> HiddenConversationsTab(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToChat = onNavigateToChat,
                        contentPadding = tabContentPadding
                    )
                    "backup" -> BackupTab(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = tabContentPadding
                    )
                    "about" -> AboutTab(hazeState = hazeState, modifier = Modifier.fillMaxSize(), contentPadding = tabContentPadding)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoHazePanel(
                            hazeState = hazeState,
                            shape = toolbarShape,
                            tint = toolbarTint,
                            blurRadius = 16.dp,
                            highlightAlpha = 0.025f
                        ),
                    shape = toolbarShape,
                    color = Color.Transparent,
                    contentColor = toolbarContentColor,
                    border = BorderStroke(1.dp, glass.outline),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { executeBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                        Text(
                            text = when (selectedSection) {
                                null -> "设置"
                                "api_config" -> "API配置"
                                "appearance" -> "界面与外观"
                                "model_features" -> "模型辅助与思考"
                                "prompts_memory" -> "提示词与记忆"
                                "personalization" -> "界面与外观"
                                "web_search" -> "联网搜索与智能工具箱"
                                "hidden_conversations" -> "其他对话"
                                "backup" -> "数据备份"
                                "about" -> "关于"
                                else -> "设置"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMenu(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onSectionSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Key,
                title = "API配置",
                subtitle = "管理AI模型API密钥和服务商配置",
                onClick = { onSectionSelected("api_config") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Palette,
                title = "界面与外观",
                subtitle = "主题模式、用户头像、字体大小与壁纸设置",
                onClick = { onSectionSelected("appearance") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Psychology,
                title = "模型辅助与思考",
                subtitle = "全模型自由直选自动命名、思考翻译与思考胶囊",
                onClick = { onSectionSelected("model_features") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.AutoAwesome,
                title = "提示词与记忆",
                subtitle = "全局提示词、创作规范、关于我画像与长记忆",
                onClick = { onSectionSelected("prompts_memory") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Search,
                title = "联网搜索与智能工具箱",
                subtitle = "Exa 免Key搜索、Open-Meteo天气、健康步数与设备硬件",
                onClick = { onSectionSelected("web_search") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.VisibilityOff,
                title = "其他对话",
                subtitle = "输入 6 位数字密码查看隐藏对话",
                onClick = { onSectionSelected("hidden_conversations") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Backup,
                title = "数据备份",
                subtitle = "备份和恢复应用数据与角色设定",
                onClick = { onSectionSelected("backup") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "版本信息和功能介绍",
                onClick = { onSectionSelected("about") }
            )
        }
    }
}

@Composable
fun ThemeModeCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit
) {
    val glass = echoGlassPalette()
    EchoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsPanelShape,
        containerColor = glass.panel,
        borderColor = glass.outline,
        showBorder = true
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("应用主题", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "选择浅色、深色或跟随系统",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppThemeMode.entries.size
                        ),
                        colors = echoSegmentedButtonColors(),
                        border = echoSegmentedButtonBorder(selected == mode)
                    ) {
                        Text(mode.label)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    hazeState: dev.chrisbanes.haze.HazeState,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val itemShape = SettingsPanelShape
    val glass = echoGlassPalette()
    EchoGlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 78.dp),
        shape = itemShape,
        containerColor = glass.panel,
        borderColor = glass.outline,
        showBorder = true
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ApiConfigTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val repository = AiAssistantApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val configs by repository.getAllApiConfigs().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    BackHandler(enabled = showAddDialog || editingConfig != null) {
        showAddDialog = false
        editingConfig = null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val defaultConfig = configs.firstOrNull { it.isDefault } ?: configs.firstOrNull()
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("新对话默认 API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (defaultConfig != null) "当前默认：${defaultConfig.name} (${defaultConfig.provider})" else "尚未设置默认 API 配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (configs.isNotEmpty()) {
                    Text(
                        "点击直接切换新对话默认生效的服务商：",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        configs.forEach { cfg ->
                            FilterChip(
                                selected = cfg.isDefault,
                                onClick = {
                                    scope.launch {
                                        repository.setDefaultConfig(cfg.id)
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (cfg.isDefault) "${cfg.name} (默认)" else cfg.name,
                                        fontWeight = if (cfg.isDefault) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (cfg.isDefault) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = echoFilterChipColors(),
                                border = echoFilterChipBorder(cfg.isDefault)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "API配置管理",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(configs) { config ->
            ApiConfigCard(
                hazeState = hazeState,
                config = config,
                onEdit = { editingConfig = it },
                onDelete = {
                    scope.launch {
                        repository.deleteApiConfig(config)
                    }
                },
                onSetDefault = {
                    scope.launch {
                        repository.setDefaultConfig(config.id)
                    }
                }
            )
        }

        item {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加API配置")
            }
        }
    }

    if (showAddDialog || editingConfig != null) {
        ApiConfigDialog(
            hazeState = hazeState,
            config = editingConfig,
            isSaving = isSaving,
            onDismiss = {
                showAddDialog = false
                editingConfig = null
            },
            onSave = { config, modelNames, enabledModelNames, modelCapabilities, modelSettings, apiAvatarUri, clearApiAvatar ->
                if (!isSaving) {
                    isSaving = true
                    scope.launch {
                        try {
                            val configId = repository.saveApiConfig(config)
                            if (modelNames.isNotEmpty()) {
                                repository.replaceSelectedModels(
                                    apiConfigId = configId,
                                    modelNames = modelNames,
                                    enabledModelNames = enabledModelNames,
                                    modelCapabilities = modelCapabilities,
                                    modelSettings = modelSettings
                                )
                            }
                            if (clearApiAvatar) {
                                AvatarManager.deleteApiModelAvatar(context, configId)
                            }
                            apiAvatarUri?.let { uri ->
                                AvatarManager.saveApiModelAvatarFromUri(context, configId, uri)
                            }
                            showAddDialog = false
                            editingConfig = null
                        } finally {
                            isSaving = false
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ApiConfigCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    config: ApiConfig,
    onEdit: (ApiConfig) -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    SettingsGlassCard(hazeState = hazeState) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = config.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    if (config.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.height(24.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("新对话默认API", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    }
                    Text(
                        text = "${config.provider} · ${config.modelName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                    Text(
                        text = "API类型: ${config.apiType.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val parsedKeys = remember(config.apiKey) { AiRepository.parseNamedApiKeys(config.apiKey) }
                    if (parsedKeys.isNotEmpty()) {
                        val keySummary = if (parsedKeys.size > 1) {
                            val names = parsedKeys.mapNotNull { it.name.ifBlank { null } }
                            if (names.isNotEmpty()) {
                                "密钥 (${parsedKeys.size}): ${names.joinToString(", ")}"
                            } else {
                                "${parsedKeys.size} 个密钥 (已配置自动故障转移)"
                            }
                        } else {
                            val firstName = parsedKeys[0].name
                            if (firstName.isNotBlank()) "密钥备注: $firstName" else null
                        }
                        if (keySummary != null) {
                            Text(
                                text = keySummary,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = { onEdit(config) }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    if (!config.isDefault) {
                        IconButton(onClick = onSetDefault) {
                            Icon(Icons.Default.StarBorder, contentDescription = "设为新对话默认API")
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
    }

    if (showDeleteDialog) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除配置") },
            text = { Text("确定要删除这个API配置吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
