package com.afadnan.taskfilemanager.data.repository

import com.afadnan.taskfilemanager.data.local.TaskDao
import com.afadnan.taskfilemanager.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {

    val allTasks: Flow<List<TaskEntity>> =
        taskDao.getAllTasks()

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun updateCompletion(
        taskId: Long,
        completed: Boolean
    ) {
        taskDao.updateCompletion(
            taskId = taskId,
            completed = completed
        )
    }

    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }

    suspend fun getTaskById(id: Long): TaskEntity? {
        return taskDao.getTaskById(id)
    }
}