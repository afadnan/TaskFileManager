package com.afadnan.taskfilemanager.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afadnan.taskfilemanager.data.local.TaskEntity
import com.afadnan.taskfilemanager.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    var showTaskDialog by remember {
        mutableStateOf(false)
    }

    var editingTask by remember {
        mutableStateOf<TaskEntity?>(null)
    }

    var taskToDelete by remember {
        mutableStateOf<TaskEntity?>(null)
    }

    var selectedFilter by remember {
        mutableStateOf(TaskFilter.ALL)
    }

    /*
     * --------------------------------------------------
     * FILTER + SORT TASKS
     * --------------------------------------------------
     *
     * Filtering:
     * ALL       -> show everything
     * ACTIVE    -> show incomplete tasks
     * COMPLETED -> show completed tasks
     *
     * Sorting:
     * 1. Active tasks first
     * 2. Higher priority first
     * 3. Completed tasks last
     */
    val filteredTasks = remember(
        tasks,
        selectedFilter
    ) {
        tasks
            .filter { task ->
                when (selectedFilter) {
                    TaskFilter.ALL -> true
                    TaskFilter.ACTIVE -> !task.isCompleted
                    TaskFilter.COMPLETED -> task.isCompleted
                }
            }
            .sortedWith(
                compareBy<TaskEntity> {
                    if (it.isCompleted) 1 else 0
                }.thenByDescending {
                    it.priority
                }
            )
    }

    /*
     * --------------------------------------------------
     * MAIN SCREEN
     * --------------------------------------------------
     */
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Tasks")
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskDialog = true
                },
                modifier = Modifier.semantics {
                    contentDescription = "Add a new task"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * --------------------------------------------------
             * FILTERS
             * --------------------------------------------------
             */
            TaskFilters(
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                }
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            /*
             * --------------------------------------------------
             * EMPTY STATE
             * --------------------------------------------------
             */
            if (filteredTasks.isEmpty()) {

                EmptyTasksContent(
                    filter = selectedFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )

            } else {

                /*
                 * --------------------------------------------------
                 * TASK LIST
                 * --------------------------------------------------
                 */
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = filteredTasks,
                        key = { task ->
                            task.id
                        }
                    ) { task ->

                        TaskCard(
                            task = task,

                            onToggle = {
                                viewModel.toggleTask(task)
                            },

                            onEdit = {
                                editingTask = task
                                showTaskDialog = true
                            },

                            onDelete = {
                                taskToDelete = task
                            }
                        )
                    }
                }
            }
        }
    }

    /*
     * --------------------------------------------------
     * ADD / EDIT TASK DIALOG
     * --------------------------------------------------
     */
    if (showTaskDialog) {

        TaskDialog(
            task = editingTask,

            onDismiss = {
                showTaskDialog = false
                editingTask = null
            },

            onSave = { title, description, priority, _ ->

                if (editingTask == null) {

                    /*
                     * ADD NEW TASK
                     */
                    viewModel.addTask(
                        title = title,
                        description = description,
                        priority = priority
                    )

                } else {

                    /*
                     * EDIT EXISTING TASK
                     *
                     * IMPORTANT:
                     * Preserve the existing completion state.
                     */
                    val existingTask = editingTask!!

                    viewModel.updateTask(
                        existingTask.copy(
                            title = title.trim(),
                            description = description.trim(),
                            priority = priority,
                            isCompleted = existingTask.isCompleted
                        )
                    )
                }

                showTaskDialog = false
                editingTask = null
            }
        )
    }

    /*
     * --------------------------------------------------
     * DELETE CONFIRMATION
     * --------------------------------------------------
     */
    taskToDelete?.let { task ->

        DeleteTaskDialog(
            task = task,

            onConfirm = {
                viewModel.deleteTask(task)
                taskToDelete = null
            },

            onDismiss = {
                taskToDelete = null
            }
        )
    }
}


/*
 * ==================================================
 * TASK FILTERS
 * ==================================================
 */

@Composable
private fun TaskFilters(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        /*
         * TaskFilter.entries is preferred over
         * TaskFilter.values() with modern Kotlin.
         */
        TaskFilter.entries.forEach { filter ->

            FilterChip(
                selected = selectedFilter == filter,

                onClick = {
                    onFilterSelected(filter)
                },

                label = {
                    Text(
                        when (filter) {
                            TaskFilter.ALL -> "All"
                            TaskFilter.ACTIVE -> "Active"
                            TaskFilter.COMPLETED -> "Completed"
                        }
                    )
                }
            )
        }
    }
}


