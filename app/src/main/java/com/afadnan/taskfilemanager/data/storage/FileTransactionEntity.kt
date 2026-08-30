package com.afadnan.taskfilemanager.data.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_transactions"
)
data class FileTransactionEntity(

    @PrimaryKey
    val id: String,

    val operation: String,

    val sourceUri: String,

    val destinationUri: String?,

    val destinationName: String?,

    val destinationIdentifier: String?,

    val temporaryUri: String?,

    val state: String,

    val bytesCopied: Long,

    val totalBytes: Long,

    val createdAt: Long,

    val updatedAt: Long
)