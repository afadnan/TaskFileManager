package com.afadnan.taskfilemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.afadnan.taskfilemanager.data.storage.FileTransactionDao
import com.afadnan.taskfilemanager.data.storage.FileTransactionEntity

@Database(
    entities = [
        TaskEntity::class,
        FileTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun fileTransactionDao(): FileTransactionDao

    companion object {

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        /**
         * Migration:
         *
         * Version 1 → Version 2
         *
         * Adds the file_transactions table.
         */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS file_transactions (
                            id TEXT NOT NULL PRIMARY KEY,
                            operation TEXT NOT NULL,
                            sourceUri TEXT NOT NULL,
                            destinationUri TEXT,
                            destinationName TEXT,
                            temporaryUri TEXT,
                            state TEXT NOT NULL,
                            bytesCopied INTEGER NOT NULL,
                            totalBytes INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }

        /**
         * Migration:
         *
         * Version 2 → Version 3
         *
         * Adds destinationIdentifier.
         *
         * destinationIdentifier is required for reliable
         * crash recovery, especially with SAF/SD-card targets.
         */
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE file_transactions
                        ADD COLUMN destinationIdentifier TEXT
                        """.trimIndent()
                    )
                }
            }

        /**
         * Returns the singleton Room database.
         */
        fun getDatabase(
            context: Context
        ): TaskDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            TaskDatabase::class.java,
                            "task_file_manager_database"
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3
                            )
                            .build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}
