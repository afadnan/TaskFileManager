
package com.afadnan.taskfilemanager.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.afadnan.taskfilemanager.data.storage.StorageItem

@Composable
fun FileList(
    items: List<StorageItem>,
    isLoading: Boolean,
    selectedItems: Set<StorageItem>,
    onItemClick: (StorageItem) -> Unit,
    onItemLongClick: (StorageItem) -> Unit
) {

    /*
     * =====================================================
     * EMPTY STATE
     * =====================================================
     */

    if (items.isEmpty() && !isLoading) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "This folder is empty"
            )
        }

        return
    }


    /*
     * =====================================================
     * SELECTED ITEMS
     * =====================================================
     *
     * Keep a stable reference to the current selection
     * during recomposition.
     */

    val selected =
        remember(selectedItems) {
            selectedItems
        }


    /*
     * =====================================================
     * FILE LIST
     * =====================================================
     *
     * The existing list remains visible while loading.
     */

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            items(
                items = items,

                /*
                 * =================================================
                 * STABLE KEY
                 * =================================================
                 */

                key = { item ->

                    when (item) {

                        is StorageItem.Document -> {

                            "document:${item.uri}"
                        }

                        is StorageItem.LocalFile -> {

                            "local:${item.file.absolutePath}"
                        }

                        is StorageItem.DocumentTarget -> {

                            "target:${item.parentUri}/${item.fileName}"
                        }
                    }
                },

                /*
                 * =================================================
                 * CONTENT TYPE
                 * =================================================
                 */

                contentType = { item ->

                    when (item) {

                        is StorageItem.Document ->
                            "document"

                        is StorageItem.LocalFile ->
                            "local"

                        is StorageItem.DocumentTarget ->
                            "target"
                    }
                }

            ) { item ->

                FileItem(
                    item = item,

                    isSelected =
                        selected.contains(item),

                    onClick = {
                        onItemClick(item)
                    },

                    onLongClick = {
                        onItemLongClick(item)
                    }
                )
            }
        }


        /*
         * =====================================================
         * LOADING INDICATOR
         * =====================================================
         *
         * The list stays visible while refreshing.
         */

        if (isLoading) {

            CircularProgressIndicator(
                modifier =
                    Modifier.align(
                        Alignment.Center
                    )
            )
        }
    }
}
