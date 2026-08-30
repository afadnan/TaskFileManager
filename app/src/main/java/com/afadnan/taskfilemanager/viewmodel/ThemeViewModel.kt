package com.afadnan.taskfilemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afadnan.taskfilemanager.data.preferences.AppTheme
import com.afadnan.taskfilemanager.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val theme: StateFlow<AppTheme> =
        themePreferences.theme.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppTheme.SYSTEM_DEFAULT
        )

    fun setTheme(
        theme: AppTheme
    ) {
        viewModelScope.launch {
            themePreferences.setTheme(theme)
        }
    }
}