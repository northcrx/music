package com.nishant.northmusic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nishant.northmusic.ui.theme.PillShape

/**
 * Shared consistent AppDialog:
 * - 28dp corner radius
 * - container color surfaceContainerHigh
 * - 24dp internal padding
 * - title in titleLarge (bold, onSurface)
 * - optional subtitle in bodyMedium muted (onSurfaceVariant)
 * - buttons at the bottom right: TextButton for Cancel and FilledButton (PillShape) for main action
 * - No outlined or square buttons
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    confirmButtonText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = onDismissRequest,
    isConfirmLoading: Boolean = false,
    isConfirmEnabled: Boolean = true,
    confirmButtonColors: ButtonColors? = null,
    buttonsContent: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header (Icon + Title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Content body
                content()

                // Bottom right action buttons
                if (buttonsContent != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = buttonsContent
                    )
                } else if (confirmButtonText != null || dismissButtonText != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButtonText != null && onDismiss != null) {
                            TextButton(
                                onClick = onDismiss,
                                shape = PillShape
                            ) {
                                Text(dismissButtonText)
                            }
                        }
                        if (confirmButtonText != null && onConfirm != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onConfirm,
                                enabled = isConfirmEnabled && !isConfirmLoading,
                                shape = PillShape,
                                colors = confirmButtonColors ?: ButtonDefaults.buttonColors()
                            ) {
                                if (isConfirmLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(confirmButtonText)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared consistent AppBottomSheet:
 * - 28dp top corners
 * - container color surfaceContainerHigh
 * - 24dp horizontal padding
 * - title in titleLarge (bold, onSurface)
 * - optional subtitle in bodyMedium muted (onSurfaceVariant)
 * - buttons at the bottom right: TextButton for Cancel and FilledButton (PillShape) for main action
 * - No outlined or square buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    confirmButtonText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = onDismissRequest,
    isConfirmLoading: Boolean = false,
    isConfirmEnabled: Boolean = true,
    confirmButtonColors: ButtonColors? = null,
    buttonsContent: (@Composable RowScope.() -> Unit)? = null,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = dragHandle,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            content()

            if (buttonsContent != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = buttonsContent
                )
            } else if (confirmButtonText != null || dismissButtonText != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButtonText != null && onDismiss != null) {
                        TextButton(
                            onClick = onDismiss,
                            shape = PillShape
                        ) {
                            Text(dismissButtonText)
                        }
                    }
                    if (confirmButtonText != null && onConfirm != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            enabled = isConfirmEnabled && !isConfirmLoading,
                            shape = PillShape,
                            colors = confirmButtonColors ?: ButtonDefaults.buttonColors()
                        ) {
                            if (isConfirmLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(confirmButtonText)
                        }
                    }
                }
            }
        }
    }
}
