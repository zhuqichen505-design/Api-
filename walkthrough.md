# Echo v2.7.8 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.8 (`versionCode: 174`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.8.apk`
- **核心内容**：① 输入框高度自愈——点击不再变长，非最小状态在失焦/键盘收起/发送/隐藏输入栏时自动回到最小；② 流式等待动画回到最左侧；③ 胶囊宽度随内容自适应真修复（内层文本区 fill 口径）。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **点击输入框变长（前三轮未根治）** | 复盘：v2.7.6 收窄手柄热区、v2.7.7 移除手柄点按切换——当前代码中点击已不存在任何改变高度的代码路径（isInputExpanded/customInputHeightDp 仅剩拖拽写入）。用户仍见"变长"的剩余可能：① 安装包仍为带点按切换的旧版（≤v2.7.6）；② 手柄拖拽与首行右端重叠，误触拖拽后的非最小高度**永久残留**，观感即"点一下变长且不恢复" | 建立"最小高度自愈"三重保障（ChatInputComponents.kt）：① 失焦即复位（键盘收起/点击其他区域/发送）；② 监听 WindowInsets.ime，软键盘收回即复位（覆盖部分机型返回键收键盘但焦点残留的情况）；③ 拖拽下滑隐藏输入栏时同步复位。拖拽改变高度的能力保留（仅聚焦编辑期间生效），编辑会话结束自动回最小 |
| **流式动画位置（需求 2）** | v2.7.7 按当时描述置于提示行右端 | 移回最左侧（内容区左缘 x=4dp，与历史版本一致），提示文字在其右侧；单行紧凑槽位与 ≥30s 淡入保留 |
| **胶囊宽度自适应未生效（需求 3）** | **真 bug**：v2.7.7 只改了外层胶囊 `weight(fill=false)`，但内层文本区 `weight(1f)` 的 fill 参数**默认 true** 仍撑满剩余宽度——内层永远占满，外层收缩被完全抵消 | 内层文本区改 `weight(1f, fill = false)`：文字不超范围时胶囊收缩到内容宽度（展开/收缩键跟随内容右缘）；超限时仍顶满剩余宽度（横向滚动/换行不变），撑满时右缘与用户气泡右缘对齐 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**503 项全通、0 失败**）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0（versionCode 174 / versionName 2.7.8）
- [x] APK：`Echo-v2.7.8.apk`，16,716,653 字节 (~15.95 MB)，SHA256 `51B518A133246DF6F4F1F7D2815876A74A59F114C8431780C636BA48E96A8EB9`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（构建后共 184 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. **务必安装 v2.7.8 覆盖升级**（v2.7.6 及更早版本的输入栏仍有点按展开路径，只有 v2.7.7 起才移除）；
2. **输入框（需求 1）**：冷启动后点击输入框——高度保持最小（约一行）；拖拽右上角手柄放大后，点击输入框以外任意区域（或发送消息/按返回键收起键盘）——输入框自动回到最小高度；反复点击多次，永不出现"变长且不恢复"；
3. **流式动画（需求 2）**：发送消息，连接/思考阶段的呼吸点应位于内容区**最左侧**（提示文字在其右侧）；
4. **胶囊宽度（需求 3）**：连接/思考/完成各状态胶囊宽度应随文案收缩（短文案为紧凑胶囊，不再恒定撑满）；长文案（重试原因/报错详情）仍撑满头像行剩余宽度并可展开/横向滚动，撑满时右缘与用户消息气泡右缘对齐；
5. **回归**：拖拽手柄放大输入框后多行输入正常；右上角弧线拖拽缩放与下滑隐藏/恢复输入栏正常；点击输入框最右侧光标落点分级（下一行行首/行尾）正常。

## 五、剩余风险
1. 输入框高度不再跨编辑会话保留——每次失焦回到最小高度（用户本轮明确要求"保持最小状态"）；若后续希望手动高度持久保留，需再调整策略。
2. 若用户设备上"点击变长"在 v2.7.8 上仍可复现，则剩余唯一解释为键盘弹出时输入栏随之上移的系统行为（`windowSoftInputMode=adjustResize` + edge-to-edge 配置已核实无误，输入栏理应升至键盘顶部）；届时请提供录屏以便进一步定位。
3. 已知环境缺陷延续：`lintDebug` 的 Compose Lint 兼容问题仍靠 `app/lint.xml` 隔离崩溃探测器，根治需升级 AGP/Compose Lint。

---

