package com.afadnan.taskfilemanager.ui.home

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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afadnan.taskfilemanager.data.local.TaskEntity
import com.afadnan.taskfilemanager.viewmodel.TaskViewModel

@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onAddTask: () -> Unit,
    onOpenFiles: () -> Unit
) {

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val totalTasks = tasks.size

    val completedTasks = tasks.count {
        it.isCompleted
    }

    val remainingTasks = tasks.count {
        !it.isCompleted
    }

    val highPriorityTasks = tasks.count {
        it.priority == 2 && !it.isCompleted
    }

    val mediumPriorityTasks = tasks.count {
        it.priority == 1 && !it.isCompleted
    }

    val lowPriorityTasks = tasks.count {
        it.priority == 0 && !it.isCompleted
    }

    val completionProgress =
        if (totalTasks == 0) {
            0f
        } else {
            completedTasks.toFloat() / totalTasks.toFloat()
        }

    /*
     * Show active tasks on dashboard.
     *
     * Maximum of 5 tasks are shown.
     */
    val todayTasks = tasks
        .filter { !it.isCompleted }
        .sortedWith(
            compareByDescending<TaskEntity> {
                it.priority
            }
        )
        .take(5)

    LazyColumn(

        modifier = Modifier.fillMaxSize(),

        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 20.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        /*
         * -----------------------------------------
         * WELCOME HEADER
         * -----------------------------------------
         */

        item {

            Column {

                Text(
                    text = "Welcome Back 👋",
                    style =
                        MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "To TaskFileManager",
                    style =
                        MaterialTheme.typography.bodyLarge,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        /*
         * -----------------------------------------
         * OVERVIEW CARD
         * -----------------------------------------
         */

        item {

            DashboardOverviewCard(
                totalTasks = totalTasks,
                completedTasks = completedTasks,
                remainingTasks = remainingTasks,
                completionProgress = completionProgress
            )
        }


        /*
         * -----------------------------------------
         * PRIORITY SUMMARY
         * -----------------------------------------
         */

        item {

            Text(
                text = "Priority Overview",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {

            PrioritySummaryCard(
                high = highPriorityTasks,
                medium = mediumPriorityTasks,
                low = lowPriorityTasks
            )
        }


        /*
         * -----------------------------------------
         * QUICK ACTIONS
         * -----------------------------------------
         */

        item {

            Text(
                text = "Quick Actions",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = onAddTask,
                    modifier = Modifier.weight(1f)
                ) {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.size(6.dp)
                    )

                    Text("Add Task")
                }

                OutlinedButton(
                    onClick = onOpenFiles,
                    modifier = Modifier.weight(1f)
                ) {

                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.size(6.dp)
                    )

                    Text("Files")
                }
            }
        }


        /*
         * -----------------------------------------
         * TASKS HEADER
         * -----------------------------------------
         */

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Active Tasks",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$remainingTasks remaining",
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.primary
                )
            }
        }


        /*
         * -----------------------------------------
         * EMPTY STATE
         * -----------------------------------------
         */

        if (todayTasks.isEmpty()) {

            item {

                EmptyTodayTasks()
            }

        } else {

            /*
             * --------------------------------------
             * TASK LIST
             * --------------------------------------
             */

            items(
                items = todayTasks,
                key = { it.id }
            ) { task ->

                HomeTaskItem(
                    task = task,
                    onToggle = {
                        viewModel.toggleTask(task)
                    }
                )
            }
        }


        /*
         * -----------------------------------------
         * COMPLETION MESSAGE
         * -----------------------------------------
         */

        if (totalTasks > 0 && completedTasks == totalTasks) {

            item {

                AllTasksCompletedCard()
            }
        }
    }
}


/*
 * =====================================================
 * DASHBOARD OVERVIEW
 * =====================================================
 */

@Composable
private fun DashboardOverviewCard(
    totalTasks: Int,
    completedTasks: Int,
    remainingTasks: Int,
    completionProgress: Float
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.size(12.dp)
                )

                Text(
                    text = "Task Overview",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                TaskStat(
                    value = totalTasks,
                    label = "Total"
                )

                TaskStat(
                    value = completedTasks,
                    label = "Completed"
                )

                TaskStat(
                    value = remainingTasks,
                    label = "Remaining"
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text =
                    if (totalTasks == 0) {
                        "No tasks created yet"
                    } else {
                        "${(completionProgress * 100).toInt()}% completed"
                    },

                style =
                    MaterialTheme.typography.labelLarge
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LinearProgressIndicator(
                progress = {
                    completionProgress
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


/*
 * =====================================================
 * TASK STAT
 * =====================================================
 */

@Composable
private fun TaskStat(
    value: Int,
    label: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = value.toString(),

            style =
                MaterialTheme.typography.headlineMedium,

            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = label,

            style =
                MaterialTheme.typography.labelMedium,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


/*
 * =====================================================
 * PRIORITY SUMMARY
 * =====================================================
 */

@Composable
private fun PrioritySummaryCard(
    high: Int,
    medium: Int,
    low: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            PriorityRow(
                label = "High priority",
                count = high,
                color =
                    MaterialTheme.colorScheme.error
            )

            PriorityRow(
                label = "Medium priority",
                count = medium,
                color =
                    MaterialTheme.colorScheme.primary
            )

            PriorityRow(
                label = "Low priority",
                count = low,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/*
 * =====================================================
 * PRIORITY ROW
 * =====================================================
 */

@Composable
private fun PriorityRow(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.size(10.dp)
        ) {

            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                drawCircle(
                    color = color,
                    radius = size.minDimension / 2
                )
            }
        }

        Spacer(
            modifier = Modifier.size(10.dp)
        )

        Text(
            text = label,

            style =
                MaterialTheme.typography.bodyMedium,

            modifier = Modifier.weight(1f)
        )

        Text(
            text = count.toString(),

            style =
                MaterialTheme.typography.titleMedium,

            fontWeight = FontWeight.Bold,

            color = color
        )
    }
}


/*
 * =====================================================
 * HOME TASK ITEM
 * =====================================================
 */

@Composable
private fun HomeTaskItem(
    task: TaskEntity,
    onToggle: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Check,

                contentDescription =
                    "Complete task",

                tint =
                    MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {

                Text(
                    text = task.title,

                    style =
                        MaterialTheme.typography.bodyLarge,

                    fontWeight =
                        FontWeight.Medium,

                    textDecoration =
                        if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                )

                if (task.description.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text = task.description,

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = onToggle
            ) {

                Text("Done")
            }
        }
    }
}


/*
 * =====================================================
 * EMPTY STATE
 * =====================================================
 */

@Composable
private fun EmptyTodayTasks() {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),

            contentAlignment =
                Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.Check,

                    contentDescription = null,

                    tint =
                        MaterialTheme.colorScheme.primary,

                    modifier =
                        Modifier.size(48.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = "You're all caught up!",

                    style =
                        MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "No active tasks right now.",

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
}


/*
 * =====================================================
 * ALL TASKS COMPLETED
 * =====================================================
 */

@Composable
private fun AllTasksCompletedCard() {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.size(12.dp)
            )

            Column {

                Text(
                    text = "Great job! 🎉",

                    style =
                        MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "You've completed all your tasks.",

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
}