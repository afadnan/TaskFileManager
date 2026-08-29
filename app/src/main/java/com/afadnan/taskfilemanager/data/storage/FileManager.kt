package com.afadnan.taskfilemanager.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

class FileManager(
    private val context: Context
) {

    fun listFiles(
        directoryUri: Uri
    ): Result<List<FileItem>> {

        return try {

            val directory =
                DocumentFile.fromTreeUri(
                    context,
                    directoryUri
                )
                    ?: return Result.failure(
                        IllegalStateException(
                            "Unable to access directory."
                        )
                    )

            if (!directory.exists()) {
                return Result.failure(
                    IllegalStateException(
                        "Directory no longer exists."
                    )
                )
            }

            if (!directory.isDirectory) {
                return Result.failure(
                    IllegalStateException(
                        "Selected location is not a directory."
                    )
                )
            }

            val files = directory
                .listFiles()
                .map { file ->

                    FileItem(
                        name = file.name ?: "Unnamed",
                        uri = file.uri,
                        isDirectory = file.isDirectory,
                        size =
                            if (file.isFile) {
                                file.length()
                            } else {
                                0L
                            },
                        lastModified =
                            file.lastModified()
                    )
                }
                .sortedWith(
                    compareByDescending<FileItem> {
                        it.isDirectory
                    }.thenBy {
                        it.name.lowercase()
                    }
                )

            Result.success(files)

        } catch (exception: SecurityException) {

            Result.failure(
                SecurityException(
                    "Storage permission was denied.",
                    exception
                )
            )

        } catch (exception: Exception) {

            Result.failure(exception)
        }
    }
}