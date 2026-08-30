package com.afadnan.taskfilemanager.data.storage

sealed interface FileOperationError {

    data object SourceNotFound : FileOperationError

    data object DestinationNotFound : FileOperationError

    data object DestinationAlreadyExists : FileOperationError

    data object InvalidSource : FileOperationError

    data object InvalidDestination : FileOperationError

    data object PermissionDenied : FileOperationError

    data object VerificationFailed : FileOperationError

    data object CommitFailed : FileOperationError

    data class IoError(
        val message: String?
    ) : FileOperationError

    data class Unknown(
        val message: String?
    ) : FileOperationError
}