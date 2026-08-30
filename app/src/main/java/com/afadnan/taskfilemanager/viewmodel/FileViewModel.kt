package com.afadnan.taskfilemanager.viewmodel

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afadnan.taskfilemanager.data.storage.FileOperationProgress
import com.afadnan.taskfilemanager.data.storage.StorageItem
import com.afadnan.taskfilemanager.data.storage.StorageLocationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileViewModel(
    private val storageLocationManager: StorageLocationManager
) : ViewModel() {

    /*
     * =====================================================
     * CURRENT DIRECTORY
     * =====================================================
     */

    private val _currentDirectory =
        MutableStateFlow<StorageItem.Document?>(null)

    val currentDirectory: StateFlow<StorageItem.Document?> =
        _currentDirectory.asStateFlow()


    /*
     * =====================================================
     * FILE OPERATION PROGRESS
     * =====================================================
     */

    private val _operationProgress =
        MutableStateFlow<FileOperationProgress?>(null)

    val operationProgress: StateFlow<FileOperationProgress?> =
        _operationProgress.asStateFlow()


    private val _isOperationRunning =
        MutableStateFlow(false)

    val isOperationRunning: StateFlow<Boolean> =
        _isOperationRunning.asStateFlow()


    /*
     * =====================================================
     * ITEMS
     * =====================================================
     */

    private val _items =
        MutableStateFlow<List<StorageItem>>(emptyList())

    val items: StateFlow<List<StorageItem>> =
        _items.asStateFlow()


    /*
     * =====================================================
     * SORTING
     * =====================================================
     */

    enum class SortField {
        NAME,
        DATE_MODIFIED,
        SIZE,
        TYPE
    }

    enum class SortDirection {
        ASCENDING,
        DESCENDING
    }

    data class SortState(
        val field: SortField = SortField.NAME,
        val direction: SortDirection = SortDirection.ASCENDING,
        val directoriesFirst: Boolean = true
    )

    private val _sortState =
        MutableStateFlow(SortState())

    val sortState: StateFlow<SortState> =
        _sortState.asStateFlow()


    /*
     * =====================================================
     * SORT METADATA
     * =====================================================
     */

    private data class SortMetadata(
        val item: StorageItem,
        val isDirectory: Boolean,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val type: String
    )


    /*
     * =====================================================
     * SEARCH
     * =====================================================
     *
     * Search works against the items already loaded for
     * the current directory.
     *
     * It does NOT repeatedly access DocumentFile while
     * the user is typing.
     *
     * Search is case-insensitive.
     */

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()


    /*
     * Filter the currently loaded and sorted items.
     */

    val filteredItems: StateFlow<List<StorageItem>> =
        combine(
            _items,
            _searchQuery
        ) { items, query ->

            val searchText =
                query.trim()

            if (searchText.isEmpty()) {

                items

            } else {

                items.filter { item ->

                    item.name.contains(
                        searchText,
                        ignoreCase = true
                    )
                }
            }

        }.stateIn(
            scope = viewModelScope,
            started =
                SharingStarted.WhileSubscribed(
                    5_000
                ),
            initialValue = emptyList()
        )


    /*
     * =====================================================
     * UPDATE SEARCH QUERY
     * =====================================================
     */

    fun setSearchQuery(
        query: String
    ) {

        _searchQuery.value =
            query
    }


    /*
     * =====================================================
     * CLEAR SEARCH
     * =====================================================
     */

    fun clearSearch() {

        _searchQuery.value =
            ""
    }


    /*
     * =====================================================
     * SELECTION
     * =====================================================
     */

    private val _selectedItems =
        MutableStateFlow<Set<StorageItem>>(emptySet())

    val selectedItems: StateFlow<Set<StorageItem>> =
        _selectedItems.asStateFlow()

    val isSelectionMode: Boolean
        get() =
            _selectedItems.value.isNotEmpty()


    /*
     * =====================================================
     * LOADING
     * =====================================================
     */

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()


    /*
     * =====================================================
     * ERROR
     * =====================================================
     */

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()


    /*
     * =====================================================
     * CLIPBOARD
     * =====================================================
     */

    enum class ClipboardOperation {
        COPY,
        MOVE
    }

    private val _clipboardItems =
        MutableStateFlow<List<StorageItem>>(emptyList())

    val clipboardItems: StateFlow<List<StorageItem>> =
        _clipboardItems.asStateFlow()


    private val _clipboardOperation =
        MutableStateFlow<ClipboardOperation?>(null)

    val clipboardOperation: StateFlow<ClipboardOperation?> =
        _clipboardOperation.asStateFlow()


    /*
     * =====================================================
     * DIRECTORY STACK
     * =====================================================
     */

    private val directoryStack =
        mutableListOf<StorageItem.Document>()


    /*
     * =====================================================
     * JOBS
     * =====================================================
     */

    private var directoryJob: Job? = null

    private var operationJob: Job? = null

    private var sortingJob: Job? = null


    /*
     * =====================================================
     * INITIALIZATION
     * =====================================================
     */

    init {
        restoreStorage()
    }


    /*
     * =====================================================
     * RESTORE STORAGE
     * =====================================================
     */

    fun restoreStorage() {

        directoryJob?.cancel()

        directoryJob =
            viewModelScope.launch {

                _isLoading.value = true
                _error.value = null

                try {

                    val root =
                        storageLocationManager
                            .getSelectedFolder()

                    if (root == null) {

                        _currentDirectory.value = null
                        _items.value = emptyList()
                        clearSearch()

                    } else {

                        directoryStack.clear()
                        clearSearch()

                        _currentDirectory.value =
                            root

                        loadDirectoryInternal(
                            root
                        )
                    }

                } catch (exception: CancellationException) {

                    throw exception

                } catch (exception: Exception) {

                    _error.value =
                        exception.message
                            ?: "Unable to load storage"

                } finally {

                    _isLoading.value = false
                }
            }
    }


    /*
     * =====================================================
     * SAVE SELECTED FOLDER
     * =====================================================
     */

    fun saveSelectedFolder(
        uri: Uri,
        flags: Int
    ) {

        directoryJob?.cancel()

        directoryJob =
            viewModelScope.launch {

                _isLoading.value = true
                _error.value = null

                try {

                    storageLocationManager
                        .saveSelectedFolder(
                            uri = uri,
                            flags = flags
                        )

                    directoryStack.clear()
                    clearSearch()
                    clearSelection()

                    val root =
                        storageLocationManager
                            .getSelectedFolder()

                    if (root != null) {

                        _currentDirectory.value =
                            root

                        loadDirectoryInternal(
                            root
                        )

                    } else {

                        _currentDirectory.value =
                            null

                        _items.value =
                            emptyList()
                    }

                } catch (exception: CancellationException) {

                    throw exception

                } catch (exception: Exception) {

                    _error.value =
                        exception.message
                            ?: "Unable to save selected folder"

                } finally {

                    _isLoading.value = false
                }
            }
    }


    /*
     * =====================================================
     * LOAD DIRECTORY
     * =====================================================
     */

    fun loadDirectory(
        directory: StorageItem.Document
    ) {

        directoryJob?.cancel()

        directoryJob =
            viewModelScope.launch {

                _isLoading.value = true
                _error.value = null

                try {

                    loadDirectoryInternal(
                        directory
                    )

                } catch (exception: CancellationException) {

                    throw exception

                } catch (exception: Exception) {

                    _error.value =
                        exception.message
                            ?: "Unable to read folder"

                    _items.value =
                        emptyList()

                } finally {

                    _isLoading.value = false
                }
            }
    }


    /*
     * =====================================================
     * LOAD DIRECTORY INTERNAL
     * =====================================================
     */

    private suspend fun loadDirectoryInternal(
        directory: StorageItem.Document
    ) {

        /*
         * -------------------------------------------------
         * Read directory on IO dispatcher.
         * -------------------------------------------------
         */

        val children =
            withContext(Dispatchers.IO) {

                directory.documentFile
                    .listFiles()
                    .map { document ->

                        StorageItem.Document(
                            uri = document.uri,
                            documentFile = document
                        )
                    }
            }


        /*
         * -------------------------------------------------
         * Apply current sorting.
         * -------------------------------------------------
         */

        val sorted =
            sortItems(
                children
            )


        _items.value =
            sorted
    }


    /*
     * =====================================================
     * SORT ITEMS
     * =====================================================
     */

    private suspend fun sortItems(
        items: List<StorageItem>
    ): List<StorageItem> {

        if (items.isEmpty()) {
            return emptyList()
        }


        /*
         * -------------------------------------------------
         * Capture current sort state.
         * -------------------------------------------------
         */

        val state =
            _sortState.value


        /*
         * -------------------------------------------------
         * Read metadata on IO dispatcher.
         * -------------------------------------------------
         */

        val metadata =
            withContext(Dispatchers.IO) {

                items.map { item ->

                    val isDirectory =
                        item.isDirectory


                    val name =
                        item.name


                    val size =
                        when (item) {

                            is StorageItem.Document -> {

                                if (isDirectory) {

                                    0L

                                } else {

                                    item.documentFile
                                        .length()
                                }
                            }

                            is StorageItem.LocalFile -> {

                                if (
                                    item.file.isDirectory
                                ) {

                                    0L

                                } else {

                                    item.file.length()
                                }
                            }

                            is StorageItem.DocumentTarget -> {

                                0L
                            }
                        }


                    val lastModified =
                        when (item) {

                            is StorageItem.Document ->
                                item.documentFile
                                    .lastModified()

                            is StorageItem.LocalFile ->
                                item.file
                                    .lastModified()

                            is StorageItem.DocumentTarget ->
                                0L
                        }


                    val type =
                        getFileType(
                            item
                        )


                    SortMetadata(
                        item = item,
                        isDirectory =
                            isDirectory,
                        name = name,
                        size = size,
                        lastModified =
                            lastModified,
                        type = type
                    )
                }
            }


        /*
         * -------------------------------------------------
         * Sort on Default dispatcher.
         * -------------------------------------------------
         */

        return withContext(Dispatchers.Default) {

            val sorted =
                when (state.field) {

                    /*
                     * NAME
                     */

                    SortField.NAME -> {

                        metadata.sortedWith(
                            compareBy<SortMetadata> {
                                it.name.lowercase()
                            }
                        )
                    }


                    /*
                     * DATE
                     */

                    SortField.DATE_MODIFIED -> {

                        metadata.sortedBy {
                            it.lastModified
                        }
                    }


                    /*
                     * SIZE
                     */

                    SortField.SIZE -> {

                        metadata.sortedBy {
                            it.size
                        }
                    }


                    /*
                     * TYPE
                     */

                    SortField.TYPE -> {

                        metadata.sortedWith(
                            compareBy<SortMetadata> {
                                it.type
                            }.thenBy {
                                it.name.lowercase()
                            }
                        )
                    }
                }


            /*
             * -------------------------------------------------
             * Apply direction.
             * -------------------------------------------------
             */

            val directionSorted =
                when (state.direction) {

                    SortDirection.ASCENDING ->
                        sorted

                    SortDirection.DESCENDING ->
                        sorted.asReversed()
                }


            /*
             * -------------------------------------------------
             * Keep directories first.
             * -------------------------------------------------
             */

            val finalSorted =
                if (state.directoriesFirst) {

                    directionSorted.sortedWith(
                        compareByDescending<SortMetadata> {
                            it.isDirectory
                        }
                    )

                } else {

                    directionSorted
                }


            finalSorted.map {
                it.item
            }
        }
    }


    /*
     * =====================================================
     * SET SORT FIELD
     * =====================================================
     */

    fun setSortField(
        field: SortField
    ) {

        val current =
            _sortState.value

        if (current.field == field) {
            return
        }

        _sortState.value =
            current.copy(
                field = field
            )

        resortCurrentItems()
    }


    /*
     * =====================================================
     * SET SORT DIRECTION
     * =====================================================
     */

    fun setSortDirection(
        direction: SortDirection
    ) {

        val current =
            _sortState.value

        if (current.direction == direction) {
            return
        }

        _sortState.value =
            current.copy(
                direction = direction
            )

        resortCurrentItems()
    }


    /*
     * =====================================================
     * TOGGLE SORT DIRECTION
     * =====================================================
     */

    fun toggleSortDirection() {

        val current =
            _sortState.value

        val newDirection =
            when (current.direction) {

                SortDirection.ASCENDING ->
                    SortDirection.DESCENDING

                SortDirection.DESCENDING ->
                    SortDirection.ASCENDING
            }

        _sortState.value =
            current.copy(
                direction = newDirection
            )

        resortCurrentItems()
    }


    /*
     * =====================================================
     * TOGGLE DIRECTORIES FIRST
     * =====================================================
     */

    fun toggleDirectoriesFirst() {

        val current =
            _sortState.value

        _sortState.value =
            current.copy(
                directoriesFirst =
                    !current.directoriesFirst
            )

        resortCurrentItems()
    }


    /*
     * =====================================================
     * RESORT CURRENT ITEMS
     * =====================================================
     */

    private fun resortCurrentItems() {

        sortingJob?.cancel()

        val currentItems =
            _items.value

        if (currentItems.isEmpty()) {
            return
        }

        sortingJob =
            viewModelScope.launch {

                try {

                    val sorted =
                        sortItems(
                            currentItems
                        )

                    _items.value =
                        sorted

                } catch (exception: CancellationException) {

                    throw exception

                } catch (exception: Exception) {

                    _error.value =
                        exception.message
                            ?: "Unable to sort files"
                }
            }
    }


    /*
     * =====================================================
     * OPEN DIRECTORY
     * =====================================================
     */

    fun openDirectory(
        directory: StorageItem.Document
    ) {

        val current =
            _currentDirectory.value

        if (current != null) {

            directoryStack.add(
                current
            )
        }

        clearSelection()
        clearSearch()

        _currentDirectory.value =
            directory

        loadDirectory(
            directory
        )
    }


    /*
     * =====================================================
     * GO BACK
     * =====================================================
     */

    fun goBack(): Boolean {

        if (directoryStack.isEmpty()) {
            return false
        }

        clearSelection()
        clearSearch()

        val previous =
            directoryStack.removeAt(
                directoryStack.lastIndex
            )

        _currentDirectory.value =
            previous

        loadDirectory(
            previous
        )

        return true
    }


    /*
     * =====================================================
     * REFRESH
     * =====================================================
     */

    fun refresh() {

        directoryJob?.cancel()

        directoryJob =
            viewModelScope.launch {

                _isLoading.value = true
                _error.value = null

                try {

                    var current =
                        _currentDirectory.value

                    if (current == null) {

                        current =
                            storageLocationManager
                                .getSelectedFolder()

                        if (current == null) {

                            _items.value =
                                emptyList()

                            clearSearch()

                            return@launch
                        }

                        _currentDirectory.value =
                            current
                    }

                    loadDirectoryInternal(
                        current
                    )

                } catch (exception: CancellationException) {

                    throw exception

                } catch (exception: Exception) {

                    _error.value =
                        exception.message
                            ?: "Unable to refresh files"

                } finally {

                    _isLoading.value = false
                }
            }
    }


    /*
     * =====================================================
     * SELECTION
     * =====================================================
     */

    fun toggleSelection(
        item: StorageItem
    ) {

        val current =
            _selectedItems.value
                .toMutableSet()

        if (current.contains(item)) {

            current.remove(item)

        } else {

            current.add(item)
        }

        _selectedItems.value =
            current
    }


    fun clearSelection() {

        _selectedItems.value =
            emptySet()
    }


    fun selectAll() {

        _selectedItems.value =
            _items.value.toSet()
    }


    /*
     * =====================================================
     * COPY SELECTED
     * =====================================================
     */

    fun copySelected() {

        val selected =
            _selectedItems.value
                .toList()

        if (selected.isEmpty()) {
            return
        }

        _clipboardItems.value =
            selected

        _clipboardOperation.value =
            ClipboardOperation.COPY

        clearSelection()
    }


    /*
     * =====================================================
     * MOVE SELECTED
     * =====================================================
     */

    fun moveSelected() {

        val selected =
            _selectedItems.value
                .toList()

        if (selected.isEmpty()) {
            return
        }

        _clipboardItems.value =
            selected

        _clipboardOperation.value =
            ClipboardOperation.MOVE

        clearSelection()
    }


    /*
     * =====================================================
     * PASTE
     * =====================================================
     */

    fun paste() {

        val destination =
            _currentDirectory.value
                ?: return

        val clipboard =
            _clipboardItems.value

        val operation =
            _clipboardOperation.value
                ?: return

        if (clipboard.isEmpty()) {
            return
        }

        if (_isOperationRunning.value) {
            return
        }

        operationJob?.cancel()

        operationJob =
            viewModelScope.launch {

                _isOperationRunning.value =
                    true

                _isLoading.value =
                    true

                _error.value =
                    null

                _operationProgress.value =
                    null

                var operationCompleted =
                    false

                try {

                    /*
                     * -------------------------------------------------
                     * Calculate total size.
                     * -------------------------------------------------
                     */

                    val totalBytes =
                        withContext(
                            Dispatchers.IO
                        ) {

                            clipboard.sumOf { item ->

                                when (item) {

                                    is StorageItem.Document -> {

                                        calculateDocumentSize(
                                            item.documentFile
                                        )
                                    }

                                    else -> {

                                        0L
                                    }
                                }
                            }
                        }


                    var completedBytes =
                        0L


                    /*
                     * -------------------------------------------------
                     * Start operation.
                     * -------------------------------------------------
                     */

                    _operationProgress.value =
                        FileOperationProgress(
                            currentFile =
                                "Starting…",

                            currentBytes =
                                0L,

                            totalBytes =
                                totalBytes,

                            isIndeterminate =
                                totalBytes <= 0L
                        )


                    /*
                     * -------------------------------------------------
                     * Process clipboard.
                     * -------------------------------------------------
                     */

                    clipboard.forEach { item ->

                        ensureActive()

                        if (
                            item !is
                                    StorageItem.Document
                        ) {

                            return@forEach
                        }


                        val itemSize =
                            withContext(
                                Dispatchers.IO
                            ) {

                                calculateDocumentSize(
                                    item.documentFile
                                )
                            }


                        when (operation) {

                            ClipboardOperation.COPY -> {

                                storageLocationManager
                                    .copyDocument(
                                        source = item,
                                        destination =
                                            destination,

                                        onProgress = { progress ->

                                            val overallBytes =
                                                completedBytes +
                                                        progress.currentBytes

                                            _operationProgress.value =
                                                FileOperationProgress(
                                                    currentFile =
                                                        progress.currentFile,

                                                    currentBytes =
                                                        overallBytes,

                                                    totalBytes =
                                                        totalBytes,

                                                    isIndeterminate =
                                                        totalBytes <= 0L
                                                )
                                        }
                                    )
                            }


                            ClipboardOperation.MOVE -> {

                                storageLocationManager
                                    .moveDocument(
                                        source = item,
                                        destination =
                                            destination,

                                        onProgress = { progress ->

                                            val overallBytes =
                                                completedBytes +
                                                        progress.currentBytes

                                            _operationProgress.value =
                                                FileOperationProgress(
                                                    currentFile =
                                                        progress.currentFile,

                                                    currentBytes =
                                                        overallBytes,

                                                    totalBytes =
                                                        totalBytes,

                                                    isIndeterminate =
                                                        totalBytes <= 0L
                                                )
                                        }
                                    )
                            }
                        }


                        completedBytes +=
                            itemSize


                        _operationProgress.value =
                            FileOperationProgress(
                                currentFile =
                                    item.documentFile.name
                                        ?: "Complete",

                                currentBytes =
                                    completedBytes,

                                totalBytes =
                                    totalBytes,

                                isIndeterminate =
                                    totalBytes <= 0L
                            )
                    }


                    /*
                     * -------------------------------------------------
                     * Operation complete.
                     * -------------------------------------------------
                     */

                    _operationProgress.value =
                        FileOperationProgress(
                            currentFile =
                                "Operation complete",

                            currentBytes =
                                totalBytes,

                            totalBytes =
                                totalBytes,

                            isIndeterminate =
                                false
                        )

                    operationCompleted =
                        true


                    /*
                     * Clear clipboard only after success.
                     */

                    clearClipboard()


                    /*
                     * Reload destination.
                     */

                    loadDirectoryInternal(
                        destination
                    )

                } catch (
                    exception: CancellationException
                ) {

                    throw exception

                } catch (
                    exception: Exception
                ) {

                    _error.value =
                        exception.message
                            ?: "Unable to paste items"

                } finally {

                    _isOperationRunning.value =
                        false

                    _isLoading.value =
                        false

                    if (operationCompleted) {

                        delay(500)

                        _operationProgress.value =
                            null
                    }
                }
            }
    }


    /*
     * =====================================================
     * CALCULATE DOCUMENT SIZE
     * =====================================================
     */

    private fun calculateDocumentSize(
        document: DocumentFile
    ): Long {

        if (!document.exists()) {
            return 0L
        }

        if (document.isDirectory) {

            return document
                .listFiles()
                .sumOf { child ->

                    calculateDocumentSize(
                        child
                    )
                }
        }

        return document.length()
    }


    /*
     * =====================================================
     * GET FILE TYPE
     * =====================================================
     */

    private fun getFileType(
        item: StorageItem
    ): String {

        if (item.isDirectory) {
            return "folder"
        }

        val name =
            item.name.lowercase()

        val extension =
            name.substringAfterLast(
                '.',
                missingDelimiterValue = ""
            )

        return if (
            extension.isEmpty()
        ) {

            "file"

        } else {

            extension
        }
    }


    /*
     * =====================================================
     * DELETE SELECTED
     * =====================================================
     */

    fun deleteSelected() {

        val selected =
            _selectedItems.value
                .toList()

        if (selected.isEmpty()) {
            return
        }

        if (operationJob?.isActive == true) {
            return
        }

        operationJob =
            viewModelScope.launch {

                _isLoading.value =
                    true

                _error.value =
                    null

                try {

                    withContext(
                        Dispatchers.IO
                    ) {

                        selected.forEach { item ->

                            ensureActive()

                            when (item) {

                                is StorageItem.Document -> {

                                    if (
                                        !storageLocationManager
                                            .deleteDocument(
                                                item
                                            )
                                    ) {

                                        throw IllegalStateException(
                                            "Unable to delete ${
                                                item.documentFile.name
                                                    ?: "item"
                                            }"
                                        )
                                    }
                                }


                                is StorageItem.LocalFile -> {

                                    if (
                                        !item.file.delete()
                                    ) {

                                        throw IllegalStateException(
                                            "Unable to delete ${
                                                item.file.name
                                            }"
                                        )
                                    }
                                }


                                is StorageItem.DocumentTarget -> {

                                    throw IllegalStateException(
                                        "This item cannot be deleted"
                                    )
                                }
                            }
                        }
                    }

                    clearSelection()

                    _currentDirectory.value
                        ?.let { current ->

                            loadDirectoryInternal(
                                current
                            )
                        }

                } catch (
                    exception: CancellationException
                ) {

                    throw exception

                } catch (
                    exception: Exception
                ) {

                    _error.value =
                        exception.message
                            ?: "Unable to delete items"

                } finally {

                    _isLoading.value =
                        false
                }
            }
    }


    /*
     * =====================================================
     * RENAME SELECTED
     * =====================================================
     */

    fun renameSelected(
        newName: String
    ) {

        val selected =
            _selectedItems.value
                .toList()

        if (selected.size != 1) {

            _error.value =
                "Select exactly one item to rename"

            return
        }

        val name =
            newName.trim()

        if (name.isEmpty()) {

            _error.value =
                "Name cannot be empty"

            return
        }

        if (operationJob?.isActive == true) {
            return
        }

        operationJob =
            viewModelScope.launch {

                _isLoading.value =
                    true

                _error.value =
                    null

                try {

                    val item =
                        selected.first()

                    when (item) {

                        is StorageItem.Document -> {

                            val success =
                                storageLocationManager
                                    .renameDocument(
                                        item = item,
                                        newName = name
                                    )

                            if (!success) {

                                throw IllegalStateException(
                                    "Unable to rename item"
                                )
                            }
                        }


                        is StorageItem.LocalFile -> {

                            withContext(
                                Dispatchers.IO
                            ) {

                                val newFile =
                                    java.io.File(
                                        item.file.parentFile,
                                        name
                                    )

                                if (
                                    !item.file.renameTo(
                                        newFile
                                    )
                                ) {

                                    throw IllegalStateException(
                                        "Unable to rename item"
                                    )
                                }
                            }
                        }


                        is StorageItem.DocumentTarget -> {

                            throw IllegalStateException(
                                "This item cannot be renamed"
                            )
                        }
                    }

                    clearSelection()

                    _currentDirectory.value
                        ?.let { current ->

                            loadDirectoryInternal(
                                current
                            )
                        }

                } catch (
                    exception: CancellationException
                ) {

                    throw exception

                } catch (
                    exception: Exception
                ) {

                    _error.value =
                        exception.message
                            ?: "Unable to rename item"

                } finally {

                    _isLoading.value =
                        false
                }
            }
    }


    /*
     * =====================================================
     * CLEAR CLIPBOARD
     * =====================================================
     */

    fun clearClipboard() {

        _clipboardItems.value =
            emptyList()

        _clipboardOperation.value =
            null
    }


    /*
     * =====================================================
     * CLEAR STORAGE
     * =====================================================
     */

    fun clearStorage() {

        operationJob?.cancel()
        directoryJob?.cancel()
        sortingJob?.cancel()

        viewModelScope.launch {

            try {

                storageLocationManager
                    .clearSelectedFolder()

                directoryStack.clear()

                _currentDirectory.value =
                    null

                _items.value =
                    emptyList()

                clearSelection()
                clearClipboard()
                clearSearch()

                _operationProgress.value =
                    null

                _isOperationRunning.value =
                    false

                _error.value =
                    null

            } catch (
                exception: CancellationException
            ) {

                throw exception

            } catch (
                exception: Exception
            ) {

                _error.value =
                    exception.message
                        ?: "Unable to clear storage"
            }
        }
    }


    /*
     * =====================================================
     * CLEAR ERROR
     * =====================================================
     */

    fun clearError() {

        _error.value =
            null
    }


    /*
     * =====================================================
     * VIEWMODEL CLEANUP
     * =====================================================
     */

    override fun onCleared() {

        sortingJob?.cancel()
        directoryJob?.cancel()
        operationJob?.cancel()

        super.onCleared()
    }
}