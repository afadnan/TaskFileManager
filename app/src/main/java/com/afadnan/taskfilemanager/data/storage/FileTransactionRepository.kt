package com.afadnan.taskfilemanager.data.storage

import java.util.UUID

class FileTransactionRepository(
    private val dao: FileTransactionDao
) {

    suspend fun create(
        operation: FileOperationType,
        source: StorageItem,
        destination: StorageItem?
    ): String {

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        dao.insert(
            FileTransactionEntity(
                id = id,

                operation = operation.name,

                sourceUri =
                    source.toPersistentIdentifier(),

                destinationUri =
                    destination?.persistentUri(),

                destinationName =
                    destination?.name,

                destinationIdentifier =
                    destination?.toPersistentIdentifier(),

                temporaryUri = null,

                state =
                    FileTransactionState.STARTED.name,

                bytesCopied = 0L,

                totalBytes = 0L,

                createdAt = now,

                updatedAt = now
            )
        )

        return id
    }

    suspend fun updateProgress(
        id: String,
        state: FileTransactionState,
        copied: Long,
        total: Long
    ) {
        dao.updateProgress(
            id = id,
            state = state.name,
            bytesCopied = copied,
            totalBytes = total,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updateState(
        id: String,
        state: FileTransactionState
    ) {
        dao.updateState(
            id = id,
            state = state.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updateTemporaryUri(
        id: String,
        temporaryUri: String
    ) {
        dao.updateTemporaryUri(
            id = id,
            temporaryUri = temporaryUri,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun getById(
        id: String
    ): FileTransactionEntity? {
        return dao.getById(id)
    }

    suspend fun complete(
        id: String
    ) {
        updateState(
            id,
            FileTransactionState.COMPLETED
        )
    }

    suspend fun fail(
        id: String
    ) {
        updateState(
            id,
            FileTransactionState.FAILED
        )
    }
}