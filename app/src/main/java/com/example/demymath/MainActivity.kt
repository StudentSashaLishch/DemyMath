package com.example.demymath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.demymath.ui.theme.DemyMathTheme
import androidx.annotation.StringRes
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.demymath.data.AppDatabase
import com.example.demymath.data.AppRepository

sealed class Screen(
    val route: String,
    @StringRes val labelResourceId: Int,
    val icon: ImageVector
) {
    object KnowledgeGraph : Screen("graph", R.string.nav_graph, Icons.Default.AccountTree)
    object Statistics : Screen("statistics", R.string.nav_stats, Icons.Default.BarChart)
    object Profile : Screen("profile", R.string.nav_profile, Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val repository = AppRepository(db)

        val viewModel: SharedViewModel by viewModels { SharedViewModelFactory(repository) }

        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            DemyMathTheme(darkTheme = isDarkTheme) {
                MainScreen(repository, viewModel, isDarkTheme, onThemeChange = { isDarkTheme = it })
            }
        }
    }
}

@Composable
fun MainScreen(
    repository: AppRepository,
    viewModel: SharedViewModel,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(Screen.KnowledgeGraph, Screen.Statistics, Screen.Profile)
    val currentUserId by viewModel.currentUserId.collectAsState()

    LaunchedEffect(Unit) {
        repository.checkAndRefreshRepetitions(1)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelResourceId)) },
                        label = { Text(stringResource(screen.labelResourceId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.KnowledgeGraph.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.KnowledgeGraph.route) {
                KnowledgeGraphScreen(currentUserId, repository, navController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    repository = repository,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange
                )
            }
            composable("learning/{topicId}") { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                LearningScreen(topicId, currentUserId, repository, navController)
            }
            composable("quiz/{topicId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("topicId") ?: ""
                QuizScreen(
                    topicId = id,
                    userId = currentUserId,
                    repository = repository,
                    navController = navController
                )
            }
            composable(
                route = "reflection/{topicId}/{score}",
                arguments = listOf(
                    navArgument("topicId") { type = NavType.StringType },
                    navArgument("score") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                val score = backStackEntry.arguments?.getInt("score") ?: 0
                val currentUserId by viewModel.currentUserId.collectAsState()

                FinalReflectionScreen(
                    topicId = topicId,
                    score = score,
                    userId = currentUserId,
                    repository = repository,
                    navController = navController
                )
            }
            composable("statistics") {
                GeneralStatisticsScreen(viewModel, navController)
            }
            composable(
                route = "topic_stats/{topicId}",
                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                val currentUserId by viewModel.currentUserId.collectAsState()

                TopicStatisticsScreen(
                    topicId = topicId,
                    userId = currentUserId,
                    repository = repository,
                    navController = navController
                )
            }
        }
    }
}