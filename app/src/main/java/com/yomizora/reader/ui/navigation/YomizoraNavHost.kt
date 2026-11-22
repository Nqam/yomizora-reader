package com.yomizora.reader.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.ui.home.HomeScreen
import com.yomizora.reader.ui.library.LibraryScreen
import com.yomizora.reader.ui.quiz.QuizScreen
import com.yomizora.reader.ui.reading.ReadingScreen
import com.yomizora.reader.ui.settings.SettingsScreen
import com.yomizora.reader.ui.words.WordsReviewScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination("home", "Главная", Icons.Rounded.Home),
    TopLevelDestination("library", "Библиотека", Icons.Rounded.AutoStories),
    TopLevelDestination("words", "Слова", Icons.Rounded.Style),
    TopLevelDestination("settings", "Настройки", Icons.Rounded.Settings)
)

@Composable
fun YomizoraNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute == "home" ||
        currentRoute?.startsWith("library") == true ||
        currentRoute == "words" ||
        currentRoute == "settings"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = backStackEntry?.destination?.hierarchy?.any {
                            it.route?.substringBefore("?") == dest.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (dest.route == "home") {
                                    val popped = navController.popBackStack("home", inclusive = false)
                                    if (!popped) {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = false
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onOpenLevel = { level ->
                        navController.navigate("library?level=${level.code}") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onOpenMaterial = { materialId ->
                        navController.navigate("reading/$materialId")
                    }
                )
            }

            composable(
                route = "library?level={level}",
                arguments = listOf(
                    navArgument("level") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val levelCode = entry.arguments?.getString("level")
                val initialLevel = levelCode?.let { JlptLevel.fromCode(it) }
                LibraryScreen(
                    initialLevel = initialLevel,
                    onOpenMaterial = { materialId ->
                        navController.navigate("reading/$materialId")
                    }
                )
            }

            composable("words") {
                WordsReviewScreen()
            }

            composable("settings") {
                SettingsScreen()
            }

            composable(
                route = "reading/{materialId}",
                arguments = listOf(navArgument("materialId") { type = NavType.StringType })
            ) {
                ReadingScreen(
                    onBack = { navController.popBackStack() },
                    onStartQuiz = { materialId ->
                        navController.navigate("quiz/$materialId")
                    }
                )
            }

            composable(
                route = "quiz/{materialId}",
                arguments = listOf(navArgument("materialId") { type = NavType.StringType })
            ) {
                QuizScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNextMaterial = { nextId ->
                        navController.navigate("reading/$nextId") {
                            popUpTo("reading/{materialId}") { inclusive = true }
                        }
                    },
                    onBackToLibrary = {
                        val popped = navController.popBackStack("library?level={level}", inclusive = false)
                        if (!popped) {
                            navController.navigate("library") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                    saveState = false
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}