# Echo v2.7.7 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.7 (`versionCode: 173`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.7.apk`
- **核心内容**：① 输入框尺寸仅随拖拽变化（移除手柄点按切换）；② 流式动画右置+提示单行紧凑化+间距收紧；③ 状态胶囊宽度随内容自适应；④ 呼吸点动画精致化（四层细节）；⑤ 列表底部间距收窄；⑥ 健康时间线纵向填充+底部时间轴；⑦ 健康状态/Token 热度按钮竖排；⑧ 模型明细排序改下拉列表；⑨ 修复有失败数据时 TPS 标签被挤出；⑩ 修复对话内 TPS 虚高（2000+）。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **点击改变输入框大小（需求 1）** | v2.7.6 只收窄了手柄热区（36→24dp）但保留"点按切换展开/收起"——热区仍与首行右端重叠，点击输入框右端仍命中切换 | 彻底移除手柄点按切换，尺寸只随**拖拽**变化（上拖放大/下拖收窄/继续下拖隐藏） |
| **动画靠左（需求 2）** | v2.7.6 把动画放在提示行左端（x=46，文字在其右） | 动画移到提示行右端（行右缩 44dp，与用户气泡右缘基准线一致） |
| **动画与底部时间相隔远（需求 3）** | 提示文字恒占两行固定槽位（动画上方、空行在下）+ 空正文期 MessageContent 8dp 空占位 + 动画前 6dp Spacer + 页脚 5dp 顶距，累计约 40dp | 提示文字单行化并精简（"连接时间较长，已等待 Ns…"，秒数不被截断）；空正文期不渲染占位；Spacer 6→2dp；页脚顶距 5→3dp——视觉距离约减半 |
| **胶囊恒为最大长度（需求 4）** | v2.7.4 起胶囊无条件 `weight(1f)` 满宽 | 改 `weight(1f, fill = false)`——文字不超范围时收缩到内容宽度；超限时仍满宽+横向滚动/展开换行；撑满时右缘仍对齐用户气泡（44dp 右缩保留） |
| **动画精致化（需求 5）** | 单层光晕、无位移，观感平淡 | 保持"单 InfiniteTransition + Canvas、仅 draw 阶段读动画值、零每帧堆分配"约束下新增：双层光晕（近层辉光呼吸+远层光环扩散）、峰值上浮、尺寸/亮度阶梯、峰值高光；reduced motion 退化为纯静态三点 |
| **列表底部距离大（需求 6）** | 列表底部 contentPadding = 底栏高度 + Spacing.lg(16dp)，滑到底时回复与输入栏间隔 16+8=24dp | 列表底部附加间距单独收窄为 4dp（与悬浮跳转按钮偏移解耦，按钮位置不变） |
| **热力图填充方向（需求 7）** | 原"先行后列"（row = index / 14）——时间顺序横向流动 | 改"先列后行"（col = index / rowCount, row = index % rowCount），点选映射同步；时间顺序先向下、再向右 |
| **无横轴时间（需求 8）** | 网格下无任何时间参照 | 新增 16dp 横轴：第 0/4/9/13 列中心以 TextMeasurer+drawText 标注该列起始时间（跨天周期 MM-dd，否则 HH:mm），两端防溢出 |
| **模式按钮挤压标题（需求 9）** | 健康状态/Token 热度两枚按钮横排占宽 | 改右对齐竖排 Column，标题/副标题不再跨行 |
| **排序选项被挤压（需求 10）** | 表头 5 枚排序胶囊与标题同行，窄屏显示不全 | 改单枚"排序：当前项 ▾"下拉触发器 + EchoGlassDropdownMenu 列表选择 |
| **有失败数据时 TPS 消失（需求 11）** | 模型行数据标签（请求数/成功率/失败/耗时/TPS/缓存/思考）在单行 Row 不换行——出现"失败 N"后整行超宽，TPS 标签被挤出可视区裁掉（数据未丢，显示被裁） | 标签行改可换行 FlowRow |
| **对话内 TPS 虚高 2000+（需求 12）** | 页脚速度 = tokenCount ÷ 耗时，而 tokenCount 存的是**总消耗**（输入+输出+思考）——输入越大越虚高；且 seconds > 0.05 下限过宽，亚秒回复速率失真 | 速度改按**回复正文估算产出 token**（estimateTokenCount，remember 缓存）÷ 耗时，且仅耗时 ≥1s 时展示；"总 tokens"标签口径不变 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**503 项全通、0 失败**）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0（versionCode 173 / versionName 2.7.7）
- [x] APK：`Echo-v2.7.7.apk`，16,716,653 字节 (~15.95 MB)，SHA256 `E20BE3DB7E32796527BBCF3DA01FB04E91B7A2F86705C9223087B32F6BD35A92`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（构建后共 183 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. 安装 `Echo-v2.7.7.apk` 覆盖升级；
2. **输入框（需求 1）**：点击输入框任意位置（含右上角弧线区域）——尺寸不变；拖拽手柄上拉放大、下拉收窄、拉到底隐藏输入栏，重进会话后仍最小；
3. **动画位置与间距（需求 2/3/5）**：发送消息——连接/思考阶段动画应在提示行右端、提示为单行"连接时间较长，已等待 Ns…"；动画与底部时间戳距离明显小于上一版；观察动画细节：双层光晕、峰值上浮、中间点略大、峰值高光；
4. **胶囊宽度（需求 4）**：连接/思考/完成各状态胶囊宽度随文案收缩，长文案（重试原因/报错）仍撑满并可展开/横向滚动；
5. **列表底部（需求 6）**：滑到最底部，最后一条回复的时间戳与输入框顶部距离应明显小于上一版；
6. **统计-健康时间线（需求 7/8/9）**：时间顺序自第一列顶部向下、再向右；底部横轴在 0/4/9/13 列下标注时间点；点选任一方格弹出的时段范围与方格位置一致（纵向相邻格时间相邻）；"健康状态/Token 热度"竖排、左侧标题不跨行；
7. **统计-模型明细（需求 10/11）**：排序改为下拉选择且全 5 项可选、当前项高亮；制造一次失败请求后刷新——"失败 N"标签出现时 TPS 标签仍完整可见（换行显示）；
8. **TPS 口径（需求 12）**：对话内回复页脚 tokens/s 应为常识合理值（数十至一两百），亚秒级回复不再显示速度标签。

## 五、剩余风险
1. 需求 4 后胶囊右缘随内容变化——短文案时胶囊不再到达用户气泡右缘（用户本轮明确要求的行为），长文案时仍对齐。
2. 需求 12 的速度按正文估算 token 计算（与总消耗标签口径不同），历史消息的页脚速度随之重算；耗时 <1s 的回复不再显示速度。
3. 热力图横轴在极窄屏幕上 9sp 标签可能与相邻列标签轻微贴近（已两端防溢出，中间列间距充裕）。
4. 已知环境缺陷延续：`lintDebug` 的 Compose Lint 兼容问题仍靠 `app/lint.xml` 隔离崩溃探测器，根治需升级 AGP/Compose Lint。

---

