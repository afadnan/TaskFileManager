package com.afadnan.taskfilemanager.ui.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afadnan.taskfilemanager.data.storage.StorageItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    item: StorageItem,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {

    /*
     * =====================================================
     * ITEM TYPE
     * =====================================================
     */

    val isDirectory =
        item.isDirectory


    /*
     * =====================================================
     * ITEM NAME
     * =====================================================
     */

    val name =
        item.name


    /*
     * =====================================================
     * FILE SIZE
     * =====================================================
     *
     * We only read metadata here.
     *
     * No file contents are opened.
     */

    val sizeText =
        when (item) {

            is StorageItem.LocalFile -> {

                if (item.file.isDirectory) {

                    null

                } else {

                    formatFileSize(
                        item.file.length()
                    )
                }
            }

            is StorageItem.Document -> {

                if (item.documentFile.isDirectory) {

                    null

                } else {

                    item.documentFile.length()
                        .takeIf { it >= 0 }
                        ?.let { size ->
                            formatFileSize(size)
                        }
                }
            }

            is StorageItem.DocumentTarget -> {
                null
            }
        }


    /*
     * =====================================================
     * MODIFIED DATE
     * =====================================================
     */

    val modifiedText =
        when (item) {

            is StorageItem.LocalFile -> {

                formatModifiedDate(
                    item.file.lastModified()
                )
            }

            is StorageItem.Document -> {

                item.documentFile
                    .lastModified()
                    .takeIf { it > 0 }
                    ?.let { timestamp ->
                        formatModifiedDate(timestamp)
                    }
            }

            is StorageItem.DocumentTarget -> {
                null
            }
        }


    /*
     * =====================================================
     * SECONDARY INFORMATION
     * =====================================================
     */

    val secondaryText =
        when {

            isDirectory && modifiedText != null ->
                modifiedText

            isDirectory ->
                "Folder"

            sizeText != null && modifiedText != null ->
                "$sizeText • $modifiedText"

            sizeText != null ->
                sizeText

            modifiedText != null ->
                modifiedText

            else ->
                "File"
        }


    /*
     * =====================================================
     * FILE ROW
     * =====================================================
     */

    Row(
        modifier =
            Modifier
                .fillMaxWidth()

                /*
                 * Selected item background.
                 */

                .background(
                    if (isSelected) {

                        MaterialTheme
                            .colorScheme
                            .secondaryContainer

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surface
                    }
                )

                /*
                 * Tap + long press.
                 */

                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )

                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * =================================================
         * ICON
         * =================================================
         */

        Icon(
            imageVector =
                if (isDirectory) {

                    Icons.Default.Folder

                } else {

                    Icons.Default.InsertDriveFile
                },

            contentDescription =
                if (isDirectory) {

                    "Folder"

                } else {

                    "File"
                },

            tint =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        /*
         * =================================================
         * NAME + DETAILS
         * =================================================
         */

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = 16.dp
                    )
        ) {

            /*
             * File/folder name.
             */

            Text(
                text = name,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )


            /*
             * Size + modified date.
             */

            Text(
                text = secondaryText,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                modifier =
                    Modifier.padding(
                        top = 2.dp
                    )
            )
        }
    }
}


/*
 * =========================================================
 * FORMAT FILE SIZE
 * =========================================================
 */

private fun formatFileSize(
    bytes: Long
): String {

    if (bytes < 1024) {

        return "$bytes B"
    }

    if (bytes < 1024 * 1024) {

        return String.format(
            Locale.getDefault(),
            "%.1f KB",
            bytes / 1024.0
        )
    }

    if (bytes < 1024 * 1024 * 1024) {

        return String.format(
            Locale.getDefault(),
            "%.1f MB",
            bytes /
                    (1024.0 * 1024.0)
        )
    }

    return String.format(
        Locale.getDefault(),
        "%.1f GB",
        bytes /
                (1024.0 * 1024.0 * 1024.0)
    )
}


/*
 * =========================================================
 * FORMAT MODIFIED DATE
 * =========================================================
 */

private fun formatModifiedDate(
    timestamp: Long
): String {

    return SimpleDateFormat(
        "dd MMM yyyy, HH:mm",
        Locale.getDefault()
    ).format(
        Date(timestamp)
    )
}
