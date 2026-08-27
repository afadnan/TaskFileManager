package com.afadnan.taskfilemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.afadnan.taskfilemanager.navigation.AppNavigation
import com.afadnan.taskfilemanager.ui.theme.TaskFileManagerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TaskFileManagerTheme {
                AppNavigation()
            }
        }
    }
}