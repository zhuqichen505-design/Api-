package com.aiassistant.ui.screens.roleplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRoleplaySessionScreen(
    viewModel: RoleplayViewModel,
    apiConfigs: List<ApiConfig>,
    onStartSession: (Long?, Long?, Long, NarrativeMode) -> Unit,
    onBack: () -> Unit
) {
    val characters by viewModel.characters.collectAsState()
    val scenarios by viewModel.scenarios.collectAsState()

    var selectedCharacter by remember { mutableStateOf<CharacterProfile?>(null) }
    var selectedScenario by remember { mutableStateOf<RoleplayScenario?>(null) }
    var selectedApiConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var selectedNarrativeMode by remember { mutableStateOf(NarrativeMode.CHARACTER) }
    var showCharacterPicker by remember { mutableStateOf(false) }
    var showScenarioPicker by remember { mutableStateOf(false) }
    var showApiConfigPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建角色扮演会话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 角色选择
            Text(
                text = "选择角色",
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                onClick = { showCharacterPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedCharacter != null) {
                            Text(
                                text = selectedCharacter!!.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (selectedCharacter!!.identity.isNotBlank()) {
                                Text(
                                    text = selectedCharacter!!.identity,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "选择一个角色（可选）",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // 场景选择
            Text(
                text = "选择场景",
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                onClick = { showScenarioPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedScenario != null) {
                            Text(
                                text = selectedScenario!!.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (selectedScenario!!.worldview.isNotBlank()) {
                                Text(
                                    text = selectedScenario!!.worldview.take(50) + if (selectedScenario!!.worldview.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "选择一个场景（可选）",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // API配置选择
            Text(
                text = "选择API配置",
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                onClick = { showApiConfigPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedApiConfig != null) {
                            Text(
                                text = selectedApiConfig!!.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${selectedApiConfig!!.provider} - ${selectedApiConfig!!.modelName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "选择API配置（可选）",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // 叙事模式选择
            Text(
                text = "叙事模式",
                style = MaterialTheme.typography.titleMedium
            )
            NarrativeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedNarrativeMode == mode,
                        onClick = { selectedNarrativeMode = mode }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (mode) {
                                NarrativeMode.CHARACTER -> "以角色身份回应"
                                NarrativeMode.AUTHOR -> "用户控制剧情，模型推进故事"
                                NarrativeMode.NARRATOR -> "只负责叙事，不代替用户"
                                NarrativeMode.MULTI -> "多个角色共同参与"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 开始按钮
            Button(
                onClick = {
                    onStartSession(
                        selectedCharacter?.id,
                        selectedScenario?.id,
                        selectedApiConfig?.id ?: 0,
                        selectedNarrativeMode
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedApiConfig != null
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始角色扮演")
            }
        }
    }

    // 角色选择对话框
    if (showCharacterPicker) {
        CharacterPickerDialog(
            characters = characters,
            selectedCharacter = selectedCharacter,
            onSelect = {
                selectedCharacter = it
                showCharacterPicker = false
            },
            onDismiss = { showCharacterPicker = false }
        )
    }

    // 场景选择对话框
    if (showScenarioPicker) {
        ScenarioPickerDialog(
            scenarios = scenarios,
            selectedScenario = selectedScenario,
            onSelect = {
                selectedScenario = it
                showScenarioPicker = false
            },
            onDismiss = { showScenarioPicker = false }
        )
    }

    // API配置选择对话框
    if (showApiConfigPicker) {
        ApiConfigPickerDialog(
            apiConfigs = apiConfigs,
            selectedApiConfig = selectedApiConfig,
            onSelect = {
                selectedApiConfig = it
                showApiConfigPicker = false
            },
            onDismiss = { showApiConfigPicker = false }
        )
    }
}

@Composable
private fun CharacterPickerDialog(
    characters: List<CharacterProfile>,
    selectedCharacter: CharacterProfile?,
    onSelect: (CharacterProfile?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择角色") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 无角色选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCharacter == null,
                        onClick = { onSelect(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("不使用角色卡")
                }

                // 角色列表
                characters.forEach { character ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCharacter?.id == character.id,
                            onClick = { onSelect(character) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(character.name)
                            if (character.identity.isNotBlank()) {
                                Text(
                                    text = character.identity,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ScenarioPickerDialog(
    scenarios: List<RoleplayScenario>,
    selectedScenario: RoleplayScenario?,
    onSelect: (RoleplayScenario?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择场景") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 无场景选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedScenario == null,
                        onClick = { onSelect(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("不使用场景卡")
                }

                // 场景列表
                scenarios.forEach { scenario ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedScenario?.id == scenario.id,
                            onClick = { onSelect(scenario) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(scenario.name)
                            if (scenario.worldview.isNotBlank()) {
                                Text(
                                    text = scenario.worldview.take(50) + if (scenario.worldview.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ApiConfigPickerDialog(
    apiConfigs: List<ApiConfig>,
    selectedApiConfig: ApiConfig?,
    onSelect: (ApiConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择API配置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                apiConfigs.forEach { config ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedApiConfig?.id == config.id,
                            onClick = { onSelect(config) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(config.name)
                            Text(
                                text = "${config.provider} - ${config.modelName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
