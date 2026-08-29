package com.afadnan.taskfilemanager.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class StorageManager(
    private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "storage_preferences"
        private const val KEY_ROOT_URI = "root_uri"
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun saveRootUri(uri: Uri): Boolean {
        return try {
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver.takePersistableUriPermission(
                uri,
                flags
            )

            preferences
                .edit()
                .putString(KEY_ROOT_URI, uri.toString())
                .apply()

            true

        } catch (exception: SecurityException) {
            false
        } catch (exception: Exception) {
            false
        }
    }

    fun getRootUri(): Uri? {

        val uriString =
            preferences.getString(
                KEY_ROOT_URI,
                null
            )

        return uriString?.let(Uri::parse)
    }

    fun hasPersistedPermission(uri: Uri): Boolean {

        return context.contentResolver
            .persistedUriPermissions
            .any { permission ->

                permission.uri == uri &&
                        permission.isReadPermission
            }
    }

    fun isRootAvailable(): Boolean {

        val uri = getRootUri()
            ?: return false

        if (!hasPersistedPermission(uri)) {
            return false
        }

        return try {

            val directory =
                DocumentFile.fromTreeUri(
                    context,
                    uri
                )

            directory?.exists() == true &&
                    directory.isDirectory

        } catch (exception: Exception) {
            false
        }
    }

    fun getRootDirectory(): DocumentFile? {

        val uri = getRootUri()
            ?: return null

        if (!hasPersistedPermission(uri)) {
            return null
        }

        return try {

            DocumentFile.fromTreeUri(
                context,
                uri
            )

        } catch (exception: Exception) {
            null
        }
    }

    fun clearRootUri() {

        val uri = getRootUri()

        if (uri != null) {
            try {

                context.contentResolver
                    .releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

            } catch (_: Exception) {
                // Permission may already be unavailable.
            }
        }

        preferences
            .edit()
            .remove(KEY_ROOT_URI)
            .apply()
    }
}