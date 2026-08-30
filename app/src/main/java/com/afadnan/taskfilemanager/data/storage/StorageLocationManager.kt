package com.afadnan.taskfilemanager.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class StorageLocationManager(
    private val context: Context,
    private val storagePreferences: StoragePreferences
) {

    /*
     * =====================================================
     * SELECTED FOLDER
     * =====================================================
     */

    val selectedFolderUri: Flow<Uri?>
        get() = storagePreferences.selectedFolderUri


    /*
     * =====================================================
     * SAVE SELECTED FOLDER
     * =====================================================
     */

    suspend fun saveSelectedFolder(
        uri: Uri,
        flags: Int
    ) = withContext(Dispatchers.IO) {

        val takeFlags =
            flags and
                    (
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )

        if (takeFlags != 0) {

            try {

                context.contentResolver
                    .takePersistableUriPermission(
                        uri,
                        takeFlags
                    )

            } catch (_: SecurityException) {
                // Some SAF providers do not support
                // persistable permissions.
            }
        }

        storagePreferences.saveSelectedFolder(uri)
    }


    /*
     * =====================================================
     * GET SELECTED FOLDER
     * =====================================================
     */

    suspend fun getSelectedFolder():
            StorageItem.Document? =
        withContext(Dispatchers.IO) {

            val uri =
                storagePreferences
                    .selectedFolderUri
                    .first()
                    ?: return@withContext null

            val hasPermission =
                context.contentResolver
                    .persistedUriPermissions
                    .any { permission ->

                        permission.uri == uri &&
                                (
                                        permission.isReadPermission ||
                                                permission.isWritePermission
                                        )
                    }

            if (!hasPermission) {

                storagePreferences
                    .clearSelectedFolder()

                return@withContext null
            }

            val document =
                DocumentFile.fromTreeUri(
                    context,
                    uri
                )
                    ?: return@withContext null

            if (!document.exists()) {
                return@withContext null
            }

            if (!document.isDirectory) {
                return@withContext null
            }

            StorageItem.Document(
                uri = uri,
                documentFile = document
            )
        }


    /*
     * =====================================================
     * CLEAR SELECTED FOLDER
     * =====================================================
     */

    suspend fun clearSelectedFolder() =
        withContext(Dispatchers.IO) {

            val uri =
                storagePreferences
                    .selectedFolderUri
                    .first()

            if (uri != null) {

                try {

                    context.contentResolver
                        .releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                } catch (_: SecurityException) {
                    // Permission may already be revoked.
                }
            }

            storagePreferences
                .clearSelectedFolder()
        }


    /*
     * =====================================================
     * COPY DOCUMENT
     * =====================================================
     *
     * Copies a file or complete directory.
     *
     * Progress is reported using REAL bytes copied.
     */

    suspend fun copyDocument(
        source: StorageItem.Document,
        destination: StorageItem.Document,
        onProgress: (FileOperationProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {

        val sourceFile =
            source.documentFile

        val destinationDirectory =
            destination.documentFile

        validateSourceAndDestination(
            sourceFile = sourceFile,
            destinationDirectory = destinationDirectory
        )

        val totalBytes =
            calculateTotalSize(sourceFile)

        var copiedBytes = 0L

        onProgress(
            FileOperationProgress(
                currentFile =
                    sourceFile.name ?: "Copying",

                currentBytes = 0L,

                totalBytes = totalBytes,

                isIndeterminate =
                    totalBytes <= 0L
            )
        )

        copiedBytes =
            if (sourceFile.isDirectory) {

                copyDirectory(
                    source = sourceFile,

                    destinationParent =
                        destinationDirectory,

                    totalBytes =
                        totalBytes,

                    copiedBytes =
                        copiedBytes,

                    onProgress =
                        onProgress
                )

            } else {

                copyFile(
                    source = sourceFile,

                    destinationParent =
                        destinationDirectory,

                    totalBytes =
                        totalBytes,

                    copiedBytes =
                        copiedBytes,

                    onProgress =
                        onProgress
                )
            }

        onProgress(
            FileOperationProgress(
                currentFile =
                    sourceFile.name ?: "Complete",

                currentBytes =
                    copiedBytes,

                totalBytes =
                    totalBytes,

                isIndeterminate = false
            )
        )
    }


    /*
     * =====================================================
     * MOVE DOCUMENT
     * =====================================================
     *
     * SAF-safe move:
     *
     * COPY
     *   ↓
     * DELETE ORIGINAL
     *
     * Progress represents the COPY phase.
     */

    suspend fun moveDocument(
        source: StorageItem.Document,
        destination: StorageItem.Document,
        onProgress: (FileOperationProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {

        val sourceFile =
            source.documentFile

        val destinationDirectory =
            destination.documentFile

        validateSourceAndDestination(
            sourceFile = sourceFile,
            destinationDirectory =
                destinationDirectory
        )

        val totalBytes =
            calculateTotalSize(sourceFile)

        var copiedBytes = 0L

        onProgress(
            FileOperationProgress(
                currentFile =
                    sourceFile.name ?: "Moving",

                currentBytes = 0L,

                totalBytes =
                    totalBytes,

                isIndeterminate =
                    totalBytes <= 0L
            )
        )

        copiedBytes =
            if (sourceFile.isDirectory) {

                copyDirectory(
                    source =
                        sourceFile,

                    destinationParent =
                        destinationDirectory,

                    totalBytes =
                        totalBytes,

                    copiedBytes =
                        copiedBytes,

                    onProgress =
                        onProgress
                )

            } else {

                copyFile(
                    source =
                        sourceFile,

                    destinationParent =
                        destinationDirectory,

                    totalBytes =
                        totalBytes,

                    copiedBytes =
                        copiedBytes,

                    onProgress =
                        onProgress
                )
            }

        /*
         * Only delete the original after the
         * complete copy succeeds.
         */

        if (!sourceFile.delete()) {

            throw IllegalStateException(
                "Item was copied, but the original could not be deleted"
            )
        }

        onProgress(
            FileOperationProgress(
                currentFile =
                    sourceFile.name ?: "Complete",

                currentBytes =
                    copiedBytes,

                totalBytes =
                    totalBytes,

                isIndeterminate = false
            )
        )
    }


    /*
     * =====================================================
     * VALIDATE SOURCE / DESTINATION
     * =====================================================
     */

    private fun validateSourceAndDestination(
        sourceFile: DocumentFile,
        destinationDirectory: DocumentFile
    ) {

        if (!sourceFile.exists()) {

            throw IllegalStateException(
                "Source does not exist"
            )
        }

        if (
            !destinationDirectory.exists() ||
            !destinationDirectory.isDirectory
        ) {

            throw IllegalStateException(
                "Destination is not a directory"
            )
        }

        if (
            sourceFile.uri ==
            destinationDirectory.uri
        ) {

            throw IllegalArgumentException(
                "Cannot copy or move an item into itself"
            )
        }

        if (
            sourceFile.isDirectory &&
            isDescendant(
                source = sourceFile,
                possibleChild =
                    destinationDirectory
            )
        ) {

            throw IllegalArgumentException(
                "Cannot copy or move a folder into itself"
            )
        }
    }


    /*
     * =====================================================
     * COPY FILE
     * =====================================================
     *
     * Returns the new total number of bytes copied.
     */

    private fun copyFile(
        source: DocumentFile,
        destinationParent: DocumentFile,
        totalBytes: Long,
        copiedBytes: Long,
        onProgress: (FileOperationProgress) -> Unit
    ): Long {

        val originalName =
            source.name
                ?: throw IllegalStateException(
                    "Source file has no name"
                )

        /*
         * Automatically resolve duplicate names.
         *
         * Example:
         *
         * report.pdf
         * report (1).pdf
         * report (2).pdf
         */

        val fileName =
            generateUniqueName(
                parent =
                    destinationParent,

                originalName =
                    originalName
            )

        val mimeType =
            source.type
                ?: "application/octet-stream"

        val destinationFile =
            destinationParent.createFile(
                mimeType,
                fileName
            )
                ?: throw IllegalStateException(
                    "Unable to create destination file"
                )

        var totalCopied =
            copiedBytes

        try {

            val input =
                context.contentResolver
                    .openInputStream(
                        source.uri
                    )
                    ?: throw IllegalStateException(
                        "Unable to open source file"
                    )

            val output =
                context.contentResolver
                    .openOutputStream(
                        destinationFile.uri
                    )
                    ?: throw IllegalStateException(
                        "Unable to open destination file"
                    )

            input.use { inputStream ->

                output.use { outputStream ->

                    val buffer =
                        ByteArray(64 * 1024)

                    while (true) {

                        val bytesRead =
                            inputStream.read(
                                buffer
                            )

                        if (bytesRead == -1) {
                            break
                        }

                        outputStream.write(
                            buffer,
                            0,
                            bytesRead
                        )

                        totalCopied +=
                            bytesRead.toLong()

                        onProgress(
                            FileOperationProgress(
                                currentFile =
                                    fileName,

                                currentBytes =
                                    totalCopied,

                                totalBytes =
                                    totalBytes,

                                isIndeterminate =
                                    totalBytes <= 0L
                            )
                        )
                    }

                    outputStream.flush()
                }
            }

        } catch (exception: Exception) {

            try {
                destinationFile.delete()
            } catch (_: Exception) {
            }

            throw exception
        }

        return totalCopied
    }


    /*
     * =====================================================
     * COPY DIRECTORY
     * =====================================================
     *
     * Recursively copies directories while keeping
     * one shared byte counter.
     */

    private fun copyDirectory(
        source: DocumentFile,
        destinationParent: DocumentFile,
        totalBytes: Long,
        copiedBytes: Long,
        onProgress: (FileOperationProgress) -> Unit
    ): Long {

        val directoryName =
            source.name
                ?: throw IllegalStateException(
                    "Source folder has no name"
                )

        /*
         * Duplicate folder names are also handled.
         */

        val uniqueDirectoryName =
            generateUniqueName(
                parent =
                    destinationParent,

                originalName =
                    directoryName
            )

        val destinationDirectory =
            destinationParent.createDirectory(
                uniqueDirectoryName
            )
                ?: throw IllegalStateException(
                    "Unable to create destination folder"
                )

        var totalCopied =
            copiedBytes

        try {

            val children =
                source.listFiles()

            children.forEach { child ->

                totalCopied =
                    if (child.isDirectory) {

                        copyDirectory(
                            source =
                                child,

                            destinationParent =
                                destinationDirectory,

                            totalBytes =
                                totalBytes,

                            copiedBytes =
                                totalCopied,

                            onProgress =
                                onProgress
                        )

                    } else {

                        copyFile(
                            source =
                                child,

                            destinationParent =
                                destinationDirectory,

                            totalBytes =
                                totalBytes,

                            copiedBytes =
                                totalCopied,

                            onProgress =
                                onProgress
                        )
                    }
            }

        } catch (exception: Exception) {

            try {
                destinationDirectory.delete()
            } catch (_: Exception) {
            }

            throw exception
        }

        return totalCopied
    }


    /*
     * =====================================================
     * CHECK DESCENDANT
     * =====================================================
     */

    private fun isDescendant(
        source: DocumentFile,
        possibleChild: DocumentFile
    ): Boolean {

        var current =
            possibleChild.parentFile

        while (current != null) {

            if (
                current.uri ==
                source.uri
            ) {

                return true
            }

            current =
                current.parentFile
        }

        return false
    }


    /*
     * =====================================================
     * DELETE DOCUMENT
     * =====================================================
     */

    suspend fun deleteDocument(
        item: StorageItem.Document
    ): Boolean =
        withContext(Dispatchers.IO) {

            item.documentFile.delete()
        }


    /*
     * =====================================================
     * RENAME DOCUMENT
     * =====================================================
     */

    suspend fun renameDocument(
        item: StorageItem.Document,
        newName: String
    ): Boolean =
        withContext(Dispatchers.IO) {

            val name =
                newName.trim()

            if (name.isEmpty()) {
                return@withContext false
            }

            item.documentFile.renameTo(
                name
            )
        }


    /*
     * =====================================================
     * GENERATE UNIQUE NAME
     * =====================================================
     */

    private fun generateUniqueName(
        parent: DocumentFile,
        originalName: String
    ): String {

        if (
            parent.findFile(
                originalName
            ) == null
        ) {

            return originalName
        }

        val dotIndex =
            originalName.lastIndexOf('.')

        val hasExtension =
            dotIndex > 0

        val baseName =
            if (hasExtension) {

                originalName.substring(
                    0,
                    dotIndex
                )

            } else {

                originalName
            }

        val extension =
            if (hasExtension) {

                originalName.substring(
                    dotIndex
                )

            } else {

                ""
            }

        var counter = 1

        while (true) {

            val candidate =
                "$baseName ($counter)$extension"

            if (
                parent.findFile(
                    candidate
                ) == null
            ) {

                return candidate
            }

            counter++
        }
    }


    /*
     * =====================================================
     * CALCULATE TOTAL SIZE
     * =====================================================
     */

    private fun calculateTotalSize(
        document: DocumentFile
    ): Long {

        if (!document.exists()) {
            return 0L
        }

        if (document.isDirectory) {

            return document
                .listFiles()
                .sumOf { child ->

                    calculateTotalSize(
                        child
                    )
                }
        }

        return document.length()
    }
}