/*
 * ==================================================
 * TASK CARD
 * ==================================================
 */

@Composable
private fun TaskCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    /*
     * Completed tasks get a slightly different
     * background to make their state obvious.
     */
    val containerColor =
        if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * --------------------------------------------------
             * CHECKBOX
             * --------------------------------------------------
             */

            Checkbox(
                checked = task.isCompleted,

                onCheckedChange = {
                    onToggle()
                },

                modifier = Modifier.semantics {
                    contentDescription =
                        if (task.isCompleted) {
                            "Mark ${task.title} as active"
                        } else {
                            "Mark ${task.title} as completed"
                        }
                }
            )

            /*
             * --------------------------------------------------
             * TASK INFORMATION
             * --------------------------------------------------
             */

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {

                /*
                 * TITLE
                 */
                Text(
                    text = task.title,

                    style =
                        MaterialTheme.typography.titleMedium,

                    fontWeight =
                        if (task.isCompleted) {
                            FontWeight.Normal
                        } else {
                            FontWeight.SemiBold
                        },

                    textDecoration =
                        if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                )

                /*
                 * DESCRIPTION
                 */
                if (task.description.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = task.description,

                        style =
                            MaterialTheme.typography.bodyMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        textDecoration =
                            if (task.isCompleted) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            }
                    )
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                /*
                 * PRIORITY
                 */
                PriorityIndicator(
                    priority = task.priority
                )
            }

            /*
             * --------------------------------------------------
             * EDIT BUTTON
             * --------------------------------------------------
             */

            IconButton(
                onClick = onEdit,

                modifier = Modifier.semantics {
                    contentDescription =
                        "Edit task ${task.title}"
                }
            ) {

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null
                )
            }

            /*
             * --------------------------------------------------
             * DELETE BUTTON
             * --------------------------------------------------
             */

            IconButton(
                onClick = onDelete,

                modifier = Modifier.semantics {
                    contentDescription =
                        "Delete task ${task.title}"
                }
            ) {

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            }
        }
    }
}


/*
 * ==================================================
 * PRIORITY INDICATOR
 * ==================================================
 *
 * 0 = Low
 * 1 = Medium
 * 2 = High
 */

@Composable
private fun PriorityIndicator(
    priority: Int
) {

    val text: String

    val color = when (priority) {

        2 -> {
            text = "High"
            MaterialTheme.colorScheme.error
        }

        1 -> {
            text = "Medium"
            MaterialTheme.colorScheme.primary
        }

        else -> {
            text = "Low"
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * Small colored priority dot.
         */
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = color,
                    shape = MaterialTheme.shapes.small
                )
        )

        Spacer(
            modifier = Modifier.size(6.dp)
        )

        Text(
            text = text,

            style =
                MaterialTheme.typography.labelMedium,

            color = color
        )
    }
}


/*
 * ==================================================
 * EMPTY STATE
 * ==================================================
 */

@Composable
private fun EmptyTasksContent(
    filter: TaskFilter,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier,

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Check,

                contentDescription = null,

                tint =
                    MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.size(48.dp)
            )

            /*
             * EMPTY STATE TITLE
             */
            Text(
                text =
                    when (filter) {
                        TaskFilter.ALL ->
                            "No tasks yet"

                        TaskFilter.ACTIVE ->
                            "No active tasks"

                        TaskFilter.COMPLETED ->
                            "No completed tasks"
                    },

                style =
                    MaterialTheme.typography.titleMedium
            )

            /*
             * EMPTY STATE DESCRIPTION
             */
            Text(
                text =
                    when (filter) {
                        TaskFilter.ALL ->
                            "Tap + to create your first task"

                        TaskFilter.ACTIVE ->
                            "All your tasks are completed"

                        TaskFilter.COMPLETED ->
                            "Completed tasks will appear here"
                    },

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


/*
 * ==================================================
 * DELETE CONFIRMATION DIALOG
 * ==================================================
 */

@Composable
private fun DeleteTaskDialog(
    task: TaskEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Delete task?")
        },

        text = {
            Text(
                "Are you sure you want to delete \"${task.title}\"?"
            )
        },

        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
