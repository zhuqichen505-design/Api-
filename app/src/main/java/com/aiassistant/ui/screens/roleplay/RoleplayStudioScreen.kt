package com.aiassistant.ui.screens.roleplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.domain.model.CharacterProfile
import com.aiassistant.domain.model.RoleplayScenario
import com.aiassistant.domain.model.RoleplaySession
import com.aiassistant.ui.components.echoHazePanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleplayStudioScreen(
    viewModel: RoleplayViewModel,
    onNavigateToCharacters: () -> Unit,
    onNavigateToScenarios: () -> Unit,
    onNavigateToCharacterEditor: (CharacterProfile?) -> Unit,
    onNavigateToScenarioEditor: (RoleplayScenario?) -> Unit,
    onNavigateToSession: (Long) -> Unit,
    onCreateNewSession: () -> Unit,
    onBack: () -> Unit
) {
    val characters by viewModel.characters.collectAsState()
    val scenarios by viewModel.scenarios.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("角色扮演工作室") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateNewSession) {
                        Icon(Icons.Default.Add, contentDescription = "新建会话")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 标签页
            TabRow(selectedTabIndex = currentTab) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("会话") },
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("角色") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text("场景") },
                    icon = { Icon(Icons.Default.Landscape, contentDescription = null) }
                )
            }

            // 内容区域
            when (currentTab) {
                0 -> SessionsTab(
                    sessions = sessions,
                    onSessionClick = { onNavigateToSession(it.id) },
                    onDeleteSession = { viewModel.deleteSession(it) }
                )
                1 -> CharactersTab(
                    characters = characters,
                    onCharacterClick = { onNavigateToCharacterEditor(it) },
                    onCreateClick = { onNavigateToCharacterEditor(null) },
                    onFavoriteClick = { id, isFavorite -> viewModel.setCharacterFavorite(id, isFavorite) }
                )
                2 -> ScenariosTab(
                    scenarios = scenarios,
                    onScenarioClick = { onNavigateToScenarioEditor(it) },
                    onCreateClick = { onNavigateToScenarioEditor(null) },
                    onFavoriteClick = { id, isFavorite -> viewModel.setScenarioFavorite(id, isFavorite) }
                )
            }
        }
    }

    // 处理UI状态
    when (val state = uiState) {
        is RoleplayUiState.SaveSuccess -> {
            LaunchedEffect(state) {
                viewModel.clearUiState()
            }
        }
        is RoleplayUiState.Error -> {
            LaunchedEffect(state) {
                viewModel.clearUiState()
            }
        }
        else -> {}
    }
}

@Composable
private fun SessionsTab(
    sessions: List<RoleplaySession>,
    onSessionClick: (RoleplaySession) -> Unit,
    onDeleteSession: (RoleplaySession) -> Unit
) {
    if (sessions.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Chat,
            title = "暂无会话",
            description = "点击右上角的 + 按钮创建新的角色扮演会话"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session) },
                    onDelete = { onDeleteSession(session) }
                )
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: RoleplaySession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
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
                Text(
                    text = "会话 #${session.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (session.currentPlotSummary.isNotBlank()) {
                    Text(
                        text = session.currentPlotSummary.take(50) + if (session.currentPlotSummary.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "模式: ${session.narrativeMode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个会话吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
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
}

@Composable
private fun CharactersTab(
    characters: List<CharacterProfile>,
    onCharacterClick: (CharacterProfile) -> Unit,
    onCreateClick: () -> Unit,
    onFavoriteClick: (Long, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 创建按钮
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建新角色")
        }

        if (characters.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Person,
                title = "暂无角色",
                description = "点击上方按钮创建你的第一个角色"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(characters) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { onCharacterClick(character) },
                        onFavoriteClick = { onFavoriteClick(character.id, !character.isFavorite) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(
    character: CharacterProfile,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (character.identity.isNotBlank()) {
                    Text(
                        text = character.identity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!character.tags.isNullOrBlank()) {
                    Text(
                        text = character.tags!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (character.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (character.isFavorite) "取消收藏" else "收藏",
                    tint = if (character.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScenariosTab(
    scenarios: List<RoleplayScenario>,
    onScenarioClick: (RoleplayScenario) -> Unit,
    onCreateClick: () -> Unit,
    onFavoriteClick: (Long, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 创建按钮
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建新场景")
        }

        if (scenarios.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Landscape,
                title = "暂无场景",
                description = "点击上方按钮创建你的第一个场景"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scenarios) { scenario ->
                    ScenarioCard(
                        scenario = scenario,
                        onClick = { onScenarioClick(scenario) },
                        onFavoriteClick = { onFavoriteClick(scenario.id, !scenario.isFavorite) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: RoleplayScenario,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
                Text(
                    text = scenario.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (scenario.worldview.isNotBlank()) {
                    Text(
                        text = scenario.worldview.take(50) + if (scenario.worldview.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!scenario.tags.isNullOrBlank()) {
                    Text(
                        text = scenario.tags!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (scenario.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (scenario.isFavorite) "取消收藏" else "收藏",
                    tint = if (scenario.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
