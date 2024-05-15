package com.nosenkomi.emotionclassification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AlertDialogExample(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector? = null,
) {
    AlertDialog(
        icon = {
            icon?.let { Icon(it, contentDescription = "Example Icon") }
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(stringResource(R.string.confirm_btn))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.dismiss_btn))
            }
        }
    )
}

@Composable
fun SimpleInfoDialog(
    onConfirmation: () -> Unit = {},
    dialogTitle: String = "Title",
    dialogText: String = "long text",
    icon: ImageVector? = null,
) {
    AlertDialog(
        icon = {
            icon?.let { Icon(it, contentDescription = "Icon") }
        },
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = dialogTitle, textAlign = TextAlign.Start)
            }
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onConfirmation()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(stringResource(R.string.confirm_btn))
            }
        },
        dismissButton = {
        }
    )
}