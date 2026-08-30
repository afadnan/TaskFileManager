package com.afadnan.taskfilemanager.ui.files

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.afadnan.taskfilemanager.data.storage.FileOperationProgress
import com.afadnan.taskfilemanager.data.storage.StorageItem
import com.afadnan.taskfilemanager.viewmodel.FileViewModel


/*
 * =============================================================
 * FILE OPERATION PROGRESS BAR
 * =============================================================
 */

@Composable
private fun FileOperationProgressBar(
    progress: FileOperationProgress
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = progress.currentFile,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text =
                        if (progress.totalBytes > 0L) {
                            "${progress.percentage}%"
                        } else {
                            "Working…"
                        },
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (progress.isIndeterminate) {

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

            } else {

                LinearProgressIndicator(
                    progress = {
                        progress.percentage
                            .coerceIn(0, 100)
                            .toFloat() / 100f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


/*
 * =============================================================
 * SEARCH BAR
 * =============================================================
 */

@Composable
private fun FileSearchBar(
    query: String,
    resultCount: Int,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
    ) {

        OutlinedTextField(

            value = query,

            onValueChange = onQueryChange,

            modifier = Modifier
                .fillMaxWidth(),

            singleLine = true,

            shape = RoundedCornerShape(16.dp),

            leadingIcon = {

                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },

            trailingIcon = {

                if (query.isNotEmpty()) {

                    IconButton(
                        onClick = onClear
                    ) {

                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },

            placeholder = {

                Text(
                    text = "Search files and folders"
                )
            },

            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),

            keyboardActions = KeyboardActions(
                onSearch = {
                    // Search is live, so nothing else is required.
                }
            )
        )

        if (query.isNotBlank()) {

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "$resultCount ${
                        if (resultCount == 1) {
                            "result"
                        } else {
                            "results"
                        }
                    }",

                modifier = Modifier.padding(
                    horizontal = 4.dp
                ),

                style = MaterialTheme.typography.labelMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/*
 * =============================================================
 * CURRENT DIRECTORY HEADER
 * =============================================================
 */

@Composable
private fun CurrentDirectoryHeader(
    directoryName: String
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 6.dp
            ),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Folder,

                        contentDescription = null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimaryContainer
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Current folder",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = directoryName,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    maxLines = 1
                )
            }
        }
    }
}


/*
 * =============================================================
 * EMPTY SEARCH RESULT
 * =============================================================
 */

@Composable
private fun EmptySearchResult(
    query: String,
    onClearSearch: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {

            Box(
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Search,

                    contentDescription = null,

                    modifier = Modifier.size(32.dp),

                    tint =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            text = "No files found",
            style =
                MaterialTheme
                    .typography
                    .titleLarge
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "No files or folders match \"$query\".",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        TextButton(
            onClick = onClearSearch
        ) {

            Text(
                text = "Clear search"
            )
        }
    }
}


/*
 * =============================================================
 * FILES SCREEN
 * =============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    fileViewModel: FileViewModel
) {

    /*
     * =========================================================
     * STATE
     * =========================================================
     */

    val currentDirectory by
    fileViewModel.currentDirectory.collectAsState()

    val operationProgress by
    fileViewModel.operationProgress.collectAsState()

    val isOperationRunning by
    fileViewModel.isOperationRunning.collectAsState()

    val items by
    fileViewModel.items.collectAsState()

    val filteredItems by
    fileViewModel.filteredItems.collectAsState()

    val selectedItems by
    fileViewModel.selectedItems.collectAsState()

    val isLoading by
    fileViewModel.isLoading.collectAsState()

    val error by
    fileViewModel.error.collectAsState()

    val clipboardItems by
    fileViewModel.clipboardItems.collectAsState()

    val sortState by
    fileViewModel.sortState.collectAsState()

    val searchQuery by
    fileViewModel.searchQuery.collectAsState()


    /*
     * =========================================================
     * LOCAL UI STATE
     * =========================================================
     */

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var showRenameDialog by remember {
        mutableStateOf(false)
    }

    var showSortMenu by remember {
        mutableStateOf(false)
    }

    var isSearchMode by remember {
        mutableStateOf(false)
    }


    /*
     * =========================================================
     * FOLDER PICKER
     * =========================================================
     */

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocumentTree()
        ) { uri ->

            if (uri != null) {

                fileViewModel.saveSelectedFolder(
                    uri = uri,
                    flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }


    /*
     * =========================================================
     * SCREEN
     * =========================================================
     */

    Scaffold(

        /*
         * =====================================================
         * TOP BAR
         * =====================================================
         */

        topBar = {

            TopAppBar(

                title = {

                    if (selectedItems.isNotEmpty()) {

                        Text(
                            text =
                                "${selectedItems.size} selected"
                        )

                    } else {

                        Text(
                            text =
                                currentDirectory
                                    ?.documentFile
                                    ?.name
                                    ?: "Files"
                        )
                    }
                },


                /*
                 * =================================================
                 * NAVIGATION
                 * =================================================
                 */

                navigationIcon = {

                    when {

                        selectedItems.isNotEmpty() -> {

                            IconButton(
                                onClick = {
                                    fileViewModel
                                        .clearSelection()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ArrowBack,

                                    contentDescription =
                                        "Clear selection"
                                )
                            }
                        }

                        currentDirectory != null -> {

                            IconButton(
                                onClick = {

                                    isSearchMode = false

                                    fileViewModel.clearSearch()

                                    fileViewModel.goBack()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ArrowBack,

                                    contentDescription =
                                        "Back"
                                )
                            }
                        }
                    }
                },


                /*
                 * =================================================
                 * ACTIONS
                 * =================================================
                 */

                actions = {

                    if (!isOperationRunning) {

                        /*
                         * =================================================
                         * SELECTION MODE
                         * =================================================
                         */

                        if (selectedItems.isNotEmpty()) {

                            /*
                             * COPY
                             */

                            IconButton(
                                onClick = {
                                    fileViewModel
                                        .copySelected()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ContentCopy,

                                    contentDescription =
                                        "Copy selected items"
                                )
                            }


                            /*
                             * MOVE
                             */

                            IconButton(
                                onClick = {
                                    fileViewModel
                                        .moveSelected()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ContentCut,

                                    contentDescription =
                                        "Move selected items"
                                )
                            }


                            /*
                             * RENAME
                             */

                            if (
                                selectedItems.size == 1
                            ) {

                                IconButton(
                                    onClick = {
                                        showRenameDialog =
                                            true
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Edit,

                                        contentDescription =
                                            "Rename"
                                    )
                                }
                            }


                            /*
                             * DELETE
                             */

                            IconButton(
                                onClick = {
                                    showDeleteDialog =
                                        true
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Delete,

                                    contentDescription =
                                        "Delete selected items"
                                )
                            }

                        } else {

                            /*
                             * =================================================
                             * NORMAL MODE
                             * =================================================
                             */

                            /*
                             * SEARCH
                             */

                            IconButton(
                                onClick = {

                                    isSearchMode = true
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Search,

                                    contentDescription =
                                        "Search files"
                                )
                            }


                            /*
                             * PASTE
                             */

                            if (
                                clipboardItems.isNotEmpty()
                            ) {

                                IconButton(
                                    onClick = {
                                        fileViewModel
                                            .paste()
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.ContentPaste,

                                        contentDescription =
                                            "Paste"
                                    )
                                }
                            }


                            /*
                             * REFRESH
                             */

                            IconButton(
                                onClick = {
                                    fileViewModel.refresh()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Refresh,

                                    contentDescription =
                                        "Refresh"
                                )
                            }


                            /*
                             * =================================================
                             * SORT MENU
                             * =================================================
                             */

                            Box {

                                IconButton(
                                    onClick = {
                                        showSortMenu = true
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.MoreVert,

                                        contentDescription =
                                            "Sort files"
                                    )
                                }


                                DropdownMenu(

                                    expanded =
                                        showSortMenu,

                                    onDismissRequest = {
                                        showSortMenu = false
                                    }
                                ) {

                                    /*
                                     * NAME A-Z
                                     */

                                    DropdownMenuItem(

                                        text = {
                                            Text("Name: A–Z")
                                        },

                                        trailingIcon = {

                                            if (
                                                sortState.field ==
                                                FileViewModel.SortField.NAME &&
                                                sortState.direction ==
                                                FileViewModel.SortDirection.ASCENDING
                                            ) {
                                                Text("✓")
                                            }
                                        },

                                        onClick = {

                                            showSortMenu = false

                                            fileViewModel
                                                .setSortDirection(
                                                    FileViewModel
                                                        .SortDirection
                                                        .ASCENDING
                                                )

                                            fileViewModel
                                                .setSortField(
                                                    FileViewModel
                                                        .SortField
                                                        .NAME
                                                )
                                        }
                                    )


                                    /*
                                     * NAME Z-A
                                     */

                                    DropdownMenuItem(

                                        text = {
                                            Text("Name: Z–A")
                                        },

                                        trailingIcon = {

                                            if (
                                                sortState.field ==
                                                FileViewModel.SortField.NAME &&
                                                sortState.direction ==
                                                FileViewModel.SortDirection.DESCENDING
                                            ) {
                                                Text("✓")
                                            }
                                        },

                                        onClick = {

                                            showSortMenu = false

                                            fileViewModel
                                                .setSortDirection(
                                                    FileViewModel
                                                        .SortDirection
                                                        .DESCENDING
                                                )

                                            fileViewModel
                                                .setSortField(
                                                    FileViewModel
                                                        .SortField
                                                        .NAME
                                                )
                                        }
                                    )


                                    /*
                                     * SIZE SMALL-LARGE
                                     */

                                    DropdownMenuItem(

                                        text = {
                                            Text(
                                                "Size: Small → Large"
                                            )
                                        },

                                        trailingIcon = {

                                            if (
                                                sortState.field ==
                                                FileViewModel.SortField.SIZE &&
                                                sortState.direction ==
                                                FileViewModel.SortDirection.ASCENDING
                                            ) {
                                                Text("✓")
                                            }
                                        },

                                        onClick = {

                                            showSortMenu = false

                                            fileViewModel
                                                .setSortDirection(
                                                    FileViewModel
                                                        .SortDirection
                                                        .ASCENDING
                                                )

                                            fileViewModel
                                                .setSortField(
                                                    FileViewModel
                                                        .SortField
                                                        .SIZE
                                                )
                                        }
                                    )


                                    /*
                                     * SIZE LARGE-SMALL
                                     */

                                    DropdownMenuItem(

                                        text = {
                                            Text(
                                                "Size: Large → Small"
                                            )
                                        },

                                        trailingIcon = {

                                            if (
                                                sortState.field ==
                                                FileViewModel.SortField.SIZE &&
                                                sortState.direction ==
                                                FileViewModel.SortDirection.DESCENDING
                                            ) {
                                                Text("✓")
                                            }
                                        },

                                        onClick = {

                                            showSortMenu = false

                                            fileViewModel
                                                .setSortDirection(
                                                    FileViewModel
                                                        .SortDirection
                                                        .DESCENDING
                                                )

                                            fileViewModel
                                                .setSortField(
                                                    FileViewModel
                                                        .SortField
                                                        .SIZE
                                                )
                                        }
                                    )


                                    /*
                                     * DATE NEWEST
                                     */

                                    DropdownMenuItem(

                                        text = {
                                            Text(
                                                "Date: Newest first"
                                            )
                                        },

                                        trailingIcon = {

                                            if (
                                                sortState.field ==
                                                FileViewModel.SortField.DATE_MODIFIED &&
                                                sortState.direction ==
                                                FileViewModel.SortDirection.DESCENDING
                                            ) {
                                                Text("✓")
                                            }
                                        },

                                        onClick = {

                                            showSortMenu = false

                                            fileViewModel
                                                .setSortDirection(
                                                    FileViewModel
                                                        .SortDirection
                                                        .DESCENDING
                                                )

                                            fileViewModel
                                                .setSortField(
                                                    FileViewModel
                                                        .SortField
                                                        .DATE_MODIFIED
                                                )
                                        }
                                    )


                                    /*
                                     * DATE OLDEST
                                     */

                                    DropdownMenuItem(

                                        text = {
                                            Text(
                                                "Date: Oldest first"
                                            )
                                        },

                                        trailingIcon = {

                                            if (
                                                sortState.field ==
                                                FileViewModel.SortField.DATE_MODIFIED &&
                                                sortState.direction ==
                                                FileViewModel.SortDirection.ASCENDING
                                            ) {
                                                Text("✓")
                                            }
                                        },

                                        onClick = {

                                            showSortMenu = false

                                            fileViewModel
                                                .setSortDirection(
                                                    FileViewModel
                                                        .SortDirection
                                                        .ASCENDING
                                                )

                                            fileViewModel
                                                .setSortField(
                                                    FileViewModel
                                                        .SortField
                                                        .DATE_MODIFIED
                                                )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },


        /*
         * =========================================================
         * CONTENT
         * =========================================================
         */

        content = { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                when {

                    /*
                     * =================================================
                     * INITIAL LOADING
                     * =================================================
                     */

                    isLoading &&
                            currentDirectory == null -> {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.align(
                                    Alignment.Center
                                )
                        )
                    }


                    /*
                     * =================================================
                     * NO STORAGE
                     * =================================================
                     */

                    currentDirectory == null -> {

                        FolderSelectionContent(
                            onSelectFolder = {

                                folderPickerLauncher
                                    .launch(null)
                            }
                        )
                    }


                    /*
                     * =================================================
                     * DIRECTORY CONTENT
                     * =================================================
                     */

                    else -> {

                        Column(
                            modifier =
                                Modifier.fillMaxSize()
                        ) {

                            /*
                             * =================================================
                             * SEARCH BAR
                             * =================================================
                             */

                            if (
                                isSearchMode &&
                                selectedItems.isEmpty()
                            ) {

                                FileSearchBar(

                                    query =
                                        searchQuery,

                                    resultCount =
                                        filteredItems.size,

                                    onQueryChange = { query ->

                                        fileViewModel
                                            .setSearchQuery(
                                                query
                                            )
                                    },

                                    onClear = {

                                        fileViewModel
                                            .clearSearch()
                                    }
                                )
                            }


                            /*
                             * =================================================
                             * CURRENT FOLDER HEADER
                             * =================================================
                             */

                            if (!isSearchMode) {

                                CurrentDirectoryHeader(

                                    directoryName =
                                        currentDirectory
                                            ?.documentFile
                                            ?.name
                                            ?: "Files"
                                )
                            }


                            /*
                             * =================================================
                             * OPERATION PROGRESS
                             * =================================================
                             */

                            if (
                                isOperationRunning &&
                                operationProgress != null
                            ) {

                                FileOperationProgressBar(
                                    progress =
                                        operationProgress!!
                                )
                            }


                            /*
                             * =================================================
                             * SEARCH RESULTS
                             * =================================================
                             */

                            if (
                                isSearchMode &&
                                searchQuery.isNotBlank() &&
                                filteredItems.isEmpty()
                            ) {

                                EmptySearchResult(

                                    query =
                                        searchQuery,

                                    onClearSearch = {

                                        fileViewModel
                                            .clearSearch()
                                    }
                                )

                            } else {

                                /*
                                 * =================================================
                                 * FILE LIST
                                 * =================================================
                                 */

                                FileList(

                                    items =
                                        if (
                                            isSearchMode
                                        ) {
                                            filteredItems
                                        } else {
                                            items
                                        },

                                    isLoading =
                                        isLoading,

                                    selectedItems =
                                        selectedItems,


                                    /*
                                     * =================================================
                                     * ITEM CLICK
                                     * =================================================
                                     */

                                    onItemClick = { item ->

                                        if (
                                            isOperationRunning
                                        ) {
                                            return@FileList
                                        }


                                        /*
                                         * Selection mode.
                                         */

                                        if (
                                            selectedItems.isNotEmpty()
                                        ) {

                                            fileViewModel
                                                .toggleSelection(
                                                    item
                                                )

                                        } else {

                                            /*
                                             * Normal mode.
                                             */

                                            if (
                                                item is StorageItem.Document &&
                                                item.documentFile.isDirectory
                                            ) {

                                                isSearchMode =
                                                    false

                                                fileViewModel
                                                    .clearSearch()

                                                fileViewModel
                                                    .openDirectory(
                                                        item
                                                    )
                                            }
                                        }
                                    },


                                    /*
                                     * =================================================
                                     * ITEM LONG CLICK
                                     * =================================================
                                     */

                                    onItemLongClick = { item ->

                                        if (
                                            isOperationRunning
                                        ) {
                                            return@FileList
                                        }

                                        fileViewModel
                                            .toggleSelection(
                                                item
                                            )
                                    }
                                )
                            }
                        }
                    }
                }


                /*
                 * =========================================================
                 * ERROR
                 * =========================================================
                 */

                if (error != null) {

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),

                        shape =
                            RoundedCornerShape(12.dp),

                        color =
                            MaterialTheme
                                .colorScheme
                                .errorContainer
                    ) {

                        Row(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    error ?: "",

                                modifier =
                                    Modifier.weight(1f),

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            TextButton(
                                onClick = {
                                    fileViewModel
                                        .clearError()
                                }
                            ) {

                                Text(
                                    text = "Dismiss"
                                )
                            }
                        }
                    }
                }
            }
        }
    )


    /*
     * =========================================================
     * DELETE CONFIRMATION
     * =========================================================
     */

    if (showDeleteDialog) {

        AlertDialog(

            onDismissRequest = {

                showDeleteDialog = false
            },

            title = {

                Text(
                    text =
                        "Delete selected items?"
                )
            },

            text = {

                Text(
                    text =
                        if (
                            selectedItems.size == 1
                        ) {

                            "Are you sure you want to permanently delete this item?"

                        } else {

                            "Are you sure you want to permanently delete " +
                                    "${selectedItems.size} items?"
                        }
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showDeleteDialog = false

                        fileViewModel
                            .deleteSelected()
                    }
                ) {

                    Text(
                        text = "Delete",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        showDeleteDialog = false
                    }
                ) {

                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }


    /*
     * =========================================================
     * RENAME DIALOG
     * =========================================================
     */

    if (
        showRenameDialog &&
        selectedItems.size == 1
    ) {

        val selectedItem =
            selectedItems.firstOrNull()

        if (selectedItem != null) {

            val currentName: String =
                when (selectedItem) {

                    is StorageItem.Document -> {

                        selectedItem
                            .documentFile
                            .name
                            ?: ""
                    }

                    is StorageItem.LocalFile -> {

                        selectedItem.file.name
                    }

                    is StorageItem.DocumentTarget -> {

                        selectedItem.name
                            ?: ""
                    }
                }


            RenameDialog(

                currentName =
                    currentName,

                onDismiss = {

                    showRenameDialog =
                        false
                },

                onRename = { newName ->

                    showRenameDialog =
                        false

                    fileViewModel
                        .renameSelected(
                            newName
                        )
                }
            )
        }
    }
}


/*
 * =============================================================
 * FOLDER SELECTION CONTENT
 * =============================================================
 */

@Composable
private fun FolderSelectionContent(
    onSelectFolder: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {

            Box(
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Storage,

                    contentDescription = null,

                    modifier =
                        Modifier.size(36.dp),

                    tint =
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                )
            }
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Text(
            text =
                "No storage folder selected",

            style =
                MaterialTheme
                    .typography
                    .headlineSmall
        )


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        Text(
            text =
                "Select a folder to browse and manage your files.",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier = Modifier.height(24.dp)
        )


        Button(
            onClick =
                onSelectFolder,

            shape =
                RoundedCornerShape(14.dp)
        ) {

            Text(
                text =
                    "Select Folder"
            )
        }
    }
}
