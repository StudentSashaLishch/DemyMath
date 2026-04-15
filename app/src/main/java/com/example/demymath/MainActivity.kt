package com.example.demymath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.example.demymath.R
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

        enableEdgeToEdge()
        setContent {
            DemyMathTheme {
                MainScreen(repository)
            }
        }
    }
}

@Composable
fun MainScreen(repository: AppRepository) {
    val navController = rememberNavController()
    val items = listOf(Screen.KnowledgeGraph, Screen.Statistics, Screen.Profile)

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
                KnowledgeGraphScreen(repository, navController)
            }
            composable(Screen.Profile.route) {
                SimpleScreen(stringResource(R.string.screen_profile_title))
            }
            composable("learning/{topicId}") { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                LearningScreen(topicId, repository, navController)
            }
            composable("quiz/{topicId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("topicId") ?: ""
                QuizScreen(id, repository, navController)
            }
            composable("reflection/{topicId}/{score}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("topicId") ?: ""
                val score = backStackEntry.arguments?.getString("score")?.toInt() ?: 0
                FinalReflectionScreen(id, score, repository, navController)
            }
            composable("statistics") {
                GeneralStatisticsScreen(repository, navController)
            }
            composable(
                route = "topic_stats/{topicId}",
                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                TopicStatisticsScreen(topicId, repository, navController)
            }
        }
    }
}

@Composable
fun SimpleScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}