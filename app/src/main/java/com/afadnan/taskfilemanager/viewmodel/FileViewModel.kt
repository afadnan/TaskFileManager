package com.afadnan.taskfilemanager.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.afadnan.taskfilemanager.data.storage.FileItem
import com.afadnan.taskfilemanager.data.storage.FileManager
import com.afadnan.taskfilemanager.data.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val fileManager =
        FileManager(application.applicationContext)

    private val storageManager =
        StorageManager(application.applicationContext)

    /*
     * Stores the folders visited by the user.
     *
     * Example:
     *
     * Documents
     *     ↓
     * Projects
     *     ↓
     * Android
     *
     * directoryStack contains:
     * Documents
     * Projects
     */
    private val directoryStack =
        mutableListOf<Uri>()

    /*
     * Current files/folders displayed in the browser.
     */
    private val _files =
        MutableStateFlow<List<FileItem>>(emptyList())

    val files: StateFlow<List<FileItem>> =
        _files.asStateFlow()

    /*
     * Currently opened directory.
     */
    private val _currentDirectory =
        MutableStateFlow<Uri?>(null)

    val currentDirectory: StateFlow<Uri?> =
        _currentDirectory.asStateFlow()

    /*
     * Loading state.
     */
    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    /*
     * Error state.
     */
    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()

    /*
     * Sorting options.
     */
    enum class SortOption {
        NAME,
        DATE_MODIFIED,
        SIZE
    }

    /*
     * Current sort option.
     *
     * Default:
     * Name
     */
    private val _sortOption =
        MutableStateFlow(SortOption.NAME)

    val sortOption: StateFlow<SortOption> =
        _sortOption.asStateFlow()

    /*
     * Current sort direction.
     *
     * true  = ascending
     * false = descending
     */
    private val _sortAscending =
        MutableStateFlow(true)

    val sortAscending: StateFlow<Boolean> =
        _sortAscending.asStateFlow()

    init {
        loadSavedDirectory()
    }

    /*
     * Restore the previously selected storage location.
     */
    private fun loadSavedDirectory() {

        val uri =
            storageManager.getRootUri()
                ?: return

        if (storageManager.isRootAvailable()) {

            openDirectory(
                uri = uri,
                addToBackStack = false
            )

        } else {

            storageManager.clearRootUri()

            _error.value =
                "Previously selected storage is no longer available."
        }
    }

    /*
     * Called when the user selects a new folder
     * through ACTION_OPEN_DOCUMENT_TREE.
     */
    fun setRootDirectory(uri: Uri) {

        viewModelScope.launch(Dispatchers.IO) {

            _isLoading.value = true
            _error.value = null

            try {

                val saved =
                    storageManager.saveRootUri(uri)

                if (!saved) {

                    _error.value =
                        "Unable to access this folder."

                    return@launch
                }

                /*
                 * A newly selected root is a new browsing
                 * session, so clear previous navigation.
                 */
                directoryStack.clear()

                /*
                 * Load the selected folder.
                 */
                val result =
                    fileManager.listFiles(uri)

                result
                    .onSuccess { files ->

                        _files.value =
                            sortFiles(files)

                        _currentDirectory.value =
                            uri
                    }
                    .onFailure { exception ->

                        _error.value =
                            exception.message
                                ?: "Unable to load files."

                        _files.value =
                            emptyList()

                        _currentDirectory.value =
                            null
                    }

            } finally {

                _isLoading.value = false
            }
        }
    }

    /*
     * Open a directory.
     *
     * addToBackStack = true
     *      Used when the user enters a child folder.
     *
     * addToBackStack = false
     *      Used for restoring the root or going backwards.
     */
    fun openDirectory(
        uri: Uri,
        addToBackStack: Boolean = true
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            _isLoading.value = true
            _error.value = null

            try {

                val result =
                    fileManager.listFiles(uri)

                result
                    .onSuccess { files ->

                        /*
                         * Only add the current directory
                         * to the stack when moving forward.
                         */
                        if (addToBackStack) {

                            _currentDirectory.value?.let {
                                directoryStack.add(it)
                            }
                        }

                        _files.value =
                            sortFiles(files)

                        _currentDirectory.value =
                            uri
                    }
                    .onFailure { exception ->

                        _error.value =
                            exception.message
                                ?: "Unable to load files."

                        _files.value =
                            emptyList()
                    }

            } finally {

                _isLoading.value = false
            }
        }
    }

    /*
     * Navigate to the previous directory.
     */
    fun goBack() {

        if (directoryStack.isEmpty()) {
            return
        }

        val previousDirectory =
            directoryStack.removeAt(
                directoryStack.lastIndex
            )

        openDirectory(
            uri = previousDirectory,
            addToBackStack = false
        )
    }

    /*
     * Whether the Back button should be displayed.
     */
    fun canGoBack(): Boolean {
        return directoryStack.isNotEmpty()
    }

    /*
     * Refresh the currently opened directory.
     *
     * This is important for later operations such as:
     *
     * Rename
     * Delete
     * Copy
     * Move
     */
    fun refresh() {

        val directory =
            _currentDirectory.value
                ?: return

        openDirectory(
            uri = directory,
            addToBackStack = false
        )
    }

    /*
     * Change the sorting option.
     *
     * Example:
     *
     * setSortOption(NAME)
     * setSortOption(DATE_MODIFIED)
     * setSortOption(SIZE)
     */
    fun setSortOption(
        option: SortOption
    ) {

        _sortOption.value = option

        /*
         * Re-sort the currently displayed files
         * without accessing storage again.
         */
        _files.value =
            sortFiles(_files.value)
    }

    /*
     * Change ascending/descending order.
     */
    fun setSortAscending(
        ascending: Boolean
    ) {

        _sortAscending.value = ascending

        /*
         * Re-sort immediately.
         */
        _files.value =
            sortFiles(_files.value)
    }

    /*
     * Toggle sorting direction.
     *
     * Useful for a menu where the user taps
     * "Ascending / Descending".
     */
    fun toggleSortDirection() {

        _sortAscending.value =
            !_sortAscending.value

        _files.value =
            sortFiles(_files.value)
    }

    /*
     * Sort files while keeping folders first.
     *
     * Folder-first behavior:
     *
     * 📁 Documents
     * 📁 Projects
     * 📁 Pictures
     *
     * 📄 notes.txt
     * 📄 report.pdf
     */
    private fun sortFiles(
        files: List<FileItem>
    ): List<FileItem> {

        val folders =
            files.filter { it.isDirectory }

        val regularFiles =
            files.filter { !it.isDirectory }

        val sortedFolders =
            sortFileGroup(folders)

        val sortedFiles =
            sortFileGroup(regularFiles)

        return sortedFolders + sortedFiles
    }

    /*
     * Sort one group according to the selected
     * sorting option.
     */
    private fun sortFileGroup(
        files: List<FileItem>
    ): List<FileItem> {

        val comparator =
            when (_sortOption.value) {

                SortOption.NAME -> {
                    compareBy<FileItem> {
                        it.name.lowercase()
                    }
                }

                SortOption.DATE_MODIFIED -> {
                    compareBy<FileItem> {
                        it.lastModified
                    }
                }

                SortOption.SIZE -> {
                    compareBy<FileItem> {
                        it.size
                    }
                }
            }

        return if (_sortAscending.value) {

            files.sortedWith(comparator)

        } else {

            files.sortedWith(comparator.reversed())
        }
    }

    /*
     * Clear the current error.
     */
    fun clearError() {
        _error.value = null
    }
}
