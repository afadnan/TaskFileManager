package com.afadnan.taskfilemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.afadnan.taskfilemanager.data.storage.StorageLocationManager

class FileViewModelFactory(
    private val storageLocationManager: StorageLocationManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                FileViewModel::class.java
            )
        ) {
            return FileViewModel(
                storageLocationManager
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
