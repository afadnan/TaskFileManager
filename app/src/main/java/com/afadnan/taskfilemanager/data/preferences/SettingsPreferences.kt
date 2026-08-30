package com.afadnan.taskfilemanager.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.afadnan.taskfilemanager.data.preferences.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "task_file_manager_settings"
)

class SettingsPreferences(
    private val context: Context
) {

    companion object {

        private val THEME_KEY =
            stringPreferencesKey("theme")
    }


    /*
     * =========================================================
     * CURRENT THEME
     * =========================================================
     */

    val theme: Flow<AppTheme> =
        context.dataStore.data.map { preferences ->

            when (
                preferences[THEME_KEY]
            ) {

                "LIGHT" ->
                    AppTheme.LIGHT

                "DARK" ->
                    AppTheme.DARK

                else ->
                    AppTheme.SYSTEM_DEFAULT
            }
        }


    /*
     * =========================================================
     * SAVE THEME
     * =========================================================
     */

    suspend fun setTheme(
        theme: AppTheme
    ) {

        context.dataStore.edit { preferences ->

            preferences[THEME_KEY] =
                theme.name
        }
    }
}