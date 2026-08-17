# Echo AI 助手更新日志 (Update Log)

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
