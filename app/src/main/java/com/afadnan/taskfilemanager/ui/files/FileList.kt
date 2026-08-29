package com.afadnan.taskfilemanager.ui.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.afadnan.taskfilemanager.data.storage.FileItem

@Composable
fun FileList(
    files: List<FileItem>,
    onFileClick: (FileItem) -> Unit
) {
    if (files.isEmpty()) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "This folder is empty"
            )
        }

        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = files,
            key = { it.uri.toString() }
        ) { file ->

            FileItemRow(
                file = file,
                onClick = {
                    onFileClick(file)
                }
            )

            HorizontalDivider()
        }
    }
}
