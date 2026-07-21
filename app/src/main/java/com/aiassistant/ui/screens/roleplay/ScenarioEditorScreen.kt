package com.aiassistant.ui.screens.roleplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.domain.model.RoleplayScenario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioEditorScreen(
    scenario: RoleplayScenario? = null,
    onSave: (RoleplayScenario) -> Unit,
    onDelete: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(scenario?.name ?: "") }
    var worldview by remember { mutableStateOf(scenario?.worldview ?: "") }
    var time by remember { mutableStateOf(scenario?.time ?: "") }
    var location by remember { mutableStateOf(scenario?.location ?: "") }
    var environment by remember { mutableStateOf(scenario?.environment ?: "") }
    var premise by remember { mutableStateOf(scenario?.premise ?: "") }
    var rules by remember { mutableStateOf(scenario?.rules ?: "") }
    var relationshipState by remember { mutableStateOf(scenario?.relationshipState ?: "") }
    var conflict by remember { mutableStateOf(scenario?.conflict ?: "") }
    var plotGoal by remember { mutableStateOf(scenario?.plotGoal ?: "") }
    var atmosphere by remember { mutableStateOf(scenario?.atmosphere ?: "") }
    var narrativePerspective by remember { mutableStateOf(scenario?.narrativePerspective ?: "") }
    var outputFormat by remember { mutableStateOf(scenario?.outputFormat ?: "") }
    var contentRestrictions by remember { mutableStateOf(scenario?.contentRestrictions ?: "") }
    var openingPrompt by remember { mutableStateOf(scenario?.openingPrompt ?: "") }
    var tags by remember { mutableStateOf(scenario?.tags ?: "") }
    var isFavorite by remember { mutableStateOf(scenario?.isFavorite ?: false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scenario == null) "创建场景" else "编辑场景") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = true }) {
                        Icon(Icons.Default.Preview, contentDescription = "预览")
                    }
                    if (scenario != null) {
                        IconButton(onClick = { isFavorite = !isFavorite }) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 分区选择器
            ScrollableTabRow(
                selectedTabIndex = currentSection,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = currentSection == 0,
                    onClick = { currentSection = 0 },
                    text = { Text("基本设定") }
                )
                Tab(
                    selected = currentSection == 1,
                    onClick = { currentSection = 1 },
                    text = { Text("世界观") }
                )
                Tab(
                    selected = currentSection == 2,
                    onClick = { currentSection = 2 },
                    text = { Text("剧情设定") }
                )
                Tab(
                    selected = currentSection == 3,
                    onClick = { currentSection = 3 },
                    text = { Text("叙事控制") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 内容区域
            when (currentSection) {
                0 -> BasicSettingSection(
                    name = name,
                    onNameChange = { name = it },
                    time = time,
                    onTimeChange = { time = it },
                    location = location,
                    onLocationChange = { location = it },
                    environment = environment,
                    onEnvironmentChange = { environment = it },
                    tags = tags,
                    onTagsChange = { tags = it }
                )
                1 -> WorldBuildingSection(
                    worldview = worldview,
                    onWorldviewChange = { worldview = it },
                    rules = rules,
                    onRulesChange = { rules = it },
                    relationshipState = relationshipState,
                    onRelationshipStateChange = { relationshipState = it }
                )
                2 -> PlotSettingSection(
                    premise = premise,
                    onPremiseChange = { premise = it },
                    conflict = conflict,
                    onConflictChange = { conflict = it },
                    plotGoal = plotGoal,
                    onPlotGoalChange = { plotGoal = it },
                    openingPrompt = openingPrompt,
                    onOpeningPromptChange = { openingPrompt = it }
                )
                3 -> NarrativeControlSection(
                    atmosphere = atmosphere,
                    onAtmosphereChange = { atmosphere = it },
                    narrativePerspective = narrativePerspective,
                    onNarrativePerspectiveChange = { narrativePerspective = it },
                    outputFormat = outputFormat,
                    onOutputFormatChange = { outputFormat = it },
                    contentRestrictions = contentRestrictions,
                    onContentRestrictionsChange = { contentRestrictions = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newScenario = RoleplayScenario(
                            id = scenario?.id ?: 0,
                            name = name,
                            worldview = worldview,
                            time = time,
                            location = location,
                            environment = environment,
                            premise = premise,
                            rules = rules,
                            relationshipState = relationshipState,
                            conflict = conflict,
                            plotGoal = plotGoal,
                            atmosphere = atmosphere,
                            narrativePerspective = narrativePerspective,
                            outputFormat = outputFormat,
                            contentRestrictions = contentRestrictions,
                            openingPrompt = openingPrompt,
                            tags = tags.ifBlank { null },
                            isFavorite = isFavorite,
                            createdAt = scenario?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(newScenario)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存场景")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除场景\"${name}\"吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 预览对话框
    if (showPreview) {
        ScenarioPreviewDialog(
            scenario = RoleplayScenario(
                id = scenario?.id ?: 0,
                name = name,
                worldview = worldview,
                time = time,
                location = location,
                environment = environment,
                premise = premise,
                rules = rules,
                relationshipState = relationshipState,
                conflict = conflict,
                plotGoal = plotGoal,
                atmosphere = atmosphere,
                narrativePerspective = narrativePerspective,
                outputFormat = outputFormat,
                contentRestrictions = contentRestrictions,
                openingPrompt = openingPrompt,
                tags = tags
            ),
            onDismiss = { showPreview = false }
        )
    }
}

@Composable
private fun BasicSettingSection(
    name: String,
    onNameChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    environment: String,
    onEnvironmentChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "基本设定",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("场景名称 *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = name.isBlank()
        )

        OutlinedTextField(
            value = time,
            onValueChange = onTimeChange,
            label = { Text("时间") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：中世纪、未来2150年、现代") }
        )

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("地点") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：魔法学院、太空站、古代宫廷") }
        )

        OutlinedTextField(
            value = environment,
            onValueChange = onEnvironmentChange,
            label = { Text("环境描述") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("描述场景的具体环境和氛围") }
        )

        OutlinedTextField(
            value = tags,
            onValueChange = onTagsChange,
            label = { Text("标签") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("用逗号分隔，例如：奇幻,冒险,校园") }
        )
    }
}

@Composable
private fun WorldBuildingSection(
    worldview: String,
    onWorldviewChange: (String) -> Unit,
    rules: String,
    onRulesChange: (String) -> Unit,
    relationshipState: String,
    onRelationshipStateChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "世界观设定",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = worldview,
            onValueChange = onWorldviewChange,
            label = { Text("世界观") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("描述这个世界的基本设定、历史背景") }
        )

        OutlinedTextField(
            value = rules,
            onValueChange = onRulesChange,
            label = { Text("世界规则") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("这个世界的运行规则，例如魔法体系、科技水平") }
        )

        OutlinedTextField(
            value = relationshipState,
            onValueChange = onRelationshipStateChange,
            label = { Text("角色关系状态") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("当前角色之间的关系状态") }
        )
    }
}

@Composable
private fun PlotSettingSection(
    premise: String,
    onPremiseChange: (String) -> Unit,
    conflict: String,
    onConflictChange: (String) -> Unit,
    plotGoal: String,
    onPlotGoalChange: (String) -> Unit,
    openingPrompt: String,
    onOpeningPromptChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "剧情设定",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = premise,
            onValueChange = onPremiseChange,
            label = { Text("剧情前提") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("故事开始的背景和起因") }
        )

        OutlinedTextField(
            value = conflict,
            onValueChange = onConflictChange,
            label = { Text("当前冲突") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("目前的主要矛盾和冲突") }
        )

        OutlinedTextField(
            value = plotGoal,
            onValueChange = onPlotGoalChange,
            label = { Text("剧情目标") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("故事要达成的目标") }
        )

        OutlinedTextField(
            value = openingPrompt,
            onValueChange = onOpeningPromptChange,
            label = { Text("开场提示") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("用于引导模型生成开场的内容") }
        )
    }
}

@Composable
private fun NarrativeControlSection(
    atmosphere: String,
    onAtmosphereChange: (String) -> Unit,
    narrativePerspective: String,
    onNarrativePerspectiveChange: (String) -> Unit,
    outputFormat: String,
    onOutputFormatChange: (String) -> Unit,
    contentRestrictions: String,
    onContentRestrictionsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "叙事控制",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = atmosphere,
            onValueChange = onAtmosphereChange,
            label = { Text("叙事氛围") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("例如：紧张刺激、温馨治愈、黑暗压抑") }
        )

        OutlinedTextField(
            value = narrativePerspective,
            onValueChange = onNarrativePerspectiveChange,
            label = { Text("叙事视角") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：第一人称、第三人称全知、第三人称有限") }
        )

        OutlinedTextField(
            value = outputFormat,
            onValueChange = onOutputFormatChange,
            label = { Text("输出格式") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("期望的输出格式，例如：包含动作描写和对话") }
        )

        OutlinedTextField(
            value = contentRestrictions,
            onValueChange = onContentRestrictionsChange,
            label = { Text("内容限制") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("内容的限制和禁忌") }
        )
    }
}

