package com.afadnan.taskfilemanager.ui.files

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DeleteDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Delete items?"
            )
        },

        text = {

            Text(
                text =
                    if (selectedCount == 1) {
                        "Are you sure you want to delete this item?"
                    } else {
                        "Are you sure you want to delete $selectedCount items?"
                    }
            )
        },

        confirmButton = {

            TextButton(
                onClick = onConfirm
            ) {

                Text(
                    text = "Delete"
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