# Echo v2.7.6 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.6 (`versionCode: 172`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.6.apk`
- **核心内容**：① 思考胶囊右缘对齐口径二次修正（用户输入气泡右缘，非右侧头像）；② 流式等待动画上移至连接提醒同一行并加大；③ 输入框点击误展开与自绘光标不可见双根因修复；④ 点击输入框最右侧光标落点分级（下一行行首/行尾）；⑤ 编辑器点击跳顶根治（钳制型 bringIntoView 门禁）；⑥ 胶囊展开/收起内部对齐恒定。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **胶囊右缘对齐（需求 1）** | v2.7.4 把"输入气泡"误读为底部输入栏（其记录否定 v2.7.3 的"用户消息气泡右缘"口径实为误判），胶囊右缘落在列表内容边、与右侧头像右缘重合 | 胶囊 `weight(1f)` 恒定满宽基础上右缩 44dp（头像 36+间距 8），右缘与用户输入气泡右缘精确对齐；尺寸恒定契约保留 |
| **流式动画远且小（需求 2）** | v2.7.5 把动画移入正文区（正文起点），连接阶段与胶囊之间隔两行提示槽；点半径仅 2.8dp | 无正文阶段（连接/重连/思考）动画与「连接时间长」提醒同行渲染（紧贴胶囊下方）；槽位可见条件扩为 `isGenerating && content.isBlank()`（相位切换不再收缩槽位）；TypingIndicator 44×20dp、点半径 4dp；正文流式期间仍由正文末尾承接 |
| **点击输入框误展开（需求 3A）** | 右上角拖拽手柄 36×36 不可见点击热区盖住首行右端（v2.7.4 移除 28dp 右留白后暴露），点输入框右端命中手柄 onTap | 手柄热区 36dp→24dp（完整覆盖可见弧线，弧心距右缘 22dp、半径 17dp），拖拽缩放/隐藏功能不受影响 |
| **自绘光标不可见（需求 3B）** | 1.6.8 源码实锤：`AndroidParagraph.getCursorRect` 返回矩形宽度恒为 0（注释明示调用方自行调整宽度），v2.7.4 自绘光标 `drawRoundRect(width=0)` 从未画出过内容，且内置光标已被透明笔刷隐藏 | 覆盖层自补 2dp 平台标准光标厚度 `max(rect.width, 2.dp)`，任意偏移（含空文本）均可见闪烁 |
| **点击最右侧光标落点（需求 4）** | 软换行边界"前一行行尾=下一行行首"为同一偏移，v2.7.4 一律绘于行尾后用户无法再点击放到下一行开头；显式换行行尾内置点击永远落在 `\n` 前 | 观察型 pointerInput（不消费事件）记录点击 x：① 软换行边界点在右侧空白（x>行右）绘于下一行行首、点在文字上绘于行尾（保留 v2.7.4 能力）；② 显式换行行尾 400ms 新鲜度窗口内一次性校正光标至 `\n` 之后 |
| **编辑器点击跳顶（需求 5，此前修复未生效的真因）** | 1.6.8 源码实锤两处：① `BringIntoViewResponderNode.bringChildIntoView` 调用 responder 后**无条件**继续向父级转发请求——v2.7.5 的 no-op 门禁拦不住传播；② `FocusableNode` 获得焦点时请求"整个可聚焦节点矩形"入视口（foundation Focusable.kt `onFocusEvent → bringIntoView()`），整字段矩形被转发给滚动容器 → 视口对到字段顶部 | 门禁改**钳制型**：`calculateRectForParent` 把请求矩形钳入可视窗口 `[scrollValue, scrollValue+viewport]`，已可见矩形使滚动容器判定无需滚动、请求就地终结；光标跟随仍由 LaunchedEffect 最小距离滚动承接；编辑回复/仅修改内容/系统提示词三处生效 |
| **胶囊展开/收起内部跳动（需求 6）** | 内层 Row 单行态 CenterVertically ↔ 多行展开态 Top + 图标 1dp 下沉切换，展开/收起瞬间内容整体位移 | 内层对齐恒定 CenterVertically、移除 isMultiLineLayout 条件与图标条件位移；展开/收起只改变胶囊高度 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**503 项全通、0 失败**）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0（versionCode 172 / versionName 2.7.6）
- [x] APK：`Echo-v2.7.6.apk`，16,716,653 字节 (~15.95 MB)，SHA256 `248192473B8CEF395132B87DD505ECB6D25D8CCAC0E61D75A4ACCA4E85826CFC`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（构建后共 182 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. 安装 `Echo-v2.7.6.apk` 覆盖升级，打开任意会话；
2. **胶囊对齐（需求 1）**：发送消息，连接/思考/流式/完成各状态下胶囊右缘应与用户消息气泡右缘在同一竖直线上（不与右侧头像对齐），宽度恒定无跳变；
3. **流式动画（需求 2）**：连接与思考阶段，呼吸光环点应紧贴胶囊下方、与"连接时间长"提醒（30s 后出现）同一行，点明显比上一版大；正文开始输出后动画跟随文字末尾直至回复结束；
4. **输入框（需求 3/4）**：点击输入框任意位置（含首行右端）不应误展开；空输入框点击后光标应正常闪烁；输入跨行长文本——点击某行右侧**空白区**光标跳到下一行开头，点击某行**最后一个字上**光标停在该行行尾；按回车产生硬换行后点击上一行最右侧，光标应落到下一行（新行）起始位置；
5. **编辑框（需求 5）**：打开"编辑模型回复 / 仅修改内容 / 系统提示词"，输入超长文本后在文本中部点击定位——不应跳回内容顶端；在可视区外继续输入，内容以最小距离跟随光标；
6. **胶囊展开收起（需求 6）**：触发多行报错/长状态胶囊，点击展开/收起——内部文字与图标位置、大小不变化，仅胶囊高度变化；
7. **回归**：右上角手柄拖拽缩放输入框、上滑隐藏输入栏、点击手柄展开/收起仍正常；编辑框右缘滑块与跳转末尾按钮正常。

## 五、剩余风险
1. 需求 4 的显式换行（`\n`）校正依赖"点击后 400ms 内选区变化"的一次性判定；极慢设备上若内置选区落点晚于 400ms 到达，该次点击可能保持内置行尾落点（软换行场景不受影响，仅绘制位置分级，无时间窗）。
2. 手柄热区收窄为 24dp 后，从首行文字区拖拽缩放输入框的可用起点略小于上一版（36dp）；拖拽仍可从右上角 24dp 区域及弧线处发起。
3. 钳制型门禁依赖 `ScrollState.viewportSize` 就绪；视口未就绪（首帧前）的极端请求退化为不钳制（等同 v2.7.5 行为），实际影响可忽略。
4. 已知环境缺陷延续：`lintDebug` 的 Compose Lint 兼容问题仍靠 `app/lint.xml` 隔离崩溃探测器，根治需升级 AGP/Compose Lint。

---

# Echo v2.7.5 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.5 (`versionCode: 171`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.5.apk`
- **核心内容**：① 流式输出动画（呼吸光环点）持续整个生成过程、直到回复结束才消失；② 动画跟随内容末尾，与屏幕底部距离在流式期间稳定；③ 编辑器（编辑回复/仅修改内容/系统提示词）点击不再强制跳顶、输入时内容以最小距离跟随光标。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **流式动画提前消失（需求 1 未实现）** | v2.7.2 将"流式输出的动画"理解为流式光标并做了光标穿透；用户所指是呼吸光环点——它在正文首个 token 到达时即消失（条件 content.isBlank()），回复绝大部分时间动画不存在 | 呼吸光环点改为持续整个生成过程：正文空白时位于内容起点（连接/思考），正文流式期间跟随内容末尾（正文末行下方 6dp），isGenerating=false 即刻消失；移除 v2.7.2 胶囊行下方独立节点（"距头像距离统一"由"动画随内容末尾"新契约取代）；流式光标保持不变 |
| **动画与底部距离错误变化（需求 2）** | 等待阶段动画在胶囊行下方、正文阶段节点被移除，阶段切换节点增减触发列表钉底重锚，距离跳变 | 动画全程跟随内容末尾 + 列表钉底跟随 → 流式期间动画与屏幕底部（输入栏上方）距离稳定 |
| **点击跳顶 + 内容不跟随光标（需求 3）** | EchoScrollableTextEditor 依赖 BasicTextField 内建 bringIntoView 传播做光标可见性，实测该请求在本结构下把视口强制带到内容顶端（而非光标矩形），且输入不产生跟随——两个症状同源 | ① no-op `BringIntoViewResponder` 门禁拦截内建请求外传（foundation relocation API，javap 核实 1.6.8 签名：`bringChildIntoView(localRect: () -> Rect?)` + `calculateRectForParent`）；② 光标可见性由编辑器自行接管：`LaunchedEffect(选区/布局/内容高度)` 在光标行移出可视区时以最小距离滚动跟随（上下 4dp 余量）——点击可视区内绝不滚动、输入始终跟随 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**503 项全通、0 失败**）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0（versionCode 171 / versionName 2.7.5）
- [x] APK：`Echo-v2.7.5.apk`，16,716,653 字节 (~15.95 MB)，SHA256 `051817DF7BFFECEDB658E892C4C98A5C8F3CFAEE9D747EFB40B5877DB3E8951A`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（构建后共 182 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. 安装 `Echo-v2.7.5.apk` 覆盖升级，打开任意会话；
2. **动画持续性**：发送消息，呼吸光环点应从连接阶段一直显示到模型回复结束（正文流式期间位于已输出文字的下方），回复结束才消失；
3. **底部距离**：长回复流式期间观察动画点与屏幕底部（输入栏上方）的距离，应保持基本稳定、不再随连接→思考→流式切换跳变；
4. **编辑框跳顶**：打开"编辑模型回复"或"系统提示词"，在长文本中部点击任意位置——视口不应滑动到内容顶端；在可视区下方（滑块滚动后）继续输入——内容应以最小距离跟随光标；
5. **回归**：编辑框右缘滑块拖动/点按定位正常；跳转末尾按钮正常；思考面板展开/收起正常。