@Composable
private fun ScenarioPreviewDialog(
    scenario: RoleplayScenario,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("场景预览: ${scenario.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (scenario.worldview.isNotBlank()) {
                    PreviewItem("世界观", scenario.worldview)
                }
                if (scenario.time.isNotBlank()) {
                    PreviewItem("时间", scenario.time)
                }
                if (scenario.location.isNotBlank()) {
                    PreviewItem("地点", scenario.location)
                }
                if (scenario.environment.isNotBlank()) {
                    PreviewItem("环境描述", scenario.environment)
                }
                if (scenario.premise.isNotBlank()) {
                    PreviewItem("剧情前提", scenario.premise)
                }
                if (scenario.rules.isNotBlank()) {
                    PreviewItem("世界规则", scenario.rules)
                }
                if (scenario.relationshipState.isNotBlank()) {
                    PreviewItem("关系状态", scenario.relationshipState)
                }
                if (scenario.conflict.isNotBlank()) {
                    PreviewItem("当前冲突", scenario.conflict)
                }
                if (scenario.plotGoal.isNotBlank()) {
                    PreviewItem("剧情目标", scenario.plotGoal)
                }
                if (scenario.atmosphere.isNotBlank()) {
                    PreviewItem("叙事氛围", scenario.atmosphere)
                }
                if (scenario.narrativePerspective.isNotBlank()) {
                    PreviewItem("叙事视角", scenario.narrativePerspective)
                }
                if (scenario.outputFormat.isNotBlank()) {
                    PreviewItem("输出格式", scenario.outputFormat)
                }
                if (scenario.contentRestrictions.isNotBlank()) {
                    PreviewItem("内容限制", scenario.contentRestrictions)
                }
                if (scenario.openingPrompt.isNotBlank()) {
                    PreviewItem("开场提示", scenario.openingPrompt)
                }
                if (!scenario.tags.isNullOrBlank()) {
                    PreviewItem("标签", scenario.tags!!)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun PreviewItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
