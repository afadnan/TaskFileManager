package com.afadnan.taskfilemanager.data.storage

import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import android.content.Context

class FileRecoveryManager(
    private val context: Context,
    private val transactionDao: FileTransactionDao
) {
    private val contentResolver: ContentResolver
        get() = context.contentResolver
    /**
     * Called when the application starts.
     *
     * Finds transactions that were interrupted by:
     *
     * - application crash
     * - Android process death
     * - force stop
     * - unexpected device shutdown
     *
     * Recovery is deliberately conservative:
     *
     * - Never delete the original source unless the destination
     *   has been verified.
     * - Never delete a committed destination.
     * - Never allow one broken transaction to stop recovery
     *   of other transactions.
     */
    suspend fun recover() {

        val transactions =
            transactionDao.getUnfinishedTransactions()

        transactions.forEach { transaction ->

            try {

                recoverTransaction(
                    transaction
                )

            } catch (_: Exception) {

                /*
                 * Never allow recovery of one transaction
                 * to prevent recovery of the remaining ones.
                 *
                 * We intentionally do not mark the transaction
                 * FAILED here because an unexpected error may
                 * simply mean that the storage provider was
                 * temporarily unavailable.
                 */
            }
        }
    }

    // =====================================================================
    // TRANSACTION RECOVERY
    // =====================================================================

    private suspend fun recoverTransaction(
        transaction: FileTransactionEntity
    ) {

        val state =
            parseTransactionState(
                transaction.state
            )

        when (state) {

            FileTransactionState.STARTED,
            FileTransactionState.COPYING,
            FileTransactionState.VERIFYING,
            FileTransactionState.COMMITTING -> {

                recoverBeforeCommit(
                    transaction
                )
            }

            FileTransactionState.COMMITTED,
            FileTransactionState.FINALIZING -> {

                recoverAfterCommit(
                    transaction
                )
            }

            FileTransactionState.COMPLETED,
            FileTransactionState.FAILED -> {

                /*
                 * Nothing to recover.
                 */
            }
        }
    }

    // =====================================================================
    // BEFORE COMMIT
    // =====================================================================

    /**
     * Recovery for transactions where the final destination
     * has not been committed yet.
     *
     * Example:
     *
     * SOURCE
     *    ↓
     * TEMPORARY
     *    ↓
     * 63%
     *    X CRASH
     *
     * Recovery:
     *
     * SOURCE       → KEEP
     * TEMPORARY    → DELETE
     * TRANSACTION  → FAILED
     */
    private suspend fun recoverBeforeCommit(
        transaction: FileTransactionEntity
    ) {

        val temporary =
            transaction.temporaryUri
                ?.let(::parsePersistentItem)

        /*
         * The final destination has not been committed.
         *
         * Therefore the source must remain untouched.
         */
        if (temporary != null) {
            deleteQuietly(temporary)
        }

        transactionDao.updateState(
            id = transaction.id,
            state = FileTransactionState.FAILED.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    // =====================================================================
    // AFTER COMMIT
    // =====================================================================

    /**
     * Recovery after the final destination has been committed.
     *
     * Possible crash:
     *
     * SOURCE
     *    ↓
     * COPY
     *    ↓
     * VERIFY
     *    ↓
     * COMMIT
     *    ↓
     * 💥 CRASH
     *
     * On restart:
     *
     * SOURCE      = exists
     * DESTINATION = exists
     *
     * COPY:
     *      destination is already complete
     *
     * MOVE:
     *      verify destination
     *      delete source
     */
    private suspend fun recoverAfterCommit(
        transaction: FileTransactionEntity
    ) {

        val operation =
            parseOperation(
                transaction.operation
            )
                ?: return

        val source =
            parsePersistentItem(
                transaction.sourceUri
            )
                ?: return

        /*
         * IMPORTANT:
         *
         * Do not rely only on destinationUri.
         *
         * For DocumentTarget, destinationUri points to the parent
         * directory, not the final file.
         *
         * destinationIdentifier contains:
         *
         * target:<parentUri>/<filename>
         *
         * Therefore we reconstruct the actual destination from
         * destinationIdentifier + destinationName.
         */
        val destination =
            parseDestination(
                transaction
            )

        when (operation) {

            FileOperationType.COPY -> {

                recoverCopyAfterCommit(
                    transaction = transaction,
                    destination = destination
                )
            }

            FileOperationType.MOVE -> {

                recoverMoveAfterCommit(
                    transaction = transaction,
                    source = source,
                    destination = destination
                )
            }

            FileOperationType.DELETE,
            FileOperationType.RENAME -> {

                /*
                 * These operations are currently not journaled
                 * by FileOperationManager.
                 */
            }
        }
    }

    // =====================================================================
    // COPY RECOVERY
    // =====================================================================

    /**
     * COPY recovery.
     *
     * If the destination exists, we consider the committed copy
     * successful only after checking that the destination can be
     * read and has the expected size/hash where possible.
     */
    private suspend fun recoverCopyAfterCommit(
        transaction: FileTransactionEntity,
        destination: StorageItem?
    ) {

        if (destination == null) {

            /*
             * We cannot safely identify the destination.
             *
             * Do not guess.
             */
            return
        }

        if (!destinationExists(destination)) {

            /*
             * Destination disappeared.
             *
             * The source remains untouched.
             */
            transactionDao.updateState(
                id = transaction.id,
                state = FileTransactionState.FAILED.name,
                updatedAt = System.currentTimeMillis()
            )

            return
        }

        val source =
            parsePersistentItem(
                transaction.sourceUri
            )

        if (source == null) {
            return
        }

        /*
         * Verify destination against source.
         *
         * This prevents us from declaring a partially-created
         * destination as completed.
         */
        val verified =
            verifySourceAndDestination(
                source = source,
                destination = destination
            )

        if (!verified) {

            /*
             * Do NOT delete the source.
             *
             * We don't know whether the destination is valid.
             */
            transactionDao.updateState(
                id = transaction.id,
                state = FileTransactionState.FAILED.name,
                updatedAt = System.currentTimeMillis()
            )

            return
        }

        /*
         * Destination is valid.
         *
         * Source remains untouched because this is COPY.
         */
        transactionDao.updateState(
            id = transaction.id,
            state = FileTransactionState.COMPLETED.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    // =====================================================================
    // MOVE RECOVERY
    // =====================================================================

    /**
     * MOVE recovery after destination commit.
     *
     * This is the most important recovery path.
     *
     * If:
     *
     * destination exists
     * +
     * destination matches source
     *
     * then it is safe to remove the source.
     */
    private suspend fun recoverMoveAfterCommit(
        transaction: FileTransactionEntity,
        source: StorageItem,
        destination: StorageItem?
    ) {

        if (destination == null) {

            /*
             * We cannot safely identify the destination.
             *
             * Never delete the source.
             */
            return
        }

        if (!destinationExists(destination)) {

            /*
             * Destination disappeared after the transaction
             * was supposedly committed.
             *
             * NEVER delete the source.
             */
            transactionDao.updateState(
                id = transaction.id,
                state = FileTransactionState.FAILED.name,
                updatedAt = System.currentTimeMillis()
            )

            return
        }

        /*
         * Verify destination before touching source.
         */
        val verified =
            verifySourceAndDestination(
                source = source,
                destination = destination
            )

        if (!verified) {

            /*
             * Destination exists but does not match source.
             *
             * Source must remain.
             */
            transactionDao.updateState(
                id = transaction.id,
                state = FileTransactionState.FAILED.name,
                updatedAt = System.currentTimeMillis()
            )

            return
        }

        /*
         * Destination is confirmed valid.
         *
         * It is now safe to enter FINALIZING.
         */
        transactionDao.updateState(
            id = transaction.id,
            state = FileTransactionState.FINALIZING.name,
            updatedAt = System.currentTimeMillis()
        )

        /*
         * If the source has already disappeared, the move was
         * effectively completed before the crash.
         */
        if (!sourceExists(source)) {

            transactionDao.updateState(
                id = transaction.id,
                state = FileTransactionState.COMPLETED.name,
                updatedAt = System.currentTimeMillis()
            )

            return
        }

        /*
         * Remove source.
         */
        val deleted =
            delete(source)

        if (deleted) {

            transactionDao.updateState(
                id = transaction.id,
                state = FileTransactionState.COMPLETED.name,
                updatedAt = System.currentTimeMillis()
            )

        } else {

            /*
             * Destination is safe.
             *
             * Source still exists.
             *
             * Keep FINALIZING so recovery can retry next time.
             */
            transactionDao.updateState(
                id = transaction.id,
                state = FileTransactionState.FINALIZING.name,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // =====================================================================
    // DESTINATION RECONSTRUCTION
    // =====================================================================

    /**
     * Reconstructs the actual destination StorageItem.
     *
     * Priority:
     *
     * 1. destinationIdentifier
     * 2. destinationUri + destinationName
     *
     * This is necessary because:
     *
     * DocumentTarget:
     *
     *     parentUri + fileName
     *
     * is different from:
     *
     * Document:
     *
     *     final document URI
     */
    private fun parseDestination(
        transaction: FileTransactionEntity
    ): StorageItem? {

        /*
         * New transactions should contain destinationIdentifier.
         */
        transaction.destinationIdentifier
            ?.let { identifier ->

                parsePersistentDestination(
                    identifier = identifier,
                    destinationName =
                        transaction.destinationName
                )?.let { destination ->

                    return destination
                }
            }

        /*
         * Backward compatibility for transactions created before
         * destinationIdentifier was introduced.
         */
        transaction.destinationUri?.let { uriString ->

            val destination =
                parsePersistentItem(
                    uriString
                )

            if (destination != null) {

                /*
                 * If this is a Document representing the parent
                 * directory, reconstruct the final file when
                 * destinationName is available.
                 */
                if (
                    destination is StorageItem.Document &&
                    destination.documentFile.isDirectory &&
                    !transaction.destinationName.isNullOrBlank()
                ) {

                    return StorageItem.DocumentTarget(
                        parentUri =
                            destination.uri,
                        parentDocument =
                            destination.documentFile,
                        fileName =
                            transaction.destinationName
                    )
                }

                return destination
            }
        }

        return null
    }

    /**
     * Parses:
     *
     * file:/storage/emulated/0/Download/report.pdf
     *
     * uri:content://...
     *
     * target:content://parent/document/report.pdf
     *
     * For a target identifier we reconstruct the parent DocumentFile
     * and the filename.
     */
    private fun parsePersistentDestination(
        identifier: String,
        destinationName: String?
    ): StorageItem? {

        return when {

            identifier.startsWith("file:") -> {

                val path =
                    identifier.removePrefix(
                        "file:"
                    )

                StorageItem.LocalFile(
                    File(path)
                )
            }

            identifier.startsWith("uri:") -> {

                val uriString =
                    identifier.removePrefix(
                        "uri:"
                    )

                val uri =
                    Uri.parse(uriString)

                val document =
                    DocumentFile.fromSingleUri(
                        context,
                        uri
                    )
                        ?: return null

                StorageItem.Document(
                    uri = uri,
                    documentFile = document
                )
            }

            identifier.startsWith("target:") -> {

                parseTargetIdentifier(
                    identifier = identifier,
                    destinationName = destinationName
                )
            }

            else -> {
                null
            }
        }
    }

    /**
     * Reconstructs a DocumentTarget.
     *
     * The persistent format generated by StorageItem is:
     *
     * target:<parentUri>/<fileName>
     *
     * Since Uri itself can contain encoded characters and '/'
     * characters, we DO NOT blindly split on every slash.
     *
     * We use the persisted destinationName when available and
     * remove that suffix from the identifier.
     */
    private fun parseTargetIdentifier(
        identifier: String,
        destinationName: String?
    ): StorageItem? {

        val raw =
            identifier.removePrefix(
                "target:"
            )

        /*
         * destinationName is the safest way to reconstruct
         * the parent URI.
         */
        if (!destinationName.isNullOrBlank()) {

            val suffix =
                "/$destinationName"

            if (raw.endsWith(suffix)) {

                val parentUriString =
                    raw.removeSuffix(suffix)

                return createDocumentTarget(
                    parentUriString = parentUriString,
                    fileName = destinationName
                )
            }
        }

        /*
         * Fallback:
         *
         * If destinationName is unavailable, try to infer the
         * final path segment.
         *
         * This is less reliable for unusual provider URIs,
         * so we only use it as a fallback.
         */
        val lastSlash =
            raw.lastIndexOf('/')

        if (lastSlash <= 0) {
            return null
        }

        val parentUriString =
            raw.substring(
                0,
                lastSlash
            )

        val fileName =
            raw.substring(
                lastSlash + 1
            )

        if (fileName.isBlank()) {
            return null
        }

        return createDocumentTarget(
            parentUriString = parentUriString,
            fileName = fileName
        )
    }

    /**
     * Creates a DocumentTarget from a parent URI.
     */
    private fun createDocumentTarget(
        parentUriString: String,
        fileName: String
    ): StorageItem? {

        return try {

            val parentUri =
                Uri.parse(
                    parentUriString
                )

            val parentDocument =
                DocumentFile.fromTreeUri(
                    context,
                    parentUri
                )
                    ?: DocumentFile.fromSingleUri(
                        context,
                        parentUri
                    )
                    ?: return null

            if (!parentDocument.isDirectory) {
                return null
            }

            StorageItem.DocumentTarget(
                parentUri = parentUri,
                parentDocument = parentDocument,
                fileName = fileName
            )

        } catch (_: Exception) {

            null
        }
    }

    // =====================================================================
    // VERIFICATION
    // =====================================================================

    /**
     * Verifies source and destination.
     *
     * Verification consists of:
     *
     * 1. File length comparison.
     * 2. SHA-256 comparison.
     *
     * If both hashes match, the destination is considered
     * identical to the source.
     */
    private fun verifySourceAndDestination(
        source: StorageItem,
        destination: StorageItem
    ): Boolean {

        if (!sourceExists(source)) {
            return false
        }

        if (!destinationExists(destination)) {
            return false
        }

        val sourceLength =
            getLength(source)

        val destinationLength =
            getLength(destination)

        /*
         * If both providers report a valid length, compare them.
         */
        if (
            sourceLength >= 0 &&
            destinationLength >= 0 &&
            sourceLength != destinationLength
        ) {
            return false
        }

        val sourceHash =
            sha256(source)
                ?: return false

        val destinationHash =
            sha256(destination)
                ?: return false

        return MessageDigest.isEqual(
            sourceHash,
            destinationHash
        )
    }

    /**
     * Calculates SHA-256 of a storage item.
     */
    private fun sha256(
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

                    if (count > 0) {

                        digest.update(
                            buffer,
                            0,
                            count
                        )
                    }
                }
            }

            digest.digest()

        } catch (_: Exception) {

            null
        }
    }

    // =====================================================================
    // INPUT STREAM
    // =====================================================================

    private fun openInputStream(
        item: StorageItem
    ) = when (item) {

        is StorageItem.LocalFile -> {

            if (item.file.exists()) {

                item.file
                    .inputStream()
                    .buffered()

            } else {
                null
            }
        }

        is StorageItem.Document -> {

            if (!item.documentFile.exists()) {
                null
            } else {

                contentResolver
                    .openInputStream(
                        item.uri
                    )
                    ?.buffered()
            }
        }

        is StorageItem.DocumentTarget -> {
            null
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

            is StorageItem.Document -> {

                if (item.documentFile.exists()) {
                    item.documentFile.length()
                } else {
                    -1L
                }
            }

            is StorageItem.DocumentTarget -> {

                /*
                 * A DocumentTarget does not itself represent an
                 * existing file.
                 */
                -1L
            }
        }
    }

    // =====================================================================
    // EXISTS
    // =====================================================================

    private fun destinationExists(
        destination: StorageItem
    ): Boolean {

        return when (destination) {

            is StorageItem.LocalFile ->
                destination.file.exists()

            is StorageItem.Document ->
                destination.documentFile.exists()

            is StorageItem.DocumentTarget ->
                destination.parentDocument
                    .findFile(
                        destination.fileName
                    )
                    ?.exists() == true
        }
    }

    private fun sourceExists(
        source: StorageItem
    ): Boolean {

        return when (source) {

            is StorageItem.LocalFile ->
                source.file.exists()

            is StorageItem.Document ->
                source.documentFile.exists()

            is StorageItem.DocumentTarget ->
                false
        }
    }

    // =====================================================================
    // DELETE
    // =====================================================================

    private fun delete(
        item: StorageItem
    ): Boolean {

        return when (item) {

            is StorageItem.LocalFile ->
                item.file.deleteRecursively()

            is StorageItem.Document ->
                item.documentFile.delete()

            is StorageItem.DocumentTarget ->
                false
        }
    }

    /**
     * Cleanup helper.
     *
     * Never throws.
     */
    private fun deleteQuietly(
        item: StorageItem
    ) {

        try {

            delete(item)

        } catch (_: Exception) {

            /*
             * Deliberately ignored.
             *
             * A later recovery attempt can retry cleanup.
             */
        }
    }

    // =====================================================================
    // PARSING HELPERS
    // =====================================================================

    private fun parsePersistentItem(
        value: String
    ): StorageItem? {

        return when {

            value.startsWith("file:") -> {

                val path =
                    value.removePrefix(
                        "file:"
                    )

                StorageItem.LocalFile(
                    File(path)
                )
            }

            value.startsWith("uri:") -> {

                val uriString =
                    value.removePrefix(
                        "uri:"
                    )

                val uri =
                    Uri.parse(
                        uriString
                    )

                val document =
                    DocumentFile.fromSingleUri(
                        context,
                        uri
                    )
                        ?: return null

                StorageItem.Document(
                    uri = uri,
                    documentFile = document
                )
            }

            value.startsWith("target:") -> {

                /*
                 * Try to reconstruct target using the encoded
                 * filename if possible.
                 *
                 * Normally destinationIdentifier should be handled
                 * by parsePersistentDestination(), because it also
                 * has destinationName available.
                 */
                parseTargetIdentifier(
                    identifier = value,
                    destinationName = null
                )
            }

            else -> {
                null
            }
        }
    }

    private fun parseTransactionState(
        value: String
    ): FileTransactionState {

        return try {

            FileTransactionState.valueOf(
                value
            )

        } catch (_: Exception) {

            /*
             * Unknown transaction states should be treated
             * conservatively.
             *
             * We don't want to accidentally delete a source.
             */
            FileTransactionState.FINALIZING
        }
    }

    private fun parseOperation(
        value: String
    ): FileOperationType? {

        return try {

            FileOperationType.valueOf(
                value
            )

        } catch (_: Exception) {

            null
        }
    }

    // =====================================================================
    // CONSTANTS
    // =====================================================================

    companion object {

        private const val BUFFER_SIZE =
            64 * 1024
    }
}