package com.afadnan.taskfilemanager.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.afadnan.taskfilemanager.ui.tasks.TasksScreen
import com.afadnan.taskfilemanager.viewmodel.TaskViewModel
import com.example.taskfilemanager.ui.files.FilesScreen
import com.example.taskfilemanager.ui.home.HomeScreen
import com.example.taskfilemanager.ui.settings.SettingsScreen

private data class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val destinations = listOf(
    AppDestination(
        route = "home",
        label = "Home",
        icon = Icons.Default.Home
    ),
    AppDestination(
        route = "tasks",
        label = "Tasks",
        icon = Icons.Default.Check
    ),
    AppDestination(
        route = "files",
        label = "Files",
        icon = Icons.Default.Folder
    ),
    AppDestination(
        route = "settings",
        label = "Settings",
        icon = Icons.Default.Settings
    )
)

@Composable
fun AppNavigation(
    taskViewModel: TaskViewModel
) {

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            AppBottomNavigation(
                navController = navController
            )
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {

            composable("home") {
                HomeScreen()
            }

            composable("tasks") {
                TasksScreen(
                    viewModel = taskViewModel
                )
            }

            composable("files") {
                FilesScreen()
            }

            composable("settings") {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(
    navController: NavHostController
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {

        destinations.forEach { destination ->

            NavigationBarItem(

                selected = currentRoute == destination.route,

                onClick = {

                    navController.navigate(destination.route) {

                        popUpTo("home") {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                },

                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },

                label = {
                    Text(destination.label)
                }
            )
        }
    }
}