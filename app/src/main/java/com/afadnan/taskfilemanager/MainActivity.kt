package com.afadnan.taskfilemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afadnan.taskfilemanager.data.local.TaskDatabase
import com.afadnan.taskfilemanager.data.repository.TaskRepository
import com.afadnan.taskfilemanager.ui.theme.TaskFileManagerTheme
import com.afadnan.taskfilemanager.viewmodel.TaskViewModelFactory
import com.afadnan.taskfilemanager.viewmodel.TaskViewModel
import com.afadnan.taskfilemanager.navigation.AppNavigation


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = TaskDatabase.getDatabase(
            applicationContext
        )

        val repository = TaskRepository(
            database.taskDao()
        )

        setContent {

            val taskViewModel: TaskViewModel = viewModel(
                factory = TaskViewModelFactory(repository)
            )

            TaskFileManagerTheme {

                AppNavigation(
                    taskViewModel = taskViewModel
                )
            }
        }
    }
}