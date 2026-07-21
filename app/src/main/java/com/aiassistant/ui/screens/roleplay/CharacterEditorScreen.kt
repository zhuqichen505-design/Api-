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
import com.aiassistant.domain.model.CharacterProfile
import com.aiassistant.ui.components.echoHazePanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditorScreen(
    character: CharacterProfile? = null,
    onSave: (CharacterProfile) -> Unit,
    onDelete: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var identity by remember { mutableStateOf(character?.identity ?: "") }
    var personality by remember { mutableStateOf(character?.personality ?: "") }
    var background by remember { mutableStateOf(character?.background ?: "") }
    var speakingStyle by remember { mutableStateOf(character?.speakingStyle ?: "") }
    var goals by remember { mutableStateOf(character?.goals ?: "") }
    var relationships by remember { mutableStateOf(character?.relationships ?: "") }
    var knowledge by remember { mutableStateOf(character?.knowledge ?: "") }
    var constraints by remember { mutableStateOf(character?.constraints ?: "") }
    var behaviorRules by remember { mutableStateOf(character?.behaviorRules ?: "") }
    var greeting by remember { mutableStateOf(character?.greeting ?: "") }
    var exampleDialogue by remember { mutableStateOf(character?.exampleDialogue ?: "") }
    var tags by remember { mutableStateOf(character?.tags ?: "") }
    var isFavorite by remember { mutableStateOf(character?.isFavorite ?: false) }
    var isDefault by remember { mutableStateOf(character?.isDefault ?: false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (character == null) "创建角色" else "编辑角色") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = true }) {
                        Icon(Icons.Default.Preview, contentDescription = "预览")
                    }
                    if (character != null) {
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
                    text = { Text("基本信息") }
                )
                Tab(
                    selected = currentSection == 1,
                    onClick = { currentSection = 1 },
                    text = { Text("性格背景") }
                )
                Tab(
                    selected = currentSection == 2,
                    onClick = { currentSection = 2 },
                    text = { Text("行为设定") }
                )
                Tab(
                    selected = currentSection == 3,
                    onClick = { currentSection = 3 },
                    text = { Text("对话示例") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 内容区域
            when (currentSection) {
                0 -> BasicInfoSection(
                    name = name,
                    onNameChange = { name = it },
                    identity = identity,
                    onIdentityChange = { identity = it },
                    tags = tags,
                    onTagsChange = { tags = it },
                    isDefault = isDefault,
                    onIsDefaultChange = { isDefault = it }
                )
                1 -> PersonalitySection(
                    personality = personality,
                    onPersonalityChange = { personality = it },
                    background = background,
                    onBackgroundChange = { background = it },
                    speakingStyle = speakingStyle,
                    onSpeakingStyleChange = { speakingStyle = it },
                    goals = goals,
                    onGoalsChange = { goals = it }
                )
                2 -> BehaviorSection(
                    relationships = relationships,
                    onRelationshipsChange = { relationships = it },
                    knowledge = knowledge,
                    onKnowledgeChange = { knowledge = it },
                    constraints = constraints,
                    onConstraintsChange = { constraints = it },
                    behaviorRules = behaviorRules,
                    onBehaviorRulesChange = { behaviorRules = it }
                )
                3 -> DialogueSection(
                    greeting = greeting,
                    onGreetingChange = { greeting = it },
                    exampleDialogue = exampleDialogue,
                    onExampleDialogueChange = { exampleDialogue = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newCharacter = CharacterProfile(
                            id = character?.id ?: 0,
                            name = name,
                            avatarUri = character?.avatarUri,
                            identity = identity,
                            personality = personality,
                            background = background,
                            speakingStyle = speakingStyle,
                            goals = goals,
                            relationships = relationships,
                            knowledge = knowledge,
                            constraints = constraints,
                            behaviorRules = behaviorRules,
                            greeting = greeting,
                            exampleDialogue = exampleDialogue,
                            tags = tags.ifBlank { null },
                            isFavorite = isFavorite,
                            isDefault = isDefault,
                            createdAt = character?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(newCharacter)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存角色")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除角色\"${name}\"吗？此操作不可恢复。") },
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
        CharacterPreviewDialog(
            character = CharacterProfile(
                id = character?.id ?: 0,
                name = name,
                avatarUri = character?.avatarUri,
                identity = identity,
                personality = personality,
                background = background,
                speakingStyle = speakingStyle,
                goals = goals,
                relationships = relationships,
                knowledge = knowledge,
                constraints = constraints,
                behaviorRules = behaviorRules,
                greeting = greeting,
                exampleDialogue = exampleDialogue,
                tags = tags,
                isFavorite = isFavorite,
                isDefault = isDefault
            ),
            onDismiss = { showPreview = false }
        )
    }
}

@Composable
private fun BasicInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    identity: String,
    onIdentityChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    isDefault: Boolean,
    onIsDefaultChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "基本信息",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("角色名称 *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = name.isBlank()
        )

        OutlinedTextField(
            value = identity,
            onValueChange = onIdentityChange,
            label = { Text("身份/职业") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：魔法师、侦探、学生") }
        )

        OutlinedTextField(
            value = tags,
            onValueChange = onTagsChange,
            label = { Text("标签") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("用逗号分隔，例如：奇幻,主角,女性") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("设为默认角色")
            Switch(
                checked = isDefault,
                onCheckedChange = onIsDefaultChange
            )
        }
    }
}

@Composable
private fun PersonalitySection(
    personality: String,
    onPersonalityChange: (String) -> Unit,
    background: String,
    onBackgroundChange: (String) -> Unit,
    speakingStyle: String,
    onSpeakingStyleChange: (String) -> Unit,
    goals: String,
    onGoalsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "性格与背景",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = personality,
            onValueChange = onPersonalityChange,
            label = { Text("性格特征") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("描述角色的性格特点，例如：温柔、坚强、幽默") }
        )

        OutlinedTextField(
            value = background,
            onValueChange = onBackgroundChange,
            label = { Text("背景故事") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("角色的过去经历和重要事件") }
        )

        OutlinedTextField(
            value = speakingStyle,
            onValueChange = onSpeakingStyleChange,
            label = { Text("说话方式和语言风格") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("例如：文雅、直率、带有口癖") }
        )

        OutlinedTextField(
            value = goals,
            onValueChange = onGoalsChange,
            label = { Text("目标、动机和当前欲望") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("角色想要达成的目标") }
        )
    }
}