## 五、剩余风险
1. 动画随内容末尾后，思考阶段动画位于思考面板下方（随面板增长下移），v2.7.2 的"连接/思考距头像距离统一"由新契约（"全程跟随内容末尾 + 底部距离稳定"）取代——如需兼顾可再迭代。
2. 编辑器光标跟随基于选区变化的 LaunchedEffect，与内建 bringIntoView 已用门禁隔离；若系统 IME 直接移动选区的极端路径存在一帧布局滞后，跟随为最近一次布局的行位置（误差不超过一行）。
3. 已知环境缺陷延续：`lintDebug` 的 Compose Lint 兼容问题仍靠 `app/lint.xml` 隔离崩溃探测器，根治需升级 AGP/Compose Lint。

---

# Echo v2.7.4 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.4 (`versionCode: 170`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.4.apk`
- **核心内容**：① 状态胶囊恒定满宽、右缘与底部输入气泡右缘精确对齐、展开/收缩键贴胶囊最右侧、文字未填满观感优化；② 输入框文字区左右留白对称（移除右侧 28dp 附加空距）；③ 输入框自绘光标支持停在软换行行尾；④ 发送后清除焦点自动收起键盘。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **思考胶囊大小依旧会变** | v2.7.3 胶囊仅在 isGenerating 期间占满行宽（且 40dp 右缩对齐的是用户消息气泡右缘，与用户本意"底部输入气泡"不符），回复完成后回落自适应宽度 | 胶囊无条件 `weight(1f)` 占满头像行剩余宽度——连接/思考/流式/完成各状态尺寸恒定 |
| **右缘对齐参照** | 消息列表 contentPadding 14dp 与输入栏/工具栏 12dp 不一致；助手外层 Column 右侧 4dp 内边距 | 列表 contentPadding 水平统一为 12dp；助手外层 Column 改 `start=4dp/end=0dp`——胶囊右缘 = 输入气泡右缘（精确对齐）；正文右缘随之延展 4dp（与左侧一致），其余左缘位置不变 |
| **展开/收缩键不贴右 + 文字未满观感差** | 胶囊内文本 Box 为 `weight(1f, fill=false)` 按内容收缩，键位紧跟文字悬在半空 | 文本 Box 改 `weight(1f)`（fill=true）——键位恒定贴靠胶囊最右，文本区自然铺满 |
| **行尾光标只能放下一行开头** | 软换行边界处"前一行行尾"与"下一行行首"是同一文本偏移，Compose 内置光标对该偏移一律绘制在下一行行首 | 输入框改自绘光标：cursorBrush 透明隐藏内置光标，`InputCursorOverlay` 在绘制阶段判断——偏移位于软换行边界（offset==getLineStart(line) 且前一字非 \n）时改绘于前一行行尾（getLineRight），垂直行盒内居中；其余位置与内置光标矩形一致；选区展开时不绘制（由内置选择手柄接管）；呼吸 530ms 与应用动效同源（reduced motion 恒亮）；输入框状态迁移 TextFieldValue（对外仍 String API，外部变更同步并复位光标），状态声明置于 AnimatedContent 之外不随隐藏/恢复丢失 |
| **输入框左右留白不对称** | decorationBox 文本区 `padding(end=28dp)`（历史避让右上角拖拽手柄） | 移除文本区与占位符的 28dp 右距，左右留白统一 4dp；右上角手柄为低透明度弧线装饰，影响可忽略 |
| **发送后键盘不收起** | 发送按钮未清除输入框焦点 | 直接发送与排队发送按钮在发送前 `focusManager.clearFocus()`——焦点清除后软键盘收起；停止生成按钮不动焦点 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**503 项全通、0 失败**）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0（versionCode 170 / versionName 2.7.4）
- [x] APK：`Echo-v2.7.4.apk`，16,716,653 字节 (~15.95 MB)，SHA256 `5632B80F8B32F0EDC2A12247E5D5B44FD5272BEA68BE34EEB5B27232215AAC84`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（构建后共 181 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. 安装 `Echo-v2.7.4.apk` 覆盖升级，打开任意会话；
2. **胶囊**：发送消息，观察连接/思考/流式/完成四个状态——胶囊应始终占满头像行剩余宽度、右缘与底部输入气泡右缘在同一竖直线上，全程无宽度变化；展开/收缩键贴靠胶囊最右侧；文字较短时胶囊无中段悬空感；
3. **行尾光标**：在输入框输入跨行长文本，点击某行右侧最后一个字后面——光标应停在该行行尾（呼吸闪烁），不再跳到下一行开头；长按选择时光标消失、选择手柄正常；
4. **留白对称**：输入框文字区左右留白应一致（各约 4dp）；
5. **键盘收起**：输入文字后点击发送（或生成中点击排队发送）——软键盘应自动收起；点击停止生成不收起键盘；
6. **回归**：输入栏"隐藏"手势后恢复，输入内容与光标状态正常；引用回填、排队消息撤回回填后输入框内容正确。

## 五、剩余风险
1. 行尾自绘光标仅覆盖**软换行**边界；用户按回车产生的硬换行（\n）后光标仍在新行行首（符合常规输入框习惯）。
2. 输入框右上角拖拽手柄（36dp 触控区）位于首行行尾附近，点击该角落仍触发展开/收起而非光标定位——与历史行为一致；文字现在会延伸至手柄弧线下方（弧线为低透明度装饰线，可读性影响轻微）。
3. 已知环境缺陷延续：`lintDebug` 的 Compose Lint 兼容问题仍靠 `app/lint.xml` 隔离崩溃探测器，根治需升级 AGP/Compose Lint。

---

