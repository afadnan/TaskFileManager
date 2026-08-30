package com.afadnan.taskfilemanager.ui.files

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {

    var name by remember {
        mutableStateOf(currentName)
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Rename"
            )
        },

        text = {

            OutlinedTextField(

                value = name,

                onValueChange = {
                    name = it
                },

                singleLine = true,

                label = {
                    Text(
                        text = "Name"
                    )
                }
            )
        },

        confirmButton = {

            TextButton(

                onClick = {

                    val trimmedName =
                        name.trim()

                    if (trimmedName.isNotEmpty()) {

                        onRename(
                            trimmedName
                        )
                    }
                }
            ) {

                Text(
                    text = "Rename"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "Cancel"
                )
            }
        }
    )
}