@Composable
private fun BehaviorSection(
    relationships: String,
    onRelationshipsChange: (String) -> Unit,
    knowledge: String,
    onKnowledgeChange: (String) -> Unit,
    constraints: String,
    onConstraintsChange: (String) -> Unit,
    behaviorRules: String,
    onBehaviorRulesChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "行为设定",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = relationships,
            onValueChange = onRelationshipsChange,
            label = { Text("关系设定") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("与其他角色或用户的关系") }
        )

        OutlinedTextField(
            value = knowledge,
            onValueChange = onKnowledgeChange,
            label = { Text("已知信息和知识边界") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("角色知道什么，不知道什么") }
        )

        OutlinedTextField(
            value = constraints,
            onValueChange = onConstraintsChange,
            label = { Text("禁止违背的设定") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("绝对不能违反的规则") }
        )

        OutlinedTextField(
            value = behaviorRules,
            onValueChange = onBehaviorRulesChange,
            label = { Text("行为约束") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("角色应该遵守的行为准则") }
        )
    }
}

@Composable
private fun DialogueSection(
    greeting: String,
    onGreetingChange: (String) -> Unit,
    exampleDialogue: String,
    onExampleDialogueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "对话示例",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = greeting,
            onValueChange = onGreetingChange,
            label = { Text("初始问候语") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("角色在会话开始时的第一句话") }
        )

        OutlinedTextField(
            value = exampleDialogue,
            onValueChange = onExampleDialogueChange,
            label = { Text("示例对话") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            placeholder = { Text("提供一些示例对话，展示角色的说话风格") }
        )
    }
}

@Composable
private fun CharacterPreviewDialog(
    character: CharacterProfile,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("角色预览: ${character.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (character.identity.isNotBlank()) {
                    PreviewItem("身份/职业", character.identity)
                }
                if (character.personality.isNotBlank()) {
                    PreviewItem("性格特征", character.personality)
                }
                if (character.background.isNotBlank()) {
                    PreviewItem("背景故事", character.background)
                }
                if (character.speakingStyle.isNotBlank()) {
                    PreviewItem("说话方式", character.speakingStyle)
                }
                if (character.goals.isNotBlank()) {
                    PreviewItem("目标动机", character.goals)
                }
                if (character.relationships.isNotBlank()) {
                    PreviewItem("关系设定", character.relationships)
                }
                if (character.knowledge.isNotBlank()) {
                    PreviewItem("知识边界", character.knowledge)
                }
                if (character.constraints.isNotBlank()) {
                    PreviewItem("禁止违背", character.constraints)
                }
                if (character.behaviorRules.isNotBlank()) {
                    PreviewItem("行为约束", character.behaviorRules)
                }
                if (character.greeting.isNotBlank()) {
                    PreviewItem("初始问候", character.greeting)
                }
                if (character.exampleDialogue.isNotBlank()) {
                    PreviewItem("示例对话", character.exampleDialogue)
                }
                if (!character.tags.isNullOrBlank()) {
                    PreviewItem("标签", character.tags!!)
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
