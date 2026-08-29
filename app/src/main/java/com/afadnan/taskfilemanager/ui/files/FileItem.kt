package com.afadnan.taskfilemanager.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afadnan.taskfilemanager.data.storage.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileItemRow(
    file: FileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                if (file.isDirectory) {
                    Icons.Default.Folder
                } else {
                    Icons.Default.Description
                },
            contentDescription =
                if (file.isDirectory) {
                    "Folder"
                } else {
                    "File"
                }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge
            )

            /*
             * Display metadata only for files.
             *
             * Example:
             * 2.4 MB • Modified 29 Aug 2026
             */
            if (!file.isDirectory) {

                val modified =
                    formatModifiedDate(
                        file.lastModified
                    )

                Text(
                    text =
                        "${formatFileSize(file.size)} • Modified $modified",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

private fun formatFileSize(
    size: Long
): String {

    if (size <= 0) {
        return "0 B"
    }

    val units = arrayOf(
        "B",
        "KB",
        "MB",
        "GB",
        "TB"
    )

    var value = size.toDouble()
    var index = 0

    while (
        value >= 1024 &&
        index < units.lastIndex
    ) {
        value /= 1024
        index++
    }

    return if (index == 0) {
        "${value.toLong()} ${units[index]}"
    } else {
        "%.1f %s".format(
            value,
            units[index]
        )
    }
}

private fun formatModifiedDate(
    timestamp: Long
): String {

    if (timestamp <= 0L) {
        return ""
    }

    val formatter =
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )

    return formatter.format(
        Date(timestamp)
    )
}
