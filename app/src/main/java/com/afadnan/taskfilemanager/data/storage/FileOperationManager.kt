package com.afadnan.taskfilemanager.data.storage

import android.content.ContentResolver
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class FileOperationManager(
    private val contentResolver: ContentResolver,
    private val transactionRepository: FileTransactionRepository
) {

    // =====================================================================
    // COPY
    // =====================================================================

    /**
     * Safely copies a file to the destination.
     *
     * Transaction flow:
     *
     * STARTED
     *     ↓
     * COPYING
     *     ↓
     * VERIFYING
     *     ↓
     * COMMITTING
     *     ↓
     * COMMITTED
     *     ↓
     * COMPLETED
     *
     * The source is NEVER modified.
     *
     * A temporary destination is created first. The temporary file is
     * only renamed to the final destination after SHA-256 verification.
     */
    suspend fun copy(
        source: StorageItem,
        destination: StorageItem,
        onProgress: ((copied: Long, total: Long) -> Unit)? = null
    ): Result<StorageItem> = withContext(Dispatchers.IO) {

        var transactionId: String? = null
        var temporary: StorageItem? = null

        try {

            // -------------------------------------------------------------
            // VALIDATE
            // -------------------------------------------------------------

            validateSource(source).getOrThrow()

            if (exists(destination)) {
                throw IOException(
                    "Destination already exists"
                )
            }

            val totalBytes =
                getLength(source)

            // -------------------------------------------------------------
            // CREATE TRANSACTION
            // -------------------------------------------------------------

            transactionId =
                transactionRepository.create(
                    operation = FileOperationType.COPY,
                    source = source,
                    destination = destination
                )

            transactionRepository.updateProgress(
                id = transactionId,
                state = FileTransactionState.STARTED,
                copied = 0L,
                total = totalBytes
            )

            // -------------------------------------------------------------
            // CREATE TEMPORARY DESTINATION
            // -------------------------------------------------------------

            temporary =
                createTemporaryDestination(
                    destination
                )

            /*
             * Persist temporary location immediately.
             *
             * If the application crashes after this point,
             * FileRecoveryManager knows what temporary object belongs
             * to this transaction.
             */
            transactionRepository.updateTemporaryUri(
                id = transactionId,
                temporaryUri =
                    temporary.toPersistentIdentifier()
            )

            // -------------------------------------------------------------
            // COPYING
            // -------------------------------------------------------------

            transactionRepository.updateProgress(
                id = transactionId,
                state = FileTransactionState.COPYING,
                copied = 0L,
                total = totalBytes
            )

            transfer(
                source = source,
                destination = temporary,
                totalBytes = totalBytes,
                transactionId = transactionId,
                onProgress = onProgress
            )

            coroutineContext.ensureActive()

            // -------------------------------------------------------------
            // VERIFYING
            // -------------------------------------------------------------

            transactionRepository.updateState(
                id = transactionId,
                state = FileTransactionState.VERIFYING
            )

            val verified =
                verifyTransfer(
                    source = source,
                    destination = temporary
                )

            if (!verified) {
                throw IOException(
                    "File verification failed"
                )
            }

            coroutineContext.ensureActive()

            // -------------------------------------------------------------
            // COMMITTING
            // -------------------------------------------------------------

            transactionRepository.updateState(
                id = transactionId,
                state = FileTransactionState.COMMITTING
            )

            val committed =
                commitTemporary(
                    temporary = temporary,
                    destination = destination
                )

            if (!committed) {
                throw IOException(
                    "Unable to commit destination"
                )
            }

            /*
             * IMPORTANT:
             *
             * At this point the destination exists and has already
             * been verified.
             */
            transactionRepository.updateState(
                id = transactionId,
                state = FileTransactionState.COMMITTED
            )

            /*
             * Copy has nothing else to finalize.
             */
            transactionRepository.complete(
                transactionId
            )

            Result.success(destination)

        } catch (exception: CancellationException) {

            /*
             * Coroutine cancellation is different from an ordinary
             * transfer failure.
             *
             * The source remains untouched.
             *
             * Remove temporary output if it is still ours.
             */
            temporary?.let {
                deleteQuietly(it)
            }

            transactionId?.let { id ->

                try {
                    transactionRepository.fail(id)
                } catch (_: Exception) {
                    // Never hide the original cancellation.
                }
            }

            throw exception

        } catch (exception: Exception) {

            /*
             * COPY never modifies the source.
             *
             * If the destination has already been committed, do not
             * delete anything belonging to the committed destination.
             *
             * RecoveryManager can safely finish the transaction.
             */
            val transaction =
                transactionId?.let {
                    transactionRepository.getById(it)
                }

            val state =
                transaction?.state?.let {
                    runCatching {
                        FileTransactionState.valueOf(it)
                    }.getOrNull()
                }

            if (
                state != FileTransactionState.COMMITTED &&
                state != FileTransactionState.FINALIZING
            ) {

                temporary?.let {
                    deleteQuietly(it)
                }

                transactionId?.let {
                    transactionRepository.fail(it)
                }
            }

            Result.failure(exception)
        }
    }

    // =====================================================================
    // MOVE
    // =====================================================================

    /**
     * Safely moves a file to the destination.
     *
     * Cross-storage move:
     *
     * SOURCE
     *    ↓
     * TEMPORARY DESTINATION
     *    ↓
     * VERIFY
     *    ↓
     * COMMIT
     *    ↓
     * FINALIZING
     *    ↓
     * DELETE SOURCE
     *    ↓
     * COMPLETED
     *
     * The source remains untouched until the destination has been
     * successfully copied and verified.
     */
    suspend fun move(
        source: StorageItem,
        destination: StorageItem,
        onProgress: ((copied: Long, total: Long) -> Unit)? = null
    ): Result<StorageItem> = withContext(Dispatchers.IO) {

        var transactionId: String? = null
        var temporary: StorageItem? = null

        try {

            // -------------------------------------------------------------
            // VALIDATE
            // -------------------------------------------------------------

            validateSource(source).getOrThrow()

            if (exists(destination)) {
                throw IOException(
                    "Destination already exists"
                )
            }

            // -------------------------------------------------------------
            // FAST LOCAL MOVE
            // -------------------------------------------------------------

            /*
             * If both source and destination are ordinary filesystem
             * files, attempt an atomic rename first.
             *
             * renameTo() is atomic on supported filesystems and avoids
             * copying the file.
             *
             * If it fails, we fall back to the fully journaled transfer.
             */
            if (
                source is StorageItem.LocalFile &&
                destination is StorageItem.LocalFile
            ) {

                val sourceParent =
                    source.file.parentFile

                val destinationParent =
                    destination.file.parentFile

                if (
                    sourceParent != null &&
                    destinationParent != null &&
                    sourceParent.toPath().root ==
                    destinationParent.toPath().root
                ) {

                    if (!destinationParent.exists()) {
                        destinationParent.mkdirs()
                    }

                    if (
                        source.file.renameTo(
                            destination.file
                        )
                    ) {
                        return@withContext Result.success(
                            destination
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // JOURNALED MOVE
            // -------------------------------------------------------------

            val totalBytes =
                getLength(source)

            transactionId =
                transactionRepository.create(
                    operation = FileOperationType.MOVE,
                    source = source,
                    destination = destination
                )

            transactionRepository.updateProgress(
                id = transactionId,
                state = FileTransactionState.STARTED,
                copied = 0L,
                total = totalBytes
            )

            // -------------------------------------------------------------
            // TEMPORARY DESTINATION
            // -------------------------------------------------------------

            temporary =
                createTemporaryDestination(
                    destination
                )

            /*
             * Persist temporary destination BEFORE starting transfer.
             */
            transactionRepository.updateTemporaryUri(
                id = transactionId,
                temporaryUri =
                    temporary.toPersistentIdentifier()
            )

            // -------------------------------------------------------------
            // COPYING
            // -------------------------------------------------------------

            transactionRepository.updateProgress(
                id = transactionId,
                state = FileTransactionState.COPYING,
                copied = 0L,
                total = totalBytes
            )

            transfer(
                source = source,
                destination = temporary,
                totalBytes = totalBytes,
                transactionId = transactionId,
                onProgress = onProgress
            )

            coroutineContext.ensureActive()

            // -------------------------------------------------------------
            // VERIFYING
            // -------------------------------------------------------------

            transactionRepository.updateState(
                id = transactionId,
                state = FileTransactionState.VERIFYING
            )

            val verified =
                verifyTransfer(
                    source = source,
                    destination = temporary
                )

            if (!verified) {
                throw IOException(
                    "File verification failed"
                )
            }

            coroutineContext.ensureActive()

            // -------------------------------------------------------------
            // COMMITTING
            // -------------------------------------------------------------

            transactionRepository.updateState(
                id = transactionId,
                state = FileTransactionState.COMMITTING
            )

            val committed =
                commitTemporary(
                    temporary = temporary,
                    destination = destination
                )

            if (!committed) {
                throw IOException(
                    "Unable to commit destination"
                )
            }

            /*
             * Destination is now valid.
             *
             * From this point onward, we MUST NOT remove the
             * destination because of a normal exception.
             */
            transactionRepository.updateState(
                id = transactionId,
                state = FileTransactionState.COMMITTED
            )

            // -------------------------------------------------------------
            // FINALIZING
            // -------------------------------------------------------------

            transactionRepository.updateState(
                id = transactionId,
                state = FileTransactionState.FINALIZING
            )

            /*
             * ONLY NOW do we delete the source.
             */
            val sourceDeleted =
                delete(source)

            if (!sourceDeleted) {

                /*
                 * Destination is already valid.
                 *
                 * Keep the transaction in FINALIZING so that
                 * FileRecoveryManager can retry source deletion.
                 */
                throw IOException(
                    "Destination committed, but source " +
                            "could not be deleted"
                )
            }

            // -------------------------------------------------------------
            // COMPLETED
            // -------------------------------------------------------------

            transactionRepository.complete(
                transactionId
            )

            Result.success(destination)

        } catch (exception: CancellationException) {

            /*
             * Cancellation must NEVER remove the source after the
             * destination has been committed.
             */
            if (transactionId != null) {

                val transaction =
                    transactionRepository.getById(
                        transactionId
                    )

                val state =
                    transaction?.state?.let {
                        runCatching {
                            FileTransactionState.valueOf(it)
                        }.getOrNull()
                    }

                if (
                    state != FileTransactionState.COMMITTED &&
                    state != FileTransactionState.FINALIZING
                ) {
                    temporary?.let {
                        deleteQuietly(it)
                    }

                    try {
                        transactionRepository.fail(
                            transactionId
                        )
                    } catch (_: Exception) {
                        // Preserve cancellation.
                    }
                }
            }

            throw exception

        } catch (exception: Exception) {

            /*
             * Retrieve the persisted transaction state.
             *
             * This is CRITICAL.
             *
             * If the destination has already been committed, we must
             * NOT mark the transaction FAILED and we must NOT delete
             * the destination.
             */
            val transaction =
                transactionId?.let {
                    transactionRepository.getById(it)
                }

            val state =
                transaction?.state?.let {
                    runCatching {
                        FileTransactionState.valueOf(it)
                    }.getOrNull()
                }

            when (state) {

                FileTransactionState.COMMITTED,
                FileTransactionState.FINALIZING -> {

                    /*
                     * Destination is valid.
                     *
                     * Do NOT delete temporary/destination data.
                     *
                     * Leave transaction recoverable.
                     */
                    if (
                        state ==
                        FileTransactionState.COMMITTED
                    ) {
                        transactionRepository.updateState(
                            id = transactionId!!,
                            state = FileTransactionState.FINALIZING
                        )
                    }
                }

                else -> {

                    /*
                     * Destination was not committed.
                     *
                     * Temporary file is safe to remove.
                     */
                    temporary?.let {
                        deleteQuietly(it)
                    }

                    transactionId?.let {
                        transactionRepository.fail(it)
                    }
                }
            }

            Result.failure(exception)
        }
    }

    // =====================================================================
    // DELETE
    // =====================================================================

    /**
     * Deletes a file or directory.
     *
     * Directory deletion is recursive for LocalFile.
     */
    suspend fun delete(
        item: StorageItem
    ): Boolean = withContext(Dispatchers.IO) {

        when (item) {

            is StorageItem.LocalFile -> {
                item.file.deleteRecursively()
            }

            is StorageItem.Document -> {
                item.documentFile.delete()
            }

            is StorageItem.DocumentTarget -> {
                false
            }
        }
    }

    // =====================================================================
    // RENAME
    // =====================================================================

    /**
     * Renames an existing file.
     */
    suspend fun rename(
        source: StorageItem,
        newName: String
    ): Result<StorageItem> =
        withContext(Dispatchers.IO) {

            try {

                val cleanName =
                    newName.trim()

                if (cleanName.isBlank()) {
                    throw IOException(
                        "Filename cannot be empty"
                    )
                }

                /*
                 * Prevent path traversal / directory injection.
                 *
                 * Rename accepts a filename, not a path.
                 */
                if (
                    cleanName.contains("/") ||
                    cleanName.contains("\\")
                ) {
                    throw IOException(
                        "Invalid filename"
                    )
                }

                validateSource(source).getOrThrow()

                when (source) {

                    is StorageItem.LocalFile -> {

                        val parent =
                            source.file.parentFile
                                ?: throw IOException(
                                    "Parent directory not found"
                                )

                        val destination =
                            File(
                                parent,
                                cleanName
                            )

                        if (
                            destination.exists()
                        ) {
                            throw IOException(
                                "A file with this name already exists"
                            )
                        }

                        if (
                            !source.file.renameTo(
                                destination
                            )
                        ) {
                            throw IOException(
                                "Rename failed"
                            )
                        }

                        Result.success(
                            StorageItem.LocalFile(
                                destination
                            )
                        )
                    }

                    is StorageItem.Document -> {

                        if (
                            !source.documentFile.renameTo(
                                cleanName
                            )
                        ) {
                            throw IOException(
                                "Rename failed"
                            )
                        }

                        /*
                         * The DocumentFile object may retain the old
                         * metadata depending on the provider.
                         *
                         * The URI is still the provider's document URI.
                         */
                        Result.success(source)
                    }

                    is StorageItem.DocumentTarget -> {

                        Result.failure(
                            IOException(
                                "Cannot rename a destination target"
                            )
                        )
                    }
                }

            } catch (exception: Exception) {

                Result.failure(exception)
            }
        }

    // =====================================================================
    // SEARCH
    // =====================================================================

    /**
     * Recursively searches files under the supplied root.
     *
     * Matching is case-insensitive.
     */
    suspend fun search(
        root: StorageItem,
        query: String
    ): List<StorageItem> =
        withContext(Dispatchers.IO) {

            val cleanQuery =
                query.trim()

            if (cleanQuery.isBlank()) {
                return@withContext emptyList()
            }

            val results =
                mutableListOf<StorageItem>()

            searchRecursive(
                item = root,
                query = cleanQuery,
                results = results
            )

            results
        }

    // =====================================================================
    // TRANSFER
    // =====================================================================

    /**
     * Performs source → temporary transfer.
     *
     * Room progress is updated approximately every 1 MB instead of
     * once per buffer.
     */
    private suspend fun transfer(
        source: StorageItem,
        destination: StorageItem,
        totalBytes: Long,
        transactionId: String,
        onProgress: ((Long, Long) -> Unit)?
    ) {

        val input =
            openInputStream(source)
                ?: throw IOException(
                    "Unable to open source"
                )

        val output =
            openOutputStream(destination)
                ?: throw IOException(
                    "Unable to open destination"
                )

        input.use { inputStream ->

            output.use { outputStream ->

                val buffer =
                    ByteArray(BUFFER_SIZE)

                var copied = 0L
                var lastDatabaseUpdate = 0L

                while (true) {

                    coroutineContext.ensureActive()

                    val count =
                        inputStream.read(buffer)

                    if (count == -1) {
                        break
                    }

                    outputStream.write(
                        buffer,
                        0,
                        count
                    )

                    copied += count

                    onProgress?.invoke(
                        copied,
                        totalBytes
                    )

                    if (
                        copied - lastDatabaseUpdate >=
                        PROGRESS_UPDATE_BYTES
                    ) {

                        transactionRepository.updateProgress(
                            id = transactionId,
                            state = FileTransactionState.COPYING,
                            copied = copied,
                            total = totalBytes
                        )

                        lastDatabaseUpdate =
                            copied
                    }
                }

                /*
                 * Make sure all buffered bytes have reached the
                 * underlying output stream.
                 */
                outputStream.flush()

                /*
                 * For normal FileOutputStream, force the data to disk.
                 *
                 * ContentResolver output streams generally do not expose
                 * an equivalent fsync operation.
                 */
                if (
                    outputStream is FileOutputStream
                ) {
                    outputStream.fd.sync()
                }
            }
        }

        /*
         * Record final transfer progress.
         */
        transactionRepository.updateProgress(
            id = transactionId,
            state = FileTransactionState.COPYING,
            copied = totalBytes,
            total = totalBytes
        )
    }

    // =====================================================================
    // VERIFICATION
    // =====================================================================

    /**
     * Verifies that source and destination contain identical data.
     *
     * First compares file lengths.
     * Then compares SHA-256 hashes.
     */
    private fun verifyTransfer(
        source: StorageItem,
        destination: StorageItem
    ): Boolean {

        val sourceLength =
            getLength(source)

        val destinationLength =
            getLength(destination)

        if (
            sourceLength >= 0 &&
            destinationLength >= 0 &&
            sourceLength != destinationLength
        ) {
            return false
        }

        val sourceHash =
            calculateSha256(source)
                ?: return false

        val destinationHash =
            calculateSha256(destination)
                ?: return false

        return MessageDigest.isEqual(
            sourceHash,
            destinationHash
        )
    }

    /**
     * Calculates SHA-256 for a storage item.
     */
    private fun calculateSha256(
        item: StorageItem
    ): ByteArray? {

        val input =
            openInputStream(item)
                ?: return null

        return try {

            val digest =
                MessageDigest.getInstance(
                    "SHA-256"
                )

            input.use { stream ->

                val buffer =
                    ByteArray(BUFFER_SIZE)

                while (true) {

                    val count =
                        stream.read(buffer)

                    if (count == -1) {
                        break
                    }

                    digest.update(
                        buffer,
                        0,
                        count
                    )
                }
            }

            digest.digest()

        } catch (_: Exception) {

            null
        }
    }

    // =====================================================================
    // TEMPORARY DESTINATION
    // =====================================================================

    /**
     * Creates a temporary destination next to the final destination.
     *
     * Local:
     *
     * report.pdf
     *     ↓
     * .report.pdf.123456.tmp
     *
     * SAF:
     *
     * report.pdf
     *     ↓
     * .report.pdf.123456.tmp
     */
    private fun createTemporaryDestination(
        destination: StorageItem
    ): StorageItem {

        val temporaryName =
            ".${destination.name}." +
                    System.nanoTime() +
                    ".tmp"

        return when (destination) {

            is StorageItem.LocalFile -> {

                val parent =
                    destination.file.parentFile
                        ?: throw IOException(
                            "Destination parent not found"
                        )

                if (!parent.exists()) {

                    if (!parent.mkdirs() &&
                        !parent.exists()
                    ) {
                        throw IOException(
                            "Unable to create destination directory"
                        )
                    }
                }

                StorageItem.LocalFile(
                    File(
                        parent,
                        temporaryName
                    )
                )
            }

            is StorageItem.DocumentTarget -> {

                val document =
                    destination.parentDocument
                        .createFile(
                            getMimeType(
                                destination.fileName
                            ),
                            temporaryName
                        )
                        ?: throw IOException(
                            "Unable to create temporary SAF file"
                        )

                StorageItem.Document(
                    uri = document.uri,
                    documentFile = document
                )
            }

            is StorageItem.Document -> {

                /*
                 * A Document destination represents an existing item.
                 *
                 * The UI should construct a DocumentTarget for a new
                 * destination.
                 */
                throw IOException(
                    "Destination document already exists"
                )
            }
        }
    }

    // =====================================================================
    // COMMIT
    // =====================================================================

    /**
     * Commits the temporary destination to the final destination.
     *
     * Local filesystem:
     *
     * .report.pdf.tmp
     *       ↓ rename
     * report.pdf
     *
     * SAF:
     *
     * temporary DocumentFile
     *       ↓ rename
     * report.pdf
     */
    private fun commitTemporary(
        temporary: StorageItem,
        destination: StorageItem
    ): Boolean {

        return when {

            temporary is StorageItem.LocalFile &&
                    destination is StorageItem.LocalFile -> {

                if (destination.file.exists()) {
                    return false
                }

                temporary.file.renameTo(
                    destination.file
                )
            }

            temporary is StorageItem.Document &&
                    destination is StorageItem.DocumentTarget -> {

                if (
                    destination.parentDocument
                        .findFile(
                            destination.fileName
                        )
                        ?.exists() == true
                ) {
                    return false
                }

                temporary.documentFile.renameTo(
                    destination.fileName
                )
            }

            else -> {
                false
            }
        }
    }

    // =====================================================================
    // INPUT STREAM
    // =====================================================================

    private fun openInputStream(
        item: StorageItem
    ): InputStream? {

        return when (item) {

            is StorageItem.LocalFile -> {

                if (!item.file.exists()) {
                    null
                } else {
                    FileInputStream(
                        item.file
                    ).buffered()
                }
            }

            is StorageItem.Document -> {

                contentResolver
                    .openInputStream(
                        item.uri
                    )
                    ?.buffered()
            }

            is StorageItem.DocumentTarget -> {
                null
            }
        }
    }

    // =====================================================================
    // OUTPUT STREAM
    // =====================================================================

    private fun openOutputStream(
        item: StorageItem
    ): OutputStream? {

        return when (item) {

            is StorageItem.LocalFile -> {

                item.file.parentFile?.let { parent ->

                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                }

                FileOutputStream(
                    item.file
                ).buffered()
            }

            is StorageItem.Document -> {

                contentResolver
                    .openOutputStream(
                        item.uri,
                        "w"
                    )
                    ?.buffered()
            }

            is StorageItem.DocumentTarget -> {

                /*
                 * DocumentTarget represents a location that does not
                 * exist yet, therefore it cannot directly provide an
                 * OutputStream.
                 *
                 * createTemporaryDestination() converts it into a
                 * real Document first.
                 */
                null
            }
        }
    }

    // =====================================================================
    // EXISTS
    // =====================================================================

    private fun exists(
        item: StorageItem
    ): Boolean {

        return when (item) {

            is StorageItem.LocalFile ->
                item.file.exists()

            is StorageItem.Document ->
                item.documentFile.exists()

            is StorageItem.DocumentTarget ->
                item.parentDocument
                    .findFile(
                        item.fileName
                    )
                    ?.exists() == true
        }
    }

    // =====================================================================
    // LENGTH
    // =====================================================================

    private fun getLength(
        item: StorageItem
    ): Long {

        return when (item) {

            is StorageItem.LocalFile -> {

                if (item.file.exists()) {
                    item.file.length()
                } else {
                    -1L
                }
            }

            is StorageItem.Document ->
                item.documentFile.length()

            is StorageItem.DocumentTarget ->
                -1L
        }
    }

    // =====================================================================
    // SOURCE VALIDATION
    // =====================================================================

    private fun validateSource(
        source: StorageItem
    ): Result<Unit> {

        if (!exists(source)) {

            return Result.failure(
                IOException(
                    "Source does not exist"
                )
            )
        }

        if (source.isDirectory) {

            return Result.failure(
                IOException(
                    "Directory transfer is not supported yet"
                )
            )
        }

        return Result.success(Unit)
    }

    // =====================================================================
    // QUIET DELETE
    // =====================================================================

    /**
     * Deletes only an item that belongs to the current transfer.
     *
     * This is used for temporary files.
     */
    private fun deleteQuietly(
        item: StorageItem
    ) {

        try {

            when (item) {

                is StorageItem.LocalFile ->
                    item.file.delete()

                is StorageItem.Document ->
                    item.documentFile.delete()

                is StorageItem.DocumentTarget -> Unit
            }

        } catch (_: Exception) {

            /*
             * Do not throw from cleanup.
             *
             * RecoveryManager can deal with unfinished transactions.
             */
        }
    }

    // =====================================================================
    // SEARCH
    // =====================================================================

    private fun searchRecursive(
        item: StorageItem,
        query: String,
        results: MutableList<StorageItem>
    ) {

        when (item) {

            is StorageItem.LocalFile -> {

                if (!item.file.exists()) {
                    return
                }

                if (
                    item.file.isFile &&
                    item.file.name.contains(
                        query,
                        ignoreCase = true
                    )
                ) {
                    results.add(item)
                }

                if (item.file.isDirectory) {

                    item.file
                        .listFiles()
                        ?.forEach { child ->

                            searchRecursive(
                                item = StorageItem.LocalFile(
                                    child
                                ),
                                query = query,
                                results = results
                            )
                        }
                }
            }

            is StorageItem.Document -> {

                if (!item.documentFile.exists()) {
                    return
                }

                if (
                    item.documentFile.isFile &&
                    item.name.contains(
                        query,
                        ignoreCase = true
                    )
                ) {
                    results.add(item)
                }

                if (item.documentFile.isDirectory) {

                    item.documentFile
                        .listFiles()
                        .forEach { child ->

                            searchRecursive(
                                item = StorageItem.Document(
                                    uri = child.uri,
                                    documentFile = child
                                ),
                                query = query,
                                results = results
                            )
                        }
                }
            }

            is StorageItem.DocumentTarget -> {
                /*
                 * A DocumentTarget does not represent an existing
                 * searchable file.
                 */
            }
        }
    }

    // =====================================================================
    // MIME TYPE
    // =====================================================================

    private fun getMimeType(
        filename: String
    ): String {

        val extension =
            filename
                .substringAfterLast(
                    '.',
                    ""
                )
                .lowercase()

        return MimeTypeMap
            .getSingleton()
            .getMimeTypeFromExtension(
                extension
            )
            ?: "application/octet-stream"
    }

    // =====================================================================
    // CONSTANTS
    // =====================================================================

    companion object {

        /**
         * 64 KB transfer buffer.
         */
        private const val BUFFER_SIZE =
            64 * 1024

        /**
         * Persist transfer progress to Room approximately every 1 MB.
         */
        private const val PROGRESS_UPDATE_BYTES =
            1024 * 1024
    }
}