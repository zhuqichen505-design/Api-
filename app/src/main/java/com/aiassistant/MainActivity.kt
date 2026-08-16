package com.aiassistant

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aiassistant.domain.model.CharacterProfile
import com.aiassistant.domain.model.RoleplayScenario
import com.aiassistant.ui.screens.chat.ChatScreen
import com.aiassistant.ui.screens.history.HistoryScreen
import com.aiassistant.ui.screens.home.FolderManagerScreen
import com.aiassistant.ui.screens.home.HomeScreen
import com.aiassistant.ui.screens.roleplay.CharacterEditorScreen
import com.aiassistant.ui.screens.roleplay.NewRoleplaySessionScreen
import com.aiassistant.ui.screens.roleplay.RoleplayMemoryScreen
import com.aiassistant.ui.screens.roleplay.RoleplayStudioScreen
import com.aiassistant.ui.screens.roleplay.RoleplayViewModel
import com.aiassistant.ui.screens.roleplay.ScenarioEditorScreen
import com.aiassistant.ui.screens.settings.SettingsScreen
import com.aiassistant.ui.screens.stats.StatsScreen
import com.aiassistant.ui.theme.AiApiAssistantTheme
import com.aiassistant.utils.AppThemeMode
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val themeManager = remember { AiAssistantApp.instance.themePreferenceManager }
            var themeMode by remember { mutableStateOf(themeManager.getThemeMode()) }
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppThemeMode.System -> systemDark
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            AiApiAssistantTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiAssistantNavigation(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            if (themeManager.saveThemeMode(mode)) {
                                themeMode = mode
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AiAssistantNavigation(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val roleplayViewModel: RoleplayViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // 主页
        composable("home") {
            HomeScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate("chat/$conversationId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                },
                onNavigateToStats = {
                    navController.navigate("stats")
                },
                onNavigateToFolders = {
                    navController.navigate("folders")
                },
                onNavigateToRoleplayStudio = {
                    navController.navigate("roleplay_studio")
                }
            )
        }

        // 对话页面
        composable(
            route = "chat/{conversationId}",
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: return@composable
            ChatScreen(
                conversationId = conversationId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoleplayMemory = { sessionId ->
                    navController.navigate("roleplay_memory/$sessionId")
                }
            )
        }

        // 角色扮演工作室
        composable("roleplay_studio") {
            RoleplayStudioScreen(
                viewModel = roleplayViewModel,
                onNavigateToCharacters = {},
                onNavigateToScenarios = {},
                onNavigateToCharacterEditor = { character ->
                    if (character != null) {
                        navController.navigate("roleplay_character_editor?characterId=${character.id}")
                    } else {
                        navController.navigate("roleplay_character_editor")
                    }
                },
                onNavigateToScenarioEditor = { scenario ->
                    if (scenario != null) {
                        navController.navigate("roleplay_scenario_editor?scenarioId=${scenario.id}")
                    } else {
                        navController.navigate("roleplay_scenario_editor")
                    }
                },
                onNavigateToSession = { conversationId ->
                    navController.navigate("chat/$conversationId")
                },
                onCreateNewSession = {
                    navController.navigate("roleplay_new_session")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 新建角色扮演会话
        composable("roleplay_new_session") {
            val apiConfigs by AiAssistantApp.instance.database.apiConfigDao().getAllConfigs().collectAsState(initial = emptyList())
            NewRoleplaySessionScreen(
                viewModel = roleplayViewModel,
                apiConfigs = apiConfigs,
                onStartSession = { characterIds, scenarioId, apiConfigId, modelName, narrativeMode ->
                    roleplayViewModel.createStorySessionAndStart(
                        characterIds = characterIds,
                        scenarioId = scenarioId,
                        apiConfigId = apiConfigId,
                        modelName = modelName,
                        narrativeMode = narrativeMode,
                        onSuccess = { conversationId ->
                            navController.navigate("chat/$conversationId") {
                                popUpTo("roleplay_studio")
                            }
                        }
                    )
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 角色卡编辑/创建
        composable(
            route = "roleplay_character_editor?characterId={characterId}",
            arguments = listOf(
                navArgument("characterId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getLong("characterId") ?: 0L
            val characters by roleplayViewModel.characters.collectAsState()
            val character = if (characterId > 0) characters.find { it.id == characterId } else null

            CharacterEditorScreen(
                character = character,
                onSave = { savedCharacter ->
                    roleplayViewModel.saveCharacter(savedCharacter)
                    navController.popBackStack()
                },
                onDelete = character?.let {
                    {
                        roleplayViewModel.deleteCharacter(it)
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 场景卡编辑/创建
        composable(
            route = "roleplay_scenario_editor?scenarioId={scenarioId}",
            arguments = listOf(
                navArgument("scenarioId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getLong("scenarioId") ?: 0L
            val scenarios by roleplayViewModel.scenarios.collectAsState()
            val scenario = if (scenarioId > 0) scenarios.find { it.id == scenarioId } else null

            ScenarioEditorScreen(
                scenario = scenario,
                onSave = { savedScenario ->
                    roleplayViewModel.saveScenario(savedScenario)
                    navController.popBackStack()
                },
                onDelete = scenario?.let {
                    {
                        roleplayViewModel.deleteScenario(it)
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 记忆管理页面
        composable(
            route = "roleplay_memory/{sessionId}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            RoleplayMemoryScreen(
                viewModel = roleplayViewModel,
                sessionId = sessionId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 设置页面
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { conversationId ->
                    navController.navigate("chat/$conversationId")
                },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }

        // 历史记录页面
        composable("history") {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { conversationId ->
                    navController.navigate("chat/$conversationId")
                }
            )
        }

        // 统计页面
        composable("stats") {
            StatsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 文件夹管理页面
        composable("folders") {
            FolderManagerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFolderSelected = {
                    navController.popBackStack()
                }
            )
        }
    }
}
