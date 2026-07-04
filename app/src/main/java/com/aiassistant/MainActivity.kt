package com.aiassistant

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aiassistant.ui.screens.chat.ChatScreen
import com.aiassistant.ui.screens.history.HistoryScreen
import com.aiassistant.ui.screens.home.FolderManagerScreen
import com.aiassistant.ui.screens.home.HomeScreen
import com.aiassistant.ui.screens.settings.SettingsScreen
import com.aiassistant.ui.screens.stats.StatsScreen
import com.aiassistant.ui.theme.AiApiAssistantTheme

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
            AiApiAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiAssistantNavigation()
                }
            }
        }
    }
}

@Composable
fun AiAssistantNavigation() {
    val navController = rememberNavController()

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
                }
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
