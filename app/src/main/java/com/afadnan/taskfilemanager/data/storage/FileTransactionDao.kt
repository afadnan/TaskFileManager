package com.afadnan.taskfilemanager.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileTransactionDao {

    /**
     * Inserts a new file transaction.
     *
     * REPLACE is useful here because a transaction with the same
     * ID can safely replace an older record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(
        transaction: FileTransactionEntity
    )

    /**
     * Returns all transactions that were not completed or
     * permanently failed.
     *
     * FileRecoveryManager uses this when the application starts.
     */
    @Query(
        """
        SELECT *
        FROM file_transactions
        WHERE state != 'COMPLETED'
          AND state != 'FAILED'
        ORDER BY createdAt ASC
        """
    )
    suspend fun getUnfinishedTransactions(): List<FileTransactionEntity>

    /**
     * Returns one transaction by ID.
     */
    @Query(
        """
        SELECT *
        FROM file_transactions
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(
        id: String
    ): FileTransactionEntity?

    /**
     * Updates transfer progress.
     *
     * Used while copying data into the temporary destination.
     */
    @Query(
        """
        UPDATE file_transactions
        SET state = :state,
            bytesCopied = :bytesCopied,
            totalBytes = :totalBytes,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateProgress(
        id: String,
        state: String,
        bytesCopied: Long,
        totalBytes: Long,
        updatedAt: Long
    )

    /**
     * Stores the temporary destination identifier.
     *
     * Example:
     *
     * file:/storage/emulated/0/Download/.report.pdf.123.tmp
     *
     * or:
     *
     * uri:content://...
     */
    @Query(
        """
        UPDATE file_transactions
        SET temporaryUri = :temporaryUri,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateTemporaryUri(
        id: String,
        temporaryUri: String,
        updatedAt: Long
    )

    /**
     * Updates only the transaction state.
     */
    @Query(
        """
        UPDATE file_transactions
        SET state = :state,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateState(
        id: String,
        state: String,
        updatedAt: Long
    )
}