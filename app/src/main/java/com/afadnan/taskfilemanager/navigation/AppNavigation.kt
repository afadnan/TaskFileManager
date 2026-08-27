package com.afadnan.taskfilemanager.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.taskfilemanager.ui.files.FilesScreen
import com.example.taskfilemanager.ui.home.HomeScreen
import com.example.taskfilemanager.ui.settings.SettingsScreen
import com.example.taskfilemanager.ui.tasks.TasksScreen

private const val HOME_ROUTE = "home"
private const val TASKS_ROUTE = "tasks"
private const val FILES_ROUTE = "files"
private const val SETTINGS_ROUTE = "settings"

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val navItems = listOf(
        BottomNavItem(
            route = HOME_ROUTE,
            label = "Home",
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            }
        ),
        BottomNavItem(
            route = TASKS_ROUTE,
            label = "Tasks",
            icon = {
                Icon(
                    imageVector = Icons.Default.Task,
                    contentDescription = "Tasks"
                )
            }
        ),
        BottomNavItem(
            route = FILES_ROUTE,
            label = "Files",
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Files"
                )
            }
        ),
        BottomNavItem(
            route = SETTINGS_ROUTE,
            label = "Settings",
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->

                    NavigationBarItem(
                        selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true,

                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(HOME_ROUTE) {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        },

                        icon = item.icon,

                        label = {
                            Text(item.label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier,
        ) {

            composable(HOME_ROUTE) {
                HomeScreen()
            }

            composable(TASKS_ROUTE) {
                TasksScreen()
            }

            composable(FILES_ROUTE) {
                FilesScreen()
            }

            composable(SETTINGS_ROUTE) {
                SettingsScreen()
            }
        }
    }
}