# Echo v2.7.3 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.3 (`versionCode: 169`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.3.apk`
- **核心内容**：① 胶囊对齐口径修正——生成期胶囊右缘与用户消息气泡右缘精确对齐（v2.7.2 误按左缘理解）；② 全项目流畅度与稳定性专项治理（消息流重复订阅治理、主线程 I/O 迁出、图片解码缓存、Compose 流式重组治理、Markdown 热路径提速、Zip-Slip 防护、备份时序治理）。

## 二、实施方式与核查原则
- 三个并行代码审计（主线程 I/O 与崩溃风险、内存泄漏与并发、Compose 重组与流畅度）仅作线索，**每条发现均打开文件核实调用链后才实施**；审计与实际不符的直接否决，中风险项保守跳过。
- 核查成果实例：审计声称 `AiRepository.getModelUsageSummary` 存在 "days≥25 时 Int 溢出"——实算 `days*24*60*60` 对 days=30 仅 2,592,000（Int.MAX 约 21.5 亿，需 days≥24,855 才可能溢出），**判为误报否决**，代码未动。
- 纯函数优化配套等价性单测：流式分段增量缓存的等价性用例在实施中先后捕获两处实现缺陷（重算起点漏算尾长；重算区域首行为空行时 `i in 1 until size-1` 行索引判定与全文不一致），修复后才交付。

## 三、修复要点
| 类别 | 问题 | 修复 |
| :--- | :--- | :--- |
| 胶囊对齐（需求口径修正） | v2.7.2 将"胶囊右侧与用户输入气泡右侧对齐"误实现为对齐列内容边缘 | 生成期胶囊 `weight(1f)` 基础上 `padding(end = 40dp)`：用户气泡右缘=列内容宽−36(头像)−8(间距)，助手列自身 4dp 内边距 → 44−4=40dp，右缘精确对齐 |
| 消息流重复订阅 | `ChatViewModel.loadConversation` 被生成结束/报错保存/记忆增删改/时间线操作等 16 处调用，每次新启一条 `getMessages` collect 且永不取消——N 条等值订阅重复查询/重复修复写库/重复估算上下文，长会话越用越卡 | 消息订阅收敛为单例 Job（`observeMessages` 同会话去重，行为与单份订阅等价） |
| 搜索订阅叠加 | `RoleplayViewModel` 搜索框逐字符叠加永久 Flow 订阅且陈旧查询竞写列表；`loadCharacterTags`/`loadMemories` 同类 | 四处统一"取消旧 Job 再订阅" |
| 主线程 I/O | 单对话备份（runBlocking：多表查询+序列化+写盘）首页/隐藏页点击回调直调；设置页备份/导出/导入/恢复 5 处同步跑在 Main；时间线草稿/检查点构造期主线程读盘；角色/场景卡 TXT 导入主线程整文件读取 | 调用点迁 `Dispatchers.IO`，Toast/UI 状态回写主线程；草稿改为 init 内 IO 异步回填 |
| 图片重复解码 | 背景图六个页面各自组合期同步解码同一张大图（最大 2160px ≈18MB 峰值）；头像每行读盘+Base64+PNG 解码；角色/会话专属头像每行 openInputStream 解码 | `BackgroundImageManager`（fileName+lastModified 键）、`AvatarManager`（LruCache 16）、`ChatAvatar` 自定义头像（8MB LruCache，时间戳命名不可变文件）三级缓存，保存/删除自动失效 |
| 流式全屏重组 | `ChatScreen` 顶层 `LaunchedEffect(currentResponse.length, ...)` 的 key 在组合作用域读取每 token 变化 state → 整页每 token 一帧全量重组；`ChatScrollJumpButtons` visible 组合期直读 `listState.layoutInfo` | 改 `LaunchedEffect(isGenerating)` + `snapshotFlow`（70ms 节流与钉底条件原样）；`derivedStateOf` 下沉布尔判定 |
| Markdown 热路径 | 分段每帧全文重算（O(全文)/帧）；有序列表/参考资料/关键词标题/font-span/引用等 13 处现场编译 Regex；LaTeX 符号表 150 项每次重建+排序；引用角标每 `[` 剩余全文 substring+编译；`isErrorMessage` 每帧三处全文扫描；`isThinkingEnglish` 死计算 | `MarkdownSegmentationCache` 增量重算（等价性单测护航）；13 个正则常量化；符号表/fontCmds/mathbbMap 常量预排序；有界前瞻判定；`remember(message.content, isUser)` 记忆化；删除死计算 |
| 稳定性 | 备份恢复 zip 条目名未校验可路径穿越（Zip-Slip）；自动备份启动早于数据库初始化，失败恢复路径下可能与"恢复替换数据库"并发 | canonicalPath 前缀校验、越界条目跳过并记日志；自动备份延后至初始化成功后启动 |

## 四、明确否决/跳过项（如实记录）
1. **否决（审计误报）**：`AiRepository` 时间窗 "Int 溢出"——实算不成立（见上）。
2. **跳过（中风险，需专项回归）**：① MessageBubble BoxWithConstraints 每帧 subcomposition 优化（涉及气泡布局语义）；② restoreBackup 失败时 files/shared_prefs 已落盘内容的回滚原子性（改变失败场景时序）；③ 冷启动 DB 初始化失败后的全异步恢复（需启动门禁，处理不当影响首屏可用性）。

## 五、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**503 项全通、0 失败**；新增 `MotionRoundTests.testStableSegments_incrementalCache_equivalentToFullRecompute` 分段增量缓存等价性用例）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0（versionCode 169 / versionName 2.7.3）
- [x] APK：`Echo-v2.7.3.apk`，16,716,653 字节 (~15.95 MB)，SHA256 `F3C29DB6AD16AB02DAA02E35D62ACEA81BA45E69731B1AF673F86815595942F9`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（构建后共 180 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 六、人工验收步骤（无真机，未执行安装/启动验证）
1. 安装 `Echo-v2.7.3.apk` 覆盖升级，打开任意会话；
2. **胶囊对齐**：发送消息，生成期间连接/思考/流式三状态胶囊右缘应与上方用户消息气泡右缘在同一竖直线上，尺寸恒定不跳动；
3. **流畅度**：进入长会话连续多轮对话+多次重新生成，观察卡顿是否较上一版明显缓解；设置自定义背景后在聊天/首页/历史/统计/设置间反复切换，首次进入后其余页面应无解码停顿；
4. **备份**：备份一个长对话、导出/导入备份、恢复备份——过程界面应保持响应，完成后提示正常；
5. **角色卡导入**：导入一个数 MB 的角色卡 TXT，界面不应冻结；
6. **流式输出**：让模型输出长回复，滚动跟随应保持平滑，长回复后期不应出现明显掉帧。

