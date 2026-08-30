package com.afadnan.taskfilemanager.data.storage

fun StorageItem.toPersistentIdentifier(): String =
    when (this) {

        is StorageItem.LocalFile ->
            "file:${file.absolutePath}"

        is StorageItem.Document ->
            "uri:$uri"

        is StorageItem.DocumentTarget ->
            "target:$parentUri/$fileName"
    }

fun StorageItem.persistentUri(): String =
    when (this) {

        is StorageItem.LocalFile ->
            file.toURI().toString()

        is StorageItem.Document ->
            uri.toString()

        is StorageItem.DocumentTarget ->
            parentUri.toString()
    }