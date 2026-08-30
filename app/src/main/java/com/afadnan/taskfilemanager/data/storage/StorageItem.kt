package com.afadnan.taskfilemanager.data.storage

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

sealed interface StorageItem {

    val name: String
    val isDirectory: Boolean

    /**
     * Normal filesystem item.
     *
     * Example:
     * /storage/emulated/0/Download/report.pdf
     */
    data class LocalFile(
        val file: File
    ) : StorageItem {

        override val name: String
            get() = file.name

        override val isDirectory: Boolean
            get() = file.isDirectory
    }

    /**
     * Existing SAF document.
     *
     * Used when browsing files/directories on:
     * - SD card
     * - user-selected SAF locations
     * - other document providers
     */
    data class Document(
        val uri: Uri,
        val documentFile: DocumentFile
    ) : StorageItem {

        override val name: String
            get() = documentFile.name ?: "Unnamed"

        override val isDirectory: Boolean
            get() = documentFile.isDirectory
    }

    /**
     * A file that does not exist yet.
     *
     * This represents:
     *
     * parent directory + desired filename
     *
     * It is particularly important for SAF because we cannot
     * construct an arbitrary non-existing DocumentFile URI.
     */
    data class DocumentTarget(
        val parentUri: Uri,
        val parentDocument: DocumentFile,
        val fileName: String
    ) : StorageItem {

        override val name: String
            get() = fileName

        override val isDirectory: Boolean
            get() = false
    }
}