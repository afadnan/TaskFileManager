package com.afadnan.taskfilemanager.data.storage

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storageDataStore by preferencesDataStore(
    name = "storage_preferences"
)

class StoragePreferences(
    private val context: Context
) {

    companion object {

        private val SELECTED_TREE_URI =
            stringPreferencesKey(
                "selected_tree_uri"
            )
    }

    /**
     * Saves the currently selected SAF folder URI.
     */
    suspend fun saveSelectedFolder(
        uri: Uri
    ) {

        context.storageDataStore.edit { preferences ->

            preferences[SELECTED_TREE_URI] =
                uri.toString()
        }
    }

    /**
     * Returns the currently saved folder URI.
     *
     * Emits null when the user has not selected a folder yet.
     */
    val selectedFolderUri: Flow<Uri?>
        get() =
            context.storageDataStore.data.map { preferences ->

                preferences[SELECTED_TREE_URI]
                    ?.let(Uri::parse)
            }

    /**
     * Removes the saved folder URI.
     *
     * Note:
     *
     * This does NOT revoke Android's SAF permission.
     * It only removes our application's remembered location.
     */
    suspend fun clearSelectedFolder() {

        context.storageDataStore.edit { preferences ->

            preferences.remove(
                SELECTED_TREE_URI
            )
        }
    }
}