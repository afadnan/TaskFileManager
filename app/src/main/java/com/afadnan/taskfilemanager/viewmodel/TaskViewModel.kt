package com.afadnan.taskfilemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afadnan.taskfilemanager.data.local.TaskEntity
import com.afadnan.taskfilemanager.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    /**
     * All tasks stored in Room.
     *
     * Room automatically emits a new list whenever
     * the database changes.
     */
    val tasks: StateFlow<List<TaskEntity>> =
        repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Add a new task.
     */
    fun addTask(
        title: String,
        description: String = "",
        priority: Int = 0
    ) {
        val cleanTitle = title.trim()
        val cleanDescription = description.trim()

        // Do not allow an empty title.
        if (cleanTitle.isBlank()) {
            return
        }

        // 0 = Low
        // 1 = Medium
        // 2 = High
        val cleanPriority = priority.coerceIn(0, 2)

        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = cleanTitle,
                    description = cleanDescription,
                    priority = cleanPriority,
                    isCompleted = false
                )
            )
        }
    }

    /**
     * Update an existing task.
     */
    fun updateTask(task: TaskEntity) {

        val cleanTitle = task.title.trim()
        val cleanDescription = task.description.trim()

        // Do not save a task without a title.
        if (cleanTitle.isBlank()) {
            return
        }

        val cleanPriority = task.priority.coerceIn(0, 2)

        val updatedTask = task.copy(
            title = cleanTitle,
            description = cleanDescription,
            priority = cleanPriority

            // isCompleted is preserved automatically
            // because it is not changed here.
        )

        viewModelScope.launch {
            repository.updateTask(updatedTask)
        }
    }

    /**
     * Delete one task.
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    /**
     * Toggle task completion.
     */
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {

            repository.updateCompletion(
                taskId = task.id,
                completed = !task.isCompleted
            )
        }
    }

    /**
     * Delete all tasks.
     */
    fun deleteAllTasks() {
        viewModelScope.launch {
            repository.deleteAllTasks()
        }
    }
}