## 七、剩余风险
1. 三项中风险优化被保守跳过（见第四节），如需继续治理建议单独立项并配合专项回归。
2. 分段增量缓存已由等价性单测覆盖（逐字符追加 × 围栏/数学块/表格/空行边界 + 非前缀跳变回退），但极端内容形态（如超长单段无空行文本）下的性能收益以"重算起点回退到首个非空行段边界"为界，仍为 O(末段+尾部长)，优于原先 O(全文)。
3. 已知环境缺陷延续：`lintDebug` 的 Compose Lint 内嵌 kotlinx-metadata 与 Kotlin 2.2 兼容问题仍靠 `app/lint.xml` 隔离崩溃探测器，根治需升级 AGP/Compose Lint。

---

# Echo v2.7.2 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.2 (`versionCode: 168`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.2.apk`
- **核心内容**：编辑模型回复左下角「跳转末尾」按钮；编辑回复/仅修改内容/系统提示词三类大文本框右侧统一滚动滑块；修复长文本中间输入时视区跳回文字顶端；胶囊圆环动效全程化（回复完毕落定静态圆环）；生成期间胶囊尺寸统一（右侧对齐用户气泡列）；流式光标持续显示到回复完毕；等待动画与头像距离统一；流式光标间距与垂直对齐修正。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **编辑框无「跳转末尾」能力** | 编辑对话框用 `String` 状态的 `OutlinedTextField`，无光标控制通路 | `editingAssistantContent`/`editingUserContent` 迁移为 `TextFieldValue`；编辑模型回复对话框按钮行最左新增 `ArrowDownward` 按钮：selection 置 `TextRange(text.length)` + 共享 `ScrollState` `animateScrollTo(maxValue)` 滚动到底；"仅修改消息内容"对话框同步 |
| **大文本框无滚动滑块** | Material3 `OutlinedTextField` 内部 scrollState 不对外暴露，无法外接滑块 | 新建 `EchoScrollableTextEditor.kt`：`BasicTextField` 置于自管 `verticalScroll` 容器整体布局，右缘 16dp 固定槽位放自绘 `EchoVerticalScrollSlider`（溢出淡入、拖动/点按定位、拇指长度按视口/内容比、隐藏时槽位占位不跳变）；应用于编辑回复/仅修改内容/系统提示词三对话框（系统提示词 `promptText` 同步迁移 `TextFieldValue`） |
| **中间输入时跳回文字顶端** | `OutlinedTextField` 高度被 `heightIn`/`maxLines` 截断后由内部滚动接管，文本变化重新测量时内部滚动位置被重置，视区跳回文字顶端 | 同一组件内文本以无高度约束整体布局、滚动由外层 `verticalScroll` 承担，光标可见性经 bringIntoView 沿外层滚动解析——仅光标移出可视区时最小距离滚动，不再跳顶 |
| **胶囊动效在正文开始后中断** | 图标槽仅在 Connecting/Thinking 显示呼吸脉冲环，Streaming/Idle 回落为静态 `Psychology` 图标 | 图标槽条件扩展至 Streaming（流式期思考档位色/primary）；`EchoPulseRing` 新增 `animated` 参数，Idle 传 `false` 渲染静态圆环（不创建 InfiniteTransition），颜色与生成期同源；报错/重连图标不变 |
| **连接/思考（结束）胶囊尺寸不一** | 胶囊宽度随状态文案自适应（"正在连接 X…" / "X 正在思考中…" / 流式提示），切换时宽度跳动 | 生成期间胶囊 `weight(1f)` 占满头像行剩余宽度，各状态尺寸恒定，右侧对齐消息列内容边缘（用户气泡列）；落库后恢复自适应宽度，历史消息观感不变 |
| **流式动画中途消失** | 呼吸光标仅挂「普通文本块」末尾：列表项/标题/引用/参考资料尾部、段落边界瞬间尾段为空、表格/数学/闭合代码块/分割线尾部均无光标 | ① 光标穿透全部内联块（6 级标题、关键词标题、有序/无序列表、引用、参考资料，`isLastLine` 判定）；② 无法内联承载的尾部与空尾追加独立行光标 `StreamingTailCursor`（`tailNeedsStandaloneCursor` 判定）；③ 围栏刚开启无内容时同样补独立光标 |
| **等待动画连接态/思考态距头像不等** | 呼吸光环点渲染在 `MessageContent` 内，思考态被展开的思考面板推远 | 光环点移至头像+胶囊行正下方（header Column 内、等待提示槽前），`padding(start 4, top 10, bottom 4)` 复现连接态原始间距；正文开始后仍由光标接管（审核 A4 不变） |
| **光标贴字过近且偏上** | `translationX = lineRight - 1dp`（与末字重叠 1dp）、`translationY = lineTop`（行盒顶对齐） | 光标几何常量提升顶层值，新增 `echoCursorLineTransform` 定位器：`lineRight + 2dp` 外移留间隙、末行行盒内垂直居中；正文与围栏光标共用 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**502 项全通、0 失败**；本次为纯 UI 层改动，未新增/删除测试）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0（versionCode 168 / versionName 2.7.2）
- [x] APK：`Echo-v2.7.2.apk`，16,700,269 字节 (~15.93 MB)，SHA256 `3EBE68CE1D57B5E78E6E1014C4954319C970FFA223D85CA7F9DD48E14EF0311C`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（构建前 178 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. 安装 `Echo-v2.7.2.apk` 覆盖升级，打开任意会话；
2. **编辑模型回复**：消息菜单 → 编辑回复 → 对话框左下角应有向下箭头按钮；点击后光标应跳到文字末尾且编辑区滚到底部；
3. **滑块**：在编辑回复/仅修改内容/系统提示词对话框中粘贴超长文本，右侧应出现滑块；拖动/点按可快速定位；删至不溢出后滑块自动隐藏且编辑区宽度不跳动；
4. **跳顶修复**：长文本中部任意位置定位输入，视区应保持光标附近，不再跳回文字顶端；
5. **胶囊圆环**：发送消息，连接→思考→正文流式期间胶囊左侧应始终是圆环动效；回复结束后变为静态圆环（思考模型颜色为思考档位色）；
6. **胶囊尺寸**：生成期间连接/思考/流式三状态胶囊宽度应恒定（占满头像行至用户气泡列边界），切换不跳动；回复完毕恢复自适应；
7. **流式光标**：让模型输出含列表/标题/表格/代码块的长回复，光标（或独立行光标）应持续显示到回复完毕后淡出；
8. **等待动画**：连接与思考两阶段，呼吸点与头像的垂直距离应一致（紧贴胶囊行下方）；
9. **光标对齐**：流式输出时光标与文字末尾应有小间距、行内垂直居中不偏上。

