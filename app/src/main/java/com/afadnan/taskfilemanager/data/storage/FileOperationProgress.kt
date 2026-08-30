package com.afadnan.taskfilemanager.data.storage

data class FileOperationProgress(
    val currentFile: String,
    val currentBytes: Long,
    val totalBytes: Long,
    val isIndeterminate: Boolean = false
) {
    val percentage: Int
        get() {
            if (totalBytes <= 0L) {
                return 0
            }

            return (
                    (currentBytes * 100L) / totalBytes
                    )
                .coerceIn(0L, 100L)
                .toInt()
        }
}
