package com.afadnan.taskfilemanager

import android.app.Application
import com.afadnan.taskfilemanager.data.local.TaskDatabase
import com.afadnan.taskfilemanager.data.storage.FileRecoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskFileManagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val database =
            TaskDatabase.getDatabase(this)

        val recoveryManager =
            FileRecoveryManager(
                context = applicationContext,
                transactionDao = database.fileTransactionDao()
            )

        /*
         * Recovery runs asynchronously so application startup
         * is not blocked by potentially large file verification.
         */
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        ).launch {

            recoveryManager.recover()
        }
    }
}