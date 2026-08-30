package com.afadnan.taskfilemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afadnan.taskfilemanager.data.local.TaskDatabase
import com.afadnan.taskfilemanager.data.preferences.AppTheme
import com.afadnan.taskfilemanager.data.preferences.ThemePreferences
import com.afadnan.taskfilemanager.data.repository.TaskRepository
import com.afadnan.taskfilemanager.data.storage.StorageLocationManager
import com.afadnan.taskfilemanager.data.storage.StoragePreferences
import com.afadnan.taskfilemanager.navigation.AppNavigation
import com.afadnan.taskfilemanager.ui.theme.TaskFileManagerTheme
import com.afadnan.taskfilemanager.viewmodel.FileViewModel
import com.afadnan.taskfilemanager.viewmodel.FileViewModelFactory
import com.afadnan.taskfilemanager.viewmodel.TaskViewModel
import com.afadnan.taskfilemanager.viewmodel.TaskViewModelFactory
import com.afadnan.taskfilemanager.viewmodel.ThemeViewModel
import com.afadnan.taskfilemanager.viewmodel.ThemeViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * =====================================================
         * DATABASE
         * =====================================================
         */

        val database =
            TaskDatabase.getDatabase(
                applicationContext
            )

        /*
         * =====================================================
         * TASK REPOSITORY
         * =====================================================
         */

        val repository =
            TaskRepository(
                database.taskDao()
            )

        /*
         * =====================================================
         * STORAGE
         * =====================================================
         */

        val storagePreferences =
            StoragePreferences(
                applicationContext
            )

        val storageLocationManager =
            StorageLocationManager(
                context = applicationContext,
                storagePreferences = storagePreferences
            )

        /*
         * =====================================================
         * THEME PREFERENCES
         * =====================================================
         */

        val themePreferences =
            ThemePreferences(
                applicationContext
            )

        /*
         * =====================================================
         * COMPOSE
         * =====================================================
         */

        setContent {

            /*
             * =================================================
             * TASK VIEWMODEL
             * =================================================
             */

            val taskViewModel: TaskViewModel =
                viewModel(
                    factory =
                        TaskViewModelFactory(
                            repository
                        )
                )

            /*
             * =================================================
             * FILE VIEWMODEL
             * =================================================
             */

            val fileViewModel: FileViewModel =
                viewModel(
                    factory =
                        FileViewModelFactory(
                            storageLocationManager
                        )
                )

            /*
             * =================================================
             * THEME VIEWMODEL
             * =================================================
             */

            val themeViewModel: ThemeViewModel =
                viewModel(
                    factory =
                        ThemeViewModelFactory(
                            themePreferences
                        )
                )

            /*
             * =================================================
             * CURRENT THEME
             * =================================================
             */

            val selectedTheme =
                themeViewModel
                    .theme
                    .collectAsState()
                    .value

            /*
             * =================================================
             * CONVERT APP THEME → DARK/LIGHT
             * =================================================
             */

            val darkTheme =
                when (selectedTheme) {

                    AppTheme.DARK -> {
                        true
                    }

                    AppTheme.LIGHT -> {
                        false
                    }

                    AppTheme.SYSTEM_DEFAULT -> {
                        isSystemInDarkTheme()
                    }

                    else -> {
                        isSystemInDarkTheme()
                    }
                }

            /*
             * =================================================
             * APPLICATION THEME
             * =================================================
             */

            TaskFileManagerTheme(
                darkTheme = darkTheme
            ) {

                AppNavigation(
                    taskViewModel = taskViewModel,
                    fileViewModel = fileViewModel,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}