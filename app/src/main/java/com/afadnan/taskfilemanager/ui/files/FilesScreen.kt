package com.afadnan.taskfilemanager.ui.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afadnan.taskfilemanager.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FileViewModel = viewModel()
) {
    val files by viewModel.files.collectAsState()
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val sortOption by viewModel.sortOption.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()

    val context = LocalContext.current

    /*
     * Search text entered by the user.
     */
    var searchQuery by remember {
        mutableStateOf("")
    }

    /*
     * Controls the More Options menu.
     */
    var showMoreMenu by remember {
        mutableStateOf(false)
    }

    /*
     * Current folder name.
     *
     * Example:
     *
     * content://.../Documents
     *
     * becomes:
     *
     * Documents
     */
    val directoryName =
        currentDirectory?.let { uri ->

            DocumentFile
                .fromTreeUri(
                    context,
                    uri
                )
                ?.name

        } ?: "Files"

    /*
     * Filter files according to the search query.
     *
     * Search is case-insensitive.
     */
    val filteredFiles = remember(
        files,
        searchQuery
    ) {

        val query =
            searchQuery.trim()

        if (query.isEmpty()) {

            files

        } else {

            files.filter { file ->

                file.name.contains(
                    query,
                    ignoreCase = true
                )
            }
        }
    }

    /*
     * SAF folder picker.
     */
    val folderPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocumentTree()
        ) { uri ->

            if (uri != null) {

                viewModel.setRootDirectory(uri)

                /*
                 * Clear an old search when selecting
                 * another storage location.
                 */
                searchQuery = ""
            }
        }

    /*
     * No folder selected yet.
     */
    if (currentDirectory == null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "File Manager",
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Text(
                text =
                    "Choose a folder to start browsing",
                modifier =
                    Modifier.padding(
                        top = 8.dp,
                        bottom = 24.dp
                    )
            )

            Button(
                onClick = {
                    folderPicker.launch(null)
                }
            ) {

                Text("Choose Folder")
            }
        }

        return
    }

    /*
     * Main file browser.
     */
    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {
                    Text(
                        text = directoryName
                    )
                },

                navigationIcon = {

                    if (viewModel.canGoBack()) {

                        IconButton(
                            onClick = {

                                searchQuery = ""

                                viewModel.goBack()
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.ArrowBack,
                                contentDescription =
                                    "Go back"
                            )
                        }
                    }
                },

                actions = {

                    /*
                     * More options button.
                     */
                    IconButton(
                        onClick = {
                            showMoreMenu = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.MoreVert,
                            contentDescription =
                                "More options"
                        )
                    }

                    /*
                     * Sort / Refresh menu.
                     */
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = {
                            showMoreMenu = false
                        }
                    ) {

                        /*
                         * Refresh.
                         */
                        DropdownMenuItem(

                            text = {
                                Text("Refresh")
                            },

                            onClick = {

                                showMoreMenu = false

                                /*
                                 * Clear search so the refreshed
                                 * directory is immediately visible.
                                 */
                                searchQuery = ""

                                viewModel.refresh()
                            }
                        )

                        HorizontalDivider()

                        /*
                         * Sort section title.
                         */
                        DropdownMenuItem(

                            text = {
                                Text(
                                    text = "Sort by",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelLarge
                                )
                            },

                            onClick = {
                                /*
                                 * This item is only a section
                                 * label, so no action is needed.
                                 */
                            },

                            enabled = false
                        )

                        /*
                         * Sort by Name.
                         */
                        DropdownMenuItem(

                            text = {
                                Text(
                                    text =
                                        if (
                                            sortOption ==
                                            FileViewModel
                                                .SortOption
                                                .NAME
                                        ) {
                                            if (sortAscending) {
                                                "✓ Name (A → Z)"
                                            } else {
                                                "✓ Name (Z → A)"
                                            }
                                        } else {
                                            "Name"
                                        }
                                )
                            },

                            onClick = {

                                viewModel.setSortOption(
                                    FileViewModel
                                        .SortOption
                                        .NAME
                                )

                                showMoreMenu = false
                            }
                        )

                        /*
                         * Sort by modified date.
                         */
                        DropdownMenuItem(

                            text = {
                                Text(
                                    text =
                                        if (
                                            sortOption ==
                                            FileViewModel
                                                .SortOption
                                                .DATE_MODIFIED
                                        ) {
                                            if (sortAscending) {
                                                "✓ Date modified (Old → New)"
                                            } else {
                                                "✓ Date modified (New → Old)"
                                            }
                                        } else {
                                            "Date modified"
                                        }
                                )
                            },

                            onClick = {

                                viewModel.setSortOption(
                                    FileViewModel
                                        .SortOption
                                        .DATE_MODIFIED
                                )

                                showMoreMenu = false
                            }
                        )

                        /*
                         * Sort by file size.
                         */
                        DropdownMenuItem(

                            text = {
                                Text(
                                    text =
                                        if (
                                            sortOption ==
                                            FileViewModel
                                                .SortOption
                                                .SIZE
                                        ) {
                                            if (sortAscending) {
                                                "✓ Size (Small → Large)"
                                            } else {
                                                "✓ Size (Large → Small)"
                                            }
                                        } else {
                                            "Size"
                                        }
                                )
                            },

                            onClick = {

                                viewModel.setSortOption(
                                    FileViewModel
                                        .SortOption
                                        .SIZE
                                )

                                showMoreMenu = false
                            }
                        )

                        HorizontalDivider()

                        /*
                         * Sort direction.
                         */
                        DropdownMenuItem(

                            text = {
                                Text(
                                    text =
                                        if (sortAscending) {
                                            "✓ Ascending"
                                        } else {
                                            "Ascending"
                                        }
                                )
                            },

                            onClick = {

                                viewModel.setSortAscending(
                                    true
                                )

                                showMoreMenu = false
                            }
                        )

                        DropdownMenuItem(

                            text = {
                                Text(
                                    text =
                                        if (!sortAscending) {
                                            "✓ Descending"
                                        } else {
                                            "Descending"
                                        }
                                )
                            },

                            onClick = {

                                viewModel.setSortAscending(
                                    false
                                )

                                showMoreMenu = false
                            }
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            /*
             * Search field.
             */
            OutlinedTextField(

                value = searchQuery,

                onValueChange = {
                    searchQuery = it
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),

                placeholder = {
                    Text("Search files")
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,
                        contentDescription =
                            "Search"
                    )
                },

                trailingIcon = {

                    if (searchQuery.isNotEmpty()) {

                        IconButton(
                            onClick = {
                                searchQuery = ""
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Clear,
                                contentDescription =
                                    "Clear search"
                            )
                        }
                    }
                },

                singleLine = true
            )

            /*
             * Loading state.
             */
            if (isLoading) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator()

                    Text(
                        text = "Loading files...",
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )
                }

                /*
                 * Error state.
                 */
            } else if (error != null) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text =
                            error
                                ?: "Unable to load files.",
                        style =
                            MaterialTheme.typography.bodyLarge
                    )

                    Button(
                        onClick = {
                            folderPicker.launch(null)
                        },
                        modifier =
                            Modifier.padding(
                                top = 16.dp
                            )
                    ) {

                        Text("Choose Another Folder")
                    }
                }

                /*
                 * Search returned no results.
                 */
            } else if (
                searchQuery.isNotBlank() &&
                filteredFiles.isEmpty()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Search,
                        contentDescription = null
                    )

                    Text(
                        text = "No files found",
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )

                    Text(
                        text =
                            "No files match \"$searchQuery\"",
                        style =
                            MaterialTheme.typography
                                .bodyMedium,
                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            )
                    )
                }

                /*
                 * Normal file list / filtered file list.
                 */
            } else {

                FileList(
                    files = filteredFiles,

                    onFileClick = { file ->

                        if (file.isDirectory) {

                            /*
                             * Clear search before entering
                             * another directory.
                             */
                            searchQuery = ""

                            viewModel.openDirectory(
                                uri = file.uri
                            )
                        }

                        /*
                         * Opening individual files will
                         * be implemented later.
                         */
                    }
                )
            }
        }
    }
}