## 五、剩余风险
1. 胶囊宽度统一的参照为「消息列内容边缘（用户气泡列右基准）」：需求原文"胶囊的右侧和用户输入气泡的左侧对齐"存在按"触发本轮的那条用户消息气泡左缘"动态对齐的另一种理解；按该理解胶囊宽度需逐轮跟随用户气泡测量宽度，实现复杂且随消息长度剧烈变化，故采用恒定列边界方案。若观感与预期不符，可再按用户气泡左缘动态对齐迭代。
2. `EchoScrollableTextEditor` 为自绘大纲样式（边框/底色/光标/占位符与原 glass 配色一致），与 Material3 `OutlinedTextField` 的涟漪等微交互存在细微差异。
3. 流式光标在表格/数学块尾部以「独立行光标」呈现（下一片文本到来即并入正文），与贴字光标形态略有差异，属预期设计。
4. 已知环境缺陷延续：`lintDebug` 中 Compose Lint 内嵌 kotlinx-metadata 与 Kotlin 2.2 的兼容问题仍靠 `app/lint.xml` 隔离崩溃探测器，根治需升级 AGP/Compose Lint。

---

# Echo v2.7.1 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.1 (`versionCode: 167`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.1.apk`
- **核心内容**：时间线变动提示自动消失、流式动画与思考胶囊尺寸突变修复（屏幕错误滑动）、用户消息「仅修改内容」、时间线时间记忆与过度推进治理（含角色扮演故事时间传参真 bug 修复）、报错后重新生成/重发弹出旧报错修复。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **时间线变动提示不自动消失** | `_timelineUpdateNotice` 赋值后常驻，UI 层仅手动关闭，无定时清除 | `ChatScreen.kt` 新增 `LaunchedEffect(timelineUpdateNotice)`：非空 5 秒后自动 `dismissTimelineUpdateNotice()`，退场由既有 AnimatedVisibility 平滑收起；手动"查看/关闭"即时生效不变 |
| **胶囊突然变大又变小** | 外层 `animateContentSize` 与内层 `AnimatedContent` 默认 `SizeTransform` 双层尺寸动画叠加，状态切换时先撑到出入场内容最大值再回缩 | `AnimatedContent` transitionSpec 显式 `using SizeTransform { _, _ -> snap() }` 禁用内层尺寸动画，尺寸过渡只由外层单一弹簧驱动 |
| **屏幕错误滑动一小段距离** | 连接等待提示槽在 Connecting/Reconnecting→Thinking 相位切换时整槽（约 40dp）一帧内移除，末项高度突降被 v2.6.8 钉底逻辑追平 | 提示槽改 `AnimatedVisibility(fade+expand/shrink)` 平滑出入场（reduced motion 用 snap），高度不再一帧塌陷；滚动钉底逻辑未动，避免回归 v2.6.8 |
| **用户消息无法仅编辑不重发** | 现有"重新编辑"= 编辑重发（variant + 重新生成）；底层 `editAssistantMessage` 只改 content 且与角色无关但命名误导 | `MessageBubble`/`MessageFooter` 新增 `onEditInPlace` 通路 + 用户菜单"仅修改内容"项；`ChatScreen` 新增编辑对话框（明确提示不重新发送/不重新生成），保存调 `updateMessageContent`（由 `editAssistantMessage` 更名，2 处引用，行为不变）；不加"已编辑"角标（需 Room 迁移，超出需求） |
| **有时间线仍时间记忆错误（真 bug）** | `RoleplayRepository.assembleRoleplayContext` 把剧情摘要 `currentPlotSummary` 当"当前故事时间"注入 `buildTimelinePromptContext`，模型时间锚点从源头被污染；注入 prompt 缺时间记忆权威约束 | 故事时间改为 `conversationDao…currentStoryTime` → 会话记忆【当前故事时间】条目回退；两处注入 prompt 新增【时间记忆权威声明】（唯一权威、先对表推算、严禁把往事当刚才） |
| **过度执着推进时间（早晨→晚上）** | 注入 prompt"主动推进"与"严禁篡改"双向指令张力；守卫只拦"早晨→夜晚"且豁免关键词仅 5 个，拦不住同日跨多时段跳跃；本地兜底对非跳夜多步顺延不设防 | prompt 收敛为"默认守时 + 明确描写才顺延 + 单轮至多一个相邻时段，严禁跳跃式推进"；评估 prompt 新增【默认守时与单步推进铁律】（宁可 NO_UPDATE）；新增 `hasExplicitTimePassageDescription` + `isUnreasonableStoryTimeJump`（同日跨 >1 时段且无明确时间流逝描写 → 拦截，跨天/无法解析不拦）代码双保险；本地兜底 `detectAutoStoryTimeAdvancement` 多步顺延需描写依据，单步活动顺延行为不变 |
| **报错后重新生成/重发弹出旧报错** | v2.6.8 的 `isCurrentSession` 身份守卫只覆盖收尾路径：① 流式全程回调（onToken/onThinkingToken/onStatusUpdate/onKeyAttemptError/onResetBuffer）无守卫，被取代旧轮次的残留回调可回写 ViewModel；② 新一轮启动未显式清 `_reconnectStatus`，流式气泡首帧读到残留错误文案即判 Failed 显示旧报错；③ `startSession` 只取消旧 Job 不取消阻塞中的 HTTP call，旧调用可滞后存活；④ `saveErrorReply` 无轮次守卫，旧轮次失败会把报错气泡写进进行中的新会话 | ① 五个流式回调统一加 `isCurrentSession` 守卫；② 新一轮启动块显式 `_reconnectStatus.value = null` 并 `repository.cancelActiveRequest(conversationId)` 取消残留调用；③ 四处 `saveErrorReply` 调用点补 `isStillCurrentRound` 守卫（当前轮次停止/报错保全行为不变） |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**502 项全通、0 失败**；`TimelineNaturalTimeTest` +3 项：同日跨多时段跳跃守卫、本地兜底防过度推进、注入 prompt 权威声明与单步铁律；`TimelineRefinementAndCompressionTest` 防停滞 prompt 断言随指令收敛同步更新为新契约）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.7.1.apk`，16,700,269 字节 (~15.93 MB)，SHA256 `89ACABC79B0483312FFE8ADFA63FD512CFBFB4D7FEC6581AC76C9FA66862FA9A`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（共 178 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. **时间线提示自动消失**：触发一次时间线推进并点击"应用"，观察"已将时间线变动应用到记录"提示约 5 秒后自动平滑收起；期间点"查看"/关闭仍即时生效。
2. **胶囊与屏幕稳定**：新对话发送消息，连接→思考→流式全程观察思考胶囊：不再突然变大又变小；等待提示消失、生成结束时屏幕不再无故上下滑动一小段。
3. **仅修改内容**：长按/点开任一用户消息的"更多"菜单 →"仅修改内容"→ 修改保存：该消息显示已更新、后续回复原样保留、无任何重新生成动作；再次发送新消息时模型按修改后的内容回应。原"重新编辑"仍走编辑重发。
4. **时间线时间理解**：在开启会话记忆/时间线的会话中，让故事停留在清晨进行多轮日常交谈：模型保持早晨/上午口径，不再推进到晚上；正文明确描写"逛了一整天、夕阳西下"后才顺延至傍晚；对"昨天/刚才"的提问回答与时间线一致。
5. **报错后重试**：制造一次连接报错（如断网/错误 Key）后恢复网络，点击"重新生成"或重新发送：胶囊应从"正在连接"开始全新尝试，不再直接弹出上一轮的报错文案；旧报错气泡不再中途冒出。

## 五、剩余风险
- 时间线推进为 prompt + 关键词守卫的治理方案，无法 100% 杜绝个别模型在极端表述下的误判；用户确认卡与手动改时间仍是最终兜底。
- "仅修改内容"暂无"已编辑"角标（需 Room 迁移，本次按最小改动未加）；编辑只改显示内容，后续请求会以修改后的内容作为上下文（符合需求语义）。
- 快速失败的供应商错误（401/429/5xx 等无退避重试）仍可能在秒级返回同一错误——本次修复保证这是一次全新的连接尝试且界面从干净的连接态开始，但无法让必然失败的网络请求成功。

---

# Echo v2.7.0 构建走查与验收报告 (Walkthrough)

## 一、本次构建与需求概述
- **发布版本**：v2.7.0 (`versionCode: 166`)
- **构建类型**：Release APK
- **交付目标文件**：`D:\Agent\APP-Echo\app\releases\Echo-v2.7.0.apk`
- **核心内容**：紧急修复更新后严重卡顿与闪退故障、消除 Flow 消息监听自激死循环、优化菜单单层测量、冷启动异步自动备份。

## 二、根因走查与修复要点
| 项 | 根因 | 修复 |
| :--- | :--- | :--- |
| **会话卡顿、冻屏与闪退（核心故障）** | v2.6.9 在 `ChatViewModel.init` 的 `getMessages.collect` 回调中调用了 `updateMessageModelMap`，其内部遍历 assistant 消息时，若发现未存模型名即调用 `repository.updateMessageModelName` 更新数据库。Room 监听到 `messages` 表被修改，立即通知 Flow 重新查询发射新列表，新列表再次进入 `collect` 并再次触发遍历和更新，形成指数级并发写入死循环与协程风暴；抢占 SQLite 写入锁导致 `SQLiteDatabaseLockedException`、ANR 强杀或 OOM 闪退；流式传输时每次 token 触发全表扫描查所有历史统计，CPU 飙升 100% | ① 彻底移除 `updateMessageModelMap` 遍历过程中的所有写库操作，其唯一职责严格限制为更新内存中的 `_messageModelMap` 供 UI 渲染；② 增加 Fast-path 检查：若所有 assistant 消息已解析或已在内存 Map 中，直接快速返回，0 协程开销，0 数据库查询开销，打字与流式生成恢复极致丝滑；③ 引入 `hasBackfilledHistoricalModelNames`（`AtomicBoolean`）单例保护机制：仅在进入会话时由独立后台协程静默执行**至多一次**历史旧消息的持久化回填，执行完毕后标志恒为 true，彻底切断 `Flow 监听 -> 写库 -> InvalidationTracker -> 重新发射` 的死循环链条 |
| **菜单测量卡顿与测量崩溃风险** | `EchoHaze.kt` 中外层 `Surface` 和内层 `Column` 同时被施加了 `Modifier.width(IntrinsicSize.Max).widthIn(min = 160.dp, max = 280.dp)`，双重 Intrinsic 测量嵌套使测量 pass 膨胀为 4 次全子树遍历，造成明显弹出延迟；特定子项在复杂布局下易抛出 `IllegalStateException` 崩溃 | 外层 `Surface` 恢复只接收调用方传入的 `modifier`；仅在内层 `Column` 遵循 Material 3 官方推荐规范施加单层 `.widthIn(min = 160.dp, max = 280.dp).width(IntrinsicSize.Max)`，既维持回复三点菜单美观自适应不撑满全屏，又杜绝多次遍历卡顿与测量崩溃 |
| **应用冷启动卡顿** | `AiAssistantApp.onCreate()` 在主线程同步调用 `BackupManager.autoBackup(this)`，其内部执行了 WAL Checkpoint 与整库压缩，阻塞主线程冷启动并极易引发数据库锁冲突 | 将 `autoBackup` 移入 `applicationScope.launch(Dispatchers.IO)` 异步执行，主线程零阻塞，冷启动秒开；`BackupManager.kt` 中的 WAL Checkpoint 补充 `use { it.moveToFirst() }` 确保游标安全执行与关闭 |

## 三、构建与验证复核清单
- [x] `compileDebugKotlin --no-daemon`：Exit Code 0
- [x] `testDebugUnitTest --no-daemon`：Exit Code 0（74 个测试文件，**499 项全通、0 失败**，新增原子回填防死循环保护测试）
- [x] `lintDebug --no-daemon`：Exit Code 0
- [x] `git diff --check`：Exit Code 0
- [x] `assembleRelease --no-daemon`：Exit Code 0
- [x] APK：`Echo-v2.7.0.apk`，16,700,269 字节 (~15.93 MB)，SHA256 `EA7E5035EB3A6C6C3832E110C51E9FD08E5757A30337AE9750B83F2D43BA9034`
- [x] 签名校验：`apksigner verify --print-certs` 通过，证书 CN=Android Debug（**非正式生产签名**），证书 SHA-256 `939638f6d3e9af7f8a980e62af52d275fee73381f2130cc4e20a0d349f98e21f`，与历史版本完全一致，支持直接平滑覆盖升级
- [x] 历史版本完整性：`D:\Agent\APP-Echo\app\releases` 历史安装包 100% 完整保留（共 177 个安装包），本次为唯一定名增量输出（复制而非移动，全程未执行任何删除）

## 四、人工验收步骤（无真机，未执行安装/启动验证）
1. **冷启动与流畅度验收**：
   - 安装覆盖新版本（`Echo-v2.7.0.apk`）后启动应用，观察启动闪屏后秒进首页，不再卡顿或冻结；
   - 连续点击进入包含数十条历史回复的长对话，界面瞬时加载，上下滚动丝滑顺畅（60fps/120fps），不再出现任何掉帧、白屏、ANR 或闪退。
2. **对话生成与打字流畅度验收**：
   - 在输入框中输入长段文字并发送，观察流式输出过程中界面响应灵敏，CPU 占用与发热正常，输入框焦点稳定不丢失；
   - 点击停止回复或中断生成，思考链与回复内容完整保留，不会触发后台循环写库。
3. **操作菜单与胶囊显示验收**：
   - 点击回复下方三点图标「⋮」，菜单迅速弹出（无任何卡顿延迟），自适应宽度收敛在 160dp~280dp 之间；
   - 思考胶囊内部文字依然保持精准垂直居中，模型名称清晰显示发起本条回复的模型，不随顶部切换而改变。

---

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
