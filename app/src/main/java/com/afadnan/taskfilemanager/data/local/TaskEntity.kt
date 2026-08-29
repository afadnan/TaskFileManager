package com.afadnan.taskfilemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val description: String = "",

    val isCompleted: Boolean = false,

    val priority: Int = 0,

    val createdAt: Long = System.currentTimeMillis()
)