package com.afadnan.taskfilemanager.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferences(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "theme_preferences",
            Context.MODE_PRIVATE
        )

    private val themeKey = "app_theme"

    val theme: Flow<AppTheme> =
        kotlinx.coroutines.flow.flow {
            val savedTheme =
                preferences.getString(
                    themeKey,
                    AppTheme.SYSTEM_DEFAULT.name
                )

            emit(
                try {
                    AppTheme.valueOf(
                        savedTheme
                            ?: AppTheme.SYSTEM_DEFAULT.name
                    )
                } catch (_: Exception) {
                    AppTheme.SYSTEM_DEFAULT
                }
            )
        }

    suspend fun setTheme(
        theme: AppTheme
    ) {
        preferences
            .edit()
            .putString(
                themeKey,
                theme.name
            )
            .apply()
    }
}