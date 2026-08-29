package com.afadnan.taskfilemanager.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afadnan.taskfilemanager.data.local.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    task: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        priority: Int,
        isCompleted: Boolean
    ) -> Unit
) {

    /*
     * Initial values.
     *
     * If task == null:
     *      We are creating a new task.
     *
     * If task != null:
     *      We are editing an existing task.
     */
    var title by remember(task) {
        mutableStateOf(task?.title ?: "")
    }

    var description by remember(task) {
        mutableStateOf(task?.description ?: "")
    }

    var priority by remember(task) {
        mutableStateOf(task?.priority ?: 0)
    }

    /*
     * VERY IMPORTANT:
     *
     * When editing a task, we keep its existing completion state.
     */
    val isCompleted = task?.isCompleted ?: false

    /*
     * Controls whether the title validation error is displayed.
     */
    var titleError by remember {
        mutableStateOf(false)
    }

    var priorityExpanded by remember {
        mutableStateOf(false)
    }

    val priorityText = when (priority) {
        2 -> "High"
        1 -> "Medium"
        else -> "Low"
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = if (task == null) {
                    "Add Task"
                } else {
                    "Edit Task"
                }
            )
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                /*
                 * TITLE
                 */
                OutlinedTextField(
                    value = title,

                    onValueChange = {
                        title = it

                        /*
                         * Remove the error as soon as
                         * the user starts entering valid text.
                         */
                        if (it.trim().isNotEmpty()) {
                            titleError = false
                        }
                    },

                    modifier = Modifier.fillMaxWidth(),

                    label = {
                        Text("Task title")
                    },

                    placeholder = {
                        Text("Enter task title")
                    },

                    singleLine = true,

                    isError = titleError,

                    supportingText = {
                        if (titleError) {
                            Text("Please enter a task title")
                        }
                    }
                )

                /*
                 * DESCRIPTION
                 */
                OutlinedTextField(
                    value = description,

                    onValueChange = {
                        description = it
                    },

                    modifier = Modifier.fillMaxWidth(),

                    label = {
                        Text("Description")
                    },

                    placeholder = {
                        Text("Optional description")
                    },

                    minLines = 3,

                    maxLines = 5
                )

                /*
                 * PRIORITY
                 */
                Text(
                    text = "Priority"
                )

                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,

                    onExpandedChange = {
                        priorityExpanded = !priorityExpanded
                    }
                ) {

                    OutlinedTextField(
                        value = priorityText,

                        onValueChange = {},

                        readOnly = true,

                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),

                        label = {
                            Text("Priority")
                        },

                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = priorityExpanded
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = priorityExpanded,

                        onDismissRequest = {
                            priorityExpanded = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Low")
                            },

                            onClick = {
                                priority = 0
                                priorityExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Medium")
                            },

                            onClick = {
                                priority = 1
                                priorityExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("High")
                            },

                            onClick = {
                                priority = 2
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    /*
                     * --------------------------------
                     * VALIDATION
                     * --------------------------------
                     */

                    val cleanTitle = title.trim()

                    /*
                     * Empty or whitespace-only title
                     * is invalid.
                     */
                    if (cleanTitle.isEmpty()) {
                        titleError = true
                        return@Button
                    }

                    /*
                     * Description is optional.
                     *
                     * Empty/whitespace description becomes "".
                     */
                    val cleanDescription = description.trim()

                    /*
                     * Make sure priority is always
                     * one of our supported values.
                     */
                    val cleanPriority = priority.coerceIn(0, 2)

                    /*
                     * Everything is valid.
                     *
                     * Send cleaned data to TasksScreen.
                     */
                    onSave(
                        cleanTitle,
                        cleanDescription,
                        cleanPriority,
                        isCompleted
                    )
                }
            ) {
                Text(
                    text = if (task == null) {
                        "Add"
                    } else {
                        "Save"
                    }
                )
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