package com.afadnan.taskfilemanager.data.storage

enum class FileOperationType {
    COPY,
    MOVE,
    DELETE,
    RENAME
}

enum class FileTransactionState {
    STARTED,
    COPYING,
    VERIFYING,
    COMMITTING,
    COMMITTED,
    FINALIZING,
    COMPLETED,
    FAILED
}