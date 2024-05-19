package com.nosenkomi.emotionclassification

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector? = null,
    dismissText: String = stringResource(id = R.string.dismiss_btn),
    confirmText: String = stringResource(id = R.string.confirm_btn),
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
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(dismissText)
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

@Composable
fun AnimatedIconButton(
    modifier: Modifier = Modifier,
    textStart: String = "start",
    textEnd: String = "end",
    iconStart: ImageVector = Icons.Default.PlayArrow,
    iconEnd: ImageVector = Icons.Default.Stop,
    isPressed: State<Boolean> = mutableStateOf(false),
    onClick: () -> Unit,
) {
    Column {
        Button(
            modifier = modifier,
            onClick = onClick
        ) {
            AnimatedContent(targetState = isPressed.value,
                transitionSpec = {
                    if (targetState) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "") {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    Icon(
                        imageVector = if (it) iconEnd else iconStart,
                        contentDescription = ""
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (it) textEnd else textStart)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

        }
    }
}